package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {

  protected def withAuthorisedAsAgent[A](body: Arn => Future[Result])(
    implicit request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    withEnrolledFor("HMRC-AS-AGENT", "AgentReferenceNumber") {
      case Some(arn) => body(Arn(arn))
      case None      => Future.failed(InsufficientEnrolments("AgentReferenceNumber identifier not found"))
    }

  protected def withAuthorisedAsClient[A](body: MtdItId => Future[Result])(
    implicit request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    withEnrolledFor("HMRC-MTD-IT", "MTDITID") {
      case Some(mtdItID) => body(MtdItId(mtdItID))
      case None          => Future.failed(InsufficientEnrolments("MTDITID identifier not found"))
    }

  protected def withEnrolledFor[A](serviceName: String, identifierKey: String)(body: Option[String] => Future[Result])(
    implicit request: Request[A],
    hc: HeaderCarrier,
    ec: ExecutionContext): Future[Result] =
    authorised(
      Enrolment(serviceName)
        and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = for {
          enrolment  <- enrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield identifier.value

        body(id)
      }

}
