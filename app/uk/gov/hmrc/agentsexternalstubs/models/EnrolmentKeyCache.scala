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

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import play.api.Logging
import uk.gov.hmrc.agentsexternalstubs.models.EnrolmentKey.{validateIdentifiers, validateService}

object EnrolmentKeyCache extends Logging {

  private lazy val cache: LoadingCache[String, Either[String, EnrolmentKey]] = CacheBuilder
    .newBuilder()
    .maximumSize(10000)
    .recordStats()
    .build[String, Either[String, EnrolmentKey]](
      new CacheLoader[String, Either[String, EnrolmentKey]] {
        override def load(key: String): Either[String, EnrolmentKey] =
          parse(key)
      }
    )

  registerManagementBean()

  def get(key: String): Either[String, EnrolmentKey] = cache.get(key)

  private def parse(s: String): Either[String, EnrolmentKey] = {
    val parts = s.split("~")
    if (parts.nonEmpty && parts.size >= 3 && parts.size % 2 == 1) {
      val service = parts.head
      val identifiers = parts.tail.sliding(2, 2).map(a => Identifier(a(0), a(1))).toSeq
      Right(EnrolmentKey(service, identifiers)).right.flatMap(validateService).right.flatMap(validateIdentifiers)
    } else Left("INVALID_ENROLMENT_KEY")
  }

  private def registerManagementBean() = {

    import java.lang.management.ManagementFactory
    import javax.management._

    val name: String = String.format(
      "%s:type=%s,name=%s",
      cache.getClass.getPackage.getName,
      cache.getClass.getSimpleName,
      "EnrolmentKeyCache"
    )

    try {
      val mBean = new EnrolmentKeyCacheMXBeanImpl(cache)
      val server = ManagementFactory.getPlatformMBeanServer
      val mxBeanName = new ObjectName(name)
      if (!server.isRegistered(mxBeanName)) server.registerMBean(mBean, mxBeanName)
    } catch {
      case ex @ (_: MalformedObjectNameException | _: InstanceAlreadyExistsException | _: MBeanRegistrationException |
          _: NotCompliantMBeanException) =>
        throw new IllegalStateException(
          String.format("An exception was thrown registering the JMX bean with the name '%s'", name),
          ex
        )
    }
  }

}

trait EnrolmentKeyCacheMXBean {
  def getRequestCount: Long

  def getHitCount: Long

  def getHitRate: Double

  def getMissCount: Long

  def getMissRate: Double

  def getLoadCount: Long

  def getLoadSuccessCount: Long

  def getLoadExceptionCount: Long

  def getLoadExceptionRate: Double

  def getTotalLoadTime: Long

  def getAverageLoadPenalty: Double

  def getEvictionCount: Long

  def getSize: Long

  def cleanUp(): Unit

  def invalidateAll(): Unit

  def refreshAll(): Unit
}

class EnrolmentKeyCacheMXBeanImpl(val cache: LoadingCache[String, Either[String, EnrolmentKey]])
    extends EnrolmentKeyCacheMXBean {
  override def getRequestCount: Long = cache.stats.requestCount

  override def getHitCount: Long = cache.stats.hitCount

  override def getHitRate: Double = cache.stats.hitRate

  override def getMissCount: Long = cache.stats.missCount

  override def getMissRate: Double = cache.stats.missRate

  override def getLoadCount: Long = cache.stats.loadCount

  override def getLoadSuccessCount: Long = cache.stats.loadSuccessCount

  override def getLoadExceptionCount: Long = cache.stats.loadExceptionCount

  override def getLoadExceptionRate: Double = cache.stats.loadExceptionRate

  override def getTotalLoadTime: Long = cache.stats.totalLoadTime

  override def getAverageLoadPenalty: Double = cache.stats.averageLoadPenalty

  override def getEvictionCount: Long = cache.stats.evictionCount

  override def getSize: Long = cache.size

  override def cleanUp(): Unit =
    cache.cleanUp()

  override def invalidateAll(): Unit =
    cache.invalidateAll()

  override def refreshAll(): Unit =
    cache.asMap.keySet().forEach(cache.refresh(_))
}
