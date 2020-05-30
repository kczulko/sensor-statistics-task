import Versions._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.github"
ThisBuild / organizationName := "kczulko"

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % scalatest,
  "org.scalatestplus" %% "scalacheck-1-14" % scalatestplusScalaCheck,
  "com.github.alexarchambault" %% "scalacheck-shapeless_1.14" % scalacheckShapeless,
  "org.typelevel" %% "cats-testkit-scalatest" % catsTestkitScalatest
).map(_ % Test)

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
