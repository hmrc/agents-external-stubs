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

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.CreateUpdateAgentRelationshipPayload._

/** ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/DES1167.json app/uk/gov/hmrc/agentsexternalstubs/models/CreateUpdateAgentRelationshipPayload.scala CreateUpdateAgentRelationshipPayload output:payload"
  * ----------------------------------------------------------------------------
  *
  *  CreateUpdateAgentRelationshipPayload
  *  -  Authorisation
  *  -  Authorise
  *  -  Deauthorise
  */
case class CreateUpdateAgentRelationshipPayload(
  acknowledgmentReference: String,
  refNumber: String,
  idType: Option[String] = None,
  agentReferenceNumber: String,
  regime: String,
  authorisation: Authorisation,
  relationshipType: Option[String] = None,
  authProfile: Option[String] = None
) {

  def withAcknowledgmentReference(acknowledgmentReference: String): CreateUpdateAgentRelationshipPayload =
    copy(acknowledgmentReference = acknowledgmentReference)
  def modifyAcknowledgmentReference(pf: PartialFunction[String, String]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(acknowledgmentReference)) copy(acknowledgmentReference = pf(acknowledgmentReference)) else this
  def withRefNumber(refNumber: String): CreateUpdateAgentRelationshipPayload = copy(refNumber = refNumber)
  def modifyRefNumber(pf: PartialFunction[String, String]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(refNumber)) copy(refNumber = pf(refNumber)) else this
  def withIdType(idType: Option[String]): CreateUpdateAgentRelationshipPayload = copy(idType = idType)
  def modifyIdType(pf: PartialFunction[Option[String], Option[String]]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(idType)) copy(idType = pf(idType)) else this
  def withAgentReferenceNumber(agentReferenceNumber: String): CreateUpdateAgentRelationshipPayload =
    copy(agentReferenceNumber = agentReferenceNumber)
  def modifyAgentReferenceNumber(pf: PartialFunction[String, String]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(agentReferenceNumber)) copy(agentReferenceNumber = pf(agentReferenceNumber)) else this
  def withRegime(regime: String): CreateUpdateAgentRelationshipPayload = copy(regime = regime)
  def modifyRegime(pf: PartialFunction[String, String]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(regime)) copy(regime = pf(regime)) else this
  def withAuthorisation(authorisation: Authorisation): CreateUpdateAgentRelationshipPayload =
    copy(authorisation = authorisation)
  def modifyAuthorisation(pf: PartialFunction[Authorisation, Authorisation]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(authorisation)) copy(authorisation = pf(authorisation)) else this
  def withRelationshipType(relationshipType: Option[String]): CreateUpdateAgentRelationshipPayload =
    copy(relationshipType = relationshipType)
  def modifyRelationshipType(
    pf: PartialFunction[Option[String], Option[String]]
  ): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(relationshipType)) copy(relationshipType = pf(relationshipType)) else this
  def withAuthProfile(authProfile: Option[String]): CreateUpdateAgentRelationshipPayload =
    copy(authProfile = authProfile)
  def modifyAuthProfile(pf: PartialFunction[Option[String], Option[String]]): CreateUpdateAgentRelationshipPayload =
    if (pf.isDefinedAt(authProfile)) copy(authProfile = pf(authProfile)) else this
}

object CreateUpdateAgentRelationshipPayload {

  import Validator._

  val acknowledgmentReferenceValidator: Validator[String] = check(
    _.matches(Common.acknowledgmentReferencePattern),
    s"""Invalid acknowledgmentReference, does not matches regex ${Common.acknowledgmentReferencePattern}"""
  )
  val refNumberValidator: Validator[String] = check(
    _.matches(Common.refNumberPattern),
    s"""Invalid refNumber, does not matches regex ${Common.refNumberPattern}"""
  )
  val defaultIdTypeValidator: Validator[Option[String]] =
    check(_.matches(Common.idTypePattern), s"""Invalid idType, does not matches regex ${Common.idTypePattern}""")
  val noUrnIdTypeValidator: Validator[Option[String]] =
    check(
      _.matches(Common.idTypePatternNoUrn),
      s"""Invalid idType, does not matches regex ${Common.idTypePatternNoUrn}"""
    )
  val agentReferenceNumberValidator: Validator[String] = check(
    _.matches(Common.agentReferenceNumberPattern),
    s"""Invalid agentReferenceNumber, does not matches regex ${Common.agentReferenceNumberPattern}"""
  )
  val regimeValidator: Validator[String] =
    check(_.matches(Common.regimePattern), s"""Invalid regime, does not matches regex ${Common.regimePattern}""")
  val authorisationValidator: Validator[Authorisation] = checkProperty(identity, Authorisation.validate)
  val relationshipTypeValidator: Validator[Option[String]] =
    check(_.isOneOf(Common.relationshipTypeEnum), "Invalid relationshipType, does not match allowed values")
  val authProfileValidator: Validator[Option[String]] =
    check(_.isOneOf(Common.authProfileEnum), "Invalid authProfile, does not match allowed values")

  def validate(
    idTypeValidator: Validator[Option[String]] = defaultIdTypeValidator
  ): Validator[CreateUpdateAgentRelationshipPayload] = Validator(
    checkProperty(_.acknowledgmentReference, acknowledgmentReferenceValidator),
    checkProperty(_.refNumber, refNumberValidator),
    checkProperty(_.idType, idTypeValidator),
    checkProperty(_.agentReferenceNumber, agentReferenceNumberValidator),
    checkProperty(_.regime, regimeValidator),
    checkProperty(_.authorisation, authorisationValidator),
    checkProperty(_.relationshipType, relationshipTypeValidator),
    checkProperty(_.authProfile, authProfileValidator)
  )

  implicit val formats: Format[CreateUpdateAgentRelationshipPayload] = Json.format[CreateUpdateAgentRelationshipPayload]

  sealed trait Authorisation { def action: String }

  object Authorisation {

    val validate: Validator[Authorisation] = {
      case x: Authorise   => Authorise.validate(x)
      case x: Deauthorise => Deauthorise.validate(x)
    }

    implicit val reads: Reads[Authorisation] = new Reads[Authorisation] {
      override def reads(json: JsValue): JsResult[Authorisation] = {
        val r0 =
          Authorise.formats.reads(json).flatMap(e => Authorise.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          Deauthorise.formats.reads(json).flatMap(e => Deauthorise.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        )
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of Authorisation, i.e. Authorise, Deauthorise"),
            r0,
            r1
          )
        )
      }

      private def aggregateErrors[T](errors: JsResult[T]*): JsError =
        errors.foldLeft(JsError())((a, r) =>
          r match {
            case e: JsError => JsError(a.errors ++ e.errors)
            case _          => a
          }
        )
    }

    implicit val writes: Writes[Authorisation] = new Writes[Authorisation] {
      override def writes(o: Authorisation): JsValue = o match {
        case x: Authorise   => Authorise.formats.writes(x)
        case x: Deauthorise => Deauthorise.formats.writes(x)
      }
    }

  }

  case class Authorise(override val action: String, isExclusiveAgent: Boolean = false) extends Authorisation {

    def withAction(action: String): Authorise = copy(action = action)
    def modifyAction(pf: PartialFunction[String, String]): Authorise =
      if (pf.isDefinedAt(action)) copy(action = pf(action)) else this
    def withIsExclusiveAgent(isExclusiveAgent: Boolean): Authorise = copy(isExclusiveAgent = isExclusiveAgent)
    def modifyIsExclusiveAgent(pf: PartialFunction[Boolean, Boolean]): Authorise =
      if (pf.isDefinedAt(isExclusiveAgent)) copy(isExclusiveAgent = pf(isExclusiveAgent)) else this
  }

  object Authorise {

    val actionValidator: Validator[String] =
      check(_.isOneOf(Common.actionEnum1), "Invalid action, does not match allowed values")

    val validate: Validator[Authorise] = Validator(checkProperty(_.action, actionValidator))

    implicit val formats: Format[Authorise] = Json.format[Authorise]

  }

  case class Deauthorise(override val action: String) extends Authorisation {

    def withAction(action: String): Deauthorise = copy(action = action)
    def modifyAction(pf: PartialFunction[String, String]): Deauthorise =
      if (pf.isDefinedAt(action)) copy(action = pf(action)) else this
  }

  object Deauthorise {

    val actionValidator: Validator[String] =
      check(_.isOneOf(Common.actionEnum0), "Invalid action, does not match allowed values")

    val validate: Validator[Deauthorise] = Validator(checkProperty(_.action, actionValidator))

    implicit val formats: Format[Deauthorise] = Json.format[Deauthorise]

  }

  object Common {
    val relationshipTypeEnum = Seq("ZA01", "ZA02")
    val actionEnum0 = Seq("De-Authorise")
    val authProfileEnum =
      Seq("ALL00001", "CORR0001", "EDIT0001", "FILE0001", "FILE0002", "FILE0003", "FILE0004", "VIEW0001")
    val refNumberPattern = """^[0-9A-Za-z]{1,15}$"""
    val acknowledgmentReferencePattern = """^[0-9A-Za-z]{1,32}$"""
    val agentReferenceNumberPattern = """^[A-Z](ARN)[0-9]{7}$"""
    val regimePattern = """^[A-Z]{2,10}$"""
    val actionEnum1 = Seq("Authorise")
    val idTypePattern = """^[0-9A-Za-z]{1,6}$"""
    val idTypePatternNoUrn = """^(?i)(?!urn$)(?-i)[0-9A-Za-z]{1,6}$"""
  }
}
