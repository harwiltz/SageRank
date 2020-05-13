name := """sagerank-server"""
organization := "io.github.harwiltz"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

resolvers += Resolver.mavenLocal

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "io.github.harwiltz" % "sagerank" % "1.0-SNAPSHOT"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.github.harwiltz.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.github.harwiltz.binders._"
