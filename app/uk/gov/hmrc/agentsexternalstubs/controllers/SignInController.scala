/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.HeaderNames
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, UsersService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.agentsexternalstubs.connectors.AuthLoginApiConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig

import scala.util.Random
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.models.admin.{AG, Group, User, UserIdGenerator}

@Singleton
class SignInController @Inject() (
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  authLoginApiConnector: AuthLoginApiConnector,
  appConfig: AppConfig,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def signIn(): Action[AnyContent] = Action.async { implicit request =>
    val userIdFromPool = request.getQueryString("userIdFromPool").isDefined
    withPayloadOrDefault[SignInRequest](SignInRequest(None, None, None, None)) { signInRequest =>
      withCurrentSession { session =>
        if (signInRequest.userId.contains(session.userId))
          Future.successful(
            Ok.withHeaders(
              HeaderNames.LOCATION                    -> routes.SignInController.session(session.authToken).url,
              HeaderNames.AUTHORIZATION               -> s"Bearer ${session.authToken}",
              uk.gov.hmrc.http.HeaderNames.xSessionId -> session.sessionId,
              "X-Planet-ID"                           -> session.planetId,
              "X-User-ID"                             -> session.userId
            )
          )
        else createNewAuthentication(signInRequest, userIdFromPool)
      }(createNewAuthentication(signInRequest, userIdFromPool))
    }
  }

  def signOut(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      authenticationService.removeAuthentication(session.authToken).map(_ => NoContent)
    }(Future.successful(NoContent))
  }

  private def createNewAuthentication(
    signInRequest: SignInRequest,
    userIdFromPool: Boolean
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] = {

    val planetId =
      signInRequest.planetId.getOrElse(Generator.planetID(Random.nextString(8)))

    val userId =
      signInRequest.userId.getOrElse(UserIdGenerator.nextUserIdFor(planetId, userIdFromPool))

    val affinityGroup = signInRequest.newUserAffinityGroup.flatMap(AG.sanitize)

    for {
      (mExistingUser, mExistingGroup) <-
        usersService.findUserAndGroup(
          userId,
          planetId
        )
      (user, mGroup) <- mExistingUser match {
                          case Some(existingUser) => Future.successful((existingUser, mExistingGroup))
                          case _ =>
                            usersService
                              .createUser(
                                signInRequest.newUserData.map(_.copy(userId = userId)).getOrElse(User(userId)),
                                planetId,
                                affinityGroup = affinityGroup
                              )
                              .map((_, Option.empty[Group]))
                        }
      isNewUser = mExistingUser.isEmpty
      maybeExistingSession <-
        if (
          appConfig.syncToAuthLoginApi &&
          signInRequest.syncToAuthLoginApi.getOrElse(false)
        ) {
          authLoginApiConnector
            .loginToGovernmentGateway(AuthLoginApi.Request.fromUserAndGroup(user, mGroup))
            .map { response =>
              Logger(getClass).info(s"Authenticated user in auth-login-api ${response.sessionAuthorityUri}")
              Some(
                AuthenticatedSession(
                  sessionId = UUID.randomUUID().toString,
                  userId = userId,
                  authToken = BearerToken
                    .unapply(response.authToken)
                    .getOrElse(response.authToken),
                  providerType = "GovernmentGateway",
                  planetId = planetId
                )
              )
            }
            .recover { case e =>
              Logger(getClass).error(s"Could not authenticate the user in auth-login-api because of $e")
              None
            }

        } else {
          Future.successful(None)
        }
      maybeNewSession <- authenticationService
                           .authenticate(
                             AuthenticateRequest(
                               sessionId = UUID.randomUUID().toString,
                               userId = userId,
                               password = signInRequest.plainTextPassword.getOrElse("p@ssw0rd"),
                               providerType = signInRequest.providerType.getOrElse("GovernmentGateway"),
                               planetId = planetId,
                               authTokenOpt = maybeExistingSession.map(_.authToken)
                             )
                           )
      result <- Future.successful(maybeNewSession match {
                  case Some(session) =>
                    if (isNewUser) {
                      Created.withHeaders(
                        HeaderNames.LOCATION                    -> routes.SignInController.session(session.authToken).url,
                        HeaderNames.AUTHORIZATION               -> s"Bearer ${session.authToken}",
                        uk.gov.hmrc.http.HeaderNames.xSessionId -> session.sessionId,
                        "X-Planet-ID"                           -> planetId,
                        "X-User-ID"                             -> user.userId
                      )
                    } else {
                      Accepted.withHeaders(
                        HeaderNames.LOCATION                    -> routes.SignInController.session(session.authToken).url,
                        HeaderNames.AUTHORIZATION               -> s"Bearer ${session.authToken}",
                        uk.gov.hmrc.http.HeaderNames.xSessionId -> session.sessionId,
                        "X-Planet-ID"                           -> planetId,
                        "X-User-ID"                             -> user.userId
                      )
                    }
                  case None => Unauthorized("SESSION_CREATE_FAILED")
                })
    } yield result
  }

  def session(authToken: String): Action[AnyContent] = Action.async {
    for {
      maybeSession <- authenticationService.findByAuthToken(authToken)
    } yield maybeSession match {
      case Some(session) =>
        Ok(RestfulResponse(session, Link("delete", routes.SignInController.signOut.url)))
          .withHeaders(
            HeaderNames.AUTHORIZATION               -> s"Bearer ${session.authToken}",
            uk.gov.hmrc.http.HeaderNames.xSessionId -> session.sessionId,
            "X-Planet-ID"                           -> session.planetId,
            "X-User-ID"                             -> session.userId
          )
      case None => notFound("AUTH_SESSION_NOT_FOUND")
    }
  }

  def currentSession: Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      okF(session, Link("delete", routes.SignInController.signOut.url))
    }(notFoundF("MISSING_AUTH_SESSION"))
  }

}
