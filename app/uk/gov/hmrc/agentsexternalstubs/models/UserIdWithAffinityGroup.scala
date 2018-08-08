package uk.gov.hmrc.agentsexternalstubs.models
import play.api.libs.json.{Format, Json}

case class UserIdWithAffinityGroup(userId: String, affinityGroup: Option[String])

object UserIdWithAffinityGroup {
  implicit val formats: Format[UserIdWithAffinityGroup] = Json.format[UserIdWithAffinityGroup]
}
