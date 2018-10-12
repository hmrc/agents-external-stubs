package uk.gov.hmrc.agentsexternalstubs.controllers

import java.net.URLDecoder

import akka.stream.Materializer
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, JsValue, Reads, Writes}
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.mvc.Http.HeaderNames
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.agentsexternalstubs.models.{Id, SpecialCase}
import uk.gov.hmrc.agentsexternalstubs.repository.SpecialCasesRepository
import uk.gov.hmrc.agentsexternalstubs.services.AuthenticationService
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.ExecutionContext

@Singleton
class SpecialCasesController @Inject()(
  specialCasesRepository: SpecialCasesRepository,
  val authenticationService: AuthenticationService)(implicit materializer: Materializer)
    extends BaseController with CurrentSession {

  implicit val reads: Reads[SpecialCase] = SpecialCase.external.reads
  implicit val writes: Writes[SpecialCase] = SpecialCase.external.writes

  val getAllSpecialCases: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.findByPlanetId(session.planetId)(1000).map {
        case sc if sc.nonEmpty => ok(sc)
        case _                 => NoContent
      }
    }(SessionRecordNotFound)
  }

  def getSpecialCase(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.findById(id, session.planetId).map {
        case Some(specialCase) => ok(specialCase)
        case None              => notFound("NOT_FOUND")
      }
    }(SessionRecordNotFound)
  }

  def createSpecialCase: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[SpecialCase](
        specialCase =>
          specialCasesRepository
            .upsert(specialCase, session.planetId)
            .map(id =>
              Created(s"Special case $id has been created.")
                .withHeaders(HeaderNames.LOCATION -> routes.SpecialCasesController.getSpecialCase(id).url))
            .recover {
              case e: DatabaseException if e.message.contains("E11000") =>
                Conflict(e.getMessage())
          }
      )
    }(SessionRecordNotFound)
  }

  def updateSpecialCase(id: String): Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[SpecialCase](
        specialCase =>
          specialCasesRepository.findById(id, session.planetId).flatMap {
            case None => notFoundF("NOT_FOUND")
            case Some(_) =>
              specialCasesRepository
                .upsert(specialCase.copy(id = Some(Id(id))), session.planetId)
                .map(id =>
                  Accepted(s"Special case $id has been updated.")
                    .withHeaders(HeaderNames.LOCATION -> routes.SpecialCasesController.getSpecialCase(id).url))

        }
      )
    }(SessionRecordNotFound)
  }

  def deleteSpecialCase(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      specialCasesRepository.delete(id, session.planetId).map(_ => NoContent)
    }(SessionRecordNotFound)
  }

  final def maybeSpecialCase(action: EssentialAction): EssentialAction = new EssentialAction {

    implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.defaultContext

    override def apply(rh: RequestHeader): Accumulator[ByteString, Result] =
      Accumulator.flatten(withPlanetId { planetId =>
        val key = SpecialCase.matchKey(rh.method, URLDecoder.decode(rh.path, "utf-8"))
        specialCasesRepository.findByMatchKey(key, planetId).map {
          case None => action(rh)
          case Some(specialCase) =>
            Accumulator.done(specialCase.response.asResult)
        }
      }(Request(rh, ()), ec, hc(rh)))
  }

}

object SpecialCasesController {}
