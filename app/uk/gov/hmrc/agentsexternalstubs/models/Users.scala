package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json, Writes}

case class Users(users: Seq[User])

object Users {
  implicit val userWrites: Writes[User] = User.plainWrites
  implicit def format: Format[Users] = Json.format[Users]
}
