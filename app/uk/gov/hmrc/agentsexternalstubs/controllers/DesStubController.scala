package uk.gov.hmrc.agentsexternalstubs.controllers

import cats.data.Validated
import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import play.api.mvc.Action
import uk.gov.hmrc.agentsexternalstubs.controllers.DesStubController.AuthoriseRequest
import uk.gov.hmrc.agentsexternalstubs.models.Validate
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class DesStubController @Inject()(val authenticationService: AuthenticationService)(implicit usersService: UsersService)
    extends BaseController with DesCurrentSession {

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withJsonBody[AuthoriseRequest] { payload =>
        AuthoriseRequest
          .validate(payload)
          .fold(
            error => badRequestF("INVALID_SUBMISSION", error.mkString(", ")),
            _ => Future.successful(Accepted)
          )
      }
    }(SessionRecordNotFound)
  }

}

object DesStubController {

  case class Authorisation(action: String, isExclusiveAgent: Option[Boolean])

  case class AuthoriseRequest(
    acknowledgmentReference: String,
    refNumber: String,
    idType: Option[String],
    agentReferenceNumber: String,
    regime: String,
    authorisation: Authorisation,
    relationshipType: Option[String],
    authProfile: Option[String]
  )

  object AuthoriseRequest {
    implicit val reads1: Reads[Authorisation] = Json.reads[Authorisation]
    implicit val reads2: Reads[AuthoriseRequest] = Json.reads[AuthoriseRequest]

    val validate: AuthoriseRequest => Validated[List[String], Unit] =
      Validate.constraints[AuthoriseRequest](
        (_.acknowledgmentReference.matches("^\\S{1,32}$"), "Invalid acknowledgmentReference"),
        (_.refNumber.matches("^[0-9A-Za-z]{1,15}$"), "Invalid refNumber"),
        (_.idType.forall(_.matches("^[0-9A-Za-z]{1,6}$")), "Invalid idType"),
        (_.agentReferenceNumber.matches("^[A-Z](ARN)[0-9]{7}$"), "Invalid agentReferenceNumber"),
        (_.relationshipType.forall(_.matches("ZA01|ZA02")), "Invalid relationshipType"),
        (_.authProfile.forall(_.matches("^\\S{1,32}$")), "Invalid authProfile"),
        (_.authorisation.action.matches("Authorise|De-Authorise"), "Invalid action")
      )

  }

}
