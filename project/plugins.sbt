addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.31")

// sbt fails to resolve spigot api without this
addSbtPlugin("org.scala-sbt" % "sbt-maven-resolver" % "0.1.0")
