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
  sbtPlugin := true,
  libraryDependencies ++= Seq(
    "org.specs2" %% "specs2-core" % "3.6.6" % "test"
  ),
  scalacOptions in Test ++= Seq("-Yrangepos")
)

lazy val root = project.in(file(".")).aggregate(core, plugin)
