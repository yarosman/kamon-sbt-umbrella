sbtPlugin := true

organization := "io.kamon"
name := "kamon-sbt-umbrella"
bintrayVcsUrl := Some("https://github.com/yarosman/kamon-sbt-umbrella")

addSbtPlugin("com.lightbend.sbt" % "sbt-aspectj" % "0.11.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")