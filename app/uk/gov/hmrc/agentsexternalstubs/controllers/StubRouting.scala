/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}

/** Some routes can be handled by IF or DES, based on a feature flag in the client.
  * We dispatch these to DES or IF based on the shape of the call.
  *
  * TODO: The inference in this class could be obviated with an environment switch
  */
@Singleton
class StubRouting @Inject() (ifStub: IfStubController, desStub: DesStubController, cc: ControllerComponents)
    extends BackendController(cc) {

  // referenceNumber is only used for trusts URN
  // otherwise ref-no is used
  def getRelationship(
    idtype: Option[String],
    referenceNumber: Option[String],
    `ref-no`: Option[String],
    arn: Option[String],
    agent: Boolean,
    `active-only`: Boolean,
    regime: String,
    from: Option[String],
    to: Option[String],
    relationship: Option[String],
    `auth-profile`: Option[String]
  ): Action[AnyContent] = {
    def routeByRefNumber = (`ref-no`, referenceNumber) match {
      case (Some(_), None) => ifStub.getRelationship(arn, agent, regime)
      case (None, Some(_)) => desStub.getRelationship(arn, agent, regime)
      case _               => desStub.getRelationship(arn, agent, regime)
    }
    idtype.map(_.toUpperCase) match {
      case Some("URN") => ifStub.getRelationship(arn, agent, regime)
      case Some("UTR") => desStub.getRelationship(arn, agent, regime)
      case _           => routeByRefNumber
    }
  }

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    (request.body \ "idType").toOption.map {
      _.toString.toUpperCase
    } match {
      case Some(""""URN"""") => ifStub.authoriseOrDeAuthoriseRelationship.apply(request)
      case _                 => desStub.authoriseOrDeAuthoriseRelationship.apply(request)
    }
  }
}
