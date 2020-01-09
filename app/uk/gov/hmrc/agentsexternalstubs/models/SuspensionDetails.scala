package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, OFormat}

case class SuspensionDetails(suspensionStatus: Boolean, regimes: Option[Set[String]])

object SuspensionDetails {
  implicit val formats: OFormat[SuspensionDetails] = Json.format
}
