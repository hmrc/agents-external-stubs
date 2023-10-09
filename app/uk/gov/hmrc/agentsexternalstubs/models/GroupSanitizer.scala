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
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator
import uk.gov.hmrc.agentmtdidentifiers.model.Identifier

object GroupSanitizer extends RecordUtils[Group] {

  def sanitize(group: Group): Group = sanitize(group.groupId)(group)

  override val gen: Gen[Group] =
    for {
      groupId       <- Gen.uuid.map(_.toString)
      affinityGroup <- Gen.oneOf(AG.Individual, AG.Organisation, AG.Agent)
      planetId = ""
    } yield Group(groupId = groupId, planetId = planetId, affinityGroup = affinityGroup)

  private val ensureGroupHaveGroupId: Update = seed =>
    group => if (group.groupId.isEmpty) group.copy(groupId = Some(GroupGenerator.groupId(seed)).get) else group

  private val ensureAgentHaveAgentCode: Update = seed =>
    group =>
      group.affinityGroup match {
        case AG.Agent =>
          if (group.agentCode.isEmpty)
            group.copy(agentCode = Some(GroupGenerator.agentCode(group.groupId)))
          else group
        case _ => group.copy(agentCode = None)
      }

  private val ensureAgentHaveAgentId: Update = _ =>
    group =>
      group.affinityGroup match {
        case AG.Agent =>
          import uk.gov.hmrc.smartstub._
          import uk.gov.hmrc.agentsexternalstubs.models.Generator._
          if (group.agentId.isEmpty)
            group.copy(agentId = Some(LegacyRelationshipRecord.agentIdGen.seeded(group.groupId).get))
          else group
        case _ => group.copy(agentId = None)
      }

  private val ensureAgentHaveFriendlyName: Update = seed =>
    group =>
      group.affinityGroup match {
        case AG.Agent =>
          if (group.agentFriendlyName.isEmpty)
            group.copy(agentFriendlyName = Some(GroupGenerator.agentFriendlyName(group.groupId)))
          else group
        case _ => group.copy(agentFriendlyName = None)
      }

  private val ensurePrincipalEnrolmentKeysAreDistinct: Update = seed =>
    group => {
      group.copy(principalEnrolments =
        group.principalEnrolments
          .groupBy(_.key)
          .collect {
            case (key, es) if es.size == 1 || Services(key).exists(_.flags.multipleEnrolment) => es
            case (_, es) =>
              Seq(es.maxBy(_.identifiers.map(_.size).getOrElse(0)))
          }
          .flatten
          .toSeq
      )
    }

  private val ensurePrincipalEnrolmentsHaveIdentifiers: Update = seed =>
    group => {
      group.copy(principalEnrolments =
        group.principalEnrolments
          .groupBy(_.key)
          .flatMap { case (_, es) =>
            es.zipWithIndex.map { case (e, i) => ensureEnrolmentHaveIdentifier(Generator.variant(seed, i))(e) }
          }
          .toSeq
      )
    }

  private val ensureDelegatedEnrolmentsHaveIdentifiers: Update = seed =>
    group => {
      group.copy(delegatedEnrolments =
        group.delegatedEnrolments
          .groupBy(_.key)
          .flatMap { case (_, es) =>
            es.zipWithIndex.map { case (e, i) => ensureEnrolmentHaveIdentifier(Generator.variant(seed, i))(e) }
          }
          .toSeq
      )
    }

  private val ensureEnrolmentHaveIdentifier: String => Enrolment => Enrolment = seed =>
    e =>
      if (e.identifiers.isEmpty) Services(e.key).flatMap(s => Generator.get(s.generator)(seed)).getOrElse(e)
      else
        e.copy(identifiers = e.identifiers.map(_.map { i =>
          val key: String =
            if (i.key.isEmpty) Services(e.key).flatMap(s => s.identifiers.headOption.map(_.name)).getOrElse("")
            else i.key
          val value: String =
            if (i.value.isEmpty)
              Services(e.key)
                .flatMap(s => s.getIdentifier(key).flatMap(i => Generator.get(i.valueGenerator)(seed)))
                .getOrElse("")
            else i.value
          Identifier(key, value)
        }))

  override val sanitizers: Seq[Update] =
    Seq(
      ensureGroupHaveGroupId,
      ensureAgentHaveAgentCode,
      ensureAgentHaveAgentId,
      ensureAgentHaveFriendlyName,
      ensurePrincipalEnrolmentKeysAreDistinct,
      ensurePrincipalEnrolmentsHaveIdentifiers,
      ensureDelegatedEnrolmentsHaveIdentifiers
    )

  override val validate: Validator[Group] = GroupValidator.validate

}
