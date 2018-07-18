package uk.gov.hmrc.agentsexternalstubs.models

trait AuthoriseContext {

  def userId: String
  def providerType: String
  def principalEnrolments: Seq[Enrolment]
  def delegatedEnrolments: Seq[Enrolment]
  def affinityGroup: Option[String]
  def confidenceLevel: Int
  def credentialStrength: Option[String]
  def authorisedServices: Set[String]
}

case class FullAuthoriseContext(user: User, authenticatedSession: AuthenticatedSession, request: AuthoriseRequest)
    extends AuthoriseContext {

  override def userId: String = user.userId

  override def providerType: String = authenticatedSession.providerType

  override def principalEnrolments: Seq[Enrolment] = user.principalEnrolments

  override def delegatedEnrolments: Seq[Enrolment] = user.delegatedEnrolments

  override def affinityGroup: Option[String] = user.affinityGroup

  override def confidenceLevel: Int = user.confidenceLevel

  override def credentialStrength: Option[String] = user.credentialStrength

  override lazy val authorisedServices: Set[String] = request.authorise.collect {
    case EnrolmentPredicate(service, _) => service
  }.toSet

}
