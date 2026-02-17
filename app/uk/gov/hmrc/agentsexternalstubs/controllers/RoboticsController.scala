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

import play.api.libs.json.{JsLookupResult, JsUndefined, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Request, Result}
import uk.gov.hmrc.agentsexternalstubs.connectors.RoboticsConnector
import uk.gov.hmrc.agentsexternalstubs.controllers.RoboticsController.{createKnownFacts, validateRequest}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, Generator, GroupGenerator, KnownFacts, RoboticsRequest, Services}
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
      validateRequest(request) match {
        case Left(errorResult) =>
          Future.successful(errorResult)

        case Right(req) =>
          val enrolment: EnrolmentKey = {

            def fail(msg: String): Nothing = throw new RuntimeException(msg)

            val service = req.targetSystem match {
              case "CESA"  => Services("IR-SA-AGENT").toRight("Service IR-SA-AGENT not found")
              case "COTAX" => Services("IR-CT-AGENT").toRight("Service IR-CT-AGENT not found")
            }

            val result: Either[String, EnrolmentKey] = for {
              s   <- service
              idf <- s.identifiers.headOption.toRight(s"No identifiers found for service ${s.name}")
              value <- Generator
                         .get(idf.valueGenerator)(session.userId)
                         .toRight(
                           s"Generator failed for valueGenerator ${idf.valueGenerator}"
                         )
            } yield EnrolmentKey.from(s.name, idf.name -> value)

            result.fold(msg => fail(s"Failed to create enrolment: $msg"), identity)
          }

          val agentId = enrolment.identifiers.head.value

          val callbackDelay = appConfig.roboticsCallbackDelay
          val knownFactsDelay = appConfig.roboticsKnownFactsDelay

          val correlationId = request.headers.get("correlationId").getOrElse(UUID.randomUUID().toString)

          val taskActor = actorSystem.actorOf(
            Props(
              new RoboticsTaskActor(
                roboticsConnector,
                knownFactsRepository,
                agentId,
                enrolment,
                req.targetSystem,
                req.postcode,
                session.planetId,
                callbackDelay,
                knownFactsDelay,
                actorSystem.scheduler,
                req.operationRequired,
                correlationId,
                req.requestId
              )
            )
          )

          actorSystem.scheduler.scheduleOnce(0.seconds, taskActor, Start)

          Future.successful(
            Ok(
              Json.obj(
                "message"   -> "success",
                "requestId" -> req.requestId
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
  enrolmentKey: EnrolmentKey,
  targetSystem: String,
  postcode: String,
  planetId: String,
  callbackDelay: Int,
  knownFactsDelay: Int,
  scheduler: Scheduler,
  operationRequired: String,
  correlationId: String,
  requestId: String
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
        "requestId"         -> requestId,
        "targetSystem"      -> targetSystem,
        "operationRequired" -> operationRequired,
        "agentId"           -> agentId,
        "status"            -> "success",
        "requestMessage"    -> requestMessage
      )

      roboticsConnector.sendCallback(callbackPayload, correlationId)

      // Schedule known facts creation after knownFactsDelay
      scheduler.scheduleOnce(knownFactsDelay.millis, self, CreateKnownFacts)(ec)

    case CreateKnownFacts =>
      createKnownFacts(enrolmentKey, postcode)
        .foreach { kf =>
          knownFactsRepository.upsert(
            KnownFacts.sanitize(kf.enrolmentKey.tag)(kf),
            planetId
          )
        }
  }
}

object RoboticsController extends HttpHelpers {
  private def workflowValue(body: JsValue): JsLookupResult =
    (body \ "requestData").asOpt[Seq[JsValue]] match {
      case Some(items) =>
        items
          .collectFirst {
            case item if (item \ "workflowData" \ "arguments").asOpt[Seq[JsValue]].exists(_.nonEmpty) =>
              item \ "workflowData" \ "arguments" \ 0 \ "value"
          }
          .getOrElse(JsUndefined("workflowData.arguments[0] not found"))
      case None =>
        JsUndefined("requestData array not found")
    }

  private[controllers] def createKnownFacts(
    enrolmentKey: EnrolmentKey,
    postcode: String
  ) =
    KnownFacts
      .generate(
        enrolmentKey,
        enrolmentKey.identifiers.head.value,
        {
          case "IRAgentPostcode" | "Postcode" => Some(postcode)
          case _                              => None
        }
      )

  def validateRequest(request: Request[JsValue]): Either[Result, RoboticsRequest] = {
    val workflowJson =
      workflowValue(request.body)
        .asOpt[String]
        .toRight(badRequest("INVALID_PAYLOAD", "workflowData is missing"))
        .flatMap { s =>
          scala.util
            .Try(Json.parse(s))
            .toEither
            .left
            .map(_ => badRequest("INVALID_JSON", "workflowData is not valid JSON"))
        }

    workflowJson.flatMap { workflow =>
      for {
        targetSystem <- (workflow \ "targetSystem")
                          .asOpt[String]
                          .toRight(badRequest("MISSING_TARGET_SYSTEM", "targetSystem is required"))
                          .flatMap {
                            case ts @ ("CESA" | "COTAX") => Right(ts)
                            case ts                      => Left(badRequest("INVALID_TARGET_SYSTEM", s"targetSystem '$ts' is not supported"))
                          }

        postcode <- (workflow \ "postcode")
                      .asOpt[String]
                      .toRight(badRequest("MISSING_POSTCODE", "postcode is required"))

        operationRequired <- (workflow \ "operationRequired")
                               .asOpt[String]
                               .toRight(badRequest("MISSING_OPERATION", "operationRequired is required"))

        requestId =
          (workflow \ "requestId").asOpt[String].getOrElse(UUID.randomUUID().toString)

        correlationId <- request.headers
                           .get("correlationId")
                           .toRight(badRequest("MISSING_CORRELATION_ID", "CorrelationId header is required"))

      } yield RoboticsRequest(
        targetSystem,
        postcode,
        operationRequired,
        requestId,
        correlationId
      )
    }
  }

}

sealed trait RoboticsMessage
case object Start extends RoboticsMessage
case object Callback extends RoboticsMessage
case object CreateKnownFacts extends RoboticsMessage
