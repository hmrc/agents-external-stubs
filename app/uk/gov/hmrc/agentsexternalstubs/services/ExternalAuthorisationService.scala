package uk.gov.hmrc.agentsexternalstubs.services
import java.net.URL
import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import play.api.libs.json.Json
import uk.gov.hmrc.agentsexternalstubs.TcpProxiesConfig
import uk.gov.hmrc.agentsexternalstubs.controllers.BearerToken
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalAuthorisationService @Inject()(
  usersService: UsersService,
  tcpProxiesConfig: TcpProxiesConfig,
  http: HttpPost,
  @Named("auth-baseUrl") authBaseUrl: URL) {

  final def maybeExternalSession(
    _planetId: String,
    createNewAuthentication: AuthenticateRequest => Future[Option[AuthenticatedSession]])(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier): Future[Option[AuthenticatedSession]] =
    if (tcpProxiesConfig.isProxyMode) {
      Future.successful(None)
    } else {
      val authRequest = AuthoriseRequest(
        Seq.empty,
        Seq(
          "credentials",
          "allEnrolments",
          "affinityGroup",
          "confidenceLevel",
          "credentialStrength",
          "credentialRole",
          "nino",
          "groupIdentifier",
          "name",
          "dateOfBirth",
          "agentInformation"
        )
      )
      http
        .POST(s"${authBaseUrl.toExternalForm}/auth/authorise", authRequest)
        .map {
          _.json match {
            case null => None
            case body => Some(body.as[AuthoriseResponse])
          }
        }
        .recover {
          case e: Upstream5xxResponse =>
            Logger(getClass).warn(s"External authorization lookup failed with [$e] for headers ${report(hc)}")
            None
          case e: Upstream4xxResponse if e.upstreamResponseCode != 401 =>
            Logger(getClass).warn(s"External authorization lookup failed with [$e] for headers ${report(hc)}")
            None
          case e: Upstream4xxResponse if e.upstreamResponseCode == 401 =>
            Logger(getClass).warn(s"External authorization not found for headers ${report(hc)}")
            None
        }
        .flatMap {
          case Some(response) =>
            val creds = response.credentials.getOrElse(throw new Exception("Missing credentials"))
            val (userId, planetId) = User.parseUserIdAtPlanetId(creds.providerId, _planetId)
            val user = User(
              userId = userId,
              groupId = response.groupIdentifier,
              affinityGroup = response.affinityGroup,
              confidenceLevel = response.confidenceLevel,
              credentialStrength = response.credentialStrength,
              credentialRole = response.credentialRole,
              nino = response.nino,
              principalEnrolments = response.allEnrolments.map(_.filterNot(_.key == "HMRC-NI")).getOrElse(Seq.empty),
              name = response.name.map(_.toString),
              dateOfBirth = response.dateOfBirth,
              agentCode = response.agentInformation.flatMap(_.agentCode),
              agentFriendlyName = response.agentInformation.flatMap(_.agentFriendlyName),
              agentId = response.agentInformation.flatMap(_.agentId)
            )
            for {
              maybeSession <- createNewAuthentication(
                               AuthenticateRequest(
                                 sessionId = hc.sessionId.map(_.value).getOrElse(UUID.randomUUID().toString),
                                 userId = userId,
                                 password = "p@ssw0rd",
                                 providerType = creds.providerType,
                                 planetId = planetId,
                                 authTokenOpt = hc.authorization.map(
                                   a =>
                                     BearerToken
                                       .unapply(a.value)
                                       .getOrElse(throw new IllegalStateException(
                                         s"Unsupported authorization token format ${a.value}")))
                               ))
              _ <- maybeSession match {
                    case Some(session) =>
                      usersService.findByUserId(userId, planetId).flatMap {
                        case Some(_) =>
                          usersService
                            .updateUser(session.userId, session.planetId, existing => merge(existing, user))
                            .recover {
                              case NonFatal(e) =>
                                Logger(getClass).warn(
                                  s"Creating user '$userId' on the planet '$planetId' failed with [$e] for an external authorisation ${Json
                                    .prettyPrint(Json.toJson(response))} and headers ${report(hc)}")
                                None
                            }
                            .andThen {
                              case _ =>
                                Logger(getClass).info(
                                  s"Creating user '$userId' updated on the planet '$planetId' based on external authorisation ${Json
                                    .prettyPrint(Json.toJson(response))} for headers ${report(hc)}")
                            }
                        case None =>
                          (for {
                            fixed <- usersService.checkAndFixUser(user, planetId)
                            user  <- usersService.createUser(fixed.copy(session.userId), session.planetId)
                            _ = Logger(getClass).info(
                              s"Creating user '$userId' on the planet '$planetId' based on external authorisation ${Json
                                .prettyPrint(Json.toJson(response))} for headers ${report(hc)}")
                          } yield user)
                            .recover {
                              case NonFatal(e) =>
                                Logger(getClass).warn(
                                  s"Creating user '$userId' on the planet '$planetId' failed with [$e] for an external authorisation ${Json
                                    .prettyPrint(Json.toJson(response))} and headers ${report(hc)}")
                                None
                            }
                      }
                    case _ =>
                      Logger(getClass).warn(
                        s"Creating user '$userId' on the planet '$planetId' failed for an external authorisation ${Json
                          .prettyPrint(Json.toJson(response))} and headers ${report(hc)}")
                      Future.successful(None)
                  }
            } yield maybeSession
          case None => Future.successful(None)
        }
        .recover {
          case NonFatal(_) => None
        }
    }

  def report(hc: HeaderCarrier): String =
    s"""Authorization:${hc.authorization
      .map(_.value)
      .getOrElse("-")} X-Session-ID:${hc.sessionId.getOrElse("-")} ForwardedFor:${hc.forwarded
      .map(_.value)
      .getOrElse("-")} RequestId:${hc.requestId.map(_.value).getOrElse("-")}"""

  private def merge(first: User, second: User): User = User(
    userId = first.userId,
    groupId = first.groupId.orElse(second.groupId),
    affinityGroup = first.affinityGroup.orElse(second.affinityGroup),
    confidenceLevel = first.confidenceLevel.orElse(second.confidenceLevel),
    credentialStrength = first.credentialStrength.orElse(second.credentialStrength),
    credentialRole = first.credentialRole.orElse(second.credentialRole),
    nino = first.nino.orElse(second.nino),
    principalEnrolments = (first.principalEnrolments ++ second.principalEnrolments).distinct,
    delegatedEnrolments = (first.delegatedEnrolments ++ second.delegatedEnrolments).distinct,
    name = first.name.orElse(second.name),
    dateOfBirth = first.dateOfBirth.orElse(second.dateOfBirth),
    agentCode = first.agentCode.orElse(second.agentCode),
    agentFriendlyName = first.agentFriendlyName.orElse(second.agentFriendlyName),
    agentId = first.agentId.orElse(second.agentId),
    recordIds = (first.recordIds ++ second.recordIds).distinct,
    isNonCompliant = first.isNonCompliant,
    planetId = first.planetId
  )

}
