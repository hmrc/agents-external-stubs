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

package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.{JsLookupResult, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.agentsexternalstubs.connectors.RoboticsConnector
import uk.gov.hmrc.agentsexternalstubs.controllers.RoboticsController.{createKnownFacts, validateTargetSystem, workflowValue}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, GroupGenerator, KnownFacts}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import org.apache.pekko.actor.{ActorSystem, Props, Scheduler}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoboticsController @Inject() (
  roboticsConnector: RoboticsConnector,
  knownFactsRepository: KnownFactsRepository,
  cc: ControllerComponents,
  val authenticationService: AuthenticationService,
  appConfig: AppConfig,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def invoke: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      validateTargetSystem(request.body) match {
        case Left(errorResult) =>
          Future.successful(errorResult)

        case Right(targetSystem) =>
          val workflow: JsValue = Json.parse(workflowValue(request.body).as[String])
          val requestId = (workflow \ "requestId").asOpt[String].getOrElse(UUID.randomUUID().toString)
          val postcode = (workflow \ "postcode").as[String]
          val operationRequired = (workflow \ "operationRequired").as[String]

          val agentId = GroupGenerator.agentId(requestId)
          val callbackDelay = appConfig.roboticsCallbackDelay
          val knownFactsDelay = appConfig.roboticsKnownFactsDelay

          // Extract correlationId from incoming request headers
          val correlationId = request.headers.get("CorrelationId").getOrElse(UUID.randomUUID().toString)

          val taskActor = actorSystem.actorOf(
            Props(
              new RoboticsTaskActor(
                roboticsConnector,
                knownFactsRepository,
                agentId,
                targetSystem,
                postcode,
                session.planetId,
                callbackDelay,
                knownFactsDelay,
                actorSystem.scheduler,
                operationRequired,
                correlationId
              )
            )
          )

          actorSystem.scheduler.scheduleOnce(0.seconds, taskActor, Start)

          Future.successful(
            Ok(
              Json.obj(
                "message"    -> "success",
                "request_Id" -> requestId
              )
            )
          )
      }
    }(SessionRecordNotFound)
  }
}

class RoboticsTaskActor(
  roboticsConnector: RoboticsConnector,
  knownFactsRepository: KnownFactsRepository,
  agentId: String,
  targetSystem: String,
  postcode: String,
  planetId: String,
  callbackDelay: Int,
  knownFactsDelay: Int,
  scheduler: Scheduler,
  operationRequired: String,
  correlationId: String
)(implicit ec: ExecutionContext)
    extends org.apache.pekko.actor.Actor with play.api.Logging {

  def receive: PartialFunction[Any, Unit] = {
    case Start =>
      // Schedule callback after callbackDelay
      scheduler.scheduleOnce(callbackDelay.millis, self, Callback)(ec)

    case Callback =>
      val requestMessage = operationRequired match {
        case "CREATE" => "Agent Created Successfully"
        case "UPDATE" => s"Agent Updated their {name}/{address}/{contact} Successfully"
        case _        => "Operation Completed Successfully"
      }

      val callbackPayload = Json.obj(
        "targetSystem"      -> targetSystem,
        "operationRequired" -> operationRequired,
        "agentId"           -> agentId.take(5).toUpperCase,
        "status"            -> "success",
        "requestMessage"    -> requestMessage
      )

      roboticsConnector.sendCallback(callbackPayload, correlationId)

      // Schedule known facts creation after knownFactsDelay
      scheduler.scheduleOnce(knownFactsDelay.millis, self, CreateKnownFacts)(ec)

    case CreateKnownFacts =>
      createKnownFacts(agentId, targetSystem, postcode)
        .foreach { kf =>
          knownFactsRepository.upsert(
            KnownFacts.sanitize(kf.enrolmentKey.tag)(kf),
            planetId
          )
        }
  }
}

object RoboticsController extends HttpHelpers {
  def validateTargetSystem(body: JsValue): Either[Result, String] = {
    val workflow = Json.parse((workflowValue(body)).as[String])

    (workflow \ "targetSystem").asOpt[String] match {
      case None =>
        Left(badRequest("MISSING_TARGET_SYSTEM", "targetSystem is required"))

      case Some(ts) if Set("CESA", "COTAX").contains(ts) =>
        Right(ts)

      case Some(ts) =>
        Left(badRequest("INVALID_TARGET_SYSTEM", s"targetSystem '$ts' is not supported"))
    }
  }

  private def workflowValue(body: JsValue): JsLookupResult =
    body \ "requestData" \ 0 \ "workflowData" \ "arguments" \ 0 \ "value"

  private[controllers] def createKnownFacts(
    agentId: String,
    targetSystem: String,
    postcode: String
  ) = {
    val enrolmentKey =
      targetSystem match {
        case "CESA"  => EnrolmentKey.from("IR-SA-AGENT", "IRAgentReference" -> agentId)
        case "COTAX" => EnrolmentKey.from("IR-CT-AGENT", "IRAgentReference" -> agentId)
      }

    KnownFacts
      .generate(
        enrolmentKey,
        agentId,
        {
          case "IRAgentPostcode" | "Postcode" => Some(postcode)
          case _                              => None
        }
      )
  }
}

sealed trait RoboticsMessage
case object Start extends RoboticsMessage
case object Callback extends RoboticsMessage
case object CreateKnownFacts extends RoboticsMessage
