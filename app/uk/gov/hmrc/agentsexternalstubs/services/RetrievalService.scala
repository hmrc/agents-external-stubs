package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession

@Singleton
class RetrievalServiceProvider @Inject()() {

  def apply(authenticatedSession: AuthenticatedSession): RetrievalService =
    new RetrievalServiceImpl(authenticatedSession)
}

trait RetrievalService {
  def userId: String
}

class RetrievalServiceImpl(authenticatedSession: AuthenticatedSession) extends RetrievalService {
  override def userId: String = authenticatedSession.userId
}
