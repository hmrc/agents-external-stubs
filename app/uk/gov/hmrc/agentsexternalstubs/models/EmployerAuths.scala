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

import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.EmployerAuths.EmpAuth

/** ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/employerAuths.schema_v0_6.json app/uk/gov/hmrc/agentsexternalstubs/models/EmployerAuths.scala EmployerAuths output:record"
  * ----------------------------------------------------------------------------
  *
  *  EmployerAuths
  *  -  EmpAuth
  *  -  -  EmpRef
  *  -  -  AoRef
  */
case class EmployerAuths(
  agentCode: String,
  empAuthList: Seq[EmpAuth],
  id: Option[String] = None
) extends Record {

  override def uniqueKey: Option[String] = Option(agentCode).map(EmployerAuths.uniqueKey)
  override def lookupKeys: Seq[String] = Seq()
  override def withId(id: Option[String]): EmployerAuths = copy(id = id)

  def withAgentCode(agentCode: String): EmployerAuths = copy(agentCode = agentCode)
  def modifyAgentCode(pf: PartialFunction[String, String]): EmployerAuths =
    if (pf.isDefinedAt(agentCode)) copy(agentCode = pf(agentCode)) else this
  def withEmpAuthList(empAuthList: Seq[EmpAuth]): EmployerAuths = copy(empAuthList = empAuthList)
  def modifyEmpAuthList(pf: PartialFunction[Seq[EmpAuth], Seq[EmpAuth]]): EmployerAuths =
    if (pf.isDefinedAt(empAuthList)) copy(empAuthList = pf(empAuthList)) else this
}

object EmployerAuths extends RecordUtils[EmployerAuths] {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
  implicit val recordType: RecordMetaData[EmployerAuths] = RecordMetaData[EmployerAuths](this)

  def uniqueKey(key: String): String = s"""agentCode:${key.toUpperCase}"""

  import Validator._

  val agentCodeValidator: Validator[String] = check(
    _.matches(Common.agentCodePattern),
    s"""Invalid agentCode, does not matches regex ${Common.agentCodePattern}"""
  )
  val empAuthListValidator: Validator[Seq[EmpAuth]] = Validator(
    checkEach(identity, EmpAuth.validate),
    check(_.size >= 1, "Invalid array size, must be greater than or equal to 1")
  )

  override val validate: Validator[EmployerAuths] =
    Validator(checkProperty(_.agentCode, agentCodeValidator), checkProperty(_.empAuthList, empAuthListValidator))

  override val gen: Gen[EmployerAuths] = for {
    agentCode   <- Generator.regex(Common.agentCodePattern)
    empAuthList <- Generator.nonEmptyListOfMaxN(1, EmpAuth.gen)
  } yield EmployerAuths(
    agentCode = agentCode,
    empAuthList = empAuthList
  )

  val empAuthListSanitizer: Update = seed =>
    entity => entity.copy(empAuthList = entity.empAuthList.map(item => EmpAuth.sanitize(seed)(item)))

  override val sanitizers: Seq[Update] = Seq(empAuthListSanitizer)

  implicit val formats: Format[EmployerAuths] = Json.format[EmployerAuths]

  case class EmpAuth(
    empRef: EmpAuth.EmpRef,
    aoRef: EmpAuth.AoRef,
    `Auth_64-8`: Boolean = false,
    Auth_OAA: Boolean = false,
    agentClientRef: Option[String] = None,
    employerName1: Option[String] = None,
    employerName2: Option[String] = None
  ) {

    def withEmpRef(empRef: EmpAuth.EmpRef): EmpAuth = copy(empRef = empRef)
    def modifyEmpRef(pf: PartialFunction[EmpAuth.EmpRef, EmpAuth.EmpRef]): EmpAuth =
      if (pf.isDefinedAt(empRef)) copy(empRef = pf(empRef)) else this
    def withAoRef(aoRef: EmpAuth.AoRef): EmpAuth = copy(aoRef = aoRef)
    def modifyAoRef(pf: PartialFunction[EmpAuth.AoRef, EmpAuth.AoRef]): EmpAuth =
      if (pf.isDefinedAt(aoRef)) copy(aoRef = pf(aoRef)) else this
    def withAuth_64_8(`Auth_64-8`: Boolean): EmpAuth = copy(`Auth_64-8` = `Auth_64-8`)
    def modifyAuth_64_8(pf: PartialFunction[Boolean, Boolean]): EmpAuth =
      if (pf.isDefinedAt(`Auth_64-8`)) copy(`Auth_64-8` = pf(`Auth_64-8`)) else this
    def withAuth_OAA(Auth_OAA: Boolean): EmpAuth = copy(Auth_OAA = Auth_OAA)
    def modifyAuth_OAA(pf: PartialFunction[Boolean, Boolean]): EmpAuth =
      if (pf.isDefinedAt(Auth_OAA)) copy(Auth_OAA = pf(Auth_OAA)) else this
    def withAgentClientRef(agentClientRef: Option[String]): EmpAuth = copy(agentClientRef = agentClientRef)
    def modifyAgentClientRef(pf: PartialFunction[Option[String], Option[String]]): EmpAuth =
      if (pf.isDefinedAt(agentClientRef)) copy(agentClientRef = pf(agentClientRef)) else this
    def withEmployerName1(employerName1: Option[String]): EmpAuth = copy(employerName1 = employerName1)
    def modifyEmployerName1(pf: PartialFunction[Option[String], Option[String]]): EmpAuth =
      if (pf.isDefinedAt(employerName1)) copy(employerName1 = pf(employerName1)) else this
    def withEmployerName2(employerName2: Option[String]): EmpAuth = copy(employerName2 = employerName2)
    def modifyEmployerName2(pf: PartialFunction[Option[String], Option[String]]): EmpAuth =
      if (pf.isDefinedAt(employerName2)) copy(employerName2 = pf(employerName2)) else this
  }

  object EmpAuth extends RecordUtils[EmpAuth] {

    val empRefValidator: Validator[EmpRef] = checkProperty(identity, EmpRef.validate)
    val aoRefValidator: Validator[AoRef] = checkProperty(identity, AoRef.validate)
    val agentClientRefValidator: Validator[Option[String]] = check(
      _.matches(Common.agentClientRefPattern),
      s"""Invalid agentClientRef, does not matches regex ${Common.agentClientRefPattern}"""
    )
    val employerName1Validator: Validator[Option[String]] = check(
      _.matches(Common.employerNamePattern),
      s"""Invalid employerName1, does not matches regex ${Common.employerNamePattern}"""
    )
    val employerName2Validator: Validator[Option[String]] = check(
      _.matches(Common.employerNamePattern),
      s"""Invalid employerName2, does not matches regex ${Common.employerNamePattern}"""
    )

    override val validate: Validator[EmpAuth] = Validator(
      checkProperty(_.empRef, empRefValidator),
      checkProperty(_.aoRef, aoRefValidator),
      checkProperty(_.agentClientRef, agentClientRefValidator),
      checkProperty(_.employerName1, employerName1Validator),
      checkProperty(_.employerName2, employerName2Validator)
    )

    override val gen: Gen[EmpAuth] = for {
      empRef    <- EmpRef.gen
      aoRef     <- AoRef.gen
      auth_64_8 <- Generator.booleanGen
      auth_oaa  <- Generator.booleanGen
    } yield EmpAuth(
      empRef = empRef,
      aoRef = aoRef,
      `Auth_64-8` = auth_64_8,
      Auth_OAA = auth_oaa
    )

    val empRefSanitizer: Update = seed => entity => entity.copy(empRef = EmpRef.sanitize(seed)(entity.empRef))

    val aoRefSanitizer: Update = seed => entity => entity.copy(aoRef = AoRef.sanitize(seed)(entity.aoRef))

    val agentClientRefSanitizer: Update = seed =>
      entity =>
        entity.copy(
          agentClientRef = agentClientRefValidator(entity.agentClientRef)
            .fold(_ => None, _ => entity.agentClientRef)
            .orElse(Generator.get(Generator.regex(Common.agentClientRefPattern))(seed))
        )

    val employerName1Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          employerName1 = employerName1Validator(entity.employerName1)
            .fold(_ => None, _ => entity.employerName1)
            .orElse(Generator.get(Generator.regex(Common.employerNamePattern))(seed))
        )

    val employerName2Sanitizer: Update = seed =>
      entity =>
        entity.copy(
          employerName2 = employerName2Validator(entity.employerName2)
            .fold(_ => None, _ => entity.employerName2)
            .orElse(Generator.get(Generator.regex(Common.employerNamePattern))(seed))
        )

    override val sanitizers: Seq[Update] =
      Seq(empRefSanitizer, aoRefSanitizer, agentClientRefSanitizer, employerName1Sanitizer, employerName2Sanitizer)

    implicit val formats: Format[EmpAuth] = Json.format[EmpAuth]

    case class EmpRef(districtNumber: String, reference: String) {

      def withDistrictNumber(districtNumber: String): EmpRef = copy(districtNumber = districtNumber)
      def modifyDistrictNumber(pf: PartialFunction[String, String]): EmpRef =
        if (pf.isDefinedAt(districtNumber)) copy(districtNumber = pf(districtNumber)) else this
      def withReference(reference: String): EmpRef = copy(reference = reference)
      def modifyReference(pf: PartialFunction[String, String]): EmpRef =
        if (pf.isDefinedAt(reference)) copy(reference = pf(reference)) else this
    }

    object EmpRef extends RecordUtils[EmpRef] {

      val districtNumberValidator: Validator[String] = check(
        _.matches(Common.districtNumberPattern),
        s"""Invalid districtNumber, does not matches regex ${Common.districtNumberPattern}"""
      )
      val referenceValidator: Validator[String] = check(
        _.matches(Common.referencePattern0),
        s"""Invalid reference, does not matches regex ${Common.referencePattern0}"""
      )

      override val validate: Validator[EmpRef] = Validator(
        checkProperty(_.districtNumber, districtNumberValidator),
        checkProperty(_.reference, referenceValidator)
      )

      override val gen: Gen[EmpRef] = for {
        districtNumber <- Generator.regex(Common.districtNumberPattern)
        reference      <- Generator.regex(Common.referencePattern0)
      } yield EmpRef(
        districtNumber = districtNumber,
        reference = reference
      )

      override val sanitizers: Seq[Update] = Seq()

      implicit val formats: Format[EmpRef] = Json.format[EmpRef]

    }

    case class AoRef(districtNumber: String, payType: String, checkCode: String, reference: String) {

      def withDistrictNumber(districtNumber: String): AoRef = copy(districtNumber = districtNumber)
      def modifyDistrictNumber(pf: PartialFunction[String, String]): AoRef =
        if (pf.isDefinedAt(districtNumber)) copy(districtNumber = pf(districtNumber)) else this
      def withPayType(payType: String): AoRef = copy(payType = payType)
      def modifyPayType(pf: PartialFunction[String, String]): AoRef =
        if (pf.isDefinedAt(payType)) copy(payType = pf(payType)) else this
      def withCheckCode(checkCode: String): AoRef = copy(checkCode = checkCode)
      def modifyCheckCode(pf: PartialFunction[String, String]): AoRef =
        if (pf.isDefinedAt(checkCode)) copy(checkCode = pf(checkCode)) else this
      def withReference(reference: String): AoRef = copy(reference = reference)
      def modifyReference(pf: PartialFunction[String, String]): AoRef =
        if (pf.isDefinedAt(reference)) copy(reference = pf(reference)) else this
    }

    object AoRef extends RecordUtils[AoRef] {

      val districtNumberValidator: Validator[String] = check(
        _.matches(Common.districtNumberPattern),
        s"""Invalid districtNumber, does not matches regex ${Common.districtNumberPattern}"""
      )
      val payTypeValidator: Validator[String] =
        check(_.matches(Common.payTypePattern), s"""Invalid payType, does not matches regex ${Common.payTypePattern}""")
      val checkCodeValidator: Validator[String] = check(
        _.matches(Common.payTypePattern),
        s"""Invalid checkCode, does not matches regex ${Common.payTypePattern}"""
      )
      val referenceValidator: Validator[String] = check(
        _.matches(Common.referencePattern1),
        s"""Invalid reference, does not matches regex ${Common.referencePattern1}"""
      )

      override val validate: Validator[AoRef] = Validator(
        checkProperty(_.districtNumber, districtNumberValidator),
        checkProperty(_.payType, payTypeValidator),
        checkProperty(_.checkCode, checkCodeValidator),
        checkProperty(_.reference, referenceValidator)
      )

      override val gen: Gen[AoRef] = for {
        districtNumber <- Generator.regex(Common.districtNumberPattern)
        payType        <- Generator.regex(Common.payTypePattern)
        checkCode      <- Generator.regex(Common.payTypePattern)
        reference      <- Generator.regex(Common.referencePattern1)
      } yield AoRef(
        districtNumber = districtNumber,
        payType = payType,
        checkCode = checkCode,
        reference = reference
      )

      override val sanitizers: Seq[Update] = Seq()

      implicit val formats: Format[AoRef] = Json.format[AoRef]

    }

  }

  object Common {
    val employerNamePattern = """^\w{1,64}$"""
    val referencePattern0 = """^[A-Za-z0-9 ]{1,10}$"""
    val agentCodePattern = """^[A-Z0-9]{1,12}$"""
    val payTypePattern = """^\w{1,6}$"""
    val districtNumberPattern = """^\d{1,3}$"""
    val referencePattern1 = """^[A-Za-z0-9 ]{1,13}$"""
    val agentClientRefPattern = """^\w{1,12}$"""
  }
}
