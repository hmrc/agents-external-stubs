package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.agentsexternalstubs.models.{AuthenticatedSession, Generator, User}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext
@Singleton
class UserDetailsStubController @Inject()(
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  ecp: ExecutionContextProvider)
    extends BaseController with CurrentSession {

  import UserDetailsStubController._

  implicit val ec: ExecutionContext = ecp.get()

  def getUser(id: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      usersService.findByUserId(id, session.planetId).map {
        case None       => notFound("NOT_FOUND", s"User details are not found")
        case Some(user) => Ok(RestfulResponse(GetUserResponse.from(user, session)))
      }
    }(SessionRecordNotFound)
  }

}

object UserDetailsStubController {

  /**
  {
    "name":"test",
    "email":"test@test.com",
    "affinityGroup" : "affinityGroup",
    "description" : "description",
    "lastName":"test",
    "dateOfBirth":"1980-06-30",
    "postCode":"NW94HD",
    "authProviderId": "12345-PID",
    "authProviderType": "Verify"
  }

    or for a gateway user that's an agent

  {
    "authProviderId" : "12345-credId",
    "authProviderType" : "GovernmentGateway",
    "name" : "test",
    "email" : "test@test.com",
    "affinityGroup" : "Agent",
    "agentCode" : "TZRXXV",
    "agentFriendlyName" : "Bodgitt & Legget LLP",
    "agentId": "BDGL",
    "credentialRole" : "admin",
    "description" : "blah"
  }
 **/
  case class GetUserResponse(
    authProviderId: String,
    authProviderType: String,
    name: String,
    email: String,
    affinityGroup: String,
    credentialRole: String,
    description: String,
    lastName: Option[String] = None,
    postCode: Option[String] = None,
    dateOfBirth: Option[String] = None,
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None)

  object GetUserResponse {
    implicit val writes: Writes[GetUserResponse] = Json.writes[GetUserResponse]

    def from(user: User, session: AuthenticatedSession): GetUserResponse = GetUserResponse(
      authProviderId = user.userId,
      authProviderType = session.providerType,
      name = (if (user.affinityGroup.contains(User.AG.Individual)) user.firstName else user.name).getOrElse("John Doe"),
      lastName = if (user.affinityGroup.contains(User.AG.Individual)) user.lastName else None,
      email = Generator.email(user.userId),
      affinityGroup = user.affinityGroup.getOrElse("none"),
      agentCode = user.agentCode,
      agentFriendlyName = user.agentFriendlyName,
      agentId = user.agentId,
      credentialRole = user.credentialRole.getOrElse("User"),
      description = s"Agent Stubs test user on the planet ${user.planetId.getOrElse("?")}",
      postCode = user.address.flatMap(_.postcode),
      dateOfBirth = user.dateOfBirth.map(_.toString("yyyy-MM-dd"))
    )
  }

}
