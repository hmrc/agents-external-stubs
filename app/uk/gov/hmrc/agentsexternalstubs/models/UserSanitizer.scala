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

case class UserSanitizer(affinityGroup: Option[String]) extends RecordUtils[User] {

  def sanitize(user: User): User = sanitize(user.userId)(user)

  override val gen: Gen[User] =
    for (userId <- Gen.uuid.map(_.toString)) yield User(userId = userId)

  private val ensureUserHaveName: Update = seed =>
    user =>
      if (user.name.isEmpty)
        affinityGroup match {
          case Some(AG.Individual) => user.copy(name = Some(UserGenerator.nameForIndividual(seed)))
          case Some(AG.Agent) =>
            user.copy(name = Some(UserGenerator.nameForAgent(seed, user.groupId.getOrElse(seed))))
          case Some(_) => user.copy(name = Some(UserGenerator.nameForOrganisation(seed)))
          case None    => user
        }
      else user

  private val ensureStrideUserHaveNoGatewayEnrolmentsNorGroupIdNorOtherData: Update = seed =>
    user =>
      if (affinityGroup.isEmpty || user.strideRoles.nonEmpty)
        user.copy(
          groupId = None,
          assignedPrincipalEnrolments = Seq.empty,
          assignedDelegatedEnrolments = Seq.empty,
          nino = None,
          additionalInformation = None,
          address = None,
          confidenceLevel = None,
          credentialStrength = None,
          credentialRole = None
        )
      else user

  private val ensureAgentsAndIndividualsHaveANino: Update = seed =>
    user =>
      affinityGroup match {
        case Some(AG.Organisation) => user // APB-6051 Organisations may also have a Nino
        case Some(_)               => if (user.nino.isEmpty) user.copy(nino = Some(Generator.ninoWithSpaces(seed))) else user
        case None                  => user.copy(nino = None)
      }

  private val ensureOnlyIndividualUserHaveConfidenceLevel: Update = seed =>
    user =>
      affinityGroup match {
        case Some(AG.Individual) =>
          if (user.confidenceLevel.isEmpty)
            user.copy(confidenceLevel = Some(250))
          else user
        case _ => user.copy(confidenceLevel = None)
      }

  private val ensureUserHaveCredentialRole: Update = seed =>
    user =>
      affinityGroup match {
        case Some(AG.Individual | AG.Agent) =>
          if (user.credentialRole.isEmpty) user.copy(credentialRole = Some(User.CR.User)) else user
        case Some(AG.Organisation) =>
          if (user.credentialRole.isEmpty) user.copy(credentialRole = Some(User.CR.Admin)) else user
        case _ => user.copy(credentialRole = None)
      }

  private val ensuresAgentsAndIndividualsHaveDateOfBirth: Update = seed =>
    user =>
      affinityGroup match {
        case Some(AG.Organisation) => user
        case Some(_) =>
          if (user.dateOfBirth.isEmpty) user.copy(dateOfBirth = Some(UserGenerator.dateOfBirth(seed))) else user
        case None => user.copy(dateOfBirth = None)
      }

  // ensure user has group id unless there is no affinity group specified (in which case the user is e.g. Stride and there is no group)
  private val ensureUserHaveGroupIdentifier: Update = seed =>
    user =>
      if (user.groupId.isEmpty && affinityGroup.nonEmpty) user.copy(groupId = Some(UserGenerator.groupId(seed)))
      else user

  private val ensurePrincipalEnrolmentKeysAreDistinct: Update = seed =>
    user => {
      user.copy(assignedPrincipalEnrolments =
        user.assignedPrincipalEnrolments
          .groupBy(_.service)
          .collect {
            case (key, eks) if eks.size == 1 || Services(key).exists(_.flags.multipleEnrolment) => eks
            case (_, eks) =>
              Seq(eks.maxBy(_.identifiers.size))
          }
          .flatten
          .toSeq
      )
    }

  private val ensurePrincipalEnrolmentsHaveIdentifiers: Update = seed =>
    user => {
      user.copy(assignedPrincipalEnrolments =
        user.assignedPrincipalEnrolments
          .groupBy(_.service)
          .flatMap { case (_, eks) =>
            eks.zipWithIndex.map { case (ek, i) => ensureEnrolmentKeyHasIdentifier(Generator.variant(seed, i), ek) }
          }
          .toSeq
      )
    }

  private val ensureDelegatedEnrolmentsHaveIdentifiers: Update = seed =>
    user => {
      user.copy(assignedDelegatedEnrolments =
        user.assignedDelegatedEnrolments
          .groupBy(_.service)
          .flatMap { case (_, eks) =>
            eks.zipWithIndex.map { case (ek, i) => ensureEnrolmentKeyHasIdentifier(Generator.variant(seed, i), ek) }
          }
          .toSeq
      )
    }

  private def ensureEnrolmentKeyHasIdentifier(seed: String, ek: EnrolmentKey): EnrolmentKey =
    ensureEnrolmentHaveIdentifier(seed)(Enrolment.from(ek)).toEnrolmentKey.get

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

  private def sanitizeAddress(addressOpt: Option[User.Address], seed: String): Option[User.Address] = {
    val newAddress = Generator.address(seed)
    addressOpt match {
      case None =>
        Some(
          User.Address(
            line1 = Some(newAddress.street.take(35)),
            line2 = Some(newAddress.town.take(35)),
            postcode = Some(newAddress.postcode),
            countryCode = Some("GB")
          )
        )
      case Some(address) =>
        Some(
          address.copy(
            line1 = address.line1.map(_.take(35)).orElse(Some(newAddress.street.take(35))),
            line2 = address.line2.map(_.take(35)).orElse(Some(newAddress.town.take(35))),
            postcode = UserValidator(affinityGroup)
              .postalCodeValidator(address.postcode)
              .fold(_ => None, _ => address.postcode)
              .orElse(Generator.get(Generator.postcode)(seed)),
            countryCode = UserValidator(affinityGroup)
              .countryCodeValidator(address.countryCode)
              .fold(_ => None, _ => address.countryCode)
              .orElse(Some("GB"))
          )
        )
    }
  }

  private val ensureUserHaveAddress: Update = seed => user => user.copy(address = sanitizeAddress(user.address, seed))

  override val sanitizers: Seq[Update] =
    Seq(
      ensureUserHaveGroupIdentifier,
      ensureUserHaveName,
      ensureStrideUserHaveNoGatewayEnrolmentsNorGroupIdNorOtherData,
      ensureAgentsAndIndividualsHaveANino,
      ensureOnlyIndividualUserHaveConfidenceLevel,
      ensureUserHaveCredentialRole,
      ensuresAgentsAndIndividualsHaveDateOfBirth,
      ensureUserHaveAddress,
      ensurePrincipalEnrolmentKeysAreDistinct,
      ensurePrincipalEnrolmentsHaveIdentifiers,
      ensureDelegatedEnrolmentsHaveIdentifiers
    )

  override val validate: Validator[User] = UserValidator(affinityGroup).validate

}
