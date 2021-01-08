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
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, RegexPatterns, Validator}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, BusinessPartnerRecordsService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class NiExemptionRegistrationStubController @Inject()(
  val authenticationService: AuthenticationService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import NiExemptionRegistrationStubController._

  def niBusinesses(utr: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[NiBusinessesPayload] { payload =>
        Utr.isValid(utr) match {
          case false => conflictF("INVALID_UTR", s"Provided UTR $utr is not valid")
          case true =>
            NiBusinessesPayload
              .validate(payload)
              .fold(
                error => conflictF("INVALID_PAYLOAD", error.mkString(", ")),
                _ =>
                  businessPartnerRecordsService.getBusinessPartnerRecord(Utr(utr), session.planetId).map {
                    case None => conflict("NOT_FOUND", s"No business partner record found for the supplied UTR: $utr")
                    case Some(record) =>
                      record.addressDetails match {
                        case address: BusinessPartnerRecord.UkAddress
                            if postalCodesMatches(address.postalCode, payload.postcode) =>
                          ok(NiBussinessesResponse.from(record))
                        case _ =>
                          conflict(
                            "NOT_MATCHES",
                            s"Post Code: ${payload.postcode} in request does not match the business post code")
                      }
                }
              )
        }
      }
    }(SessionRecordNotFound)
  }

}

object NiExemptionRegistrationStubController {

  case class NiBusinessesPayload(postcode: String)

  object NiBusinessesPayload {

    import Validator._

    val validate: Validator[NiBusinessesPayload] = Validator(
      check(_.postcode.isRight(RegexPatterns.validPostcode), "Invalid postcode")
    )

    implicit val formats: Format[NiBusinessesPayload] = Json.format[NiBusinessesPayload]
  }

  case class NiBussinessesSubscription(status: String, eori: Option[String])
  case class NiBussinessesResponse(name: String, subscription: NiBussinessesSubscription)

  object NiBussinessesResponse {

    def from(record: BusinessPartnerRecord): NiBussinessesResponse = {
      val name =
        record.organisation
          .map(_.organisationName)
          .orElse(record.individual.map(i => s"${i.firstName} ${i.lastName}"))
          .getOrElse("")
      val eoriOpt = record.eori
      val status = if (eoriOpt.isDefined) "NI_SUBSCRIBED" else "NI_NOT_SUBSCRIBED"
      val subscription = NiBussinessesSubscription(status, eoriOpt)
      NiBussinessesResponse(name, subscription)
    }

    implicit val formats1: Format[NiBussinessesSubscription] = Json.format[NiBussinessesSubscription]
    implicit val formats2: Format[NiBussinessesResponse] = Json.format[NiBussinessesResponse]
  }

  private def canonical(str: String): String = str.toUpperCase.replace(" ", "")
  def postalCodesMatches(postCode1: String, postCode2: String): Boolean = canonical(postCode1) == canonical(postCode2)

}
