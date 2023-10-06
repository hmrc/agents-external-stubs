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

package uk.gov.hmrc.agentsexternalstubs.models.admin

import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agentsexternalstubs.models.Enrolment

case class Group(
  planetId: String,
  groupId: String,
  affinityGroup: String,
  agentId: Option[String] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  principalEnrolments: Seq[Enrolment] = Seq.empty,
  delegatedEnrolments: Seq[Enrolment] = Seq.empty,
  suspendedRegimes: Set[String] = Set.empty
) {
  def findIdentifierValue(serviceName: String, identifierName: String): Option[String] =
    principalEnrolments
      .find(_.key == serviceName)
      .flatMap(_.identifiers.flatMap(_.find(_.key == identifierName)))
      .map(_.value)

  def findIdentifierValue(
    serviceName: String,
    identifierName1: String,
    identifierName2: String,
    join: (String, String) => String
  ): Option[String] =
    for {
      enrolment <- principalEnrolments.find(_.key == serviceName)
      part1     <- enrolment.identifierValueOf(identifierName1)
      part2     <- enrolment.identifierValueOf(identifierName2)
    } yield join(part1, part2)

  def findDelegatedIdentifierValues(serviceName: String, identifierName: String): Seq[String] =
    delegatedEnrolments
      .filter(_.key == serviceName)
      .flatMap(_.identifiers.flatMap(_.find(_.key == identifierName)))
      .map(_.value)

  def findDelegatedIdentifierValues(serviceName: String): Seq[Seq[String]] =
    delegatedEnrolments
      .filter(_.key == serviceName)
      .flatMap(_.identifiers.map(_.map(_.value)))

  def uniqueKeys: Seq[String] =
    Seq(Group.groupIdKey(groupId)) ++ agentCode.toSeq.map(Group.agentCodeIndexKey) ++ principalEnrolments.flatMap(ek =>
      ek.toEnrolmentKeyTag.map(Group.principalEnrolmentIndexKey)
    )

  def lookupKeys: Seq[String] =
    Seq(Group.groupIdKey(groupId), Group.affinityGroupKey(affinityGroup)) ++ delegatedEnrolments.flatMap(ek =>
      ek.toEnrolmentKeyTag.map(Group.delegatedEnrolmentIndexKey)
    )
}

object Group {
  implicit val format: OFormat[Group] = Json.format[Group]

  val compressedFormat: OFormat[Group] = {
    implicit val enrolmentFormat = Enrolment.tinyFormat // Use the space-saving Json representation for Enrolment
    Json.format[Group]
  }

  def validate(group: Group): Either[List[String], Group] =
    GroupValidator.validate(group) match {
      case Valid(())       => Right(group)
      case Invalid(errors) => Left(errors)
    }

  object Individual {
    def unapply(group: Group): Option[Group] =
      if (group.affinityGroup == AG.Individual) Some(group) else None
  }

  object Organisation {
    def unapply(group: Group): Option[Group] =
      if (group.affinityGroup == AG.Organisation) Some(group) else None
  }

  object Agent {
    def unapply(group: Group): Option[Group] =
      if (group.affinityGroup == AG.Agent) Some(group) else None
  }

  object Matches {
    def apply(affinityGroupMatches: String => Boolean, serviceName: String): MatchesAffinityGroupAndEnrolment =
      MatchesAffinityGroupAndEnrolment(affinityGroupMatches, serviceName)
  }

  case class MatchesAffinityGroupAndEnrolment(affinityGroupMatches: String => Boolean, serviceName: String) {
    def unapply(group: Group): Option[(Group, String)] = if (!affinityGroupMatches(group.affinityGroup)) None
    else {
      group.principalEnrolments
        .find(_.key == serviceName)
        .flatMap(_.identifiers)
        .flatMap(_.map(_.value).headOption)
        .map((group, _))
    }
  }

  def groupIdKey(groupId: String): String = s"gid:$groupId"
  def principalEnrolmentIndexKey(key: String): String = s"penr:${key.toLowerCase}"
  def delegatedEnrolmentIndexKey(key: String): String = s"denr:${key.toLowerCase}"
  def affinityGroupKey(affinityGroup: String): String = s"ag:${affinityGroup.toLowerCase}"
  def agentCodeIndexKey(agentCode: String): String = s"ac:$agentCode"
}

object AG {
  final val Individual = "Individual"
  final val Organisation = "Organisation"
  final val Agent = "Agent"

  val values = Set(Individual, Organisation, Agent)
  val all: String => Boolean = values.contains

  def sanitize(affinityGroup: String): Option[String] = affinityGroup match {
    case ag if AG.values.contains(ag) => Some(ag)
    case "none" | "None" | "NONE"     => None
    case empty if empty.isEmpty       => None
    case x                            => throw new IllegalArgumentException(s"Invalid affinity group: $x")
  }
}
