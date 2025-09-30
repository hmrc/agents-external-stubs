/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.JsObject
import play.api.libs.ws.WSClient
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support._

class CompaniesHouseControllerISpec extends ServerBaseISpec with TestRequests {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "Calling" when {

    "GET /companies-house-api-proxy/company/:companynumber" should {
      "respond 200 with company details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val companyNumber = "01234567"

        val result = get(s"/companies-house-api-proxy/company/$companyNumber")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[String]("company_number", be(companyNumber))
            and haveProperty[String]("company_status", be("active"))
            and haveProperty[String]("company_name")
        )
      }
    }

    "GET /companies-house-api-proxy/company/:companynumber/officers" should {
      "respond 200 with company details" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val companyNumber = "01234567"
        val surname = "Norton"

        val result = get(s"/companies-house-api-proxy/company/$companyNumber/officers?surname=$surname")

        result should haveStatus(200)
        result should haveValidJsonBody(
          haveProperty[Seq[JsObject]](
            "items",
            eachElement(haveProperty[String]("name", be(s"${surname.toUpperCase}, Jane")))
          ) and haveProperty[JsObject](
            "links",
            haveProperty[String]("self", be(s"/company/$companyNumber/appointments"))
          )
        )
      }
    }

  }
}
