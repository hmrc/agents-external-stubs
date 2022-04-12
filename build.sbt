import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

ThisBuild / libraryDependencySchemes += "org.typelevel" %% "cats-core" % "always"

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

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"          %% "bootstrap-backend-play-28" % "5.21.0",
  "uk.gov.hmrc"          %% "simple-reactivemongo"      % "8.0.0-play-28",
  "uk.gov.hmrc"          %% "agent-mtd-identifiers"     % "0.35.0-play-28",
  "com.kenshoo"          %% "metrics-play"              % "2.7.3_0.8.2",
  "uk.gov.hmrc"          %% "domain"                    % "7.0.0-play-28",
  "com.github.blemale"   %% "scaffeine"                 % "4.0.1",
  "org.typelevel"        %% "cats-core"                 % "2.6.1",
  "uk.gov.hmrc"          %% "stub-data-generator"       % "0.5.3",
  "io.github.wolfendale" %% "scalacheck-gen-regexp"     % "0.1.3",
  "com.typesafe.play"    %% "play-json"                 % "2.9.2",
  "com.typesafe.play"    %% "play-json-joda"            % "2.9.2",
  ws
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"         % scope,
  "org.scalatestplus"      %% "mockito-3-12"       % "3.2.10.0"      % scope,
  "uk.gov.hmrc"            %% "reactivemongo-test" % "5.0.0-play-28" % scope,
  "com.github.tomakehurst"  % "wiremock-jre8"      % "2.26.1"        % scope,
  "com.github.pathikrit"   %% "better-files"       % "3.9.1"         % scope,
  "com.vladsch.flexmark"    % "flexmark-all"       % "0.35.10"       % scope
)

val jettyVersion = "9.2.24.v20180105"

val jettyOverrides = Set(
  "org.eclipse.jetty"           % "jetty-server"       % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-servlet"      % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-security"     % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-servlets"     % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-continuation" % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-webapp"       % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-xml"          % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-client"       % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-http"         % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-io"           % jettyVersion % IntegrationTest,
  "org.eclipse.jetty"           % "jetty-util"         % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-api"      % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-common"   % jettyVersion % IntegrationTest,
  "org.eclipse.jetty.websocket" % "websocket-client"   % jettyVersion % IntegrationTest
)

def tmpMacWorkaround(): Seq[ModuleID] =
  if (sys.props.get("os.name").fold(false)(_.toLowerCase.contains("mac")))
    Seq("org.reactivemongo" % "reactivemongo-shaded-native" % "0.16.1-osx-x86-64" % "runtime,test,it")
  else Seq()

lazy val root = (project in file("."))
  .settings(
    name := "agents-external-stubs",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
    majorVersion := 0,
    scalacOptions ++= Seq(
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    PlayKeys.playDefaultPort := 9009,
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
    ),
    libraryDependencies ++= tmpMacWorkaround() ++ compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.7" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.7" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    Compile / unmanagedResourceDirectories  += baseDirectory.value / "resources",
    routesImport ++= Seq(
      "uk.gov.hmrc.agentsexternalstubs.binders.UrlBinders._",
      "uk.gov.hmrc.agentsexternalstubs.models._"
    ),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(
    IntegrationTest / Keys.fork := true,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / scalafmtOnCompile := true
  )
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings)
