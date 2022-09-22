/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.Logging
import play.api.http.HeaderNames
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, ControllerComponents, RequestHeader}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.duration.{Duration, MINUTES}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class PerfDataRequest(numAgents: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)

object PerfDataRequest {
  implicit val format: OFormat[PerfDataRequest] = Json.format[PerfDataRequest]
}

class PerfDataController @Inject() (
  val authenticationService: AuthenticationService,
  cc: ControllerComponents,
  wsClient: WSClient
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession with Logging {

  def generate: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withPayload[PerfDataRequest] { perfDataRequest =>
      Future.sequence(processBatches(perfDataRequest))

      Future successful Accepted(
        s"Processing can take a while, please check later for creation of " +
          s"${perfDataRequest.numAgents * (1 + perfDataRequest.clientsPerAgent + perfDataRequest.teamMembersPerAgent)} records"
      )
    }
  }

  private def processBatches(perfDataRequest: PerfDataRequest)(implicit
    requestHeader: RequestHeader
  ): List[Future[Option[Int]]] = {
    val runPrefix = Random.alphanumeric.filter(_.isUpper).take(4).mkString
    logger.info(s"Prefix for identifying clients and team members generated in this run: '$runPrefix'")

    val batchesOfIndexes = buildBatchesForProcessing(perfDataRequest.numAgents)
    logger.info(s"Processing will be done across ${batchesOfIndexes.size} batch(es)")

    batchesOfIndexes.map { batchOfIndexes =>
      logger.info(s"Starting batch having indexes: ${batchOfIndexes.mkString(", ")}")

      processBatch(runPrefix, batchOfIndexes, perfDataRequest.clientsPerAgent, perfDataRequest.teamMembersPerAgent)
        .map { maybeStatus =>
          maybeStatus match {
            case None =>
              logger.info(s"Done batch")
            case Some(status) =>
              if (status != CREATED) {
                logger.error(s"Encountered $maybeStatus during processing")
              } else {
                logger.info(s"Done batch with status $status")
              }
          }
          maybeStatus
        }
    }
  }

  private def buildBatchesForProcessing(numAgents: Int): List[Seq[Int]] = {
    val agencyNumbers = 1 to numAgents

    val numParallels = Runtime.getRuntime.availableProcessors() match {
      case count if count > 4               => 4
      case count if count == 1 | count == 2 => 2
      case count                            => count - 1
    }

    val groupSize = agencyNumbers.size / numParallels

    agencyNumbers.grouped(if (groupSize < 1) 1 else groupSize).toList
  }

  private def processBatch(runPrefix: String, batchOfIndexes: Seq[Int], clientsPerAgent: Int, teamMembersPerAgent: Int)(
    implicit requestHeader: RequestHeader
  ): Future[Option[Int]] = {
    var workAccum = Future[Option[Int]](None)

    for (index <- batchOfIndexes)
      workAccum = workAccum flatMap (maybeLatestStatus => {
        logger.info(s"Processing agency #$index")
        generateForOneAgent(runPrefix, index, clientsPerAgent, teamMembersPerAgent) map { status =>
          if (status != CREATED && maybeLatestStatus.isEmpty) Some(status) else maybeLatestStatus
        }
      })

    workAccum
  }

  private def generateForOneAgent(runPrefix: String, index: Int, clientsPerAgent: Int, teamMembersPerAgent: Int)(
    implicit requestHeader: RequestHeader
  ): Future[Int] =
    for {
      signInResponse      <- signIn(index)
      authSessionResponse <- getAuthSession(signInResponse)
      authenticatedSession = authSessionResponse.json.as[AuthenticatedSession]
      _ <- setUserAsAgent(authenticatedSession)
      massGenerationResponse <-
        massGenerate(runPrefix, index, clientsPerAgent, teamMembersPerAgent, authenticatedSession)
    } yield massGenerationResponse.status

  private def signIn(index: Int)(implicit requestHeader: RequestHeader): Future[WSResponse] = {
    val url = s"$baseUrl/agents-external-stubs/sign-in"

    wsClient
      .url(url)
      .post(
        Json.toJson(
          SignInRequest(
            userId = Some(f"perf-test-agent-$index%03d"),
            plainTextPassword = None,
            providerType = None,
            planetId = Some(f"perf-test-planet-$index%03d"),
            syncToAuthLoginApi = None
          )
        )
      )
  }

  private def getAuthSession(signedIn: WSResponse)(implicit requestHeader: RequestHeader): Future[WSResponse] = {
    val url = s"$baseUrl${signedIn.header(HeaderNames.LOCATION).getOrElse("")}"

    wsClient
      .url(url)
      .get()
  }

  private def setUserAsAgent(
    authenticatedSession: AuthenticatedSession
  )(implicit requestHeader: RequestHeader): Future[WSResponse] = {
    val url = s"$baseUrl/agents-external-stubs/users"

    wsClient
      .url(url)
      .withHttpHeaders(authHeaders(authenticatedSession): _*)
      .put(
        Json.toJson(
          UserGenerator
            .agent(userId = authenticatedSession.userId)
            .withPrincipalEnrolment(
              "HMRC-AS-AGENT",
              "AgentReferenceNumber",
              Generator.arn(authenticatedSession.userId).value
            )
        )
      )
  }

  private def massGenerate(
    runPrefix: String,
    index: Int,
    clientsPerAgent: Int,
    teamMembersPerAgent: Int,
    authenticatedSession: AuthenticatedSession
  )(implicit
    requestHeader: RequestHeader
  ): Future[WSResponse] = {

    val granPermsGenRequest = GranPermsGenRequest(
      s"$runPrefix$index",
      teamMembersPerAgent,
      clientsPerAgent,
      fillFriendlyNames = false,
      None,
      None,
      None,
      None
    )

    val url = s"$baseUrl/agents-external-stubs/test/gran-perms/generate-users"

    wsClient
      .url(url)
      .withHttpHeaders(authHeaders(authenticatedSession): _*)
      .withRequestTimeout(Duration(1, MINUTES))
      .post(
        Json.toJson(granPermsGenRequest)
      )
  }

  private def baseUrl(implicit requestHeader: RequestHeader): String =
    s"${if (requestHeader.secure) "https" else "http"}://${requestHeader.host}"

  private def authHeaders(authenticatedSession: AuthenticatedSession): Seq[(String, String)] =
    Seq(
      HeaderNames.AUTHORIZATION               -> s"Bearer ${authenticatedSession.authToken}",
      uk.gov.hmrc.http.HeaderNames.xSessionId -> authenticatedSession.sessionId
    )

}
