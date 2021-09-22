import sbt.Compile
import sbt.Keys.baseDirectory

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
  .settings(moduleName := "itemstack-count-infrastructure-core")
  .settings()

lazy val infra_redis = project
  .dependsOn(core)
  .in(file("infra-redis"))
  .settings(moduleName := "itemstack-count-infrastructure-infra-redis")
  .settings(libraryDependencies ++= Seq("dev.profunktor" %% "redis4cats-effects" % "1.0.0"))

// region token replacement settings keys

val tokenReplacementMap =
  settingKey[Map[String, String]]("Map specifying what tokens should be replaced to")

val filteredResourceGenerator = taskKey[Seq[File]]("Resource generator to filter resources")

// endregion

val filesToBeReplacedInResourceFolder = Seq("plugin.yml")

lazy val bukkit = {
  project
    .dependsOn(core, infra_redis)
    .in(file("bukkit"))
    .settings(moduleName := "itemstack-count-infrastructure-bukkit")
    .settings(
      resolvers ++= Seq(
        "hub.spigotmc.org" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots"
      ),
      assembly / assemblyOutputPath := baseDirectory.value / "target" / "build" / s"itemstack-count_${version.value}.jar",
      libraryDependencies ++= Seq(
        // logging infrastructure
        "org.slf4j" % "slf4j-jdk14" % "1.7.28",
        "org.typelevel" %% "log4cats-slf4j" % "2.1.1",
        "dev.profunktor" %% "redis4cats-log4cats" % "1.0.0",

        // spigot dependency
        "org.spigotmc" % "spigot-api" % "1.17.1-R0.1-SNAPSHOT" % Provided
      ),
      Compile / filteredResourceGenerator :=
        ResourceFilter.filterResources(
          filesToBeReplacedInResourceFolder,
          Map("name" -> moduleName.value, "version" -> version.value),
          (Compile / resourceManaged).value,
          (Compile / resourceDirectory).value
        ),
      Compile / resourceGenerators += (Compile / filteredResourceGenerator),
      Compile / unmanagedResources += baseDirectory.value / "LICENSE.txt",
      // トークン置換を行ったファイルをunmanagedResourcesのコピーから除外する
      Compile / unmanagedResources / excludeFilter :=
        filesToBeReplacedInResourceFolder
          .foldLeft((unmanagedResources / excludeFilter).value)(_ || _),
      assembly / assemblyMergeStrategy ~= (old => {
        case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
        // redis4cats -> lettuce introduces netty dependencies, we concat version properties
        case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
        case x                                                    => old(x)
      })
    )
}
