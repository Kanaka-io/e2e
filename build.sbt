organization in ThisBuild := "io.kanaka"

version in ThisBuild := "0.1-SNAPSHOT"


lazy val core = project.in(file("core")).settings(
    name := "e2e-core",
    scalaVersion := "2.11.7",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
)

lazy val plugin = project.in(file("plugin")).settings(
  name := "e2e-plugin",
  scalaVersion := "2.10.4",
  sbtPlugin := true
)

lazy val root = project.in(file(".")).aggregate(core, plugin)
