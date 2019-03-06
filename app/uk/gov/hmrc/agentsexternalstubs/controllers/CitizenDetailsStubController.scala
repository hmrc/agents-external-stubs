package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.CitizenDetailsStubController.GetCitizenResponse
import uk.gov.hmrc.agentsexternalstubs.models.User
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.controller.{BackendController, BaseController}

import scala.concurrent.ExecutionContext

@Singleton
class CitizenDetailsStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  cc: ControllerComponents)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getCitizen(idName: String, taxId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      idName match {
        case "nino" =>
          Nino.isValid(taxId) match {
            case false => badRequestF("INVALID_NINO", s"Provided NINO $taxId is not valid")
            case true =>
              usersService.findByNino(taxId, session.planetId).map {
                case None       => notFound("CITIZEN_RECORD_NOT_FOUND", s"Citizen record for $idName=$taxId not found")
                case Some(user) => Ok(RestfulResponse(GetCitizenResponse.from(user)))
              }
          }
        case _ => badRequestF("TAX_IDENTIFIER_NOT_SUPPORTED", s"tax identifier $idName not supported")
      }
    }(SessionRecordNotFound)
  }

}

object CitizenDetailsStubController {

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
  case class GetCitizenResponse(
    name: GetCitizenResponse.Names,
    ids: GetCitizenResponse.Ids,
    dateOfBirth: Option[String])

  object GetCitizenResponse {

    case class Name(firstName: String, lastName: Option[String] = None)
    case class Names(current: Name, previous: Seq[Name] = Seq.empty)
    case class Ids(nino: Option[Nino])

    implicit val formats1: Format[Name] = Json.format[Name]
    implicit val formats2: Format[Names] = Json.format[Names]
    implicit val formats3: Format[Ids] = Json.format[Ids]
    implicit val formats4: Format[GetCitizenResponse] = Json.format[GetCitizenResponse]

    private def convertName(name: Option[String]): Name =
      name
        .map(n => {
          val nameParts = n.split(" ")
          val (fn, ln) = if (nameParts.length > 1) {
            (nameParts.init.mkString(" "), Some(nameParts.last))
          } else (nameParts.headOption.getOrElse("John"), Some("Doe"))
          Name(fn, ln)
        })
        .getOrElse(Name("John", Some("Doe")))

    def from(user: User): GetCitizenResponse =
      GetCitizenResponse(
        name = Names(current = convertName(user.name)),
        ids = Ids(nino = user.nino),
        dateOfBirth = user.dateOfBirth.map(_.toString("ddMMyyyy"))
      )

  }
}
