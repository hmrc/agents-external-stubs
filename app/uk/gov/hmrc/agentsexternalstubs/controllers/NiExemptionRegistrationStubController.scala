package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.Action
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, RegexPatterns, Validator}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, BusinessPartnerRecordsService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext

@Singleton
class NiExemptionRegistrationStubController @Inject()(
  val authenticationService: AuthenticationService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  ecp: ExecutionContextProvider)
    extends BaseController with CurrentSession {

  import NiExemptionRegistrationStubController._

  implicit val ec: ExecutionContext = ecp.get()

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
