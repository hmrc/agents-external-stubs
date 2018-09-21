package uk.gov.hmrc.agentsexternalstubs.controllers

object BearerToken {

  private val regex = """Bearer\s(.+)""".r

  def unapply(token: String): Option[String] = token.trim match {
    case regex(authToken) => Some(authToken)
    case _                => None
  }
}
