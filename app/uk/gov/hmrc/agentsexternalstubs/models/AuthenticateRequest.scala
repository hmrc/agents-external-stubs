package uk.gov.hmrc.agentsexternalstubs.models

case class AuthenticateRequest(
  sessionId: String,
  userId: String,
  password: String,
  providerType: String,
  planetId: String,
  authTokenOpt: Option[String] = None)
