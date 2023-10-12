import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer = "7.21.0"
  private val mongoVer = "1.3.0"
  
  lazy val compile = Seq(
    ws,
    "uk.gov.hmrc"          %% "bootstrap-backend-play-28" % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-28"        % mongoVer,
    "uk.gov.hmrc"          %% "agent-mtd-identifiers"     % "1.14.0",
    "com.kenshoo"          %% "metrics-play"              % "2.7.3_0.8.2",
    "com.github.blemale"   %% "scaffeine"                 % "4.0.1",
    "org.typelevel"        %% "cats-core"                 % "2.6.1",
    "uk.gov.hmrc"          %% "stub-data-generator"       % "1.0.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"     % "0.1.3",
    "com.typesafe.play"    %% "play-json"                 % "2.9.2"
  )

  lazy val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"        % "5.1.0"      % "test, it",
    "org.scalamock"          %% "scalamock"                 % "5.2.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-12"              % "3.2.10.0"   % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"   % mongoVer     % "test, it",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"    % bootstrapVer % "test, it",
    "com.github.tomakehurst"  % "wiremock-jre8"             % "2.26.1"     % "test, it",
    "com.github.pathikrit"   %% "better-files"              % "3.9.1"      % "test, it",
    "com.vladsch.flexmark"    % "flexmark-all"              % "0.35.10"    % "test, it"
  )

}
