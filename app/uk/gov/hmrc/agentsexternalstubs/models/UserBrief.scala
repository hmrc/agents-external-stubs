package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class UserBrief(
  userId: String,
  groupId: Option[String],
  affinityGroup: Option[String],
  credentialRole: Option[String])

object UserBrief {
  implicit val formats: Format[UserBrief] = Json.format[UserBrief]
}
