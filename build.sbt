name := "cats-effect-study"

version := "1.0"

scalaVersion := "2.13.4"

val catsEffectVersion = "2.3.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-effect" % catsEffectVersion withSources () withJavadoc (),
  "org.typelevel" %% "cats-effect-laws" % catsEffectVersion withSources () withJavadoc ()
)

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds"
)
