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
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.admin.User.AdditionalInformation
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, EnrolmentKey, Generator, Identifier}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class User(
  userId: String,
  groupId: Option[String] = None,
  confidenceLevel: Option[Int] = None,
  credentialStrength: Option[String] = None,
  credentialRole: Option[String] = None,
  nino: Option[Nino] = None,
  assignedPrincipalEnrolments: Seq[EnrolmentKey] = Seq.empty,
  assignedDelegatedEnrolments: Seq[EnrolmentKey] = Seq.empty,
  name: Option[String] = None,
  dateOfBirth: Option[LocalDate] = None,
  planetId: Option[String] = None,
  isNonCompliant: Option[Boolean] = None,
  complianceIssues: Option[Seq[String]] = None,
  recordIds: Seq[String] = Seq.empty,
  address: Option[User.Address] = None,
  additionalInformation: Option[AdditionalInformation] = None,
  strideRoles: Seq[String] = Seq.empty
) {

  def uniqueKeys: Seq[String] =
    Seq(
      Some(User.userIdKey(userId)),
      nino.map(n => User.ninoIndexKey(n.value)),
      credentialRole.find(_ == User.CR.Admin).flatMap(_ => groupId.map(User.groupIdIndexKey))
    ).collect { case Some(x) =>
      x
    }

  def lookupKeys: Seq[String] =
    Seq(
      groupId.map(User.groupIdIndexKey),
      credentialRole.map(cr => User.credentialRoleKey(cr)),
      groupId
        .flatMap(gid => credentialRole.map(cr => (gid, cr)))
        .map(gidcr => User.groupIdWithCredentialRoleKey(gidcr._1, gidcr._2))
    ).collect { case Some(x) => x } ++ assignedPrincipalEnrolments.map(ek =>
      User.assignedPrincipalEnrolmentIndexKey(ek.tag)
    ) ++ assignedDelegatedEnrolments.map(ek => User.assignedDelegatedEnrolmentIndexKey(ek.tag))

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
}

object User {

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

  object CR {
    final val Admin = "Admin"
    final val User = "User"
    final val Assistant = "Assistant"

    val all: String => Boolean = Set(Admin, User, Assistant).contains
  }

  case class Enrolments(
    principal: Seq[Enrolment] = Seq.empty,
    delegated: Seq[Enrolment] = Seq.empty,
    assigned: Seq[EnrolmentKey] = Seq.empty
  )

  def validate(user: User, affinityGroup: Option[String]): Either[List[String], User] =
    UserValidator(affinityGroup).validate(user) match {
      case Valid(())       => Right(user)
      case Invalid(errors) => Left(errors)
    }

  implicit class UserBuilder(val user: User) extends AnyVal {
    def withStrideRole(role: String): User = user.copy(strideRoles = user.strideRoles :+ role)

    def withAssignedPrincipalEnrolment(enrolmentKey: EnrolmentKey): User =
      user.copy(assignedPrincipalEnrolments = user.assignedPrincipalEnrolments ++ Seq(enrolmentKey))

    def withAssignedPrincipalEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withAssignedPrincipalEnrolment(EnrolmentKey(service, Seq(Identifier(identifierKey, identifierValue))))

    def withAssignedPrincipalEnrolment(service: String, identifiers: Seq[Identifier]): User =
      withAssignedPrincipalEnrolment(EnrolmentKey(service, identifiers))

    def withAssignedDelegatedEnrolment(enrolmentKey: EnrolmentKey): User =
      user.copy(assignedDelegatedEnrolments = user.assignedDelegatedEnrolments ++ Seq(enrolmentKey))

    def withAssignedDelegatedEnrolment(service: String, identifierKey: String, identifierValue: String): User =
      withAssignedDelegatedEnrolment(EnrolmentKey(service, Seq(Identifier(identifierKey, identifierValue))))
  }

  def userIdKey(userId: String): String = s"uid:$userId"
  def ninoIndexKey(nino: String): String = s"nino:${nino.replace(" ", "").toLowerCase}"
  def assignedPrincipalEnrolmentIndexKey(key: String): String = s"apenr:${key.toLowerCase}"
  def assignedDelegatedEnrolmentIndexKey(key: String): String = s"adenr:${key.toLowerCase}"
  def groupIdIndexKey(groupId: String): String = s"gid:$groupId"
  def agentCodeIndexKey(agentCode: String): String = s"ac:$agentCode"
  def affinityGroupKey(affinityGroup: String): String = s"ag:${affinityGroup.toLowerCase}"
  def groupIdWithCredentialRoleKey(groupId: String, credentialRole: String): String =
    s"gid+cr:${groupId}__${credentialRole.toLowerCase}"
  def credentialRoleKey(credentialRole: String): String =
    s"cr:${credentialRole.toLowerCase}"

  import play.api.libs.functional.syntax._

  def reads(tolerateEnrolmentKeysWithNoIdentifiers: Boolean): Reads[User] = {
    implicit val enrolmentKeyReads: Reads[EnrolmentKey] =
      if (tolerateEnrolmentKeysWithNoIdentifiers) EnrolmentKey.tolerantReads else EnrolmentKey.reads
    (
      (JsPath \ "userId").readNullable[String].map(_.orNull) and
        (JsPath \ "groupId").readNullable[String] and
        (JsPath \ "confidenceLevel").readNullable[Int] and
        (JsPath \ "credentialStrength").readNullable[String] and
        (JsPath \ "credentialRole").readNullable[String] and
        (JsPath \ "nino").readNullable[Nino] and
        (JsPath \ "assignedPrincipalEnrolments").readNullable[Seq[EnrolmentKey]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "assignedDelegatedEnrolments").readNullable[Seq[EnrolmentKey]].map(_.getOrElse(Seq.empty)) and
        (JsPath \ "name").readNullable[String] and
        (JsPath \ "dateOfBirth").readNullable[LocalDate] and
        (JsPath \ "planetId").readNullable[String] and
        (JsPath \ "isNonCompliant").readNullable[Boolean] and
        (JsPath \ "complianceIssues").readNullable[Seq[String]] and
        (JsPath \ "recordIds").readNullable[Seq[String]].map(_.map(_.distinct).getOrElse(Seq.empty)) and
        (JsPath \ "address").readNullable[Address] and
        (JsPath \ "additionalInformation").readNullable[AdditionalInformation] and
        (JsPath \ "strideRoles").readNullable[Seq[String]].map(_.getOrElse(Seq.empty))
    )(User.apply _)
  }

  implicit val reads: Reads[User] = reads(tolerateEnrolmentKeysWithNoIdentifiers = false)
  val tolerantReads: Reads[User] = reads(tolerateEnrolmentKeysWithNoIdentifiers = true)

  implicit val writes: OWrites[User] = Json.writes[User]

  val formats = OFormat(reads, writes)

  def parseUserIdAtPlanetId(credId: String, defaultPlanetId: => String): (String, String) = {
    val at = credId.indexOf('@')
    if (at >= 0) (credId.substring(0, at), credId.substring(at + 1))
    else (credId, defaultPlanetId)
  }

  def knownFactsOf(user: User): Map[String, String] = {
    val postcodeOpt = user.address.flatMap(_.postcode)
    val vatRegDateOpt = user.additionalInformation
      .flatMap(_.vatRegistrationDate.map(_.format(DateTimeFormatter.ofPattern("dd/MM/yy"))))
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
