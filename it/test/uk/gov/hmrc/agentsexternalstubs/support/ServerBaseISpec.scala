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

package uk.gov.hmrc.agentsexternalstubs.support

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Application

import scala.concurrent.ExecutionContext

abstract class ServerBaseISpec
    extends BaseISpec with BeforeAndAfterAll with ScalaFutures with JsonMatchers with WSResponseMatchers with MongoDB
    with IntegrationPatience {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(25, Seconds)),
    interval = scaled(Span(150, Millis))
  )

  val playServer: TestPlayServer = TestPlayServer
  def port: Int = playServer.port
  def wireMockPort: Int = playServer.wireMockPort

  override def beforeAll(): Unit = {
    super.beforeAll()
    playServer.run()
  }

  override lazy val app: Application = playServer.app

  val url = s"http://localhost:$port"

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

}
