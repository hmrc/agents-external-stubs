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
import org.joda.time.LocalDate
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.User.AdditionalInformation
import uk.gov.hmrc.domain.{EmpRef, Nino}

object ApiPlatform {
  /*
  val individualDetails: IndividualDetails
  val saUtr: Option[SaUtr]
  val nino: Option[Nino]
  val mtdItId: Option[MtdItId]
  val vrn: Option[Vrn]
  val vatRegistrationDate: Option[LocalDate]
  val eoriNumber: Option[EoriNumber]
   */

  case class TestUser(
    userId: String,
    userFullName: String,
    emailAddress: Option[String] = None,
    password: Option[String] = None,
    individualDetails: Option[IndividualDetails] = None,
    organisationDetails: Option[OrganisationDetails] = None,
    saUtr: Option[String] = None,
    nino: Option[String] = None,
    mtdItId: Option[String] = None,
    eoriNumber: Option[String] = None,
    empRef: Option[EmpRef] = None,
    ctUtr: Option[String] = None,
    vrn: Option[String] = None,
    vatRegistrationDate: Option[LocalDate] = None,
    arn: Option[String] = None,
    lisaManRefNum: Option[String] = None,
    secureElectronicTransferReferenceNumber: Option[String] = None,
    pensionSchemeAdministratorIdentifier: Option[String] = None
  ) {

    val affinityGroup: String =
      if (individualDetails.isDefined) AG.Individual
      else if (organisationDetails.isDefined) AG.Organisation
      else AG.Agent

    val services: Seq[String] = Seq(
      nino.map(_ => "national-insurance"),
      ctUtr.map(_ => "corporation-tax"),
      empRef.map(_ => "paye-for-employers"),
      saUtr.map(_ => "self-assessment"),
      vrn.map(_ => "submit-vat-returns"),
      vrn.map(_ => "mtd-vat"),
      mtdItId.map(_ => "mtd-income-tax"),
      arn.map(_ => "agent-services"),
      lisaManRefNum.map(_ => "lisa"),
      secureElectronicTransferReferenceNumber.map(_ => "secure-electronic-transfer"),
      pensionSchemeAdministratorIdentifier.map(_ => "relief-at-source"),
      eoriNumber.map(_ => "customs-services")
    ).collect { case Some(x) => x }
  }

  case class Address(line1: String, line2: String, postcode: String)
  case class IndividualDetails(firstName: String, lastName: String, dateOfBirth: LocalDate, address: Address)
  case class OrganisationDetails(name: String, address: Address)

  object TestUser {

    import play.api.libs.json.JodaWrites._
    import play.api.libs.json.JodaReads._

    implicit lazy val formats1: Format[Address] = Json.format[Address]
    implicit lazy val formats2: Format[IndividualDetails] = Json.format[IndividualDetails]
    implicit lazy val formats3: Format[OrganisationDetails] = Json.format[OrganisationDetails]
    implicit lazy val formats4: Format[TestUser] = Json.format[TestUser]

    def asUserAndGroup(testUser: TestUser): (User, Group) = {
      val groupId = GroupGenerator.groupId(seed = testUser.hashCode().toString)
      val user = User(
        userId = testUser.userId,
        groupId = Some(groupId),
        confidenceLevel = if (testUser.affinityGroup == AG.Individual) Some(250) else None,
        credentialStrength = Some("strong"),
        nino = if (testUser.affinityGroup == AG.Individual) testUser.nino.map(Nino.apply) else None,
        name = (testUser.affinityGroup match {
          case AG.Individual   => testUser.individualDetails.map(d => d.firstName + " " + d.lastName)
          case AG.Organisation => testUser.organisationDetails.map(d => d.name)
          case _               => None
        }).orElse(Option(testUser.userFullName)),
        dateOfBirth =
          if (testUser.affinityGroup == AG.Individual) testUser.individualDetails.map(_.dateOfBirth) else None,
        address = (testUser.affinityGroup match {
          case AG.Individual   => testUser.individualDetails.map(_.address)
          case AG.Organisation => testUser.organisationDetails.map(_.address)
          case _               => None
        }).map(a =>
          User.Address(
            line1 = Option(a.line1),
            line2 = Option(a.line2),
            postcode = Option(a.postcode),
            countryCode = Some("GB")
          )
        ),
        assignedPrincipalEnrolments = mapServicesToEnrolments(testUser).flatMap(_.toEnrolmentKey),
        additionalInformation =
          testUser.vatRegistrationDate.map(date => AdditionalInformation(vatRegistrationDate = Some(date)))
      )
      val group = Group(
        groupId = groupId,
        planetId = "",
        affinityGroup = testUser.affinityGroup,
        agentFriendlyName = if (testUser.affinityGroup == AG.Agent) Some(testUser.userFullName) else None,
        principalEnrolments = mapServicesToEnrolments(testUser)
      )
      (user, group)
    }

    def mapServicesToEnrolments(testUser: ApiPlatform.TestUser): Seq[Enrolment] =
      testUser.services
        .map {
          case "national-insurance" => None
          case "self-assessment"    => testUser.saUtr.map(Enrolment("IR-SA", "UTR", _))
          case "corporation-tax"    => testUser.ctUtr.map(Enrolment("IR-CT", "UTR", _))
          case "paye-for-employers" =>
            testUser.empRef.map(empRef =>
              Enrolment(
                "IR-PAYE",
                Some(
                  Seq(
                    Identifier("TaxOfficeNumber", empRef.taxOfficeNumber),
                    Identifier("TaxOfficeReference", empRef.taxOfficeReference)
                  )
                )
              )
            )
          case "submit-vat-returns" => testUser.vrn.map(Enrolment("HMCE-VATDEC-ORG", "VATRegNo", _))
          case "mtd-vat"            => testUser.vrn.map(Enrolment("HMRC-MTD-VAT", "VRN", _))
          case "mtd-income-tax"     => testUser.mtdItId.map(Enrolment("HMRC-MTD-IT", "MTDITID", _))
          case "agent-services"     => testUser.arn.map(Enrolment("HMRC-AS-AGENT", "AgentReferenceNumber", _))
          case "lisa"               => testUser.lisaManRefNum.map(Enrolment("HMRC-LISA-ORG", "ZREF", _))
          case "secure-electronic-transfer" =>
            testUser.secureElectronicTransferReferenceNumber.map(Enrolment("HMRC-SET-ORG", "SETReference", _))
          case "relief-at-source" =>
            testUser.pensionSchemeAdministratorIdentifier.map(Enrolment("HMRC-PSA-ORG", "PSAID", _))
          case "customs-services" => testUser.eoriNumber.map(Enrolment("HMRC-CUS-ORG", "EORINumber", _))
        }
        .collect { case Some(x) => x }
  }

}
