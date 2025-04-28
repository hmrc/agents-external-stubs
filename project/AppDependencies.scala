import sbt.*

object AppDependencies {

  private val bootstrapVer = "9.11.0"
  private val mongoVer = "1.9.0"
  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-30"        % mongoVer,
    "uk.gov.hmrc"          %% "agent-mtd-identifiers"     % "1.15.0",
    "com.kenshoo"          %% "metrics-play"              % "2.7.3_0.8.2",
    "com.github.blemale"   %% "scaffeine"                 % "5.2.1",
    "org.typelevel"        %% "cats-core"                 % "2.6.1",
    "uk.gov.hmrc"          %% "stub-data-generator"       % "1.1.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"     % "0.1.3",
   // "com.typesafe.play"    %% "play-json"                 % "2.9.2"
    "org.playframework"    %% "play-json"                 % "3.0.4"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"    % bootstrapVer % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30"   % mongoVer     % "test, it",
    "org.scalamock"          %% "scalamock"                 % "5.2.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-12"              % "3.2.10.0"   % "test, it",
    "com.github.pathikrit"   %% "better-files"              % "3.9.1"      % "test, it"
  )

}
