package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.controllers.GetCitizenResponse.{Ids, Name, Names}
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class CitizenDetailsStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService)
    extends BaseController with CurrentSession {

  def getCitizen(idName: String, taxId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      idName match {
        case "nino" =>
          Nino.isValid(taxId) match {
            case false => Future.successful(BadRequest(s"Provided NINO $taxId is not valid"))
            case true =>
              usersService.findByNino(taxId, session.planetId).map {
                case None       => NotFound(s"Citizen record for $idName=$taxId not found")
                case Some(user) => Ok(Json.toJson(toGetCitizenResponse(user)))
              }
          }
        case _ => Future.successful(BadRequest(s"tax identifier $idName not supported"))
      }
    }(SessionRecordNotFound)
  }

  def toGetCitizenResponse(user: User): GetCitizenResponse =
    GetCitizenResponse(
      name = Names(current = Name("John", Some("Smith"))),
      ids = Ids(nino = user.nino),
      dateOfBirth = "11121971")

}

/**
  * {
  *   "name": {
  *     "current": {
  *       "firstName": "John",
  *       "lastName": "Smith"
  *     },
  *     "previous": []
  *   },
  *   "ids": {
  *     "nino": "AA055075C"
  *   },
  *   "dateOfBirth": "11121971"
  * }
  */
case class GetCitizenResponse(name: GetCitizenResponse.Names, ids: GetCitizenResponse.Ids, dateOfBirth: String)

object GetCitizenResponse {

  case class Name(firstName: String, lastName: Option[String] = None)
  case class Names(current: Name, previous: Seq[Name] = Seq.empty)
  case class Ids(nino: Option[Nino])

  implicit val formats1: Format[Name] = Json.format[Name]
  implicit val formats2: Format[Names] = Json.format[Names]
  implicit val formats3: Format[Ids] = Json.format[Ids]
  implicit val formats4: Format[GetCitizenResponse] = Json.format[GetCitizenResponse]

}
