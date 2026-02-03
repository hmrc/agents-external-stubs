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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.agentsexternalstubs.connectors.RoboticsConnector
import uk.gov.hmrc.agentsexternalstubs.controllers.RoboticsController.{createKnownFacts, validateTargetSystem, workflowValue}
import uk.gov.hmrc.agentsexternalstubs.models.{EnrolmentKey, GroupGenerator, KnownFacts}
import uk.gov.hmrc.agentsexternalstubs.repository.KnownFactsRepository
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoboticsController @Inject() (
  roboticsConnector: RoboticsConnector,
  knownFactsRepository: KnownFactsRepository,
  cc: ControllerComponents,
  val authenticationService: AuthenticationService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def invoke: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      validateTargetSystem(request.body) match {
        case Left(errorResult) =>
          Future.successful(errorResult)

        case Right(targetSystem) =>
          val workflow = workflowValue(request.body)
          val requestId =
            (workflow \ "requestId").asOpt[String].getOrElse(UUID.randomUUID().toString)

          val postcode =
            (workflow \ "agentDetails" \ "address" \ "postcode").as[String]

          val agentId = GroupGenerator.agentId(requestId)
          val callbackDelay = appConfig.roboticsCallbackDelay
          val knownFactsDelay = appConfig.roboticsKnownFactsDelay

          val callbackPayload = Json.obj(
            "targetSystem" -> targetSystem,
            "agentId"      -> agentId,
            "status"       -> "success"
          )

          Future {
            Thread.sleep(callbackDelay)
            roboticsConnector.sendCallback(callbackPayload)
            Thread.sleep(knownFactsDelay)
            createKnownFacts(agentId, targetSystem, postcode, session.planetId)
              .map { kf =>
                knownFactsRepository.upsert(
                  KnownFacts.sanitize(kf.enrolmentKey.tag)(kf),
                  session.planetId
                )
              }
              .getOrElse(Future.unit)
          }

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

object RoboticsController extends HttpHelpers {
  def validateTargetSystem(body: JsValue): Either[Result, String] =
    (workflowValue(body) \ "targetSystem")
      .asOpt[String] match {

      case None =>
        Left(
          badRequest(
            "MISSING_TARGET_SYSTEM",
            "targetSystem is required"
          )
        )

      case Some(ts) if Set("CESA", "COTAX").contains(ts) =>
        Right(ts)

      case Some(ts) =>
        Left(
          badRequest(
            "INVALID_TARGET_SYSTEM",
            s"targetSystem '$ts' is not supported"
          )
        )
    }

  private def workflowValue(body: JsValue) =
    body \ "requestData" \ "workflowData" \ "arguments" \ "value"

  private def createKnownFacts(agentId: String, targetSystem: String, postcode: String, planetId: String) = {
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
