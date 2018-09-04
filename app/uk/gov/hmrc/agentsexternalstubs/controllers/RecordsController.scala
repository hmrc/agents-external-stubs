package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, BusinessDetailsRecordsService, LegacyRelationshipRecordsService, VatCustomerInformationRecordsService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

@Singleton
class RecordsController @Inject()(
  businessDetailsRecordsService: BusinessDetailsRecordsService,
  legacyRelationshipRecordsService: LegacyRelationshipRecordsService,
  vatCustomerInformationRecordsService: VatCustomerInformationRecordsService,
  recordsRepository: RecordsRepository,
  val authenticationService: AuthenticationService)
    extends BaseController with CurrentSession {

  val getRecords: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      recordsRepository
        .findAll(session.planetId)
        .collect[List](1000)
        .flatMap(list => ok(list.groupBy(_.getClass.getSimpleName).mapValues(_.map(Record.toJson))))
    }(SessionRecordNotFound)
  }

  def storeBusinessDetails(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[BusinessDetailsRecord](
        record =>
          businessDetailsRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ =>
              Created.withHeaders(
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
        ok(result, Link("create", routes.RecordsController.storeBusinessDetails(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeLegacyAgent(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[LegacyAgentRecord](
        record =>
          legacyRelationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ => Created))
    }(SessionRecordNotFound)
  }

  def generateLegacyAgent(seedOpt: Option[String], minimal: Boolean): Action[AnyContent] = Action.async {
    implicit request =>
      withCurrentSession { session =>
        val seed = seedOpt.getOrElse(session.sessionId)
        implicit val optionGenStrategy: Generator.OptionGenStrategy = Generator.AlwaysSome
        val record = LegacyAgentRecord.seed(seed)
        val result = if (minimal) record else LegacyAgentRecord.sanitize(seed)(record)
        ok(result, Link("create", routes.RecordsController.storeLegacyAgent(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeLegacyRelationship(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[LegacyRelationshipRecord](
        record =>
          legacyRelationshipRecordsService
            .store(record, autoFill, session.planetId)
            .map(
              _ =>
                Created.withHeaders(
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
        ok(result, Link("create", routes.RecordsController.storeLegacyRelationship(minimal).url))
      }(SessionRecordNotFound)
  }

  def storeVatCustomerInformation(autoFill: Boolean): Action[JsValue] = Action.async(parse.json) { implicit request =>
    withCurrentSession { session =>
      withPayload[VatCustomerInformationRecord](
        record =>
          vatCustomerInformationRecordsService
            .store(record, autoFill, session.planetId)
            .map(_ =>
              Created.withHeaders(
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
        ok(result, Link("create", routes.RecordsController.storeVatCustomerInformation(minimal).url))
      }(SessionRecordNotFound)
  }

}
