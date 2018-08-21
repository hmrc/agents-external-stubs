package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.Instant

import cats.data.Validated
import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDateTime
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Constraints, Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.DesStubController.{AuthoriseRequest, GetRelationshipQuery}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthoriseResponse, RelationshipRecord, Validate}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, RelationshipRecordsService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class DesStubController @Inject()(
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService)(implicit usersService: UsersService)
    extends BaseController with DesCurrentSession {

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withJsonBody[AuthoriseRequest] { payload =>
        AuthoriseRequest
          .validate(payload)
          .fold(
            error => badRequestF("INVALID_SUBMISSION", error.mkString(", ")),
            _ =>
              if (payload.authorisation.action == "Authorise")
                relationshipRecordsService
                  .authorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
              else
                relationshipRecordsService
                  .deAuthorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
          )
      }
    }(SessionRecordNotFound)
  }

  def getRelationship(
    idtype: Option[String],
    `ref-no`: Option[String],
    arn: Option[String],
    agent: Boolean,
    `active-only`: Boolean,
    regime: String,
    from: Option[String],
    to: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      GetRelationshipQuery.form.bindFromRequest.fold(
        hasErrors => badRequestF("INVALID_SUBMISSION", hasErrors.errors.map(_.message).mkString(", ")),
        query => Future.successful(Ok("NOT_IMPLEMENTED"))
      )
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
        (_.idType.forall(_.matches("^[A-Z]{1,6}$")), "Invalid idType"),
        (_.agentReferenceNumber.matches("^[A-Z](ARN)[0-9]{7}$"), "Invalid agentReferenceNumber"),
        (_.relationshipType.forall(_.matches("ZA01|ZA02")), "Invalid relationshipType"),
        (_.authProfile.forall(_.matches("^\\S{1,32}$")), "Invalid authProfile"),
        (_.authorisation.action.matches("Authorise|De-Authorise"), "Invalid action")
      )

    def toRelationshipRecord(r: AuthoriseRequest): RelationshipRecord =
      RelationshipRecord(
        regime = r.regime,
        arn = r.agentReferenceNumber,
        idType = r.idType.getOrElse("none"),
        refNumber = r.refNumber,
        active = false,
        relationshipType = r.relationshipType,
        authProfile = r.authProfile
      )

  }

  case class AuthoriseResponse(processingDate: Instant = Instant.now())

  object AuthoriseResponse {
    implicit val writes: Writes[AuthoriseResponse] = Json.writes[AuthoriseResponse]
  }

  case class GetRelationshipQuery(
    idtype: Option[String],
    `ref-no`: Option[String],
    arn: Option[String],
    agent: Boolean,
    `active-only`: Boolean,
    regime: String,
    from: Option[String],
    to: Option[String])

  object GetRelationshipQuery {
    implicit val format: Format[GetRelationshipQuery] = Json.format[GetRelationshipQuery]

    private val queryConstraint: Constraint[GetRelationshipQuery] = Constraint(
      q =>
        if (q.agent && q.arn.isEmpty) Invalid("Missing arn")
        else if (!q.agent && q.`ref-no`.isEmpty) Invalid("Missing ref-no")
        else if ((!q.`active-only` || q.to.isDefined) && q.from.isEmpty) Invalid("Missing from date")
        else if (!q.`active-only` && q.to.isEmpty) Invalid("Missing to date")
        else Valid)

    val form: Form[GetRelationshipQuery] = Form[GetRelationshipQuery](
      mapping(
        "idtype" -> optional(nonEmptyText.verifying(Constraints.pattern("^[A-Z]{1,6}$".r, "idtype", "Invalid idtype"))),
        "ref-no" -> optional(
          nonEmptyText.verifying(Constraints.pattern("^[0-9A-Za-z]{1,15}$".r, "ref-no", "Invalid ref-no"))),
        "arn"         -> optional(nonEmptyText.verifying(Constraints.pattern("^[A-Z]ARN[0-9]{7}$".r, "arn", "Invalid arn"))),
        "agent"       -> boolean,
        "active-only" -> boolean,
        "regime"      -> nonEmptyText.verifying(Constraints.pattern("^[A-Z]{3,10}$".r, "regime", "Invalid regime")),
        "from" -> optional(nonEmptyText.verifying(Constraints.pattern(
          "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r,
          "from",
          "Invalid from date"
        ))),
        "to" -> optional(nonEmptyText.verifying(Constraints.pattern(
          "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-\n]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r,
          "to",
          "Invalid to date"
        )))
      )(GetRelationshipQuery.apply)(GetRelationshipQuery.unapply).verifying(queryConstraint))

  }

}
