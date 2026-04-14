/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.services

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.agentsexternalstubs.models._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class UcrStubService @Inject() (
  usersService: UsersService,
  groupsService: GroupsService
)(implicit ec: ExecutionContext)
    extends Logging {

  def validateSystemId(systemId: Option[String]): Either[Result, Boolean] =
    systemId match {
      case None =>
        Left(Results.BadRequest(Json.obj("code" -> "BAD_REQUEST", "reason" -> "required header [system-id] not found")))
      case Some(id) if id.length < 3 || id.length > 20 =>
        Left(
          Results.BadRequest(
            Json.obj("code" -> "BAD_REQUEST", "reason" -> "system-id must be between 3 and 20 characters")
          )
        )
      case _ => Right(true)
    }

  def processIndividualIdentifierSearch(
    body: JsValue,
    planetId: String,
    correlationId: String
  ): Future[Result] = {
    val identifierType = (body \ "identifier" \ "type").asOpt[String].getOrElse("")
    val identifierValue = (body \ "identifier" \ "value").asOpt[String].getOrElse("")

    findIndividualByIdentifier(identifierType, identifierValue, planetId).flatMap {
      case Some(user) => buildIndividualResponse(user, planetId, correlationId)
      case None       => noMatchResponse(correlationId, "individuals")
    }
  }

  def processOrganisationIdentifierSearch(
    body: JsValue,
    planetId: String,
    correlationId: String
  ): Future[Result] = {
    val identifierType = (body \ "identifier" \ "type").asOpt[String].getOrElse("")
    val identifierValue = (body \ "identifier" \ "value").asOpt[String].getOrElse("")

    findOrganisationByIdentifier(identifierType, identifierValue, planetId).flatMap {
      case Some((_, group)) => buildOrganisationResponse(group, identifierValue, correlationId)
      case None             => noMatchResponse(correlationId, "organisations")
    }
  }

  private def findIndividualByIdentifier(
    identifierType: String,
    identifierValue: String,
    planetId: String
  ): Future[Option[User]] =
    identifierType.toUpperCase match {
      case "NINO" => usersService.findByNino(identifierValue, planetId)
      case "UTR"  => usersService.findByUtr(identifierValue, planetId)
      case _      => Future.successful(None)
    }

  private def findOrganisationByIdentifier(
    identifierType: String,
    identifierValue: String,
    planetId: String
  ): Future[Option[(User, Group)]] =
    identifierType.toUpperCase match {
      case "UTR" =>
        usersService
          .findByPrincipalEnrolmentKey(EnrolmentKey.from("IR-CT", "UTR" -> identifierValue), planetId)
          .flatMap {
            case Some(user) =>
              user.groupId
                .map(gid => groupsService.findByGroupId(gid, planetId).map(_.map(g => (user, g))))
                .getOrElse(Future.successful(None))
            case None => Future.successful(None)
          }
      case _ => Future.successful(None)
    }

  private def buildIndividualResponse(
    user: User,
    planetId: String,
    correlationId: String
  ): Future[Result] =
    user.groupId
      .map(gid => groupsService.findByGroupId(gid, planetId))
      .getOrElse(Future.successful(None))
      .flatMap { maybeGroup =>
        val identifiers = buildIndividualIdentifiers(user, maybeGroup)
        findResource("/resources/ucr/individuals/green-template.json")
          .map(replaceIdentifiers(_, identifiers))
          .map(jsonResponse(_, correlationId))
      }

  private def buildOrganisationResponse(
    group: Group,
    identifierValue: String,
    correlationId: String
  ): Future[Result] = {
    val identifiers = buildOrganisationIdentifiers(group, identifierValue)
    findResource("/resources/ucr/organisations/green-template.json")
      .map(replaceIdentifiers(_, identifiers))
      .map(jsonResponse(_, correlationId))
  }

  private def buildIndividualIdentifiers(user: User, maybeGroup: Option[Group]): String = {
    val ninoId = user.nino.map(n => ucrIdentifier("NINO", n.value, n.value, "NPS"))
    val utrId = user.utr.map(u => ucrIdentifier("UTR", u, u, "ETMP"))
    val vrnId = maybeGroup
      .flatMap(_.findIdentifierValue("HMRC-MTD-VAT", "VRN"))
      .map(vrn => ucrIdentifier("VRN", vrn, vrn, "ETMP"))
    val emprefId = maybeGroup
      .flatMap(_.findIdentifierValue("IR-PAYE", "TaxOfficeNumber", "TaxOfficeReference", (a, b) => s"$a/$b"))
      .map(empref => ucrIdentifier("EMPREF", empref, empref, "ETMP"))

    Seq(ninoId, utrId, vrnId, emprefId).flatten.mkString(",")
  }

  private def buildOrganisationIdentifiers(group: Group, searchedUtr: String): String = {
    val utrId = Some(ucrOrgIdentifier("UTR", searchedUtr, searchedUtr, "COTAX"))
    val vrnId = group
      .findIdentifierValue("HMRC-MTD-VAT", "VRN")
      .map(vrn => ucrOrgIdentifier("VRN", vrn, vrn, "ETMP"))
    val emprefId = group
      .findIdentifierValue("IR-PAYE", "TaxOfficeNumber", "TaxOfficeReference", (a, b) => s"$a/$b")
      .map(empref => ucrOrgIdentifier("EMPREF", empref, empref, "ETMP"))

    Seq(utrId, vrnId, emprefId).flatten.mkString(",")
  }

  private def ucrIdentifier(idType: String, value: String, thirdPartyKey: String, sourceSystem: String): String =
    s"""{
       |  "identifier": { "type": "$idType", "value": "$value" },
       |  "sourceSystems": [{ "thirdPartyKey": "$thirdPartyKey", "name": "$sourceSystem" }]
       |}""".stripMargin

  private def ucrOrgIdentifier(idType: String, value: String, thirdPartyKey: String, sourceSystem: String): String =
    s"""{
       |  "identifier": { "type": "$idType", "value": "$value", "invalidFormat": false },
       |  "sourceSystems": [{ "thirdPartyKey": "$thirdPartyKey", "name": "$sourceSystem" }]
       |}""".stripMargin

  private def replaceIdentifiers(maybeJson: Option[String], identifiers: String): Option[String] =
    maybeJson.map(_.replace("%%%IDENTIFIERS%%%", identifiers))

  private def noMatchResponse(correlationId: String, entityType: String): Future[Result] =
    findResource(s"/resources/ucr/$entityType/no-match-template.json")
      .map(jsonResponse(_, correlationId))

  private def jsonResponse(maybeJson: Option[String], correlationId: String): Result =
    maybeJson.fold[Result](Results.NotFound)(jsonStr =>
      Results.Ok(Json.parse(jsonStr)).withHeaders("x-correlation-id" -> correlationId)
    )

  private def findResource(resourcePath: String): Future[Option[String]] = Future {
    Option(getClass.getResourceAsStream(resourcePath))
      .map(Source.fromInputStream(_).mkString)
  }
}
