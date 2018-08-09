package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class Users(users: Seq[UserIdWithAffinityGroup])

object Users {
  implicit def format: Format[Users] = Json.format[Users]
}
