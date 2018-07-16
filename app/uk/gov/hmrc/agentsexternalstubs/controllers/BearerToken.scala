package uk.gov.hmrc.agentsexternalstubs.controllers

object BearerToken {

  private val regex = """Bearer\s([a-zA-Z0-9-]+)""".r

  def unapply(token: String): Option[String] = token.trim match {
    case regex(authToken) => Some(authToken)
    case _                => None
  }
}
