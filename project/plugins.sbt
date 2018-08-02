resolvers ++= Seq(
  Resolver.url("hmrc-sbt-plugin-releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(
    Resolver.ivyStylePatterns),
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.13.0")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.12")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.1.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "0.9.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.15")