name := """web-amandroid"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "commons-io" % "commons-io" % "2.4",
  "org.apache.commons" % "commons-email" % "1.4",
  "com.typesafe.play" %% "play-mailer" % "5.0.0-M1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0-RC1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
