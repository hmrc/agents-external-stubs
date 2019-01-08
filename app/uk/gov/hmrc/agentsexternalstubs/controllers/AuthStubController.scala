package uk.gov.hmrc.agentsexternalstubs.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.concurrent.ExecutionContextProvider
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.agentsexternalstubs.connectors.AgentAccessControlConnector
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthStubController @Inject()(
  authenticationService: AuthenticationService,
  usersService: UsersService,
  agentAccessControlConnector: AgentAccessControlConnector,
  ecp: ExecutionContextProvider)
    extends BaseController {

  import AuthStubController._

  implicit val ec: ExecutionContext = ecp.get()

  val authorise: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authenticationService.findByAuthTokenOrLookupExternal(authToken)
          response <- request.body.validate[AuthoriseRequest] match {
                       case JsSuccess(authoriseRequest, _) =>
                         maybeSession match {
                           case Some(authenticatedSession) =>
                             for {
                               maybeUser <- usersService
                                             .findByUserId(authenticatedSession.userId, authenticatedSession.planetId)
                               result <- Future(maybeUser match {
                                          case Some(user) =>
                                            Authorise.prepareAuthoriseResponse(
                                              FullAuthoriseContext(
                                                user,
                                                authenticatedSession,
                                                authoriseRequest,
                                                agentAccessControlConnector))
                                          case None =>
                                            Left("SessionRecordNotFound")
                                        }) map (_.fold(
                                          error => unauthorized(error),
                                          response => Ok(Json.toJson(response))))
                             } yield result
                           case None =>
                             unauthorizedF("SessionRecordNotFound")
                         }
                       case JsError(errors) =>
                         Future.successful(
                           BadRequest(errors
                             .map { case (p, ve) => s"$p -> [${ve.map(v => v.message).mkString(",")}]" }
                             .mkString("\n")))
                     }
        } yield response
      case Some(token) =>
        Logger(getClass).warn(s"Unsupported bearer token format $token")
        unauthorizedF("InvalidBearerToken")
      case None =>
        unauthorizedF("MissingBearerToken")
    }
  }

  private def withAuthorisedUserAndSession(body: (User, AuthenticatedSession) => Future[Result])(
    implicit request: Request[AnyContent]): Future[Result] =
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authenticationService.findByAuthTokenOrLookupExternal(authToken)
          result <- maybeSession match {
                     case Some(authenticatedSession) =>
                       for {
                         maybeUser <- usersService
                                       .findByUserId(authenticatedSession.userId, authenticatedSession.planetId)
                         result <- maybeUser match {
                                    case Some(user) => body(user, authenticatedSession)
                                    case None =>
                                      unauthorizedF("UserRecordNotFound")
                                  }
                       } yield result
                     case None =>
                       unauthorizedF("SessionRecordNotFound")
                   }
        } yield result
      case Some(token) =>
        Logger(getClass).warn(s"Unsupported bearer token format $token")
        unauthorizedF("InvalidBearerToken")
      case None =>
        unauthorizedF("MissingBearerToken")
    }

  val getAuthority: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedUserAndSession { (user, session) =>
      Future.successful(Ok(Json.toJson(Authority.prepareAuthorityResponse(user, session))))
    }
  }

  val getIds: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedUserAndSession { (user, _) =>
      Future.successful(Ok(Json.toJson(Authority.prepareIdsResponse(user))))
    }
  }

  val getEnrolments: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedUserAndSession { (user, _) =>
      Future.successful(Ok(Json.toJson(Authority.prepareEnrolmentsResponse(user))))
    }
  }

  private def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  private def unauthorized(reason: String): Result =
    Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")
}

object AuthStubController {

  object Authorise {

    def prepareAuthoriseResponse(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
      checkPredicates(context).fold(error => Left(error), _ => retrieveDetails(context))

    def checkPredicates(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Either[String, Unit] =
      context.request.authorise.foldLeft[Either[String, Unit]](Right(()))(
        (result, p: Predicate) => result.fold(error => Left(error), _ => p.validate(context))
      )

    def retrieveDetails(context: FullAuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
      context.request.retrieve.foldLeft[Retrieve.MaybeResponse](Right(AuthoriseResponse()))((result, r: String) =>
        result.fold(error => Left(error), response => addDetailToResponse(response, r, context)))

    def addDetailToResponse(response: AuthoriseResponse, retrieve: String, context: AuthoriseContext)(
      implicit ex: ExecutionContext): Retrieve.MaybeResponse =
      Retrieve.of(retrieve).fill(response, context)
  }

  object Authority {

    def prepareAuthorityResponse(user: User, session: AuthenticatedSession): Response = Response(
      uri = s"/auth/oid/${user.userId}",
      confidenceLevel = user.confidenceLevel.getOrElse(50),
      credentialStrength = user.credentialStrength.getOrElse("weak"),
      userDetailsLink = s"/user-details/id/${user.userId}",
      legacyOid = user.userId,
      ids = s"/auth/_ids",
      lastUpdated = "2017-02-14T11:23:52.955Z",
      loggedInAt = "2017-02-14T11:23:52.955Z",
      enrolments = s"/auth/_enrolments",
      affinityGroup = user.affinityGroup.getOrElse("none"),
      correlationId = UUID.randomUUID().toString,
      credId = user.userId,
      credentials = Some(Credentials(user.userId)),
      accounts = Accounts.from(user)
    )

    case class Response(
      uri: String,
      confidenceLevel: Int,
      credentialStrength: String,
      userDetailsLink: String,
      legacyOid: String,
      ids: String,
      lastUpdated: String,
      loggedInAt: String,
      enrolments: String,
      affinityGroup: String,
      correlationId: String,
      credId: String,
      credentials: Option[Credentials],
      accounts: Option[Accounts])

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]
    }

    case class Credentials(gatewayId: String)

    object Credentials {
      implicit val writes: Writes[Credentials] = Json.writes[Credentials]
    }

    case class Accounts(agent: Option[AgentAccount])

    object Accounts {

      def from(user: User): Option[Accounts] = user.affinityGroup match {
        case Some(User.AG.Agent) => Some(Accounts(user.credentialRole.map(AgentAccount.apply)))
        case _                   => None
      }

      implicit val writes: Writes[Accounts] = Json.writes[Accounts]
    }

    case class AgentAccount(agentUserRole: String)

    object AgentAccount {
      implicit val writes: Writes[AgentAccount] = Json.writes[AgentAccount]
    }

    case class Ids(internalId: String, externalId: String)

    object Ids {
      implicit val writes: Writes[Ids] = Json.writes[Ids]
    }

    def prepareIdsResponse(user: User): Ids = Ids(user.userId, user.userId)

    def prepareEnrolmentsResponse(user: User): Seq[Enrolment] =
      if (user.affinityGroup.contains(User.AG.Individual) && user.nino.isDefined)
        user.principalEnrolments :+ Enrolment("HMRC-NI", "NINO", user.nino.get.value)
      else user.principalEnrolments
  }

}
