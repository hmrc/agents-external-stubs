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

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, Vrn}
import uk.gov.hmrc.domain.{AgentCode, Nino, TaxIdentifier}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

/*

  This connector has been copy-pasted from agent-client-relationship
  to support EnrolmentStoreProxyConnectorSpec.

 */

case class ES8Request(userId: String, `type`: String)
object ES8Request {
  implicit val writes = Json.writes[ES8Request]
}

@Singleton
class EnrolmentStoreProxyConnector @Inject()(
  @Named("enrolment-store-proxy-baseUrl") espBaseUrl: URL,
  @Named("tax-enrolments-baseUrl") teBaseUrl: URL,
  http: HttpGet with HttpPost with HttpDelete,
  metrics: Metrics)
    extends TaxIdentifierSupport with HttpAPIMonitor {
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  // ES0 - principal
  def getPrincipalUserIdFor(
    taxIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url =
      new URL(espBaseUrl, s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/users?type=principal")
    monitor(s"ConsumedAPI-ES-getPrincipalUserIdFor-${enrolmentKeyPrefix.replace("~", "_")}-GET") {
      http.GET[HttpResponse](url.toString)
    }.map { response =>
        if (response.status == 204) throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        else response.json
      }
      .map(json => {
        val userIds = (json \ "principalUserIds").as[Seq[String]]
        if (userIds.isEmpty) {
          throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        } else {
          if (userIds.lengthCompare(1) > 0) {
            Logger(getClass).warn(s"Multiple userIds found for $enrolmentKeyPrefix")
          }
          userIds.head
        }
      })
  }

  // ES1 - principal
  def getPrincipalGroupIdFor(
    taxIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url =
      new URL(espBaseUrl, s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=principal")
    monitor(s"ConsumedAPI-ES-getPrincipalGroupIdFor-${enrolmentKeyPrefix.replace("~", "_")}-GET") {
      http.GET[HttpResponse](url.toString)
    }.map { response =>
        if (response.status == 204) throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        else response.json
      }
      .map(json => {
        val groupIds = (json \ "principalGroupIds").as[Seq[String]]
        if (groupIds.isEmpty) {
          throw new Exception(s"UNKNOWN_${identifierNickname(taxIdentifier)}")
        } else {
          if (groupIds.lengthCompare(1) > 0) {
            Logger(getClass).warn(s"Multiple groupIds found for $enrolmentKeyPrefix")
          }
          groupIds.head
        }
      })
  }

  // ES1 - delegated
  def getDelegatedGroupIdsFor(
    taxIdentifier: TaxIdentifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] = {
    val enrolmentKey = enrolmentKeyPrefixFor(taxIdentifier) + "~" + taxIdentifier.value
    getDelegatedGroupIdsFor(enrolmentKey)
  }

  def getDelegatedGroupIdsForHMCEVATDECORG(
    vrn: Vrn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] =
    getDelegatedGroupIdsFor(s"HMCE-VATDEC-ORG~VATRegNo~${vrn.value}")

  protected def getDelegatedGroupIdsFor(
    enrolmentKey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Set[String]] = {
    val url =
      new URL(espBaseUrl, s"/enrolment-store-proxy/enrolment-store/enrolments/$enrolmentKey/groups?type=delegated")
    monitor(s"ConsumedAPI-ES-getDelegatedGroupIdsFor-${enrolmentKey.split("~").take(2).mkString("_")}-GET") {
      http.GET[HttpResponse](url.toString)
    }.map { response =>
      if (response.status == 204) Set.empty
      else (response.json \ "delegatedGroupIds").as[Seq[String]].toSet
    }
  }

  // ES8
  def allocateEnrolmentToAgent(groupId: String, userId: String, taxIdentifier: TaxIdentifier, agentCode: AgentCode)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Unit] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = new URL(
      teBaseUrl,
      s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey?legacy-agentCode=${agentCode.value}")
    monitor(s"ConsumedAPI-TE-allocateEnrolmentToAgent-${enrolmentKeyPrefix.replace("~", "_")}-POST") {
      http.POST[ES8Request, HttpResponse](url.toString, ES8Request(userId, "delegated"))
    }.map(_ => ())
      .recover {
        case e: Upstream4xxResponse if e.upstreamResponseCode == Status.CONFLICT =>
          Logger(getClass).warn(
            s"An attempt to allocate new enrolment $enrolmentKeyPrefix resulted in conflict with an existing one.")
          ()
      }
  }

  // ES9
  def deallocateEnrolmentFromAgent(groupId: String, taxIdentifier: TaxIdentifier, agentCode: AgentCode)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext): Future[Unit] = {
    val enrolmentKeyPrefix = enrolmentKeyPrefixFor(taxIdentifier)
    val enrolmentKey = enrolmentKeyPrefix + "~" + taxIdentifier.value
    val url = new URL(
      teBaseUrl,
      s"/tax-enrolments/groups/$groupId/enrolments/$enrolmentKey?legacy-agentCode=${agentCode.value}")
    monitor(s"ConsumedAPI-TE-deallocateEnrolmentFromAgent-${enrolmentKeyPrefix.replace("~", "_")}-DELETE") {
      http.DELETE[HttpResponse](url.toString)
    }.map(_ => ())
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
    case "MTDITID" => MtdItId(value)
    case "NINO" => Nino(value)
    case "VRN" => Vrn(value)
    case "AgentReferenceNumber" => Arn(value)
    case _ => throw new Exception(s"Invalid tax identifier type ${`type`}")
  }
}
