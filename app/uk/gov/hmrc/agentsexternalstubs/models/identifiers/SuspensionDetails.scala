/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.models.identifiers

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentsexternalstubs.models.identifiers.Service._

case class SuspensionDetails(
  suspensionStatus: Boolean,
  regimes: Option[Set[String]]
) {

  val suspendedRegimes: Set[String] =
    regimes.fold(Set.empty[String]) { rs =>
      if (rs.contains("ALL") || rs.contains("AGSV"))
        SuspensionDetails.validSuspensionRegimes
      else
        rs
    }

  def isRegimeSuspended(service: Service): Boolean =
    suspendedRegimes.contains(SuspensionDetails.serviceToRegime(service))

  def isRegimeSuspended(id: String): Boolean = {
    def idToService(id: String): Service =
      SuspensionDetails.serviceToRegime
        .find(_._1.id == id)
        .map(_._1)
        .getOrElse(throw new IllegalArgumentException(s"Service of ID '$id' not known"))

    suspendedRegimes.contains(SuspensionDetails.serviceToRegime(idToService(id)))
  }

  def suspendedRegimesForServices(serviceIds: Set[String]): Set[String] =
    SuspensionDetails.serviceToRegime.view
      .filterKeys(s => serviceIds.contains(s.id))
      .values
      .toSet
      .intersect(suspendedRegimes)

  def isAnyRegimeSuspendedForServices(ids: Set[String]): Boolean = suspendedRegimesForServices(ids).nonEmpty

  override def toString: String = suspendedRegimes.toSeq.sorted.mkString(",")

}

object SuspensionDetails {

  lazy val serviceToRegime: Map[Service, String] = Map(
    MtdIt                -> "ITSA",
    MtdItSupp            -> "ITSA",
    Vat                  -> "VATC",
    Trust                -> "TRS",
    TrustNT              -> "TRS",
    CapitalGains         -> "CGT",
    PersonalIncomeRecord -> "PIR",
    Ppt                  -> "PPT",
    Cbc                  -> "CBC",
    CbcNonUk             -> "CBC",
    Pillar2              -> "PLR"
  )

  private val suspendableServices = Seq(
    MtdIt,
    Vat,
    Trust,
    CapitalGains,
    PersonalIncomeRecord,
    Ppt,
    Pillar2
  )

  lazy val validSuspensionRegimes: Set[String] =
    serviceToRegime.view.filterKeys(suspendableServices.contains(_)).values.toSet

  implicit val formats: OFormat[SuspensionDetails] = Json.format

  val notSuspended: SuspensionDetails = SuspensionDetails(suspensionStatus = false, None)

}

case class SuspensionDetailsNotFound(message: String) extends Exception(message)
