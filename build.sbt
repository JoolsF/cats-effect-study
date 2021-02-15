name := "cats-effect-study"

version := "1.0"

scalaVersion := "2.13.4"

libraryDependencies += "org.typelevel" %% "cats-effect" % "2.3.1" withSources() withJavadoc()

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds")
