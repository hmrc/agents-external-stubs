/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.tools

import play.api.libs.json.{JsValue, Json}

import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption.CREATE_NEW
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.{Files, Path, Paths}
import scala.jdk.CollectionConverters.*
import scala.util.Try

object EtProfileTestTool:

  private val MainClass = "uk.gov.hmrc.agentsexternalstubs.tools.EtProfileTestTool"
  private val RunPrefix = s"sbt \"Test / runMain $MainClass"

  private val ApiBase = "https://test-api.service.hmrc.gov.uk"
  private val TokenUrl = s"$ApiBase/oauth/token"
  private val ServicesUrl = s"$ApiBase/create-test-user/services"
  private val AgentUrl = s"$ApiBase/create-test-user/agents"
  private val OrganisationUrl = s"$ApiBase/create-test-user/organisations"

  private val MinimumRequestIntervalMillis = 750L
  private val ThrottleRetryDelaysMillis = Seq(2000L, 4000L, 8000L)

  private val http = HttpClient.newBuilder().followRedirects(Redirect.NEVER).build()
  private var lastRequestAtNanos = 0L

  final case class Config(clientId: String, clientSecret: String, redirectUri: String)

  final case class Workspace(workDir: Path):
    val configPath: Path = workDir.resolve("operator.env")
    val rawDir: Path = workDir.resolve("raw")

  final case class V3MatrixResult(
    slug: String,
    label: String,
    createStatus: Int,
    invitationPrefix: Option[String],
    listStatus: Option[Int],
    listed: Option[Boolean],
    getStatus: Option[Int],
    cancelStatus: Option[Int],
    finalGetStatus: Option[Int],
    finalInvitationStatus: Option[String]
  ):
    val passed: Boolean =
      createStatus == 201 && listStatus.contains(200) && listed.contains(true) && getStatus.contains(200) &&
        cancelStatus.contains(204) && finalGetStatus.contains(200) && finalInvitationStatus.contains("Cancelled")

    def sanitisedJson: JsValue = Json.obj(
      "scenario"              -> slug,
      "label"                 -> label,
      "createHttpStatus"      -> createStatus,
      "invitationPrefix"      -> invitationPrefix,
      "listHttpStatus"        -> listStatus,
      "listed"                -> listed,
      "getHttpStatus"         -> getStatus,
      "cancelHttpStatus"      -> cancelStatus,
      "finalGetHttpStatus"    -> finalGetStatus,
      "finalInvitationStatus" -> finalInvitationStatus,
      "result"                -> (if passed then "PASS" else "FAIL")
    )

  def main(args: Array[String]): Unit =
    try
      args.toSeq match
        case Seq() | Seq("help" | "-h" | "--help") => usage()
        case Seq("credentials-help")               => credentialsHelp()
        case command +: arguments                  => runStateful(command, arguments)
    catch
      case error: ToolError => fail(error.getMessage)
      case error: Exception => fail(Option(error.getMessage).getOrElse(error.getClass.getSimpleName))

  private def runStateful(command: String, arguments: Seq[String]): Unit =
    val (positionals, workspace) = parseArguments(command, arguments)
    printWorkspace(workspace)
    (command, positionals) match
      case ("init-config", Seq())              => initConfig(workspace)
      case ("check-config", Seq())             => checkConfig(workspace)
      case ("preflight", Seq())                => preflight(workspace)
      case ("ui-org-help", Seq())              => uiOrganisationHelp(workspace)
      case ("capture-ui-html", Seq())          => captureUiOrganisationHtml(workspace)
      case ("validate-org", Seq(source))       => validateOrganisation(workspace, source)
      case ("create-agent", Seq())             => createAgent(workspace)
      case ("create-api-org", Seq())           => createApiOrganisation(workspace)
      case ("oauth-url", Seq())                => printOauthUrl(workspace)
      case ("exchange-code", Seq())            => exchangeCode(workspace)
      case ("refresh-token", Seq())            => refreshAgentToken(workspace)
      case ("v2-invite", Seq(source, trigger)) => createV2Invitation(workspace, source, trigger)
      case ("v3-smoke", Seq(source))           => v3Smoke(workspace, source)
      case ("mongo-help", Seq(source))         => mongoHelp(workspace, source)
      case ("capture-mongo", Seq(source))      => captureMongoEvidence(workspace, source)
      case ("v3-matrix", Seq(source))          => v3Matrix(workspace, source)
      case ("v3-disabled-check", Seq(source))  => v3DisabledCheck(workspace, source)
      case _ => throw ToolError(s"Invalid arguments for '$command'.\n\n${commandUsage(command)}")

  private def parseArguments(command: String, arguments: Seq[String]): (Seq[String], Workspace) =
    val workDirIndexes = arguments.zipWithIndex.collect { case ("--work-dir", index) => index }
    ensure(workDirIndexes.size == 1, missingOrDuplicateWorkDir(command, workDirIndexes.size))
    val optionIndex = workDirIndexes.head
    ensure(optionIndex + 1 < arguments.size, s"--work-dir requires a directory.\n\n${commandUsage(command)}")
    val workDirValue = arguments(optionIndex + 1)
    ensure(workDirValue.nonEmpty && workDirValue != "--work-dir", "--work-dir requires a non-empty directory.")
    val positionals = arguments.patch(optionIndex, Nil, 2)
    ensure(!positionals.exists(_.startsWith("--")), s"Unknown option for '$command'.\n\n${commandUsage(command)}")
    val resolved = Paths.get(workDirValue).toAbsolutePath.normalize()
    (positionals, Workspace(resolved))

  private def initConfig(workspace: Workspace): Unit =
    Files.createDirectories(workspace.workDir)
    Files.createDirectories(workspace.rawDir)
    setPermissions(workspace.workDir, "rwx------")
    setPermissions(workspace.rawDir, "rwx------")
    ensure(
      !Files.exists(workspace.configPath),
      s"Configuration already exists and was not changed:\n  ${workspace.configPath}"
    )
    Files.writeString(workspace.configPath, configTemplate, CREATE_NEW)
    setPermissions(workspace.configPath, "rw-------")
    println(
      s"""Configuration template created:
        |  ${workspace.configPath}
        |
        |Next [MANUAL + CLI]:
        |  1. Add the credentials obtained from Developer Hub to operator.env.
        |  2. If they have not been obtained yet, run '$RunPrefix credentials-help\"'.
        |  3. Validate the completed configuration with:
        |     ${command(workspace, "check-config")}
        |
        |Do not commit, paste or share operator.env.
        |""".stripMargin
    )

  private def checkConfig(workspace: Workspace): Unit =
    prepareRawDirectory(workspace)
    validatedAppToken(workspace)
    println("Configuration and Developer Hub credentials: PASS")
    printNext("CLI AUTOMATED", "Confirm that ET advertises every required profile:", command(workspace, "preflight"))

  private def preflight(workspace: Workspace): Unit =
    prepareRawDirectory(workspace)
    val token = validatedAppToken(workspace)
    val response = get(ServicesUrl, token, "application/vnd.hmrc.1.0+json")
    saveResponse(workspace, "services.json", response)
    if response.statusCode() == 403 then
      throw ToolError(
        "Create Test User services returned HTTP 403. Confirm the sandbox application is subscribed to Create Test User API 1.0."
      )
    requireStatus(workspace, response, 200, "Create Test User services", "services.json")
    printChecks(EtProfileTestSupport.validateServices(response.body()))
    println(s"Preflight complete. Private responses are under ${workspace.rawDir}.")
    printNext(
      "BROWSER REQUIRED",
      "Create the first organisation through the Developer Hub UI. Display the exact instructions with:",
      command(workspace, "ui-org-help")
    )

  private def uiOrganisationHelp(workspace: Workspace): Unit =
    val uiHtml = workspace.rawDir.resolve("ui-organisation.html")
    println(
      s"""Developer Hub UI organisation — required APB-12584 scenario
        |
        |[BROWSER REQUIRED]
        |This scenario must use the Developer Hub Create Test User page. The later
        |create-api-org command is a separate scenario and does not replace this UI test.
        |
        |  1. Open https://developer.service.hmrc.gov.uk/api-test-user
        |  2. Sign in to Developer Hub if prompted.
        |  3. Select Organisation and click Create. There is no profile selector.
        |  4. Keep the generated result page open until its HTML has been captured.
        |
        |The Developer Hub UI automatically requests every service available for an
        |organisation. The result must contain MTD-VAT, MTD-IT, CGT, PPT, Pillar 2, CBC
        |and both Trusts identifiers. The later create-api-org command separately tests explicit
        |profile selection through Create Test User API 1.0.
        |
        |[LOCAL FILE REQUIRED]
        |In the browser, use Save Page As and select "Webpage, HTML Only". Save it as:
        |  $uiHtml
        |
        |Protect the saved page:
        |  chmod 600 $uiHtml
        |
        |[CLI AUTOMATED]
        |Extract the credentials and identifiers into protected JSON, then validate them:
        |  ${command(workspace, "capture-ui-html")}
        |
        |The tool never prints extracted values. Never paste the generated password, HTML
        |or JSON into Git, Jira, screenshots or chat.
        |""".stripMargin
    )

  private def captureUiOrganisationHtml(workspace: Workspace): Unit =
    val htmlPath = workspace.rawDir.resolve("ui-organisation.html")
    val jsonPath = workspace.rawDir.resolve("ui-organisation.json")
    ensure(
      Files.isRegularFile(htmlPath),
      s"Saved Developer Hub page not found:\n  $htmlPath\n\nRun ${command(workspace, "ui-org-help")}"
    )
    ensure(!Files.exists(jsonPath), s"Refusing to overwrite existing UI organisation JSON:\n  $jsonPath")

    val organisation = EtProfileTestSupport.organisationFromDeveloperHubHtml(Files.readString(htmlPath))
    printChecks(EtProfileTestSupport.validateOrganisation(organisation))
    writePrivateNew(jsonPath, Json.prettyPrint(organisation))
    println(s"UI organisation captured and validated: PASS\nProtected JSON written to:\n  $jsonPath")
    printNext(
      "CLI AUTOMATED",
      "Create the test agent used for the V2 invitation:",
      command(workspace, "create-agent")
    )

  private def validateOrganisation(workspace: Workspace, source: String): Unit =
    printChecks(EtProfileTestSupport.validateOrganisation(readJson(organisationPath(workspace, source))))
    source match
      case "ui" =>
        println("UI organisation validation: PASS")
        printNext(
          "CLI AUTOMATED",
          "Create the test agent used for the V2 invitation:",
          command(workspace, "create-agent")
        )
      case "api" =>
        println("API organisation validation: PASS")
        printNext(
          "CLI AUTOMATED",
          "Trigger AES pull-through for the API-created organisation:",
          command(workspace, "v2-invite api mtd-vat")
        )
      case _ => ()

  private def createAgent(workspace: Workspace): Unit =
    prepareRawDirectory(workspace)
    val response = postJson(
      AgentUrl,
      validatedAppToken(workspace),
      "application/vnd.hmrc.1.0+json",
      Json.obj("serviceNames" -> Json.arr("agent-services"))
    )
    val agentPath = saveResponse(workspace, "agent.json", response)
    requireStatus(workspace, response, 201, "Create agent test user", "agent.json")
    val agent = parse(response.body())
    Seq("userId", "password", "agentServicesAccountNumber")
      .foreach(field => EtProfileTestSupport.requiredString(agent, Seq(field)))
    println("Create agent test user: PASS (HTTP 201)")
    println(s"Agent credentials and ARN saved privately to:\n  $agentPath")
    println("Sensitive values were not printed.")
    printNext(
      "BROWSER REQUIRED",
      "Prepare the agent OAuth authorisation URL and display the browser instructions:",
      command(workspace, "oauth-url")
    )

  private def createApiOrganisation(workspace: Workspace): Unit =
    prepareRawDirectory(workspace)
    val response = postJson(
      OrganisationUrl,
      validatedAppToken(workspace),
      "application/vnd.hmrc.1.0+json",
      Json.obj("serviceNames" -> EtProfileTestSupport.OrganisationServices)
    )
    val organisationPath = saveResponse(workspace, "api-organisation.json", response)
    requireStatus(workspace, response, 201, "Create organisation through API", "api-organisation.json")
    println("Create organisation through API: PASS (HTTP 201)")
    printChecks(EtProfileTestSupport.validateOrganisation(parse(response.body())))
    println("API organisation validation: PASS")
    println(s"API organisation saved privately to:\n  $organisationPath")
    println("Sensitive values were not printed.")
    printNext(
      "CLI AUTOMATED",
      "Trigger AES pull-through for the API-created organisation:",
      command(workspace, "v2-invite api mtd-vat")
    )

  private def printOauthUrl(workspace: Workspace): Unit =
    val currentConfig = config(workspace)
    validatedAppToken(workspace, currentConfig)
    println("[BROWSER REQUIRED]")
    println("Open this URL in a browser and sign in with the generated test agent:")
    println(EtProfileTestSupport.oauthAuthoriseUrl(currentConfig.clientId, currentConfig.redirectUri))
    println(
      s"""
        |  1. Open ${workspace.rawDir.resolve("agent.json")} in a local editor.
        |  2. Sign in with that test agent's userId and password.
        |  3. Approve access when prompted.
        |  4. From the redirect URL, copy only the value after code= and before &.
        |  5. Save the code as the only line in:
        |       ${workspace.rawDir.resolve("oauth-code.txt")}
        |  6. Protect the file with chmod 600, then run:
        |       ${command(workspace, "exchange-code")}
        |
        |The OAuth code is sensitive, short-lived and single-use. Do not paste it into
        |a command argument, Git, Jira, screenshots or chat.
        |""".stripMargin
    )

  private def exchangeCode(workspace: Workspace): Unit =
    prepareRawDirectory(workspace)
    val currentConfig = config(workspace)
    validatedAppToken(workspace, currentConfig)
    val codePath = workspace.rawDir.resolve("oauth-code.txt")
    ensure(Files.isRegularFile(codePath), s"Save the OAuth code as the only content in:\n  $codePath")
    val code = Files.readString(codePath).trim
    ensure(code.nonEmpty, s"OAuth code file is empty:\n  $codePath")
    val response = postForm(
      TokenUrl,
      Seq(
        "client_id"     -> currentConfig.clientId,
        "client_secret" -> currentConfig.clientSecret,
        "grant_type"    -> "authorization_code",
        "redirect_uri"  -> currentConfig.redirectUri,
        "code"          -> code
      )
    )
    val tokenPath = saveResponse(workspace, "agent-token.json", response)
    requireStatus(workspace, response, 200, "OAuth authorization_code exchange", "agent-token.json")
    val token = parse(response.body())
    EtProfileTestSupport.requiredString(token, Seq("access_token"))
    val scopes = EtProfileTestSupport.requiredString(token, Seq("scope")).split(" ").toSet
    val missing = EtProfileTestSupport.RequiredAgentScopes.toSet -- scopes
    ensure(missing.isEmpty, s"Token is missing scopes: ${missing.toSeq.sorted.mkString(", ")}")
    println("OAuth authorization_code exchange: PASS (HTTP 200)")
    println("Required V2 and V3 scopes: PASS")
    println(s"Agent OAuth token saved privately to:\n  $tokenPath")
    println("Sensitive values were not printed.")
    println(s"Delete $codePath now; the code is single-use.")
    printNext(
      "CLI AUTOMATED",
      "Send the UI organisation's supported V2 MTD-VAT invitation to trigger AES pull-through:",
      command(workspace, "v2-invite ui mtd-vat")
    )

  private def refreshAgentToken(workspace: Workspace): Unit =
    refreshAgentTokenValue(workspace)
    printNext(
      "CLI AUTOMATED",
      "Continue with the command that reported an expired token. For the final V3 run use:",
      command(workspace, "v3-matrix api")
    )

  private def refreshAgentTokenValue(workspace: Workspace): String =
    prepareRawDirectory(workspace)
    val currentConfig = config(workspace)
    val tokenPath = workspace.rawDir.resolve("agent-token.json")
    ensure(
      Files.isRegularFile(tokenPath),
      s"Missing agent OAuth token:\n  $tokenPath\n\nRun ${command(workspace, "oauth-url")} and then exchange-code first."
    )
    val refreshToken = Try(EtProfileTestSupport.requiredString(readJson(tokenPath), Seq("refresh_token"))).getOrElse:
      throw ToolError(
        s"Agent token has no refresh_token. Repeat browser authorisation with:\n  ${command(workspace, "oauth-url")}"
      )
    val response = postForm(
      TokenUrl,
      Seq(
        "client_id"     -> currentConfig.clientId,
        "client_secret" -> currentConfig.clientSecret,
        "grant_type"    -> "refresh_token",
        "refresh_token" -> refreshToken
      )
    )
    val refreshResponsePath = saveResponse(workspace, "agent-token-refresh-response.json", response)
    if response.statusCode() != 200 then
      throw ToolError(
        s"OAuth refresh returned HTTP ${response.statusCode()}; expected 200. The refresh token may have expired or been revoked.\n" +
          s"Inspect $refreshResponsePath, then repeat browser authorisation with:\n  ${command(workspace, "oauth-url")}"
      )
    val refreshed = parse(response.body())
    val accessToken = EtProfileTestSupport.requiredString(refreshed, Seq("access_token"))
    val scopes = EtProfileTestSupport.requiredString(refreshed, Seq("scope")).split(" ").toSet
    val missing = EtProfileTestSupport.RequiredAgentScopes.toSet -- scopes
    ensure(missing.isEmpty, s"Refreshed token is missing scopes: ${missing.toSeq.sorted.mkString(", ")}")
    EtProfileTestSupport.requiredString(refreshed, Seq("refresh_token"))
    writePrivate(tokenPath, Json.prettyPrint(refreshed))
    println("OAuth refresh_token exchange: PASS (HTTP 200)")
    println(s"Fresh agent token saved privately to:\n  $tokenPath")
    println("Sensitive values were not printed.")
    accessToken

  private def createV2Invitation(workspace: Workspace, source: String, triggerArgument: String): Unit =
    prepareRawDirectory(workspace)
    val trigger = EtProfileTestSupport.V2InvitationTrigger
      .from(triggerArgument)
      .getOrElse(throw ToolError("Invitation trigger must be 'mtd-vat' or 'mtd-it'."))
    val organisation = readJson(organisationPath(workspace, source))
    val agent = readJson(workspace.rawDir.resolve("agent.json"))
    val arn = EtProfileTestSupport.requiredString(agent, Seq("agentServicesAccountNumber"))
    val requestBody = EtProfileTestSupport.v2InvitationBody(organisation, trigger)
    val filenamePrefix = s"$source-v2-${trigger.argument}-invitation"
    writePrivate(workspace.rawDir.resolve(s"$filenamePrefix-request.json"), Json.prettyPrint(requestBody))

    val response = postJson(
      s"$ApiBase/agents/$arn/invitations",
      agentToken(workspace),
      "application/vnd.hmrc.2.0+json",
      requestBody
    )
    val responsePath = saveResponse(workspace, s"$filenamePrefix-response.json", response)
    writePrivate(
      workspace.rawDir.resolve(s"$filenamePrefix-response.headers"),
      response
        .headers()
        .map()
        .asScala
        .toSeq
        .sortBy(_._1)
        .map((name, values) => s"$name: ${values.asScala.mkString(", ")}")
        .mkString("\n")
    )
    val errorCode = Try(parse(response.body())).toOption.flatMap(json => (json \ "code").asOpt[String])
    if response.statusCode() == 403 && errorCode.contains("CLIENT_REGISTRATION_NOT_FOUND") then
      throw ToolError(
        s"""V2 ${trigger.label} invitation for $source organisation: FAIL (HTTP 403 CLIENT_REGISTRATION_NOT_FOUND)
          |The request was accepted but AES could not resolve the organisation using this trigger.
          |Retrying unchanged data is not expected to help.
          |The sanitised error response is stored privately at:
          |  $responsePath
          |
          |Verify whether the organisation is absent from AES Mongo with:
          |  ${command(workspace, s"mongo-help $source")}
          |""".stripMargin
      )
    requireStatus(
      workspace,
      response,
      204,
      s"V2 ${trigger.label} invitation for $source organisation",
      s"$filenamePrefix-response.json"
    )
    println(s"V2 ${trigger.label} invitation for $source organisation: PASS (HTTP 204)")
    printNext(
      "DATABASE ACCESS REQUIRED",
      s"Inspect the AES user and group collections for the $source organisation. Display the query and checklist with:",
      command(workspace, s"mongo-help $source")
    )

  private def v3Smoke(workspace: Workspace, source: String): Unit =
    prepareRawDirectory(workspace)
    val organisation = readJson(organisationPath(workspace, source))
    val agent = readJson(workspace.rawDir.resolve("agent.json"))
    val arn = EtProfileTestSupport.requiredString(agent, Seq("agentServicesAccountNumber"))
    val scenario = EtProfileTestSupport.V3Scenario(
      "trusts-smoke",
      "Trusts V3 smoke",
      "TRUSTS",
      EtProfileTestSupport.requiredString(organisation, Seq("trustsUniqueTaxpayerReference")),
      None
    )
    val result = runV3Scenario(
      workspace,
      s"$ApiBase/agents/$arn/invitations",
      refreshAgentTokenValue(workspace),
      scenario
    )
    ensure(result.passed, s"V3 Trusts smoke lifecycle failed. Inspect ${workspace.rawDir}.")
    println("Exact V3 media type and Trusts create/get/cancel lifecycle: PASS")
    println("Sensitive identifiers and the invitation ID were not printed.")

  private def captureMongoEvidence(workspace: Workspace, source: String): Unit =
    prepareRawDirectory(workspace)
    readJson(organisationPath(workspace, source))
    val consolePath = workspace.rawDir.resolve(s"$source-mongo-console.txt")
    ensure(
      Files.isRegularFile(consolePath),
      s"""Saved Jenkins console output not found:
        |  $consolePath
        |
        |Generate the query and display the exact Jenkins instructions with:
        |  ${command(workspace, s"mongo-help $source")}
        |""".stripMargin
    )
    val evidence = Try(EtProfileTestSupport.mongoEvidenceFromConsole(Files.readString(consolePath)))
      .recover:
        case error: IllegalArgumentException => throw ToolError(error.getMessage)
      .get
    printChecks(EtProfileTestSupport.validateMongoEvidence(evidence))
    val evidencePath = workspace.rawDir.resolve(s"$source-mongo-result.json")
    writePrivate(evidencePath, Json.prettyPrint(evidence))
    println(s"AES Mongo evidence for the $source organisation: PASS")
    println(s"Protected structured result written to:\n  $evidencePath")
    println("No user IDs, enrolment values or known facts were printed.")
    if source == "ui" then
      printNext(
        "CLI AUTOMATED",
        "Create the separate API organisation required by APB-12584:",
        command(workspace, "create-api-org")
      )
    else
      printNext(
        "MANUAL DEPLOYMENT CONFIRMATION REQUIRED",
        "After the pre-V3 evidence is reviewed as GO and V3 is enabled in ET, run the complete Organisation matrix:",
        command(workspace, "v3-matrix api")
      )

  private def v3Matrix(workspace: Workspace, source: String): Unit =
    prepareRawDirectory(workspace)
    val organisation = readJson(organisationPath(workspace, source))
    val mongoPath = workspace.rawDir.resolve(s"$source-mongo-result.json")
    ensure(
      Files.isRegularFile(mongoPath),
      s"Missing validated Mongo evidence:\n  $mongoPath\n\nRun ${command(workspace, s"mongo-help $source")} first."
    )
    val mongoEvidence = readJson(mongoPath)
    printChecks(EtProfileTestSupport.validateMongoEvidence(mongoEvidence))
    val scenarios = EtProfileTestSupport.v3OrganisationScenarios(organisation, mongoEvidence)
    val token = refreshAgentTokenValue(workspace)
    val agent = readJson(workspace.rawDir.resolve("agent.json"))
    val arn = EtProfileTestSupport.requiredString(agent, Seq("agentServicesAccountNumber"))
    val invitationsUrl = s"$ApiBase/agents/$arn/invitations"
    val results = scenarios.map(scenario => runV3Scenario(workspace, invitationsUrl, token, scenario))

    println("\nV3 Organisation invitation lifecycle matrix")
    println(
      f"${"Scenario"}%-38s ${"Create"}%6s ${"Prefix"}%7s ${"List"}%5s ${"Get"}%5s ${"Cancel"}%7s ${"Final"}%10s ${"Result"}%7s"
    )
    results.foreach: result =>
      println(
        f"${result.label}%-38s ${result.createStatus}%6d ${result.invitationPrefix.getOrElse("-")}%7s " +
          f"${result.listStatus.map(_.toString).getOrElse("-")}%5s ${result.getStatus.map(_.toString).getOrElse("-")}%5s " +
          f"${result.cancelStatus.map(_.toString).getOrElse("-")}%7s " +
          f"${result.finalInvitationStatus.getOrElse("-")}%10s ${if result.passed then "PASS" else "FAIL"}%7s"
      )

    val trustScenario = scenarios
      .find(_.slug == "trusts-taxable")
      .getOrElse:
        throw ToolError("Internal error: taxable Trusts scenario is missing.")
    val relationshipUrl =
      s"$ApiBase/agents/$arn/service/${encode(trustScenario.service)}/client/${encode(trustScenario.clientId)}/relationship"
    val relationshipBefore = get(relationshipUrl, token, "application/vnd.hmrc.3.0+json")
    saveResponse(workspace, s"$source-v3-relationship-before-response.json", relationshipBefore)
    val relationshipBeforePassed =
      relationshipBefore.statusCode() == 200 && Try(
        (parse(relationshipBefore.body()) \ "agentAuthorised").as[Boolean]
      ).toOption.contains(false)
    val deauthorise = delete(relationshipUrl, token, "application/vnd.hmrc.3.0+json")
    saveResponse(workspace, s"$source-v3-deauthorise-response.txt", deauthorise)
    val relationshipAfter = get(relationshipUrl, token, "application/vnd.hmrc.3.0+json")
    saveResponse(workspace, s"$source-v3-relationship-after-response.json", relationshipAfter)
    val relationshipAfterPassed =
      relationshipAfter.statusCode() == 200 && Try(
        (parse(relationshipAfter.body()) \ "agentAuthorised").as[Boolean]
      ).toOption.contains(false)
    val endpointChecks = Seq(
      EtProfileTestSupport.Check("Check absent relationship", relationshipBeforePassed),
      EtProfileTestSupport.Check("Idempotent deauthorisation", deauthorise.statusCode() == 204),
      EtProfileTestSupport.Check("Relationship remains absent", relationshipAfterPassed)
    )
    println("\nRemaining V3 endpoint checks")
    endpointChecks.foreach(check => println(f"${check.name}%-52s ${if check.passed then "PASS" else "FAIL"}"))

    val summaryPath = workspace.workDir.resolve("v3-matrix-summary.json")
    val summary = Json.obj(
      "environment"    -> "externaltest",
      "source"         -> source,
      "matrix"         -> results.map(_.sanitisedJson),
      "endpointChecks" -> endpointChecks.map(check =>
        Json.obj("name" -> check.name, "result" -> (if check.passed then "PASS" else "FAIL"))
      ),
      "result" -> (if results.forall(_.passed) && endpointChecks.forall(_.passed) then "PASS" else "FAIL")
    )
    writePrivate(summaryPath, Json.prettyPrint(summary))
    println(s"\nSanitised summary written to:\n  $summaryPath")
    println(s"Private requests and responses remain under:\n  ${workspace.rawDir}")
    ensure(
      results.forall(_.passed) && endpointChecks.forall(_.passed),
      s"V3 matrix failed. Inspect the protected responses under ${workspace.rawDir}; do not paste them into chat or Jira."
    )
    println("V3 Organisation matrix and all six endpoint operations: PASS")
    println(
      "The comprehensive negative/error matrix belongs to agent-authorisation-api-acceptance-tests; " +
        "run its non-default V3 pack before final Jira sign-off."
    )

  private def v3DisabledCheck(workspace: Workspace, source: String): Unit =
    prepareRawDirectory(workspace)
    val organisation = readJson(organisationPath(workspace, source))
    printChecks(EtProfileTestSupport.validateOrganisation(organisation))
    val token = refreshAgentTokenValue(workspace)
    val agent = readJson(workspace.rawDir.resolve("agent.json"))
    val arn = EtProfileTestSupport.requiredString(agent, Seq("agentServicesAccountNumber"))
    val vrn = EtProfileTestSupport.requiredString(organisation, Seq("vrn"))
    val invitationsUrl = s"$ApiBase/agents/$arn/invitations"
    val relationshipUrl =
      s"$ApiBase/agents/$arn/service/MTD-VAT/client/${encode(vrn)}/relationship"

    val v3Response = get(relationshipUrl, token, "application/vnd.hmrc.3.0+json")
    saveResponse(workspace, s"$source-v3-disabled-relationship-response.json", v3Response)
    val v3Body = Try(parse(v3Response.body())).getOrElse(Json.obj())
    val v2Response = get(invitationsUrl, token, "application/vnd.hmrc.2.0+json")
    saveResponse(workspace, s"$source-v3-disabled-v2-invitations-response.json", v2Response)
    val checks = EtProfileTestSupport.validateV3Disabled(v3Response.statusCode(), v3Body, v2Response.statusCode())

    println("\nV3-disabled compatibility checks")
    checks.foreach(check => println(f"${check.name}%-62s ${if check.passed then "PASS" else "FAIL"}"))
    val passed = checks.forall(_.passed)
    val summaryPath = workspace.workDir.resolve("v3-disabled-summary.json")
    val summary = Json.obj(
      "environment" -> "externaltest",
      "source"      -> source,
      "checks"      -> checks.map(check =>
        Json.obj("name" -> check.name, "result" -> (if check.passed then "PASS" else "FAIL"))
      ),
      "result" -> (if passed then "PASS" else "FAIL")
    )
    writePrivate(summaryPath, Json.prettyPrint(summary))
    println(s"\nSanitised summary written to:\n  $summaryPath")
    println(s"Private responses remain under:\n  ${workspace.rawDir}")
    ensure(
      passed,
      "V3-disabled verification failed. Confirm features.enable-v3=false was deployed and Agent Authorisation API " +
        s"was restarted, then inspect the protected responses under ${workspace.rawDir}."
    )
    println("V3 unavailable and V2 remains available: PASS")

  private def runV3Scenario(
    workspace: Workspace,
    invitationsUrl: String,
    token: String,
    scenario: EtProfileTestSupport.V3Scenario
  ): V3MatrixResult =
    val prefix = s"v3-matrix-${scenario.slug}"
    cancelPreviousPendingMatrixInvitation(workspace, invitationsUrl, token, prefix)
    writePrivate(workspace.rawDir.resolve(s"$prefix-request.json"), Json.prettyPrint(scenario.requestBody))
    val create = postJson(invitationsUrl, token, "application/vnd.hmrc.3.0+json", scenario.requestBody)
    saveResponse(workspace, s"$prefix-create-response.json", create)
    val invitationId =
      if create.statusCode() == 201 then
        Try(EtProfileTestSupport.requiredString(parse(create.body()), Seq("invitationId"))).toOption
      else None
    invitationId match
      case None =>
        V3MatrixResult(scenario.slug, scenario.label, create.statusCode(), None, None, None, None, None, None, None)
      case Some(id) =>
        val invitationUrl = s"$invitationsUrl/$id"
        val listResponse = get(invitationsUrl, token, "application/vnd.hmrc.3.0+json")
        saveResponse(workspace, s"$prefix-list-response.json", listResponse)
        val listed = Try(EtProfileTestSupport.invitationListed(parse(listResponse.body()), id)).getOrElse(false)
        val getResponse = get(invitationUrl, token, "application/vnd.hmrc.3.0+json")
        saveResponse(workspace, s"$prefix-get-response.json", getResponse)
        val cancelResponse = patch(invitationUrl, token, "application/vnd.hmrc.3.0+json")
        saveResponse(workspace, s"$prefix-cancel-response.txt", cancelResponse)
        val finalResponse = get(invitationUrl, token, "application/vnd.hmrc.3.0+json")
        saveResponse(workspace, s"$prefix-final-response.json", finalResponse)
        val finalStatus = Try((parse(finalResponse.body()) \ "status").as[String]).toOption
        V3MatrixResult(
          scenario.slug,
          scenario.label,
          create.statusCode(),
          id.headOption.map(_.toString),
          Some(listResponse.statusCode()),
          Some(listed),
          Some(getResponse.statusCode()),
          Some(cancelResponse.statusCode()),
          Some(finalResponse.statusCode()),
          finalStatus
        )

  private def cancelPreviousPendingMatrixInvitation(
    workspace: Workspace,
    invitationsUrl: String,
    token: String,
    prefix: String
  ): Unit =
    val previousCreatePath = workspace.rawDir.resolve(s"$prefix-create-response.json")
    if Files.isRegularFile(previousCreatePath) then
      val invitationId = Try(
        EtProfileTestSupport.requiredString(readJson(previousCreatePath), Seq("invitationId"))
      ).toOption
      invitationId.foreach: id =>
        val invitationUrl = s"$invitationsUrl/$id"
        val current = get(invitationUrl, token, "application/vnd.hmrc.3.0+json")
        saveResponse(workspace, s"$prefix-recovery-get-response.json", current)
        requireStatus(
          workspace,
          current,
          200,
          "Inspect previous matrix invitation",
          s"$prefix-recovery-get-response.json"
        )
        val status = Try((parse(current.body()) \ "status").as[String]).toOption
        if status.contains("Pending") then
          val cancel = patch(invitationUrl, token, "application/vnd.hmrc.3.0+json")
          saveResponse(workspace, s"$prefix-recovery-cancel-response.txt", cancel)
          requireStatus(
            workspace,
            cancel,
            204,
            "Cancel previous pending matrix invitation",
            s"$prefix-recovery-cancel-response.txt"
          )
          println(
            s"Recovered interrupted ${prefix.stripPrefix("v3-matrix-")} scenario by cancelling its pending invitation."
          )

  private def mongoHelp(workspace: Workspace, source: String): Unit =
    val organisation = organisationPath(workspace, source)
    val userId = EtProfileTestSupport.requiredString(readJson(organisation), Seq("userId"))
    val queryPath = workspace.rawDir.resolve(s"$source-mongo-query.js")
    val quotedUserId = Json.stringify(Json.toJson(userId))
    val query =
      s"""var d=db.getSiblingDB("agents-external-stubs");var u=d.getCollection("users").findOne({userId:$quotedUserId},{_id:0,userId:1,groupId:1,planetId:1,assignedPrincipalEnrolments:1,recordIds:1});var g=u?d.getCollection("groups").findOne({groupId:u.groupId,planetId:u.planetId},{_id:0,groupId:1,affinityGroup:1,principalEnrolments:1}):null;var r=u&&u.recordIds?d.getCollection("records").find({_id:{$$in:u.recordIds.map(function(x){return ObjectId(x);})}},{_id:0,pptReference:1,"legalEntityDetails.dateOfApplication":1,cbcId:1,"primaryContact.email":1,plrReference:1,"upeDetails.registrationDate":1}).toArray():[];print("APB12584_RESULT="+JSON.stringify({user:u,group:g,records:r}))"""
    writePrivate(queryPath, query)
    val consolePath = workspace.rawDir.resolve(s"$source-mongo-console.txt")
    println(
      s"""AES Mongo verification for the $source organisation
        |
        |[DATABASE ACCESS REQUIRED]
        |The Scala tool cannot perform this check without an approved ET Mongo access route.
        |The tool read userId privately from:
        |  $organisation
        |and generated a ready-to-paste, single-line read-only Jenkins query at:
        |  $queryPath
        |The userId was not printed. Keep both files private.
        |
        |[BROWSER REQUIRED — JENKINS]
        |  1. Open the Platform Tools query-mongo job:
        |       https://build.tax.service.gov.uk/job/PlatformTools/job/query-mongo/
        |  2. Select Build with Parameters.
        |  3. Set ENVIRONMENT to externaltest.
        |  4. Set REPLICA_SET_CHOICE to protected.
        |  5. Open $queryPath in a local editor and copy its one line into QUERY.
        |     Do not add a 'use' command or a leading dot.
        |
        |If externaltest is not offered or Jenkins denies access, stop and ask a team
        |member with External Test query-mongo permission to run the generated query. Do not
        |substitute a write-capable route or use another environment.
        |
        |  6. Open Console Output after the build completes.
        |  7. Copy the complete console output into this private local file:
        |       $consolePath
        |  8. Protect the file:
        |       chmod 600 $consolePath
        |  9. Let the tool extract and validate only its marked JSON result:
        |       ${command(workspace, s"capture-mongo $source")}
        |
        |Interpret the data, not just the Jenkins build status:
        |  user: null               FAIL — the organisation is absent from AES.
        |  group: null              FAIL — its AES group is absent.
        |  group.affinityGroup      Must be Organisation.
        |  records                  Supplies the private V3 PPT, CBC and Pillar 2 known facts.
        |  Finished: SUCCESS        Only confirms that Jenkins executed the query.
        |A missing user or group is a pre-V3 NO-GO. Do not continue as though this check passed.
        |
        |Confirm every expected service and identifier:
        |  HMCE-VATDEC-ORG   / VATRegNo
        |  HMRC-MTD-VAT      / VRN
        |  HMRC-MTD-IT       / MTDITID
        |  HMRC-CGT-PD       / CGTPDRef
        |  HMRC-PPT-ORG      / EtmpRegistrationNumber
        |  HMRC-PILLAR2-ORG  / PLRID
        |  HMRC-CBC-ORG      / cbcId and UTR
        |  HMRC-TERS-ORG     / SAUTR
        |  HMRC-TERSNT-ORG   / URN
        |
        |Values must match the creation result. CBC's UTR is completed locally by AES.
        |For V3 testing, keep these record values private and use:
        |  legalEntityDetails.dateOfApplication  as the PPT known fact
        |  primaryContact[0].email               as the CBC known fact
        |  upeDetails.registrationDate           as the Pillar 2 known fact
        |Do not paste the complete result into chat or Jira. Record only a sanitised summary.
        |The AES browser user page or REST Query page is an optional diagnostic cross-check,
        |not a replacement for the Jira-required Mongo verification.
        |
        |The query emits one line beginning APB12584_RESULT=. The capture-mongo command
        |finds that line automatically; do not edit or manually reconstruct its JSON.
        |""".stripMargin
    )

  private def validatedAppToken(workspace: Workspace): String =
    validatedAppToken(workspace, config(workspace))

  private def validatedAppToken(workspace: Workspace, credentials: Config): String =
    prepareRawDirectory(workspace)
    val response = postForm(
      TokenUrl,
      Seq(
        "client_id"     -> credentials.clientId,
        "client_secret" -> credentials.clientSecret,
        "grant_type"    -> "client_credentials"
      )
    )
    saveResponse(workspace, "app-token.json", response)
    if response.statusCode() != 200 then
      val reason =
        if response.statusCode() == 401 then "The client ID or client secret may be incorrect, deleted or expired."
        else "Developer Hub did not accept the configured application credentials."
      throw ToolError(
        s"""Developer Hub credential validation failed (HTTP ${response.statusCode()}).
          |$reason
          |
          |No credential values were printed.
          |Run '$RunPrefix credentials-help\"' for recovery instructions.
          |""".stripMargin
      )
    val token = EtProfileTestSupport.requiredString(parse(response.body()), Seq("access_token"))
    println("OAuth client_credentials: PASS (HTTP 200)")
    token

  private def config(workspace: Workspace): Config =
    ensure(
      Files.isRegularFile(workspace.configPath),
      s"""APB-12584 ET credentials are not configured.
        |
        |Expected file:
        |  ${workspace.configPath}
        |
        |Create it with:
        |  $RunPrefix init-config --work-dir ${workspace.workDir}\"
        |
        |For instructions on obtaining credentials:
        |  $RunPrefix credentials-help\"
        |""".stripMargin
    )
    val values = Files
      .readAllLines(workspace.configPath)
      .asScala
      .map(_.trim)
      .filter(line => line.nonEmpty && !line.startsWith("#"))
      .map: line =>
        line.split("=", 2) match
          case Array(key, value) => key.trim -> value.trim
          case _                 => throw ToolError(s"Invalid configuration line in ${workspace.configPath}")
      .toMap
    Config(
      requiredConfig(workspace, values, "CLIENT_ID"),
      requiredConfig(workspace, values, "CLIENT_SECRET"),
      values.get("REDIRECT_URI").filter(_.nonEmpty).getOrElse("https://www.example.com/auth-redirect")
    )

  private def requiredConfig(workspace: Workspace, values: Map[String, String], name: String): String =
    values
      .get(name)
      .filter(_.nonEmpty)
      .getOrElse:
        throw ToolError(
          s"""$name is missing from:
            |  ${workspace.configPath}
            |
            |Run '$RunPrefix credentials-help\"' for setup instructions.
            |""".stripMargin
        )

  private def agentToken(workspace: Workspace): String =
    tokenFrom(workspace, "agent-token.json", "Complete OAuth and run exchange-code first.")

  private def tokenFrom(workspace: Workspace, filename: String, hint: String): String =
    val path = workspace.rawDir.resolve(filename)
    ensure(Files.isRegularFile(path), s"Missing workflow file:\n  $path\n\n$hint")
    EtProfileTestSupport.requiredString(readJson(path), Seq("access_token"))

  private def organisationPath(workspace: Workspace, source: String): Path = source match
    case "ui"  => workspace.rawDir.resolve("ui-organisation.json")
    case "api" => workspace.rawDir.resolve("api-organisation.json")
    case _     => throw ToolError("Organisation source must be 'ui' or 'api'.")

  private def get(url: String, token: String, accept: String): HttpResponse[String] =
    send(
      HttpRequest
        .newBuilder(URI.create(url))
        .header("Authorization", s"Bearer $token")
        .header("Accept", accept)
        .GET()
        .build()
    )

  private def postJson(url: String, token: String, accept: String, body: JsValue): HttpResponse[String] =
    send(
      HttpRequest
        .newBuilder(URI.create(url))
        .header("Authorization", s"Bearer $token")
        .header("Accept", accept)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body)))
        .build()
    )

  private def patch(url: String, token: String, accept: String): HttpResponse[String] =
    send(
      HttpRequest
        .newBuilder(URI.create(url))
        .header("Authorization", s"Bearer $token")
        .header("Accept", accept)
        .method("PATCH", HttpRequest.BodyPublishers.noBody())
        .build()
    )

  private def delete(url: String, token: String, accept: String): HttpResponse[String] =
    send(
      HttpRequest
        .newBuilder(URI.create(url))
        .header("Authorization", s"Bearer $token")
        .header("Accept", accept)
        .DELETE()
        .build()
    )

  private def postForm(url: String, fields: Seq[(String, String)]): HttpResponse[String] =
    val body = fields.map((name, value) => s"${encode(name)}=${encode(value)}").mkString("&")
    send(
      HttpRequest
        .newBuilder(URI.create(url))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build()
    )

  private def send(request: HttpRequest): HttpResponse[String] =
    def execute(retryDelays: Seq[Long]): HttpResponse[String] =
      paceRequest()
      val response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
      (response.statusCode(), retryDelays) match
        case (429, delay +: remaining) =>
          println(s"API Platform rate limit received; retrying in ${delay / 1000} seconds.")
          Thread.sleep(delay)
          execute(remaining)
        case _ => response

    execute(ThrottleRetryDelaysMillis)

  private def paceRequest(): Unit = synchronized:
    val now = System.nanoTime()
    val elapsedMillis = (now - lastRequestAtNanos) / 1000000L
    val waitMillis = MinimumRequestIntervalMillis - elapsedMillis
    if lastRequestAtNanos != 0L && waitMillis > 0 then Thread.sleep(waitMillis)
    lastRequestAtNanos = System.nanoTime()

  private def encode(value: String): String = java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def parse(value: String): JsValue =
    Try(Json.parse(value)).getOrElse(throw ToolError("Response was not valid JSON."))

  private def readJson(path: Path): JsValue =
    ensure(Files.isRegularFile(path), s"Missing file:\n  $path")
    parse(Files.readString(path))

  private def prepareRawDirectory(workspace: Workspace): Unit =
    Files.createDirectories(workspace.rawDir)
    setPermissions(workspace.rawDir, "rwx------")

  private def saveResponse(workspace: Workspace, filename: String, response: HttpResponse[String]): Path =
    val path = workspace.rawDir.resolve(filename)
    writePrivate(path, response.body())
    path

  private def writePrivate(path: Path, contents: String): Unit =
    Files.writeString(path, contents)
    setPermissions(path, "rw-------")

  private def writePrivateNew(path: Path, contents: String): Unit =
    Files.writeString(path, contents, CREATE_NEW)
    setPermissions(path, "rw-------")

  private def setPermissions(path: Path, permissions: String): Unit =
    Try(Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions)))

  private def requireStatus(
    workspace: Workspace,
    response: HttpResponse[String],
    expected: Int,
    operation: String,
    responseFilename: String
  ): Unit =
    val recovery =
      if response.statusCode() == 401 then
        s"\nThe agent token may have expired. Refresh it with:\n  ${command(workspace, "refresh-token")}"
      else ""
    ensure(
      response.statusCode() == expected,
      s"$operation returned HTTP ${response.statusCode()}; expected $expected. " +
        s"Inspect ${workspace.rawDir.resolve(responseFilename)}$recovery"
    )

  private def printChecks(checks: Seq[EtProfileTestSupport.Check]): Unit =
    checks.foreach(check => println(f"${check.name}%-52s ${if check.passed then "PASS" else "FAIL"}"))
    val failures = checks.filterNot(_.passed).map(_.name)
    ensure(failures.isEmpty, s"Checks failed: ${failures.mkString(", ")}")

  private def printWorkspace(workspace: Workspace): Unit =
    println(s"Work directory:\n  ${workspace.workDir}\n\nConfiguration:\n  ${workspace.configPath}\n")

  private def command(workspace: Workspace, arguments: String): String =
    s"$RunPrefix $arguments --work-dir ${workspace.workDir}\""

  private def printNext(stepType: String, description: String, nextCommand: String): Unit =
    println(s"\nNext [$stepType]:\n$description\n  $nextCommand")

  private def credentialsHelp(): Unit =
    println(
      s"""APB-12584 sandbox application credential setup
        |
        |The tool cannot create or retrieve Developer Hub credentials. A tester with access
        |to the sandbox application must complete these steps:
        |
        |  1. Open https://developer.service.hmrc.gov.uk/developer/login
        |  2. Sign in with an existing Developer Hub account. If you do not have one, use
        |     "Register for an account" and complete the account activation first.
        |  3. Open Applications after signing in.
        |  4. Select the sandbox application intended for APB-12584 testing, or create one.
        |  5. Open its credentials page and copy the client ID.
        |  6. Generate a client secret if no suitable active secret is available.
        |  7. Confirm the application is subscribed to Create Test User API 1.0.
        |  8. Confirm the application is subscribed to Agent Authorisation API 2.0.
        |  9. Before the V3 phase, confirm the application is subscribed to Agent
        |     Authorisation API 3.0 as well as 2.0.
        | 10. Confirm https://www.example.com/auth-redirect is a registered redirect URI,
        |     or record the registered alternative you will use.
        | 11. From the agents-external-stubs repository root, run:
        |     $RunPrefix init-config --work-dir $$PWD/.local/et-testing/APB-12584"
        | 12. Add CLIENT_ID, CLIENT_SECRET and REDIRECT_URI to:
        |     $$PWD/.local/et-testing/APB-12584/operator.env
        | 13. Validate the completed configuration with:
        |     $RunPrefix check-config --work-dir $$PWD/.local/et-testing/APB-12584"
        |
        |Never put credentials in command arguments, Git, Jira, screenshots or chat.
        |The tool never prints credential values.
        |
        |Full local runbook:
        |  docs/apb-12584-et-profile-test.md
        |
        |HMRC guidance:
        |  https://developer.service.hmrc.gov.uk/api-documentation/docs/testing
        |  https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation/credentials
        |""".stripMargin
    )

  private def usage(): Unit =
    println(
      s"""APB-12584 External Test profile verification tool
        |
        |Purpose:
        |  Prove that organisations created through both Developer Hub and Create Test User
        |  API are imported into Agents External Stubs with the new APB-12584 profiles,
        |  then run the repeatable V3 Organisation and six-endpoint verification.
        |
        |Step types:
        |  [BROWSER REQUIRED]          A tester must complete an interactive web journey.
        |  [CLI AUTOMATED]             This Scala tool performs and validates the operation.
        |  [DATABASE ACCESS REQUIRED]  A tester must use the approved ET Mongo access route.
        |  [MANUAL CONFIRMATION]       A tester must confirm deployments or review evidence.
        |
        |Commands that do not require --work-dir:
        |  help
        |      Show this overview.
        |  credentials-help
        |      Explain how to obtain and configure sandbox credentials.
        |
        |Commands that require --work-dir <directory>:
        |  init-config --work-dir <directory>
        |      Create a protected blank operator.env and raw directory.
        |  check-config --work-dir <directory>
        |      Validate operator.env against the OAuth token endpoint.
        |  preflight --work-dir <directory>
        |      Validate credentials and confirm all required ET profiles.
        |  ui-org-help --work-dir <directory>
        |      Explain the required Developer Hub UI organisation journey.
        |  capture-ui-html --work-dir <directory>
        |      Extract and validate the UI organisation from saved Developer Hub HTML.
        |  validate-org ui|api --work-dir <directory>
        |      Validate a captured organisation and its identifiers.
        |  create-agent --work-dir <directory>
        |      Create the test agent used for the V2 invitation.
        |  create-api-org --work-dir <directory>
        |      Create an organisation with every profile through the API.
        |  oauth-url --work-dir <directory>
        |      Validate credentials and print the agent authorisation URL.
        |  exchange-code --work-dir <directory>
        |      Exchange the saved browser OAuth code for an agent token.
        |  refresh-token --work-dir <directory>
        |      Refresh an existing agent token without repeating the browser journey.
        |  v2-invite ui|api mtd-vat|mtd-it --work-dir <directory>
        |      Send a V2 invitation to trigger AES pull-through. Use mtd-vat for the
        |      supported APB-12584 acceptance path; mtd-it is diagnostic only.
        |  v3-smoke ui|api --work-dir <directory>
        |      Prove the exact V3 media type with a taxable Trusts
        |      create/get/cancel lifecycle. The invitation is not left pending.
        |  mongo-help ui|api --work-dir <directory>
        |      Generate the AES Mongo query and explain the approved Jenkins journey.
        |  capture-mongo ui|api --work-dir <directory>
        |      Extract and validate the marked result from saved Jenkins console output.
        |  v3-matrix ui|api --work-dir <directory>
        |      Refresh OAuth, run every Organisation profile through create/get/cancel,
        |      test list/check/deauthorise, and write a sanitised PASS/FAIL summary.
        |  v3-disabled-check ui|api --work-dir <directory>
        |      After V3 is disabled and AAA restarted, prove the V3-only relationship
        |      endpoint is unavailable while the V2 invitations endpoint still works.
        |
        |Guided workflow:
        |  1. [BROWSER]  credentials-help: register/sign in and prepare the application.
        |  2. [CLI]      init-config -> check-config -> preflight.
        |  3. [BROWSER]  ui-org-help: create and save the required UI organisation.
        |  4. [CLI]      capture-ui-html -> create-agent.
        |  5. [BROWSER]  oauth-url: sign in as the agent and grant authority.
        |  6. [CLI]      exchange-code -> v2-invite ui mtd-vat.
        |  7. [DATABASE] mongo-help ui -> capture-mongo ui: verify the UI organisation in AES.
        |  8. [CLI]      create-api-org -> v2-invite api mtd-vat.
        |  9. [DATABASE] mongo-help api -> capture-mongo api: verify the API organisation in AES.
        | 10. [MANUAL]   Review the pre-V3 GO decision, enable V3 and confirm deployment.
        | 11. [CLI]      v3-matrix api: run the complete Organisation and endpoint matrix.
        | 12. [CI]       Run the acceptance repository's non-default V3 pack for errors.
        | 13. [MANUAL]   Disable V3 in ET and restart or redeploy AAA.
        | 14. [CLI]      v3-disabled-check api: prove V3 is unavailable and V2 still works.
        | 15. [MANUAL]   Attach only sanitised summaries and record the final GO/NO-GO.
        |
        |Example:
        |  $RunPrefix preflight --work-dir /secure/path/APB-12584\"
        |
        |The active work directory and configuration path are printed before every stateful command.
        |Passwords, client secrets and access tokens are never printed.
        |""".stripMargin
    )

  private def commandUsage(command: String): String =
    command match
      case "validate-org"      => s"Usage: $RunPrefix validate-org ui|api --work-dir <directory>\""
      case "v2-invite"         => s"Usage: $RunPrefix v2-invite ui|api mtd-vat|mtd-it --work-dir <directory>\""
      case "v3-smoke"          => s"Usage: $RunPrefix v3-smoke ui|api --work-dir <directory>\""
      case "mongo-help"        => s"Usage: $RunPrefix mongo-help ui|api --work-dir <directory>\""
      case "capture-mongo"     => s"Usage: $RunPrefix capture-mongo ui|api --work-dir <directory>\""
      case "v3-matrix"         => s"Usage: $RunPrefix v3-matrix ui|api --work-dir <directory>\""
      case "v3-disabled-check" =>
        s"Usage: $RunPrefix v3-disabled-check ui|api --work-dir <directory>\""
      case known
          if Set(
            "init-config",
            "check-config",
            "preflight",
            "ui-org-help",
            "capture-ui-html",
            "create-agent",
            "create-api-org",
            "oauth-url",
            "exchange-code",
            "refresh-token"
          ).contains(known) =>
        s"Usage: $RunPrefix $known --work-dir <directory>\""
      case _ => "Run the help command to list supported commands."

  private def missingOrDuplicateWorkDir(command: String, count: Int): String =
    if count == 0 then s"""--work-dir is required for '$command'.
        |
        |${commandUsage(command)}
        |
        |Run '$RunPrefix credentials-help\"' for the complete setup procedure.
        |""".stripMargin
    else s"--work-dir must be supplied exactly once for '$command'."

  private def configTemplate: String =
    """# APB-12584 sandbox Developer Hub application credentials.
      |# Obtain these values using the credentials-help instructions.
      |# Never commit or share this file.
      |
      |CLIENT_ID=
      |CLIENT_SECRET=
      |REDIRECT_URI=https://www.example.com/auth-redirect
      |""".stripMargin

  private def ensure(condition: Boolean, message: => String): Unit =
    if !condition then throw ToolError(message)

  private final case class ToolError(message: String) extends RuntimeException(message)

  private def fail(message: String): Nothing =
    System.err.println(s"ERROR: $message")
    sys.exit(1)
