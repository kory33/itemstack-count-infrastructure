ThisBuild / name := "itemstack-count-infrastructure"
ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "3.0.2"

ThisBuild / semanticdbEnabled := true

ThisBuild / libraryDependencies ++= Seq(
  // effect libraries
  "org.typelevel" %% "cats-effect" % "3.2.2",

  // test libraries
  "org.scalactic" %% "scalactic" % "3.2.9",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.5.0"

lazy val core = project
  .in(file("core"))
  .settings()

lazy val infra_redis = project
  .dependsOn(core)
  .in(file("infra-redis"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.profunktor" %% "redis4cats-effects" % "1.0.0"
    )
  )

lazy val bukkit = project
  .dependsOn(core, infra_redis)
  .in(file("bukkit"))
  .settings(
    resolvers ++= Seq(
      "hub.spigotmc.org" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots"
    ),
    libraryDependencies ++= Seq(
      // logging infrastructure
      "org.slf4j" % "slf4j-jdk14" % "1.7.28",
      "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
      "dev.profunktor" %% "redis4cats-log4cats" % "1.0.0",

      // spigot dependency
      "org.spigotmc" % "spigot-api" % "1.17.1-R0.1-SNAPSHOT" % Provided
    )
  )
