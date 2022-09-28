/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.controllers

import java.util.UUID
import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.agentsexternalstubs.connectors.AgentAccessControlConnector
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, AuthorisationCache, GroupsService, UsersService}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthStubController @Inject() (
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  groupsService: GroupsService,
  agentAccessControlConnector: AgentAccessControlConnector,
  appConfig: AppConfig,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  import AuthStubController._

  val authCacheFlag: Option[Unit] = if (appConfig.authCacheEnabled) Some(()) else None

  val authorise: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(BearerToken(authToken)) =>
        for {
          maybeSession <- authenticationService.findByAuthTokenOrLookupExternal(authToken)
          response <- request.body.validate[AuthoriseRequest] match {
                        case JsSuccess(authoriseRequest, _) =>
                          maybeSession match {
                            case Some(authenticatedSession) =>
                              authCacheFlag.flatMap(_ =>
                                AuthorisationCache.get(authenticatedSession, authoriseRequest)
                              ) match {
                                case Some(maybeResponse) =>
                                  Future.successful(
                                    maybeResponse
                                      .fold(error => unauthorized(error), response => Ok(Json.toJson(response)))
                                  )
                                case None =>
                                  for {
                                    maybeUser <-
                                      usersService
                                        .findUserAndGroup(authenticatedSession.userId, authenticatedSession.planetId)
                                    result <- Future(maybeUser match {
                                                case (Some(user), maybeGroup) =>
                                                  Authorise.prepareAuthoriseResponse(
                                                    FullAuthoriseContext(
                                                      user,
                                                      maybeGroup,
                                                      usersService,
                                                      groupsService,
                                                      authenticatedSession,
                                                      authoriseRequest,
                                                      agentAccessControlConnector
                                                    )
                                                  )
                                                case _ =>
                                                  Left("SessionRecordNotFound")
                                              }) map { maybeResponse =>
                                                if (authCacheFlag.isDefined)
                                                  AuthorisationCache
                                                    .put(authenticatedSession, authoriseRequest, maybeResponse)
                                                maybeResponse.fold(
                                                  error => unauthorized(error),
                                                  response => Ok(Json.toJson(response))
                                                )
                                              }
                                  } yield result
                              }
                            case None =>
                              unauthorizedF("SessionRecordNotFound")
                          }
                        case JsError(errors) =>
                          Future.successful(
                            BadRequest(
                              errors
                                .map { case (p, ve) => s"$p -> [${ve.map(v => v.message).mkString(",")}]" }
                                .mkString("\n")
                            )
                          )
                      }
        } yield response
      case Some(token) =>
        Logger(getClass).warn(s"Unsupported bearer token format $token")
        unauthorizedF("InvalidBearerToken")
      case None =>
        unauthorizedF("MissingBearerToken")
    }
  }

  private def withAuthorisedUserAndSession(
    body: (User, AuthenticatedSession) => Future[Result]
  )(implicit request: Request[AnyContent]): Future[Result] =
    withAuthorisedUserGroupAndSession { case (user, _, session) =>
      body(user, session)
    }

  private def withAuthorisedUserGroupAndSession(
    body: (User, Group, AuthenticatedSession) => Future[Result]
  )(implicit request: Request[AnyContent]): Future[Result] = request.headers.get(HeaderNames.AUTHORIZATION) match {
    case Some(BearerToken(authToken)) =>
      for {
        maybeSession <- authenticationService.findByAuthTokenOrLookupExternal(authToken)
        result <- maybeSession match {
                    case Some(authenticatedSession) =>
                      for {
                        maybeUser <- usersService
                                       .findUserAndGroup(authenticatedSession.userId, authenticatedSession.planetId)
                        result <- maybeUser match {
                                    case (Some(user), Some(group)) => body(user, group, authenticatedSession)
                                    case _ =>
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
    withAuthorisedUserGroupAndSession { (user, group, session) =>
      Future.successful(Ok(Json.toJson(Authority.prepareAuthorityResponse(user, group, session))))
    }
  }

  val getIds: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedUserAndSession { (user, _) =>
      Future.successful(Ok(Json.toJson(Authority.prepareIdsResponse(user))))
    }
  }

  val getEnrolments: Action[AnyContent] = Action.async { implicit request =>
    withAuthorisedUserGroupAndSession { (user, group, _) =>
      Future.successful(Ok(Json.toJson(Authority.prepareEnrolmentsResponse(user, group))))
    }
  }

  def getUserByOid(oid: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      for {
        maybeUser <- usersService.findByUserId(oid, session.planetId)
        maybeGroup <- maybeUser
                        .flatMap(_.groupId)
                        .fold(Future.successful(Option.empty[Group]))(groupId =>
                          groupsService.findByGroupId(groupId, session.planetId)
                        )
      } yield (maybeUser, maybeGroup) match {
        case (Some(user), Some(group)) => ok(Authority.prepareAuthorityResponse(user, group, session))
        case _                         => notFound(s"User $oid not found on a planet ${session.planetId}")
      }
    }(SessionRecordNotFound)
  }

  def getEnrolmentsByOid(oid: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      for {
        maybeUser <- usersService.findByUserId(oid, session.planetId)
        maybeGroup <- maybeUser
                        .flatMap(_.groupId)
                        .fold(Future.successful(Option.empty[Group]))(groupId =>
                          groupsService.findByGroupId(groupId, session.planetId)
                        )
      } yield (maybeUser, maybeGroup) match {
        case (Some(user), Some(group)) => ok(Authority.prepareEnrolmentsResponse(user, group))
        case _                         => notFound(s"User $oid not found on a planet ${session.planetId}")
      }
    }(SessionRecordNotFound)
  }

  override def unauthorizedF(reason: String): Future[Result] =
    Future.successful(unauthorized(reason))

  override def unauthorized(reason: String): Result =
    Unauthorized("")
      .withHeaders("WWW-Authenticate" -> s"""MDTP detail="$reason"""")
}

object AuthStubController {

  object Authorise {

    def prepareAuthoriseResponse(context: AuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
      checkPredicates(context).fold(error => Left(error), _ => retrieveDetails(context))

    def checkPredicates(context: AuthoriseContext): Either[String, Unit] =
      context.request.authorise.foldLeft[Either[String, Unit]](Right(()))((result, p: Predicate) =>
        result.fold(error => Left(error), _ => p.validate(context))
      )

    def retrieveDetails(context: AuthoriseContext)(implicit ex: ExecutionContext): Retrieve.MaybeResponse =
      context.request.retrieve.foldLeft[Retrieve.MaybeResponse](Right(AuthoriseResponse()))((result, r: String) =>
        result.fold(error => Left(error), response => addDetailToResponse(response, r, context))
      )

    def addDetailToResponse(response: AuthoriseResponse, retrieve: String, context: AuthoriseContext)(implicit
      ex: ExecutionContext
    ): Retrieve.MaybeResponse =
      Retrieve.of(retrieve).fill(response, context)
  }

  object Authority {

    def prepareAuthorityResponse(user: User, group: Group, session: AuthenticatedSession): Response = Response(
      uri = s"/auth/oid/${user.userId}",
      confidenceLevel = user.confidenceLevel.getOrElse(50),
      credentialStrength = user.credentialStrength.getOrElse("weak"),
      userDetailsLink = s"/user-details/id/${user.userId}",
      legacyOid = user.userId,
      ids = s"/auth/_ids",
      lastUpdated = "2017-02-14T11:23:52.955Z",
      loggedInAt = "2017-02-14T11:23:52.955Z",
      enrolments = s"/auth/_enrolments",
      affinityGroup = group.affinityGroup,
      correlationId = UUID.randomUUID().toString,
      credId = user.userId,
      credentials = Some(Credentials(user.userId)),
      accounts = Accounts.from(user, group)
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
      accounts: Option[Accounts]
    )

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]
    }

    case class Credentials(gatewayId: String)

    object Credentials {
      implicit val writes: Writes[Credentials] = Json.writes[Credentials]
    }

    case class Accounts(
      paye: Option[Accounts.Paye] = None,
      sa: Option[Accounts.Sa] = None,
      ct: Option[Accounts.Ct] = None,
      vat: Option[Accounts.Vat] = None,
      epaye: Option[Accounts.Epaye] = None,
      agent: Option[Accounts.Agent] = None
    )

    object Accounts {

      def from(user: User, group: Group): Option[Accounts] = group.affinityGroup match {
        case AG.Agent =>
          Some(
            Accounts(
              agent = Some(
                Agent(
                  agentUserRole = user.credentialRole
                    .map { case User.CR.Admin | User.CR.User => "admin"; case User.CR.Assistant => "assistant" }
                    .getOrElse("link"),
                  agentUserId = group.agentId.getOrElse("link"),
                  agentCode = group.agentCode.getOrElse("link"),
                  link = "link",
                  payeReference = group.findIdentifierValue("IR-PAYE-AGENT", "IRAgentReference")
                )
              ),
              ct = group.findIdentifierValue("IR-CT", "UTR").map(Ct.apply("link", _)),
              sa = group.findIdentifierValue("IR-SA", "UTR").map(Sa.apply("link", _)),
              vat = group.findIdentifierValue("HMCE-VATDEC-ORG", "VRN").map(Vat.apply("link", _))
            )
          )
        case AG.Individual =>
          Some(
            Accounts(
              sa = group.findIdentifierValue("IR-SA", "UTR").map(Sa.apply("link", _)),
              paye = user.nino.map(nino => Paye.apply("link", nino.value.replace(" ", ""))),
              vat = group.findIdentifierValue("HMCE-VATDEC-ORG", "VRN").map(Vat.apply("link", _))
            )
          )
        case AG.Organisation =>
          Some(
            Accounts(
              sa = group.findIdentifierValue("IR-SA", "UTR").map(Sa.apply("link", _)),
              ct = group.findIdentifierValue("IR-CT", "UTR").map(Ct.apply("link", _)),
              vat = group.findIdentifierValue("HMCE-VATDEC-ORG", "VRN").map(Vat.apply("link", _)),
              epaye = group
                .findIdentifierValue("IR-PAYE", "TaxOfficeNumber", "TaxOfficeReference", _ + "/" + _)
                .map(Epaye.apply("link", _))
            )
          )
        case _ => None
      }

      implicit val writes: Writes[Accounts] = Json.writes[Accounts]

      case class Agent(
        agentUserRole: String,
        agentUserId: String,
        agentCode: String,
        link: String,
        payeReference: Option[String] = None
      )

      object Agent {
        implicit val writes: Writes[Agent] = Json.writes[Agent]
      }

      case class Ct(link: String, utr: String)

      object Ct {
        implicit val formats: Format[Ct] = Json.format[Ct]
      }

      case class Epaye(link: String, empRef: String)

      object Epaye {
        implicit val formats: Format[Epaye] = Json.format[Epaye]
      }

      case class Paye(link: String, nino: String)

      object Paye {
        implicit val formats: Format[Paye] = Json.format[Paye]
      }

      case class Sa(link: String, utr: String)

      object Sa {
        implicit val formats: Format[Sa] = Json.format[Sa]
      }

      case class Vat(link: String, vrn: String)

      object Vat {
        implicit val formats: Format[Vat] = Json.format[Vat]
      }
    }

    case class Ids(internalId: String, externalId: String)

    object Ids {
      implicit val writes: Writes[Ids] = Json.writes[Ids]
    }

    def prepareIdsResponse(user: User): Ids = Ids(user.userId, user.userId)

    def prepareEnrolmentsResponse(user: User, group: Group): Seq[Enrolment] =
      if (group.affinityGroup == AG.Individual || group.affinityGroup == AG.Organisation && user.nino.isDefined)
        group.principalEnrolments :+ Enrolment("HMRC-NI", "NINO", user.nino.get.value)
      else group.principalEnrolments
  }

}
