/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.{Json, OFormat}

/** Request for generating large number of users for the purpose of testing Granular Permissions.
  */
case class GranPermsGenRequest(
  idPrefix: String,
  numberOfAgents: Int,
  numberOfClients: Int,
  clientTypeDistribution: Option[Map[String, Double]],
  individualServiceDistribution: Option[Map[String, Double]],
  organisationServiceDistribution: Option[Map[String, Double]],
  genMethod: Option[String]
)

object GranPermsGenRequest {
  val GenMethodRandom = "random"
  val GenMethodProportional = "proportional"
  val defaultClientTypeDistribution: Map[String, Double] = Map("Individual" -> 4.0, "Organisation" -> 5.0)
  val defaultIndividualServiceDistribution: Map[String, Double] = Map(
    "HMRC-MTD-IT"  -> 1.0,
    "HMRC-MTD-VAT" -> 1.0,
    "HMRC-CGT-PD"  -> 1.0,
    "HMRC-PPT-ORG" -> 1.0
  )
  val defaultOrganisationServiceDistribution: Map[String, Double] = Map(
    "HMRC-MTD-VAT"    -> 1.0,
    "HMRC-CGT-PD"     -> 1.0,
    "HMRC-PPT-ORG"    -> 1.0,
    "HMRC-TERS-ORG"   -> 1.0,
    "HMRC-TERSNT-ORG" -> 1.0
  )

  implicit val format = Json.format[GranPermsGenRequest]
}

case class GranPermsGenResponse(
  createdAgents: Seq[User],
  createdClients: Seq[User]
)

object GranPermsGenResponse {
  implicit val format = Json.format[GranPermsGenResponse]
}
