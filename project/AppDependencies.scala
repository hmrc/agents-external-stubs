import sbt.*

object AppDependencies {

  private val bootstrapVer = "10.5.0"

  private val mongoVer = "2.11.0"

  private val playVer = "play-30"

  lazy val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-backend-$playVer" % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-$playVer"        % mongoVer,
    "uk.gov.hmrc"          %% "domain-play-30"              % "11.0.0",
    "com.github.blemale"   %% "scaffeine"                   % "5.3.0",
    "org.typelevel"        %% "cats-core"                   % "2.13.0",
    // NOTE: 1.6.0 exists in some environments but is not resolvable from this local build;
    // we keep 1.4.0 here to preserve a working compile while still depending on the library at runtime.
    "uk.gov.hmrc"          %% "stub-data-generator"         % "1.4.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"       % "0.1.3",
    "org.playframework"    %% "play-json"                   % "3.0.5"
  )

  lazy val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% s"bootstrap-test-$playVer"  % bootstrapVer,
    "uk.gov.hmrc.mongo"    %% s"hmrc-mongo-test-$playVer" % mongoVer,
    "com.github.pathikrit" %% "better-files"              % "3.9.2"
  ).map(_ % Test)
}
