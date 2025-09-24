import sbt.*
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / scalaVersion := "2.13.16"

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs",
    organization := "uk.gov.hmrc",
    majorVersion := 0,
    scalacOptions ++= Seq(
      "-Xlint:-missing-interpolator,_",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Wconf:src=target/.*:s", // silence warnings from compiled files
      "-Wconf:src=routes/.*:s", // silence warnings from routes files
      "-Wconf:src=*html:w", // silence html warnings as they are wrong
      "-language:implicitConversions"
    ),
    PlayKeys.playDefaultPort := 9009,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scoverageSettings,
    Compile / unmanagedResourceDirectories  += baseDirectory.value / "resources",
    routesImport ++= Seq(
      "uk.gov.hmrc.agentsexternalstubs.binders.UrlBinders._",
      "uk.gov.hmrc.agentsexternalstubs.models._"
    ),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .settings(
    //fix for scoverage compile errors for scala 2.13.10
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)  //To prevent https://github.com/scalatest/scalatest/issues/1427

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(majorVersion := 0)
  .settings(libraryDependencies ++= AppDependencies.test)
  .settings(
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
