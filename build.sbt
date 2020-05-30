import Versions._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github"
ThisBuild / organizationName := "kczulko"

val scalatestplusScalaCheckVersion = "3.1.2.0"
val disciplineScalatestVersion = "1.0.1"

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.1.2",
  "org.typelevel" %% "cats-laws" % "2.1.1",
  // "org.scalacheck" %% "scalacheck" % "1.14.1",
  "org.scalatestplus" %% "scalacheck-1-14" % scalatestplusScalaCheckVersion,
  "org.typelevel" %% "discipline-scalatest" % disciplineScalatestVersion,
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % "1.2.3",
    "org.typelevel" %% "cats-testkit-scalatest" % "1.0.1"
)

val deps = Seq(
  "co.fs2" %% "fs2-core" % Versions.fs2,
  "co.fs2" %% "fs2-io" % Versions.fs2,
  "org.typelevel" %% "cats-effect" % Versions.catsEffect,
  "org.typelevel" %% "kittens" % Versions.kittens
)

lazy val root = (project in file("."))
  .settings(
    name := "sensor-statistics-task",
    libraryDependencies ++= (deps ++ testDeps),
  )
