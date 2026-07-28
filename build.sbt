import sbt.*
import sbt.Tags
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.7.4"
Global / concurrentRestrictions += Tags.limitAll(1)

lazy val commonProjectSettings = Seq(
  scalacOptions ++= Seq(
    "-feature",
    "-Werror",
    "-language:implicitConversions"
  ),
  Compile / scalafmtOnCompile := true,
  Test / scalafmtOnCompile := true,
  Test / logBuffered := false,
  Compile / scalacOptions ~= (_.distinct),
  Test / scalacOptions ~= (_.distinct)
)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 60.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs",
    organization := "uk.gov.hmrc",
    scalacOptions ++= Seq(
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=routes/.*:s", // silence warnings from routes files
      "-Wconf:src=.*html:w", // silence html warnings as they are wrong
    ),
    PlayKeys.playDefaultPort := 9009,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scoverageSettings,
    commonProjectSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    routesImport ++= Seq(
      "uk.gov.hmrc.agentsexternalstubs.binders.UrlBinders.{*, given}",
      "uk.gov.hmrc.agentsexternalstubs.models._"
    ),
  )
  .settings(
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //To prevent https://github.com/scalatest/scalatest/issues/1427

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    commonProjectSettings,
    Test / parallelExecution := false
  )
