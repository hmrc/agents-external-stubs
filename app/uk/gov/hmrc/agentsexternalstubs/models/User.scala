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

import cats.data.Validated.{Invalid, Valid}
import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.User.AdditionalInformation
import uk.gov.hmrc.domain.Nino

case class User(
  userId: String,
  groupId: Option[String] = None,
  affinityGroup: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  enrolments: User.Enrolments = User.Enrolments.none,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  agentCode: Option[String] = None,
  agentFriendlyName: Option[String] = None,
  agentId: Option[String] = None,
  planetId: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty,
  suspendedRegimes: Option[Set[String]] = None
) {

  def uniqueKeys: Seq[String] =
    Seq(
      Some(User.userIdKey(userId)),
      nino.map(n => User.ninoIndexKey(n.value)),
      credentialRole.find(_ == User.CR.Admin).flatMap(_ => agentCode.map(User.agentCodeIndexKey)),
      credentialRole.find(_ == User.CR.Admin).flatMap(_ => groupId.map(User.groupIdIndexKey))
    ).collect { case Some(x) =>
      x
    } ++ enrolments.principal.map(_.toEnrolmentKeyTag).collect { case Some(key) => User.enrolmentIndexKey(key) }

  def lookupKeys: Seq[String] =
    Seq(
      groupId.map(User.groupIdIndexKey),
      affinityGroup.map(User.affinityGroupKey),
      agentCode.map(User.agentCodeIndexKey),
      agentCode
        .flatMap(ac => credentialRole.map(cr => (ac, cr)))
        .map(accr => User.agentCodeWithCredentialRoleKey(accr._1, accr._2)),
      groupId
        .flatMap(gid => credentialRole.map(cr => (gid, cr)))
        .map(gidcr => User.groupIdWithCredentialRoleKey(gidcr._1, gidcr._2))
    ).collect { case Some(x) => x } ++ enrolments.delegated
      .map(_.toEnrolmentKeyTag)
      .collect { case Some(key) => User.enrolmentIndexKey(key) }

  def isIndividual: Boolean = affinityGroup.contains(User.AG.Individual)
  def isOrganisation: Boolean = affinityGroup.contains(User.AG.Organisation)
  def isAgent: Boolean = affinityGroup.contains(User.AG.Agent)

  def isAdmin: Boolean = credentialRole.contains(User.CR.Admin)
  def isUser: Boolean = credentialRole.contains(User.CR.User)
  def isAssistant: Boolean = credentialRole.contains(User.CR.Assistant)

  lazy val firstName: Option[String] =
    name
      .map(_.split(" ").dropRight(1))
      .map(a => if (a.nonEmpty) a.mkString(" ") else Generator.get(Generator.forename())(userId).getOrElse("John"))

  def lastName: Option[String] = name.map(_.split(" ").last)

  def withRecordId(recordId: String): User = copy(recordIds = recordIds :+ recordId)

  final val facts: String => Option[String] = {
    case n if n.toLowerCase.contains("postcode") => address.flatMap(_.postcode)
    case _                                       => None
  }

  def findIdentifierValue(serviceName: String, identifierName: String): Option[String] =
    enrolments.principal
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
      enrolment <- enrolments.principal.find(_.key == serviceName)
      part1     <- enrolment.identifierValueOf(identifierName1)
      part2     <- enrolment.identifierValueOf(identifierName2)
    } yield join(part1, part2)

  def findDelegatedIdentifierValues(serviceName: String, identifierName: String): Seq[String] =
    enrolments.delegated
      .filter(_.key == serviceName)
      .flatMap(_.identifiers.flatMap(_.find(_.key == identifierName)))
      .map(_.value)

  def findDelegatedIdentifierValues(serviceName: String): Seq[Seq[String]] =
    enrolments.delegated
      .filter(_.key == serviceName)
      .flatMap(_.identifiers.map(_.map(_.value)))

  def updatePrincipalEnrolments(f: Seq[Enrolment] => Seq[Enrolment]): User =
    this.copy(enrolments = this.enrolments.copy(principal = f(this.enrolments.principal)))
  def updateDelegatedEnrolments(f: Seq[Enrolment] => Seq[Enrolment]): User =
    this.copy(enrolments = this.enrolments.copy(delegated = f(this.enrolments.delegated)))
  def updateAssignedEnrolments(f: Seq[EnrolmentKey] => Seq[EnrolmentKey]): User =
    this.copy(enrolments = this.enrolments.copy(assigned = f(this.enrolments.assigned)))
}

object User {

  import play.api.libs.json.JodaWrites._
  import play.api.libs.json.JodaReads._

  case class Address(
    line1: Option[String] = None,
    line2: Option[String] = None,
    line3: Option[String] = None,
    line4: Option[String] = None,
    postcode: Option[String] = None,
    countryCode: Option[String] = None
  ) {

    def isUKAddress: Boolean = countryCode.contains("GB")

  }

  object Address {
    implicit lazy val formats: Format[Address] = Json.format[Address]
  }

  case class AdditionalInformation(vatRegistrationDate: Option[LocalDate] = None)

  object AdditionalInformation {
    implicit lazy val formats: Format[AdditionalInformation] = Json.format[AdditionalInformation]
  }

  object AG {
    final val Individual = "Individual"
    final val Organisation = "Organisation"
    final val Agent = "Agent"

    val all: String => Boolean = Set(Individual, Organisation, Agent).contains
  }

  object CR {
    final val Admin = "Admin"
    final val User = "User"
    final val Assistant = "Assistant"

    val all: String => Boolean = Set(Admin, User, Assistant).contains
  }

  object Individual {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Individual) Some(user) else None)
  }

  object Organisation {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Organisation) Some(user) else None)
  }

  object Agent {
    def unapply(user: User): Option[User] =
      user.affinityGroup.flatMap(ag => if (ag == AG.Agent) Some(user) else None)
  }

  object Matches {
    def apply(affinityGroupMatches: String => Boolean, serviceName: String): MatchesAffinityGroupAndEnrolment =
      MatchesAffinityGroupAndEnrolment(affinityGroupMatches, serviceName)
  }

  case class MatchesAffinityGroupAndEnrolment(affinityGroupMatches: String => Boolean, serviceName: String) {
    def unapply(user: User): Option[(User, String)] =
      user.affinityGroup
        .flatMap(ag => if (affinityGroupMatches(ag)) Some(user) else None)
        .flatMap(_.enrolments.principal.find(_.key == serviceName))
        .flatMap(_.identifiers)
        .flatMap(_.map(_.value).headOption)
        .map((user, _))
  }

  case class Enrolments(
    principal: Seq[Enrolment] = Seq.empty,
    delegated: Seq[Enrolment] = Seq.empty,
    assigned: Seq[EnrolmentKey] = Seq.empty
  )

  object Enrolments {
    import play.api.libs.functional.syntax._
    implicit val reads: Reads[Enrolments] = (
      (JsPath \ "principal").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "delegated").readNullable[Seq[Enrolment]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "assigned").readNullable[Seq[EnrolmentKey]].map(_.getOrElse(Seq.empty))
    )(Enrolments.apply _)
    implicit val writes: Writes[Enrolments] = Json.writes[Enrolments]

    val none = Enrolments(Seq.empty, Seq.empty, Seq.empty)
  }

  def validate(user: User): Either[List[String], User] = UserValidator.validate(user) match {
    case Valid(())       => Right(user)
    case Invalid(errors) => Left(errors)
  }

  implicit class UserBuilder(val user: User) extends AnyVal {
    def withPrincipalEnrolment(enrolment: Enrolment): User =
      user.updatePrincipalEnrolments(_ :+ enrolment)

    def withPrincipalEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withPrincipalEnrolment(Enrolment(service, identifierKey, identifierValue))

    def withPrincipalEnrolment(enrolmentKey: String): User =
      withPrincipalEnrolment(
        Enrolment.from(EnrolmentKey.parse(enrolmentKey).fold(e => throw new Exception(e), identity))
      )

    def withStrideRole(role: String): User = user.copy(strideRoles = user.strideRoles :+ role)

    def withDelegatedEnrolment(enrolment: Enrolment): User =
      user.updateDelegatedEnrolments(_ :+ enrolment)

    def withDelegatedEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withDelegatedEnrolment(Enrolment(service, Some(Seq(Identifier(identifierKey, identifierValue)))))

    def withDelegatedEnrolment(enrolmentKey: String): User =
      withDelegatedEnrolment(
        Enrolment.from(EnrolmentKey.parse(enrolmentKey).fold(e => throw new Exception(e), identity))
      )
  }

  def userIdKey(userId: String): String = s"uid:$userId"
  def ninoIndexKey(nino: String): String = s"nino:${nino.replace(" ", "").toLowerCase}"
  def enrolmentIndexKey(key: String): String = s"enr:${key.toLowerCase}"
  def groupIdIndexKey(groupId: String): String = s"gid:$groupId"
  def agentCodeIndexKey(agentCode: String): String = s"ac:$agentCode"
  def affinityGroupKey(affinityGroup: String): String = s"ag:${affinityGroup.toLowerCase}"
  def groupIdWithCredentialRoleKey(groupId: String, credentialRole: String): String =
    s"gid+cr:${groupId}__${credentialRole.toLowerCase}"
  def agentCodeWithCredentialRoleKey(agentCode: String, credentialRole: String): String =
    s"ac+cr:${agentCode}__${credentialRole.toLowerCase}"

  import play.api.libs.functional.syntax._

  implicit val reads: Reads[User] = (
    (JsPath \ "userId").readNullable[String].map(_.orNull) and
      (JsPath \ "groupId").readNullable[String] and
      (JsPath \ "affinityGroup").readNullable[String] and
      (JsPath \ "confidenceLevel").readNullable[Int] and
      (JsPath \ "credentialStrength").readNullable[String] and
      (JsPath \ "credentialRole").readNullable[String] and
      (JsPath \ "nino").readNullable[Nino] and
      (JsPath \ "enrolments").readNullable[Enrolments].map(_.getOrElse(Enrolments.none)) and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "dateOfBirth").readNullable[LocalDate] and
      (JsPath \ "agentCode").readNullable[String] and
      (JsPath \ "agentFriendlyName").readNullable[String] and
      (JsPath \ "agentId").readNullable[String] and
      (JsPath \ "planetId").readNullable[String] and
      (JsPath \ "isNonCompliant").readNullable[Boolean] and
      (JsPath \ "complianceIssues").readNullable[Seq[String]] and
      (JsPath \ "recordIds").readNullable[Seq[String]].map(_.map(_.distinct).getOrElse(Seq.empty)) and
      (JsPath \ "address").readNullable[Address] and
      (JsPath \ "additionalInformation").readNullable[AdditionalInformation] and
      (JsPath \ "strideRoles").readNullable[Seq[String]].map(_.getOrElse(Seq.empty)) and
      (JsPath \ "suspendedRegimes").readNullable[Set[String]]
  )(User.apply _)

  implicit val writes: Writes[User] = Json.writes[User]

  val formats = Format(reads, writes)

  def parseUserIdAtPlanetId(credId: String, defaultPlanetId: => String): (String, String) = {
    val at = credId.indexOf('@')
    if (at >= 0) (credId.substring(0, at), credId.substring(at + 1))
    else (credId, defaultPlanetId)
  }

  def knownFactsOf(user: User): Map[String, String] = {
    val postcodeOpt = user.address.flatMap(_.postcode)
    val vatRegDateOpt = user.additionalInformation
      .flatMap(_.vatRegistrationDate.map(_.toString("dd/MM/yy")))
    Seq(
      "PostCode"            -> postcodeOpt,
      "BusinessPostcode"    -> postcodeOpt,
      "businesspostcode"    -> postcodeOpt,
      "Postcode"            -> postcodeOpt,
      "POSTCODE"            -> postcodeOpt,
      "AgencyPostcode"      -> postcodeOpt,
      "IRAgentPostcode"     -> postcodeOpt,
      "IRPCODE"             -> postcodeOpt,
      "IREFFREGDATE"        -> vatRegDateOpt,
      "VATRegistrationDate" -> vatRegDateOpt,
      "CountryCode"         -> user.address.flatMap(_.countryCode)
    ).collect { case (a, Some(b)) =>
      (a, b)
    }.toMap[String, String]
  }

}
