resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("org.playframework" % "sbt-plugin"            % "3.0.9")
addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "3.24.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.6.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "2.3.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"          % "2.5.2")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.6.4")

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always