package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, OFormat}

case class SuspensionDetails(suspensionStatus: Boolean, regimes: Option[Set[String]]) {
  //PERSONAL-INCOME-RECORD service has no enrolment / regime so cannot be suspended
  private val validSuspensionRegimes = Set("ITSA", "VATC", "TRS", "CGT")

  val suspendedRegimes: Set[String] =
    this.regimes.fold(Set.empty[String])(rs => if (rs.contains("ALL")) validSuspensionRegimes else rs)
}

object SuspensionDetails {
  implicit val formats: OFormat[SuspensionDetails] = Json.format
}
