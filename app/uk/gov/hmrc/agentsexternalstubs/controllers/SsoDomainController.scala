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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.{Action, _}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future.successful

@Singleton
class SsoDomainController @Inject() (cc: MessagesControllerComponents)(implicit val configuration: Configuration)
    extends BackendController(cc) {

  def validate(domain: String): Action[AnyContent] = Action {
    if (domain != "www.google.com") NoContent else BadRequest
  }

  def digitalFlag(flag: String): Action[AnyContent] = Action {
    Ok
  }

  def getDomains: Action[AnyContent] = Action.async {
    successful(Ok(domainsJson))
  }

  def domainsJson =
    s"""
       {
      |   "internalDomains" : [
      |      "localhost"
      |   ],
      |   "externalDomains" : [
      |      "127.0.0.1",
      |      "online-qa.ibt.hmrc.gov.uk",
      |      "ibt.hmrc.gov.uk"
      |   ]
      |}
      |
   """.stripMargin
}
