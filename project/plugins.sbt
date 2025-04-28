resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"        % "sbt-auto-build"      % "3.24.0")
addSbtPlugin("org.playframework"  % "sbt-plugin"          % "3.0.6")
addSbtPlugin("uk.gov.hmrc"        % "sbt-distributables"  % "2.5.0")
addSbtPlugin("org.scoverage"      % "sbt-scoverage"       % "2.0.12")
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"        % "2.5.0")
addSbtPlugin("org.scalastyle"       % "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))

//fix for scoverage compile errors for scala 2.13.10
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

//addSbtPlugin("org.playframework"    % "sbt-plugin"            % "3.0.3")
//addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"        % "3.22.0") DONE
//addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"    % "2.5.0") DONE
//addSbtPlugin("org.scalameta"        % "sbt-scalafmt"          % "2.5.0") DONE
//addSbtPlugin("org.scalastyle"       % "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
//addSbtPlugin("org.scoverage"        % "sbt-scoverage"         % "2.0.12") DONE
//addSbtPlugin("io.github.irundaia"   % "sbt-sassify"           % "1.5.2") DONE