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
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.stubs.TestStubs
import uk.gov.hmrc.agentsexternalstubs.support.{NotAuthorized, ServerBaseISpec, TestRequests}

class CitizenDetailsStubControllerISpec extends ServerBaseISpec with TestRequests with TestStubs {

  lazy val wsClient = app.injector.instanceOf[WSClient]
  private val testPlanetId = "testPlanet"
  private val testUserId = "testUserId"
  private val testUtr = "6214961180"

  "CitizenDetailsStubController" when {
    "GET /citizen-details/nino/:nino" should {
      "respond 200 with citizen data if found" in {
        val user = userService
          .createUser(
            UserGenerator
              .individual(
                userId = testUserId,
                nino = "HW 82 78 56 C",
                name = "Alan Brian Foo-Foe",
                dateOfBirth = "1975-12-18"
              ),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession(user.userId, planetId = user.planetId.get)

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result should haveStatus(200)
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "dateOfBirth").as[String] shouldBe "18121975"
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }

      "respond 400 if called without suffix" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856")

        result should haveStatus(400)
      }

      "respond 200 with citizen data if found for another suffix" in {
        val user = userService
          .createUser(
            UserGenerator
              .individual(
                userId = testUserId,
                nino = "HW 82 78 56 C",
                name = "Alan Brian Foo-Foe",
                dateOfBirth = "1975-12-18"
              ),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession(user.userId, planetId = user.planetId.get)

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856A")

        result should haveStatus(200)
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "dateOfBirth").as[String] shouldBe "18121975"
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(
              userId = session.userId,
              nino = "JH 59 92 01 D",
              name = "Alan Brian Foo-Foe",
              dateOfBirth = "1975-12-18"
            )
        )

        val result = CitizenDetailsStub.getCitizen("nino", "HW827856C")

        result should haveStatus(404)
      }

      "respond 400 if nino not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("nino", "W82785C")

        result should haveStatus(400)
      }

      "respond 400 if tax identifier type not supported" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")

        result should haveStatus(400)
      }

      "respond 401 if not authenticated" in {
        val result = CitizenDetailsStub.getCitizen("foo", "HW827856C")(NotAuthorized)

        result should haveStatus(401)
      }
    }
    "GET /citizen-details/nino-no-suffix/:nino" should {
      "respond 200 with citizen data if found" in {
        val user = userService
          .createUser(
            UserGenerator
              .individual(
                userId = testUserId,
                nino = "HW 82 78 56 C",
                name = "Alan Brian Foo-Foe",
                dateOfBirth = "1975-12-18"
              ),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession =
          SignIn.signInAndGetSession(user.userId, planetId = user.planetId.get)

        val result = CitizenDetailsStub.getCitizen("nino-no-suffix", "HW827856")

        result should haveStatus(200)
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "dateOfBirth").as[String] shouldBe "18121975"
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }

      "respond 400 if called with suffix" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("nino-no-suffix", "HW827856C")

        result should haveStatus(400)
      }

      "respond 404 if not found" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()
        Users.update(
          UserGenerator
            .individual(
              userId = session.userId,
              nino = "JH 59 92 01 D",
              name = "Alan Brian Foo-Foe",
              dateOfBirth = "1975-12-18"
            )
        )

        val result = CitizenDetailsStub.getCitizen("nino-no-suffix", "HW827856")

        result should haveStatus(404)
      }

      "respond 400 if nino not valid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession()

        val result = CitizenDetailsStub.getCitizen("nino-no-suffix", "W82785")

        result should haveStatus(400)
      }

      "respond 401 if not authenticated" in {
        val result = CitizenDetailsStub.getCitizen("nino-no-suffix", "HW827856C")(NotAuthorized)

        result should haveStatus(401)
      }
    }

    "GET /citizen-details/:nino/designatory-details" should {
      "return user designatory details for Individuals" in {

        val user = userService
          .createUser(
            UserGenerator.individual(userId = testUserId),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

        val result = CitizenDetailsStub.getDesignatoryDetails(user.nino.get.value.replace(" ", ""))

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[JsObject](
          "person",
          haveProperty[String]("firstName", be(user.firstName.get)) and haveProperty[String](
            "lastName",
            be(user.lastName.get)
          ) and haveProperty[String]("nino", be(user.nino.get.value)) and haveProperty[String]("sex") and haveProperty[
            Boolean
          ]("deceased")
        ) and haveProperty[JsObject](
          "address",
          haveProperty[String]("line1") and haveProperty[String]("postcode") and haveProperty[String]("country")
        ))
      }

      "return user designatory details for Agents" in {

        val user = userService
          .createUser(
            UserGenerator.agent(userId = testUserId),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

        val result = CitizenDetailsStub.getDesignatoryDetails(user.nino.get.value.replace(" ", ""))

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[JsObject](
          "person",
          haveProperty[String]("firstName", be(user.firstName.get)) and haveProperty[String](
            "lastName",
            be(user.lastName.get)
          ) and haveProperty[String]("nino", be(user.nino.get.value)) and haveProperty[String]("sex") and haveProperty[
            Boolean
          ]("deceased")
        ) and haveProperty[JsObject](
          "address",
          haveProperty[String]("line1") and haveProperty[String]("postcode") and haveProperty[String]("country")
        ))
      }
    }

    "GET /citizen-details/:nino/designatory-details/basic" should {
      "return basic user details" in {

        val user = userService
          .createUser(
            UserGenerator.individual(userId = testUserId),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Individual)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

        val result = CitizenDetailsStub.getDesignatoryDetailsBasic(user.nino.get.value.replace(" ", ""))

        result should haveStatus(200)
        result.json
          .as[JsObject] should (haveProperty[String]("etag") and haveProperty[String](
          "firstName",
          be(user.firstName.get)
        ) and haveProperty[String]("lastName", be(user.lastName.get)) and haveProperty[String](
          "nino",
          be(user.nino.get.value)
        ) and haveProperty[Boolean]("deceased"))
      }
    }

    "GET /citizen-details/sautr/:sautr" should {
      "return Bad request when utr supplied is invalid" in {
        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)
        val result = CitizenDetailsStub.getCitizen("sautr", "01234567890")

        result should haveStatus(400)
      }

      "return citizen details for an agent" in {
        val user = userService
          .createUser(
            UserGenerator.agent(
              userId = testUserId,
              name = "Alan Brian Foo-Foe",
              deceased = true,
              nino = "HW 82 78 56 C",
              utr = testUtr
            ),
            planetId = testPlanetId,
            affinityGroup = Some(AG.Agent)
          )
          .futureValue

        implicit val session: AuthenticatedSession = SignIn.signInAndGetSession(testUserId, planetId = testPlanetId)

        val result = CitizenDetailsStub.getCitizen("sautr", user.utr.get)

        result should haveStatus(200)
        val json = result.json
        (json \ "ids" \ "nino").as[String] shouldBe "HW 82 78 56 C"
        (json \ "ids" \ "sautr").as[String] shouldBe testUtr
        (json \ "deceased").as[Boolean] shouldBe true
        (json \ "name" \ "current" \ "firstName").as[String] shouldBe "Alan Brian"
        (json \ "name" \ "current" \ "lastName").as[String] shouldBe "Foo-Foe"
      }
    }
  }
}
