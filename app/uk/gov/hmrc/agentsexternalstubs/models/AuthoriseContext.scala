package uk.gov.hmrc.agentsexternalstubs.models

case class AuthoriseContext(user: User, authenticatedSession: AuthenticatedSession, request: AuthoriseRequest) {

  def principalEnrolments: Seq[Enrolment] = user.principalEnrolments

  def delegatedEnrolments: Seq[Enrolment] = user.delegatedEnrolments

  def affinityGroup: Option[String] = user.affinityGroup

  def confidenceLevel: Int = user.confidenceLevel

  def credentialStrength: Option[String] = user.credentialStrength

  lazy val authorisedServices
    : Set[String] = request.authorise.collect { case EnrolmentPredicate(service) => service }.toSet

}
