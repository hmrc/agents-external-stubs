import sbt.*

object AppDependencies {

  private val bootstrapVer = "10.1.0"
  private val mongoVer = "2.7.0"
  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% "bootstrap-backend-play-30" % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-play-30"        % mongoVer,
    "uk.gov.hmrc"          %% "agent-mtd-identifiers"     % "2.2.0",
    "com.github.blemale"   %% "scaffeine"                 % "5.3.0",
    "org.typelevel"        %% "cats-core"                 % "2.13.0",
    "uk.gov.hmrc"          %% "stub-data-generator"       % "1.4.0", // cannot update without moving to Scala 3
    "io.github.wolfendale" %% "scalacheck-gen-regexp"     % "0.1.3",
    "org.playframework" %% "play-json" % "3.0.4"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapVer % "test, it",
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30" % mongoVer     % "test, it",
    "org.scalamock"        %% "scalamock"               % "7.3.2"      % "test, it",
    "org.scalatestplus"    %% "mockito-3-12"            % "3.2.10.0"   % "test, it",
    "com.github.pathikrit" %% "better-files"            % "3.9.2"      % "test, it"
  )
}
