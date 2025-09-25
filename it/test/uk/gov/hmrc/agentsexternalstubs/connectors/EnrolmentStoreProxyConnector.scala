/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.connectors

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig
import uk.gov.hmrc.domain.{AgentCode, Nino, TaxIdentifier}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/*

  This connector has been copy-pasted from agent-client-relationship
  to support EnrolmentStoreProxyConnectorISpec.

 */

case class ES8Request(userId: String, `type`: String)
object ES8Request {
  implicit val writes = Json.writes[ES8Request]
}

@Singleton
class EnrolmentStoreProxyConnector @Inject() (appConfig: AppConfig, http: HttpClientV2, metrics: Metrics)
  extends TaxIdentifierSupport {

  // ES0 - principal
  def getPrincipalUserIdFor(
    taxIdentifier: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = appConfig.enrolmentStoreProxyUrl + s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=principal"
    http
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        if (response.status == 204) throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        else response.json
      }
      .map { json =>
        val userIds = (json \ "principalUserIds").as[Seq[String]]
        if (userIds.isEmpty) {
          throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        } else {
          if (userIds.lengthCompare(1) > 0) {
            Logger(getClass).warn(s"Multiple userIds found for $enrolmentKeyPrefix")
          }
          userIds.head
        }
      }
  }

  // ES1 - principal
  def getPrincipalGroupIdFor(
    taxIdentifier: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = appConfig.enrolmentStoreProxyUrl + s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=principal"
    http
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        if (response.status == 204) throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        else response.json
      }
      .map { json =>
        val groupIds = (json \ "principalGroupIds").as[Seq[String]]
        if (groupIds.isEmpty) {
          throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        } else {
          if (groupIds.lengthCompare(1) > 0) {
            Logger(getClass).warn(s"Multiple groupIds found for $enrolmentKeyPrefix")
          }
          groupIds.head
        }
      }
  }

  // ES1 - delegated
  def getDelegatedGroupIdsFor(
    taxIdentifier: TaxIdentifier
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] = {
    val enrolmentKey = enrolmentKeyPrefixFor(taxIdentifier) + "~" + taxIdentifier.value
    getDelegatedGroupIdsFor(enrolmentKey)
  }

  def getDelegatedGroupIdsForHMCEVATDECORG(
    vrn: Vrn
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] =
    getDelegatedGroupIdsFor(s"HMCE-VATDEC-ORG~VATRegNo~${vrn.value}")

  protected def getDelegatedGroupIdsFor(
    enrolmentKey: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] = {
    val url = appConfig.enrolmentStoreProxyUrl + s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=delegated"
    http
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        if (response.status == 204) Set.empty
        else (response.json \ "delegatedGroupIds").as[Seq[String]].toSet
      }
  }

  // ES8
  def allocateEnrolmentToAgent(groupId: String, userId: String, taxIdentifier: TaxIdentifier, agentCode: AgentCode)(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = appConfig.taxEnrolmentsUrl +
      s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey?legacy-agentCode=${agentCode.value}"
    http
      .post(url"$url")
      .withBody(Json.toJson(ES8Request(userId, "delegated")))
      .execute[HttpResponse]
      .map(_ => ())
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == Status.CONFLICT =>
          Logger(getClass).warn(
            s"An attempt to allocate new enrolment $enrolmentKeyPrefix resulted in conflict with an existing one."
          )
          ()
      }
  }

  // ES9
  def deallocateEnrolmentFromAgent(groupId: String, taxIdentifier: TaxIdentifier, agentCode: AgentCode)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Unit] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = appConfig.taxEnrolmentsUrl +
        s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey?legacy-agentCode=${agentCode.value}"
    http
      .delete(url"$url")
      .execute[HttpResponse]
      .map(_ => ())
  }

}

trait TaxIdentifierSupport {

  protected def enrolmentKeyPrefixFor(taxIdentifier: TaxIdentifier): String = taxIdentifier match {
    case _: Arn     => "HMRC-AS-AGENT~AgentReferenceNumber"
    case _: MtdItId => "HMRC-MTD-IT~MTDITID"
    case _: Vrn     => "HMRC-MTD-VAT~VRN"
    case _: Nino    => "HMRC-MTD-IT~NINO"
    case _          => throw new IllegalArgumentException(s"Tax identifier not supported $taxIdentifier")
  }

  protected def identifierNickname(taxIdentifier: TaxIdentifier): String = taxIdentifier match {
    case _: Arn     => "ARN"
    case _: MtdItId => "MTDITID"
    case _: Vrn     => "VRN"
    case _: Nino    => "NINO"
    case _          => throw new IllegalArgumentException(s"Tax identifier not supported $taxIdentifier")
  }

}

object TaxIdentifierSupport extends TaxIdentifierSupport {
  def from(value: String, `type`: String): TaxIdentifier = `type` match {
    case "MTDITID"              => MtdItId(value)
    case "NINO"                 => Nino(value)
    case "VRN"                  => Vrn(value)
    case "AgentReferenceNumber" => Arn(value)
    case _                      => throw new Exception(s"Invalid tax identifier type ${`type`}")
  }
}
