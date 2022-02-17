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
import uk.gov.hmrc.agentsexternalstubs.models.EmployerAuthsPayload._

/** ----------------------------------------------------------------------------
  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
  * How to regenerate? Run this command in the project root directory:
  * sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema docs/schemas/empRefsForAgentAuths.schema_v0_3.json app/uk/gov/hmrc/agentsexternalstubs/models/EmployerAuthsPayload.scala EmployerAuthsPayload output:payload"
  * ----------------------------------------------------------------------------
  *
  *  EmployerAuthsPayload
  *  -  EmpRef
  */
case class EmployerAuthsPayload(empRefList: Seq[EmpRef]) {

  def withEmpRefList(empRefList: Seq[EmpRef]): EmployerAuthsPayload = copy(empRefList = empRefList)
  def modifyEmpRefList(pf: PartialFunction[Seq[EmpRef], Seq[EmpRef]]): EmployerAuthsPayload =
    if (pf.isDefinedAt(empRefList)) copy(empRefList = pf(empRefList)) else this
}

object EmployerAuthsPayload {

  import Validator._

  val empRefListValidator: Validator[Seq[EmpRef]] = checkEach(identity, EmpRef.validate)

  val validate: Validator[EmployerAuthsPayload] = Validator(checkProperty(_.empRefList, empRefListValidator))

  implicit val formats: Format[EmployerAuthsPayload] = Json.format[EmployerAuthsPayload]

  case class EmpRef(districtNumber: String, reference: String) {

    def withDistrictNumber(districtNumber: String): EmpRef = copy(districtNumber = districtNumber)
    def modifyDistrictNumber(pf: PartialFunction[String, String]): EmpRef =
      if (pf.isDefinedAt(districtNumber)) copy(districtNumber = pf(districtNumber)) else this
    def withReference(reference: String): EmpRef = copy(reference = reference)
    def modifyReference(pf: PartialFunction[String, String]): EmpRef =
      if (pf.isDefinedAt(reference)) copy(reference = pf(reference)) else this
  }

  object EmpRef {

    val districtNumberValidator: Validator[String] = check(
      _.matches(Common.districtNumberPattern),
      s"""Invalid districtNumber, does not matches regex ${Common.districtNumberPattern}"""
    )
    val referenceValidator: Validator[String] = check(
      _.matches(Common.referencePattern),
      s"""Invalid reference, does not matches regex ${Common.referencePattern}"""
    )

    val validate: Validator[EmpRef] = Validator(
      checkProperty(_.districtNumber, districtNumberValidator),
      checkProperty(_.reference, referenceValidator)
    )

    implicit val formats: Format[EmpRef] = Json.format[EmpRef]

  }

  object Common {
    val districtNumberPattern = """^\d{1,3}$"""
    val referencePattern = """^[A-Z0-9]{1,10}$"""
  }
}
