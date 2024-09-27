resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "3.22.0")
addSbtPlugin("com.typesafe.play"  % "sbt-plugin"          % "2.8.22")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"  % "2.4.0")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"       % "2.0.7")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"        % "2.4.0")

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
