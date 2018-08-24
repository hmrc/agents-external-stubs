package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.Instant

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Constraints, Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentmtdidentifiers.model.MtdItId
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class DesStubController @Inject()(
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  businessDetailsRecordsService: BusinessDetailsRecordsService
)(implicit usersService: UsersService)
    extends BaseController with DesCurrentSession {

  import DesStubController._

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[AuthoriseRequest] { payload =>
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
      GetRelationships.form.bindFromRequest.fold(
        hasErrors => badRequestF("INVALID_SUBMISSION", hasErrors.errors.map(_.message).mkString(", ")),
        query =>
          relationshipRecordsService
            .findByQuery(query, session.planetId)
            .map(records => Ok(Json.toJson(GetRelationships.Response.from(records))))
      )
    }(SessionRecordNotFound)
  }

  def getLegacyRelationshipsByUtr(utr: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validUtr(utr)
        .fold(
          error => badRequestF("INVALID_UTR", error),
          _ =>
            legacyRelationshipRecordsService
              .getLegacyRelationshipsByUtr(utr, session.planetId)
              .map(ninoWithAgentList => Ok(Json.toJson(GetLegacyRelationships.Response.from(ninoWithAgentList))))
        )
    }(SessionRecordNotFound)
  }

  def getLegacyRelationshipsByNino(nino: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validNinoNoSpaces(nino)
        .fold(
          error => badRequestF("INVALID_NINO", error),
          _ =>
            legacyRelationshipRecordsService
              .getLegacyRelationshipsByNino(nino, session.planetId)
              .map(ninoWithAgentList => Ok(Json.toJson(GetLegacyRelationships.Response.from(ninoWithAgentList))))
        )
    }(SessionRecordNotFound)
  }

  def getBusinessDetails(idType: String, idNumber: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      idType match {
        case "nino" =>
          RegexPatterns
            .validNinoNoSpaces(idNumber)
            .fold(
              error => badRequestF("INVALID_NINO", error),
              _ =>
                businessDetailsRecordsService
                  .getBusinessDetails(Nino(idNumber), session.planetId)
                  .map {
                    case Some(record) => Ok(Json.toJson(record))
                    case None         => notFound("NOT_FOUND_NINO")
                }
            )
        case "mtdbsa" =>
          RegexPatterns
            .validMtdbsa(idNumber)
            .fold(
              error => badRequestF("INVALID_MTDBSA", error),
              _ =>
                businessDetailsRecordsService
                  .getBusinessDetails(MtdItId(idNumber), session.planetId)
                  .map {
                    case Some(record) => Ok(Json.toJson(record))
                    case None         => notFound("NOT_FOUND_MTDBSA")
                }
            )
        case _ => notFoundF("ID_TYPE_NOT_SUPPORTED")
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

    import Validator._

    val validate: Validator[AuthoriseRequest] = Validator(
      check(_.acknowledgmentReference.matches("^\\S{1,32}$"), "Invalid acknowledgmentReference"),
      check(_.refNumber.matches("^[0-9A-Za-z]{1,15}$"), "Invalid refNumber"),
      check(_.idType.forall(_.matches("^[A-Z]{1,6}$")), "Invalid idType"),
      check(_.agentReferenceNumber.isRight(RegexPatterns.validArn), "Invalid agentReferenceNumber"),
      check(_.relationshipType.forall(_.matches("ZA01|ZA02")), "Invalid relationshipType"),
      check(_.authProfile.forall(_.matches("^\\S{1,32}$")), "Invalid authProfile"),
      check(_.authorisation.action.matches("Authorise|De-Authorise"), "Invalid action")
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

  object GetRelationships {

    private val queryConstraint: Constraint[RelationshipRecordQuery] = Constraint(
      q =>
        if (q.agent && q.arn.isEmpty) Invalid("Missing arn")
        else if (!q.agent && q.refNumber.isEmpty) Invalid("Missing ref-no")
        else if ((!q.activeOnly || q.to.isDefined) && q.from.isEmpty) Invalid("Missing from date")
        else if (!q.activeOnly && q.to.isEmpty) Invalid("Missing to date")
        else Valid)

    val form: Form[RelationshipRecordQuery] = Form[RelationshipRecordQuery](
      mapping(
        "regime" -> nonEmptyText.verifying(Constraints.pattern("^[A-Z]{3,10}$".r, "regime", "Invalid regime")),
        "arn"    -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validArn, "arn"))),
        "idtype" -> default(
          nonEmptyText.verifying(Constraints.pattern("^[A-Z]{1,6}$".r, "idtype", "Invalid idtype")),
          "none"),
        "ref-no" -> optional(
          nonEmptyText.verifying(Constraints.pattern("^[0-9A-Za-z]{1,15}$".r, "ref-no", "Invalid ref-no"))),
        "active-only" -> boolean,
        "agent"       -> boolean,
        "from" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "from")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString)),
        "to" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "to")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString))
      )(RelationshipRecordQuery.apply)(RelationshipRecordQuery.unapply).verifying(queryConstraint))

    case class Individual(firstName: String, lastName: String)

    case class Organisation(organisationName: String)

    case class Relationship(
      referenceNumber: String,
      agentReferenceNumber: String,
      dateFrom: LocalDate,
      dateTo: Option[LocalDate] = None,
      contractAccountCategory: String,
      activity: Option[String] = None,
      relationshipType: Option[String] = None,
      authProfile: Option[String] = None,
      individual: Option[Individual] = None,
      organisation: Option[Organisation] = None
    )

    object Relationship {
      implicit val writes1: Writes[Individual] = Json.writes[Individual]
      implicit val writes2: Writes[Organisation] = Json.writes[Organisation]
      implicit val writes3: Writes[Relationship] = Json.writes[Relationship]

      def from(record: RelationshipRecord): Relationship = Relationship(
        referenceNumber = record.refNumber,
        agentReferenceNumber = record.arn,
        dateFrom = record.startDate.getOrElse(throw new Exception("Missing startDate of relationship")),
        dateTo = record.endDate,
        contractAccountCategory = "33", // magic number!
        relationshipType = record.relationshipType,
        authProfile = record.authProfile,
        individual = decideIndividual(record),
        organisation = decideOrganisation(record)
      )

      def decideIndividual(record: RelationshipRecord): Option[Individual] =
        if (record.regime == "ITSA") {
          val nameParts: Array[String] =
            UserGenerator.nameForIndividual(record.idType + "/" + record.refNumber).split(" ")
          Some(Individual(nameParts.init.mkString(" "), nameParts.last))
        } else None

      def decideOrganisation(record: RelationshipRecord): Option[Organisation] =
        if (record.regime != "ITSA")
          Some(Organisation(UserGenerator.nameForOrganisation(record.idType + "/" + record.refNumber)))
        else None
    }

    case class Response(relationship: Seq[Relationship])

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]

      def from(records: Seq[RelationshipRecord]): Response =
        Response(relationship = records.map(Relationship.from))
    }
  }

  object GetLegacyRelationships {

    case class Response(agents: Seq[Response.LegacyAgent])

    object Response {
      def from(ninoWithAgentList: List[(String, LegacyAgentRecord)]): Response =
        Response(agents = ninoWithAgentList.map { case (nino, agent) => LegacyAgent.from(nino, agent) })

      case class LegacyAgent(
        id: String,
        nino: String,
        agentId: String,
        agentOwnRef: Option[String] = None,
        hasAgent: Option[Boolean] = None,
        isRegisteredAgent: Option[Boolean] = None,
        govAgentId: Option[String] = None,
        agentName: String,
        agentPhoneNo: Option[String] = None,
        address1: String,
        address2: String,
        address3: Option[String] = None,
        address4: Option[String] = None,
        postcode: Option[String] = None,
        isAgentAbroad: Boolean = false,
        agentCeasedDate: Option[String] = None
      )

      object LegacyAgent {

        def from(nino: String, a: LegacyAgentRecord): LegacyAgent = LegacyAgent(
          id = "",
          nino = nino,
          agentId = a.agentId,
          agentOwnRef = a.agentOwnRef,
          hasAgent = a.hasAgent,
          isRegisteredAgent = a.isRegisteredAgent,
          govAgentId = a.govAgentId,
          agentName = a.agentName,
          agentPhoneNo = a.agentPhoneNo,
          address1 = a.address1,
          address2 = a.address2,
          address3 = a.address3,
          address4 = a.address4,
          postcode = a.postcode,
          isAgentAbroad = a.isAgentAbroad,
          agentCeasedDate = a.agentCeasedDate
        )
      }

      implicit val formats1: Format[LegacyAgent] = Json.format[LegacyAgent]
      implicit val formats: Format[Response] = Json.format[Response]
    }
  }

}
