package uk.gov.hmrc.agentsexternalstubs.services
import java.net.URL
import java.util.UUID

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.TcpProxiesConfig
import uk.gov.hmrc.agentsexternalstubs.controllers.BearerToken
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.http.{HeaderCarrier, HttpPost}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class ExternalAuthorisationService @Inject()(
  usersService: UsersService,
  tcpProxiesConfig: TcpProxiesConfig,
  http: HttpPost,
  @Named("auth-baseUrl") authBaseUrl: URL) {

  final def maybeExternalSession(
    planetId: String,
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
          case NonFatal(e) =>
            Logger(getClass).warn(s"External authorization lookup failed with $e")
            None
        }
        .flatMap {
          case Some(response) =>
            val creds = response.credentials.getOrElse(throw new Exception("Missing credentials"))
            val userId = creds.providerId
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
                      Logger(getClass).info(
                        s"New session '${session.sessionId}' created on planet '$planetId' from an external user '$userId' authorization.")
                      usersService.findByUserId(userId, planetId).flatMap {
                        case Some(_) =>
                          usersService.updateUser(session.userId, session.planetId, existing => merge(user, existing))
                        case None =>
                          usersService.createUser(user.copy(session.userId), session.planetId)
                      }
                    case _ => Future.successful(None)
                  }
            } yield maybeSession
          case None => Future.successful(None)
        }
        .recover {
          case NonFatal(e) =>
            Logger(getClass).warn(s"External authorization failed with $e")
            None
        }
    }

  private def merge(newUser: User, existing: User): User = User(
    userId = newUser.userId,
    groupId = newUser.groupId.orElse(existing.groupId),
    affinityGroup = newUser.affinityGroup.orElse(existing.affinityGroup),
    confidenceLevel = newUser.confidenceLevel.orElse(existing.confidenceLevel),
    credentialStrength = newUser.credentialStrength.orElse(existing.credentialStrength),
    credentialRole = newUser.credentialRole.orElse(existing.credentialRole),
    nino = newUser.nino.orElse(existing.nino),
    principalEnrolments = (newUser.principalEnrolments ++ existing.principalEnrolments).distinct,
    delegatedEnrolments = (newUser.delegatedEnrolments ++ existing.delegatedEnrolments).distinct,
    name = newUser.name.orElse(existing.name),
    dateOfBirth = newUser.dateOfBirth.orElse(existing.dateOfBirth),
    agentCode = newUser.agentCode.orElse(existing.agentCode),
    agentFriendlyName = newUser.agentFriendlyName.orElse(existing.agentFriendlyName),
    agentId = newUser.agentId.orElse(existing.agentId)
  )

}
