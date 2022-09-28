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

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object GroupValidator {

  type GroupConstraint = Group => Validated[String, Unit]

  val validateAffinityGroup: GroupConstraint = group =>
    group.affinityGroup match {
      case AG.Individual | AG.Organisation | AG.Agent => Valid(())
      case _ =>
        Invalid("affinityGroup must be none, or one of [Individual, Organisation, Agent]")
    }

  val validateAgentCode: GroupConstraint = group =>
    group.agentCode match {
      case Some(_) if group.affinityGroup == AG.Agent => Valid(())
      case None if group.affinityGroup == AG.Agent =>
        Invalid("agentCode is required for Agent")
      case _ => Valid(())
    }

  val validateSuspendedRegimes: GroupConstraint = group => {
    val validRegimes = Set("ALL", "ITSA", "VATC", "TRS", "CGT", "PPT", "PIR", "AGSV")
    if (group.suspendedRegimes.isEmpty) Valid(())
    else
      group.suspendedRegimes
        .map(regime =>
          if (validRegimes.contains(regime)) Valid(())
          else Invalid(s"suspended regime $regime not valid")
        )
        .reduce(_ combine _)
  }

  val validateEachPrincipalEnrolment: GroupConstraint = group =>
    if (group.principalEnrolments.isEmpty) Valid(())
    else {
      group.principalEnrolments
        .map(e =>
          Validated
            .cond(
              Services(e.key)
                .map(_.affinityGroups)
                .forall(_.contains(group.affinityGroup)), // TODO fix unrelated comparison
              (),
              s"Service ${e.key} is not available for this user's affinity group"
            )
            .andThen(_ => Enrolment.validate(e))
        )
        .reduce(_ combine _)
    }

  val validatePrincipalEnrolmentsAreDistinct: GroupConstraint = group =>
    if (group.principalEnrolments.isEmpty) Valid(())
    else {
      val keys = group.principalEnrolments.map(_.key)
      if (keys.size == keys.distinct.size) Valid(())
      else {
        val repeated: Iterable[String] = keys.groupBy(identity).filter { case (_, k) => k.size > 1 }.map(_._2.head)
        val redundant = repeated.map(r => (r, Services.apply(r))).collect {
          case (_, Some(s)) if !s.flags.multipleEnrolment => s.name
          case (r, None)                                  => r
        }
        if (redundant.isEmpty) Valid(()) else Invalid(s"Repeated principal enrolments: ${redundant.mkString(", ")}")
      }
    }

  val validateEachDelegatedEnrolment: GroupConstraint = group =>
    group.delegatedEnrolments match {
      case s if s.isEmpty => Valid(())
      case _ if group.affinityGroup == AG.Agent =>
        group.delegatedEnrolments
          .map(e =>
            Validated
              .cond(
                Services(e.key)
                  .map(_.affinityGroups)
                  .forall(ag => ag.contains(AG.Individual) || ag.contains(AG.Organisation)),
                (),
                s"Enrolment for ${e.key} may not be delegated to an Agent."
              )
              .andThen(_ => Enrolment.validate(e))
          )
          .reduce(_ combine _)
      case _ => Invalid("Only Agents can have delegated enrolments")
    }

  val validateDelegatedEnrolmentsValuesAreDistinct: GroupConstraint = group =>
    if (group.delegatedEnrolments.isEmpty) Valid(())
    else {
      val results = group.delegatedEnrolments
        .groupBy(_.key)
        .collect { case (key, es) if es.size > 1 => (key, es) }
        .map { case (key, es) =>
          val keys = es.map(e => e.toEnrolmentKeyTag.getOrElse(e.key))
          if (keys.size == keys.distinct.size) Valid(())
          else Invalid(s", $key")
        }
      if (results.isEmpty) Valid(())
      else
        results
          .reduce(_ combine _)
          .leftMap(keys => s"Delegated enrolment values must be distinct $keys")
    }

  private val constraints: Seq[GroupConstraint] = Seq(
    validateAffinityGroup,
    validateAgentCode,
    validateEachPrincipalEnrolment,
    validatePrincipalEnrolmentsAreDistinct,
    validateEachDelegatedEnrolment,
    validateDelegatedEnrolmentsValuesAreDistinct,
    validateSuspendedRegimes
  )

  val validate: Group => Validated[List[String], Unit] = Validator.validate(constraints: _*)
}
