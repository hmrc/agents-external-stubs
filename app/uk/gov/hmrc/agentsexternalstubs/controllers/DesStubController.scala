package uk.gov.hmrc.agentsexternalstubs.controllers

import java.time.Instant

import javax.inject.{Inject, Singleton}
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Constraints, Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Utr}
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessPartnerRecord, SubscribeAgentServicesPayload, _}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DesStubController @Inject()(
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  recordsRepository: RecordsRepository
)(implicit usersService: UsersService)
    extends BaseController with DesCurrentSession {

  import DesStubController._

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[CreateUpdateAgentRelationshipPayload] { payload =>
        CreateUpdateAgentRelationshipPayload
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
      withValidIdentifier(idType, idNumber) {
        case ("nino", nino) =>
          businessDetailsRecordsService
            .getBusinessDetails(Nino(nino), session.planetId)
            .map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => notFound("NOT_FOUND_NINO")
            }
        case ("mtdbsa", mtdbsa) =>
          businessDetailsRecordsService
            .getBusinessDetails(MtdItId(mtdbsa), session.planetId)
            .map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => notFound("NOT_FOUND_MTDBSA")
            }
      }
    }(SessionRecordNotFound)
  }

  def getBusinessPartnerRecord(idType: String, idNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        withValidIdentifier(idType, idNumber) {
          case ("arn", arn) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Arn(arn), session.planetId)
              .map {
                case Some(record) => Ok(Json.toJson(record))
                case None         => notFound("NOT_FOUND")
              }
          case ("utr", utr) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Utr(utr), session.planetId)
              .map {
                case Some(record) => Ok(Json.toJson(record))
                case None         => notFound("NOT_FOUND")
              }
        }
      }(SessionRecordNotFound)
  }

  def getVatCustomerInformation(vrn: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validVrn(vrn)
        .fold(
          error => badRequestF("INVALID_VRN", error),
          _ =>
            vatCustomerInformationRecordsService.getCustomerInformation(vrn, session.planetId).map {
              case Some(record) => Ok(Json.toJson(record))
              case None         => Ok(Json.obj())
          }
        )
    }(SessionRecordNotFound)
  }

  def subscribeAgentServices(utr: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      RegexPatterns
        .validUtr(utr)
        .fold(
          error => badRequestF("INVALID_UTR", error),
          _ =>
            withPayload[SubscribeAgentServicesPayload] { payload =>
              SubscribeAgentServicesPayload
                .validate(payload)
                .fold(
                  error => badRequestF("INVALID_PAYLOAD", error.mkString(", ")),
                  _ =>
                    businessPartnerRecordsService.getBusinessPartnerRecord(Utr(utr), session.planetId).flatMap {
                      case None => badRequestF("NOT_FOUND")
                      case Some(existingRecord) =>
                        businessPartnerRecordsService
                          .store(
                            SubscribeAgentServices.toBusinessPartnerRecord(payload, existingRecord),
                            autoFill = false,
                            session.planetId)
                          .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, session.planetId))
                          .map {
                            case Some(record) =>
                              ok(SubscribeAgentServices.Response(record.safeId, record.agentReferenceNumber.get))
                            case None =>
                              internalServerError("SERVER_ERROR", "BusinessPartnerRecord creation failed silently.")
                          }
                  }
                )
          }
        )

    }(SessionRecordNotFound)
  }

  def register(idType: String, idNumber: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[RegistrationPayload] { payload =>
        withValidIdentifier(idType, idNumber) {
          case ("utr", utr) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Utr(utr), session.planetId)
              .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record =>
                record.isAnAgent == payload.isAnAgent))
          case ("nino", nino) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecord(Nino(nino), session.planetId)
              .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record =>
                record.isAnAgent == payload.isAnAgent))
          case ("eori", eori) =>
            businessPartnerRecordsService
              .getBusinessPartnerRecordByEori(eori, session.planetId)
              .flatMap(getOrCreateBusinessPartnerRecord(payload, idType, idNumber, session.planetId)(record =>
                record.isAnAgent == payload.isAnAgent))
        }
      }
    }(SessionRecordNotFound)
  }

  private def getOrCreateBusinessPartnerRecord[T <: Record](
    payload: RegistrationPayload,
    idType: String,
    idNumber: String,
    planetId: String)(matches: T => Boolean)(implicit ec: ExecutionContext): Option[T] => Future[Result] = {
    case Some(record) =>
      if (matches(record)) okF(record, Registration.fixSchemaDifferences _)
      else notFoundF("NOT_FOUND", "BusinessPartnerRecord exists but fails match expectations.")
    case None =>
      if (payload.organisation.isDefined || payload.individual.isDefined) {
        businessPartnerRecordsService
          .store(Registration.toBusinessPartnerRecord(payload, idType, idNumber), autoFill = false, planetId)
          .flatMap(id => recordsRepository.findById[BusinessPartnerRecord](id, planetId))
          .map {
            case Some(record: T) =>
              if (matches(record)) ok(record, Registration.fixSchemaDifferences _)
              else internalServerError("SERVER_ERROR", "Created BusinessPartnerRecord fails match expectations.")
            case _ =>
              internalServerError("SERVER_ERROR", "BusinessPartnerRecord creation failed silently.")
          }
      } else notFoundF("NOT_FOUND")
  }

  private def withValidIdentifier(idType: String, idNumber: String)(
    pf: PartialFunction[(String, String), Future[Result]])(implicit ec: ExecutionContext): Future[Result] =
    idType match {
      case "nino"   => validateIdentifier(RegexPatterns.validNinoNoSpaces, "INVALID_NINO", idType, idNumber)(pf)
      case "mtdbsa" => validateIdentifier(RegexPatterns.validMtdbsa, "INVALID_MTDBSA", idType, idNumber)(pf)
      case "utr"    => validateIdentifier(RegexPatterns.validUtr, "INVALID_UTR", idType, idNumber)(pf)
      case "arn"    => validateIdentifier(RegexPatterns.validArn, "INVALID_ARN", idType, idNumber)(pf)
      case "vrn"    => validateIdentifier(RegexPatterns.validVrn, "INVALID_VRN", idType, idNumber)(pf)
      case "eori"   => validateIdentifier(RegexPatterns.validEori, "INVALID_EORI", idType, idNumber)(pf)
      case _        => badRequestF("INVALID_IDTYPE")
    }

  private def validateIdentifier(matcher: RegexPatterns.Matcher, errorCode: String, idType: String, idNumber: String)(
    pf: PartialFunction[(String, String), Future[Result]]): Future[Result] =
    matcher(idNumber).fold(
      error => badRequestF(errorCode, error),
      _ =>
        if (pf.isDefinedAt((idType, idNumber))) pf((idType, idNumber))
        else badRequestF(errorCode, "Unsupported identifier type")
    )

}

object DesStubController {

  object AuthoriseRequest {

    def toRelationshipRecord(r: CreateUpdateAgentRelationshipPayload): RelationshipRecord =
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

  object SubscribeAgentServices {

    def toBusinessPartnerRecord(
      payload: SubscribeAgentServicesPayload,
      existingRecord: BusinessPartnerRecord): BusinessPartnerRecord = {
      val address = payload.agencyAddress match {
        case SubscribeAgentServicesPayload.UkAddress(l1, l2, l3, l4, pc, cc) =>
          BusinessPartnerRecord.UkAddress(l1, l2, l3, l4, pc, cc)
        case SubscribeAgentServicesPayload.ForeignAddress(l1, l2, l3, l4, pc, cc) =>
          BusinessPartnerRecord.ForeignAddress(l1, l2, l3, l4, pc, cc)
      }
      existingRecord
        .modifyAgentReferenceNumber {
          case None => Some(Generator.arn(existingRecord.utr.get).value)
        }
        .withAgencyDetails(
          Some(
            BusinessPartnerRecord
              .AgencyDetails()
              .withAgencyName(Option(payload.agencyName))
              .withAgencyAddress(Some(address))
              .withAgencyEmail(payload.agencyEmail)))
        .modifyContactDetails {
          case Some(contactDetails) =>
            Some(
              contactDetails
                .withPhoneNumber(payload.telephoneNumber)
                .withEmailAddress(payload.agencyEmail)
            )
        }
        .withAddressDetails(address)
        .withIsAnAgent(true)
        .withIsAnASAgent(true)
    }

    case class Response(safeId: String, agentRegistrationNumber: String)

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]
    }
  }

  object Registration {

    def toBusinessPartnerRecord(payload: RegistrationPayload, idType: String, idNumber: String): BusinessPartnerRecord =
      BusinessPartnerRecord
        .seed(idNumber)
        .withNino(if (idType == "nino") Some(idNumber) else None)
        .withUtr(if (idType == "utr") Some(idNumber) else None)
        .withEori(if (idType == "eori") Some(idNumber) else None)
        .withIsAnIndividual(payload.individual.isDefined)
        .withIsAnOrganisation(payload.organisation.isDefined)
        .withIsAnAgent(payload.isAnAgent)
        .withIsAnASAgent(false)
        .withIndividual(
          payload.individual.map(
            i =>
              BusinessPartnerRecord.Individual
                .seed(idNumber)
                .withFirstName(i.firstName)
                .withLastName(i.lastName)
                .modifyDateOfBirth { case dob => i.dateOfBirth.getOrElse(dob) }
          )
        )
        .withOrganisation(
          payload.organisation.map(
            o =>
              BusinessPartnerRecord.Organisation
                .seed(idNumber)
                .withOrganisationName(o.organisationName)
                .withIsAGroup(false)
                .withOrganisationType(o.organisationType)))

    def fixSchemaDifferences(value: JsValue): JsValue = value match {
      case obj: JsObject =>
        (obj \ "addressDetails").asOpt[JsObject] match {
          case Some(address) => obj.-("addressDetails").+("address", address)
          case None          => obj
        }
      case other => other
    }

  }

}
