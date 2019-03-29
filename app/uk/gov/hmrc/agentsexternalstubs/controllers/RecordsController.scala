package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import play.mvc.Http.HeaderNames
import reactivemongo.api.Cursor
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.agentsexternalstubs.syntax._
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext

@Singleton
class RecordsController @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  relationshipRecordsService: RelationshipRecordsService,
  employerAuthRecordsService: EmployerAuthsRecordsService,
  recordsRepository: RecordsRepository,
  val authenticationService: AuthenticationService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  val getRecords: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      recordsRepository
        .findByPlanetId(session.planetId)
        .collect[List](1000, Cursor.FailOnError())
        .flatMap(list => okF(list.groupBy(Record.typeOf).mapValues(_.map(Record.toJson))))
    }(SessionRecordNotFound)
  }

  def getRecord(recordId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      recordsRepository
        .findById[Record](recordId, session.planetId)
        .map {
          case Some(record) => ok(Record.toJson(record))
          case None         => notFound("NOT_FOUND_RECORD_ID")
        }
    }(SessionRecordNotFound)
  }

  def updateRecord(recordId: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      recordsRepository
        .findById[Record](recordId, session.planetId)
        .flatMap {
          case None => notFoundF("NOT_FOUND_RECORD_ID")
          case Some(record) =>
            Record
              .fromJson(Record.typeOf(record), request.body) |> whenSuccess { payload =>
              recordsRepository
                .store(payload.withId(Some(recordId)), session.planetId)
                .map(id => Accepted.withHeaders(HeaderNames.LOCATION -> routes.RecordsController.getRecord(id).url))
            }
        }
    }(SessionRecordNotFound)
  }

  def deleteRecord(recordId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      recordsRepository
        .findById[Record](recordId, session.planetId)
        .flatMap {
          case Some(_) => recordsRepository.remove(recordId, session.planetId).map(_ => NoContent)
          case None    => notFoundF("NOT_FOUND_RECORD_ID")
        }
    }(SessionRecordNotFound)
  }

  def storeBusinessDetails(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[BusinessDetailsRecord](
        record =>
          businessDetailsRecordsService
            .store(record, autoFill, session.planetId)
            .map(recordId =>
              Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url))).withHeaders(
                HeaderNames.LOCATION -> routes.DesStubController.getBusinessDetails("mtdbsa", record.mtdbsa).url)))
    }(SessionRecordNotFound)
  }

  def generateBusinessDetails(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = BusinessDetailsRecord.seed(seed)
        val result = if (minimal) record else BusinessDetailsRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeBusinessDetails(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeLegacyAgent(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[LegacyAgentRecord](
        record =>
          legacyRelationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(recordId => Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url)))))
    }(SessionRecordNotFound)
  }

  def generateLegacyAgent(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = LegacyAgentRecord.seed(seed)
        val result = if (minimal) record else LegacyAgentRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeLegacyAgent(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeLegacyRelationship(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[LegacyRelationshipRecord](
          record =>
            legacyRelationshipRecordsService
              .store(record, autoFill, session.planetId)
              .map(
                recordId =>
                  Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url))).withHeaders(
                    HeaderNames.LOCATION -> record.nino
                      .map(nino => routes.DesStubController.getLegacyRelationshipsByNino(nino).url)
                      .orElse(record.utr.map(utr => routes.DesStubController.getLegacyRelationshipsByUtr(utr).url))
                      .getOrElse(""))))
      }(SessionRecordNotFound)
  }

  def generateLegacyRelationship(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = LegacyRelationshipRecord.seed(seed)
        val result = if (minimal) record else LegacyRelationshipRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeLegacyRelationship(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeVatCustomerInformation(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[VatCustomerInformationRecord](
          record =>
            vatCustomerInformationRecordsService
              .store(record, autoFill, session.planetId)
              .map(recordId =>
                Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url))).withHeaders(
                  HeaderNames.LOCATION -> routes.DesStubController.getVatCustomerInformation(record.vrn).url)))
      }(SessionRecordNotFound)
  }

  def generateVatCustomerInformation(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = VatCustomerInformationRecord.seed(seed)
        val result = if (minimal) record else VatCustomerInformationRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeVatCustomerInformation(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeBusinessPartnerRecord(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) {
    implicit request =>
      withCurrentSession { session =>
        withPayload[BusinessPartnerRecord](record =>
          businessPartnerRecordsService
            .store(record, autoFill, session.planetId)
            .map(recordId => Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url)))))
      }(SessionRecordNotFound)
  }

  def generateBusinessPartnerRecord(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = BusinessPartnerRecord.seed(seed)
        val result = if (minimal) record else BusinessPartnerRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeBusinessPartnerRecord(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeRelationship(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[RelationshipRecord](
        record =>
          relationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(recordId => Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url)))))
    }(SessionRecordNotFound)
  }

  def storeEmployerAuths(autoFill: Boolean): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[EmployerAuths](
        record =>
          employerAuthRecordsService
            .store(record, autoFill, session.planetId)
            .map(recordId => Created(RestfulResponse(Link("self", routes.RecordsController.getRecord(recordId).url)))))
    }(SessionRecordNotFound)
  }

  def generateRelationship(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = RelationshipRecord.seed(seed)
        val result = if (minimal) record else RelationshipRecord.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeRelationship(minimal).url))
      }(SessionRecordNotFound)
  }

  def generateEmployerAuths(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = EmployerAuths.seed(seed)
        val result = if (minimal) record else EmployerAuths.sanitize(seed)(record)
        okF(result, Link("create", routes.RecordsController.storeEmployerAuths(minimal).url))
      }(SessionRecordNotFound)
  }

}
