package io.kamon.sbt.umbrella

import bintray.BintrayPlugin
import com.lightbend.sbt.SbtAspectj._
import com.lightbend.sbt.SbtAspectj.autoImport._
import sbt.Keys._
import sbt._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._

import scala.sys.process.Process

object KamonSbtUmbrella extends AutoPlugin {

  override def requires: Plugins      = BintrayPlugin
  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] = Seq(
    scalaVersion := scalaVersionSetting.value,
    crossScalaVersions := crossScalaVersionsSetting.value,
    version := versionSetting.value,
    isSnapshot := isSnapshotVersion(version.value),
    organization := "io.kamon",
    releaseCrossBuild := false,
    releaseProcess := kamonReleaseProcess.value,
    releaseSnapshotDependencies := releaseSnapshotDependenciesTask.value,
    releaseCommitMessage := releaseCommitMessageSetting.value,
    licenses += (("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
    scalacOptions := Seq(
      "-encoding",
      "utf8",
      "-g:vars",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Xlog-reflective-calls",
      "-Ywarn-dead-code"
    ),
    javacOptions := Seq(
      "-Xlint:-options"
    ),
    crossPaths := true,
    pomIncludeRepository := { x =>
      false
    },
    publishArtifact in Test := false,
    publishMavenStyle := publishMavenStyleSetting.value,
    resolvers += Resolver.bintrayRepo("kamon-io", "releases")
  )

  object autoImport {
    val aspectJ        = "org.aspectj"      % "aspectjweaver"   % "1.9.1"
    val hdrHistogram   = "org.hdrhistogram" % "HdrHistogram"    % "2.1.10"
    val slf4jApi       = "org.slf4j"        % "slf4j-api"       % "1.7.25"
    val slf4jnop       = "org.slf4j"        % "slf4j-nop"       % "1.7.25"
    val logbackClassic = "ch.qos.logback"   % "logback-classic" % "1.2.3"
    val scalatest      = "org.scalatest"    %% "scalatest"      % "3.0.5"

    def akkaDependency(moduleName: String) = Def.setting {
      "com.typesafe.akka" %% s"akka-$moduleName" % "2.5.14"
    }

    def compileScope(deps: ModuleID*): Seq[ModuleID]  = deps map (_ % "compile")
    def testScope(deps: ModuleID*): Seq[ModuleID]     = deps map (_ % "test")
    def providedScope(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
    def optionalScope(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile,optional")

    val noPublishing = Seq(publish := ((): Unit), publishLocal := ((): Unit), publishArtifact := false)

    val aspectJSettings = inConfig(Aspectj)(defaultAspectjSettings) ++ aspectjDependencySettings ++ Seq(
      aspectjVersion in Aspectj := "1.9.1",
      fork in run := true,
      fork in Test := true,
      javaOptions in Test ++= (aspectjWeaverOptions in Aspectj).value,
      javaOptions in run ++= (aspectjWeaverOptions in Aspectj).value,
      aspectjLintProperties in Aspectj += "invalidAbsoluteTypeName = ignore"
    )
  }

  private def scalaVersionSetting = Def.setting {
    if (sbtPlugin.value) scalaVersion.value else "2.12.6"
  }

  private def crossScalaVersionsSetting = Def.setting {
    if (sbtPlugin.value) Seq(scalaVersion.value) else Seq("2.11.12", "2.12.6")
  }

  private def versionSetting = Def.setting {
    val originalVersion = (version in ThisBuild).value
    if (isSnapshotVersion(originalVersion)) {
      val gitRevision = Process("git rev-parse HEAD").lineStream.head
      originalVersion.replace("SNAPSHOT", gitRevision)
    } else {
      originalVersion
    }
  }

  private def releaseSnapshotDependenciesTask = Def.task {
    val moduleIds = (managedClasspath in Runtime).value.flatMap(_.get(moduleID.key))
    val snapshots = moduleIds.filter(m => m.isChanging || isSnapshotVersion(m.revision))
    snapshots
  }

  private def releaseCommitMessageSetting = Def.setting {
    val currentVersion = if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value
    if (isSnapshotVersion(currentVersion))
      s"set version to $currentVersion"
    else
      s"release version $currentVersion"
  }

  private def publishMavenStyleSetting = Def.setting {
    if (sbtPlugin.value) false else publishMavenStyle.value
  }

  private def isSnapshotVersion(version: String): Boolean = {
    (version matches """(?:\d+\.)?(?:\d+\.)?(?:\d+)(?:-[A-Z0-9]*)?-[0-9a-f]{5,40}""") || (version endsWith "-SNAPSHOT")
  }

  private def aspectjDependencySettings = Seq(
    ivyConfigurations += Aspectj,
    libraryDependencies ++= (aspectjVersion in Aspectj) { version =>
      Seq(
        "org.aspectj" % "aspectjtools"  % version % Aspectj.name,
        "org.aspectj" % "aspectjweaver" % version % Aspectj.name,
        "org.aspectj" % "aspectjrt"     % version % Aspectj.name
      )
    }.value
  )

  private def kamonReleaseProcess = Def.setting {
    val publishStep =
      if (sbtPlugin.value) releaseStepCommandAndRemaining("publish")
      else releaseStepCommandAndRemaining("+publish")

    Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishStep,
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  }
}
