import sbt.Keys.libraryDependencies

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

lazy val root = (project in file("."))
  .settings(
    name := "hack9-reset-lambda",
    version := "1.0",
    scalaVersion := "2.12.9",
    retrieveManaged := true,
    libraryDependencies ++= Seq(
      "io.github.mkotsur" %% "aws-lambda-scala" % "0.1.1",
      "org.scanamo" %% "scanamo" % "1.0.0-M10",
      "org.scala-lang.modules" %% "scala-java8-compat" %"0.9.0",
      "com.spikhalskiy.futurity" % "futurity-core" % "0.3-RC3"
    )
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
