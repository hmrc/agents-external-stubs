/*
 * Copyright 2021 HM Revenue & Customs
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
        else createNewAuthentication(signInRequest, userIdFromPool, Some(session))
      }(createNewAuthentication(signInRequest, userIdFromPool, None))
    }
  }

  def signOut(): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      authenticationService.removeAuthentication(session.authToken).map(_ => NoContent)
    }(Future.successful(NoContent))
  }

  private def createNewAuthentication(
    signInRequest: SignInRequest,
    userIdFromPool: Boolean,
    sessionOpt: Option[AuthenticatedSession]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Result] = {

    val planetId =
      signInRequest.planetId.getOrElse(Generator.planetID(Random.nextString(8)))

    val userId =
      signInRequest.userId.getOrElse(UserIdGenerator.nextUserIdFor(planetId, userIdFromPool))

    for {
      maybeUser <- usersService.tryCreateUser(User(userId), planetId)
      maybeExistingSession <-
        if (
          appConfig.syncToAuthLoginApi &&
          signInRequest.syncToAuthLoginApi.getOrElse(false) &&
          sessionOpt.isEmpty
        ) {
          val user: User = maybeUser.fold(identity, identity)
          authLoginApiConnector
            .loginToGovernmentGateway(AuthLoginApi.Request.fromUser(user))
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
              sessionOpt
            }

        } else {
          Future.successful(sessionOpt)
        }
      maybeNewSession <- authenticationService
                           .authenticate(
                             AuthenticateRequest(
                               sessionId = maybeExistingSession.map(_.sessionId).getOrElse(UUID.randomUUID().toString),
                               userId = userId,
                               password = signInRequest.plainTextPassword.getOrElse("p@ssw0rd"),
                               providerType = signInRequest.providerType.getOrElse("GovernmentGateway"),
                               planetId = planetId,
                               authTokenOpt = maybeExistingSession.map(_.authToken)
                             )
                           )
                           .recoverWith { case e =>
                             Logger(getClass).warn(
                               s"Saving authenticated sessions for token ${maybeExistingSession.map(_.authToken).getOrElse("none")} failed with ${e.getMessage}, trying again with unique token."
                             )
                             authenticationService.authenticate(
                               AuthenticateRequest(
                                 sessionId = UUID.randomUUID().toString,
                                 userId = userId,
                                 password = signInRequest.plainTextPassword.getOrElse("p@ssw0rd"),
                                 providerType = signInRequest.providerType.getOrElse("GovernmentGateway"),
                                 planetId = planetId
                               )
                             )
                           }
      result <- Future.successful(maybeNewSession match {
                  case Some(session) =>
                    maybeUser match {
                      case Right(user) =>
                        Created.withHeaders(
                          HeaderNames.LOCATION                    -> routes.SignInController.session(session.authToken).url,
                          HeaderNames.AUTHORIZATION               -> s"Bearer ${session.authToken}",
                          uk.gov.hmrc.http.HeaderNames.xSessionId -> session.sessionId,
                          "X-Planet-ID"                           -> planetId,
                          "X-User-ID"                             -> user.userId
                        )

                      case Left(user) =>
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

  def session(authToken: String): Action[AnyContent] = Action.async { implicit request =>
    for {
      maybeSession <- authenticationService.findByAuthToken(authToken)
    } yield maybeSession match {
      case Some(session) =>
        Ok(RestfulResponse(session, Link("delete", routes.SignInController.signOut().url)))
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
      okF(session, Link("delete", routes.SignInController.signOut().url))
    }(notFoundF("MISSING_AUTH_SESSION"))
  }

}
