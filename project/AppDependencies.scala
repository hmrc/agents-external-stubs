import sbt.*

object AppDependencies {

  private val playVer = "play-30"

  private val bootstrapVer = "10.5.0"

  private val mongoVer = "2.12.0"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVer" % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVer"        % mongoVer,
    "uk.gov.hmrc"          %% s"domain-$playVer"            % "11.0.0",
    "uk.gov.hmrc"          %% "stub-data-generator"         % "1.4.0", // cannot update without moving to Scala 3
    "org.playframework"    %% "play-json"                   % "3.0.6",
    "org.typelevel"        %% "cats-core"                   % "2.13.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"       % "0.1.3",
    "com.github.blemale"   %% "scaffeine"                   % "5.3.0"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVer"  % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVer" % mongoVer,
    "com.github.pathikrit" %% "better-files"              % "3.9.2"
  ).map(_ % Test)
}
