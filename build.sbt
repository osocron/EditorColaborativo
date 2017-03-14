name := "EditorColaborativo"

version := "1.0"

scalaVersion := "2.12.1"

assemblyJarName := "EditorColaborativo.jar"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
)

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
