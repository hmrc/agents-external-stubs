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

package uk.gov.hmrc.agentsexternalstubs.models

import org.scalacheck.Gen

import java.util.UUID

object GroupGenerator {

  import uk.gov.hmrc.agentsexternalstubs.models.Generator._
  import uk.gov.hmrc.smartstub._

  def nameForIndividual(groupId: String): String =
    (for {
      fn <- forename().suchThat(_.nonEmpty)
      ln <- surname.suchThat(_.nonEmpty)
    } yield s"$fn $ln").seeded(groupId).get

  val nameForAgentGen: Gen[String] = for {
    f <- forename().suchThat(_.nonEmpty); s <- surname.suchThat(_.nonEmpty)
  } yield s"$f $s"

  def nameForAgent(seed: String): String =
    s"${forename().suchThat(_.nonEmpty).seeded(seed).getOrElse("Agent")} ${surname.suchThat(_.nonEmpty).seeded(seed).getOrElse("Cooper")}"

  def nameForOrganisation(groupId: String): String =
    company
      .suchThat(_.nonEmpty)
      .map(_.replaceAll("[^A-Za-z0-9 /s //.]", "A"))
      .seeded(groupId)
      .getOrElse(s"Acme-${UUID.randomUUID().toString.take(5)}")

  private final val groupIdGen = pattern"9Z9Z-Z9Z9-9Z9Z-Z9Z9".gen

  def groupId(seed: String): String = groupIdGen.seeded(seed).get

  private final val agentCodeGen = pattern"ZZZZZZ999999".gen

  def agentCode(seed: String): String = agentCodeGen.seeded(seed).get

  private final val agentIdGen = pattern"999999".gen

  def agentId(seed: String): String = agentIdGen.seeded(seed).get

  val agencyNameGen: Gen[String] = for {
    ln <- surname
    suffix <- Gen.oneOf(
                " Accountants",
                " and Company",
                " & Co",
                " Professional Services",
                " Accountancy",
                " Chartered Accountants & Business Advisers",
                " Group of Accountants",
                " Professional",
                " & " + ln
              )
  } yield s"$ln$suffix"

  def agentFriendlyName(groupId: String): String =
    agencyNameGen.seeded(groupId + "_agent").get

  def generate(planetId: String, affinityGroup: String, groupId: Option[String] = None) = affinityGroup match {
    case AG.Individual   => individual(planetId, groupId)
    case AG.Organisation => organisation(planetId, groupId)
    case AG.Agent        => agent(planetId, groupId)
  }

  def individual(planetId: String, groupId: Option[String]): Group =
    (for {
      groupId <- groupId.fold(groupIdGen)(Gen.const)
    } yield Group(
      planetId = planetId,
      groupId = groupId,
      affinityGroup = AG.Individual
    )).sample.get

  def agent(
    planetId: String,
    groupId: Option[String],
    agentCode: Option[String] = None,
    agentFriendlyName: Option[String] = None,
    agentId: Option[String] = None,
    delegatedEnrolments: Seq[Enrolment] = Seq.empty
  ): Group =
    (for {
      groupId           <- groupId.fold(groupIdGen)(Gen.const)
      agentCode         <- agentCode.fold(agentCodeGen)(Gen.const)
      agentFriendlyName <- agentFriendlyName.fold(agencyNameGen)(Gen.const)
      agentId           <- agentId.fold(agentIdGen)(Gen.const)
    } yield Group(
      planetId = planetId,
      groupId = groupId,
      affinityGroup = AG.Agent,
      agentCode = Some(agentCode),
      agentFriendlyName = Some(agentFriendlyName),
      agentId = Some(agentId),
      delegatedEnrolments = delegatedEnrolments
    )).sample.get

  def organisation(planetId: String, groupId: Option[String], name: String = null): Group =
    (for {
      groupId <- groupId.fold(groupIdGen)(Gen.const)
    } yield Group(
      planetId = planetId,
      groupId = groupId,
      affinityGroup = AG.Organisation
    )).sample.get
}
