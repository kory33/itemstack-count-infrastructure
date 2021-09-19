ThisBuild / name := "itemstack-count-infrastructure"
ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.0.2"

ThisBuild / libraryDependencies ++= Seq(
  // effect libraries
  "org.typelevel" %% "cats-effect" % "3.2.2",
  "co.fs2" %% "fs2-core" % "3.1.0",
  "co.fs2" %% "fs2-io" % "3.1.0",

  // our protocol implementation will use typenbt as a foundation to deal with NBTs
  ("net.katsstuff" %% "typenbt" % "0.5.1").cross(CrossVersion.for3Use2_13),

  // to make datatype-generic programming easier
  "org.typelevel" %% "shapeless3-deriving" % "3.0.2",

  // test libraries
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test",
)

lazy val core = project
  .in(file("core"))
  .settings()

lazy val bukkit = project
  .dependsOn(core)
  .in(file("bukkit"))
  .settings()

