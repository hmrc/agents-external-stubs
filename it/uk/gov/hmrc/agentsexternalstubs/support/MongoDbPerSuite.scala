/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.{BeforeAndAfterAll, TestSuite}
import play.api.Application
import uk.gov.hmrc.agentsexternalstubs.repository.{AuthenticatedSessionsRepository, UsersRepositoryMongo}
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.{Duration, _}

trait MongoDbPerSuite extends MongoSpecSupport with BeforeAndAfterAll {
  me: TestSuite =>

  private implicit val timeout: Duration = 5 seconds

  def app: Application
  def await[A](future: Future[A])(implicit timeout: Duration): A

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(mongo().drop())
    await(app.injector.instanceOf[AuthenticatedSessionsRepository].ensureIndexes)
    await(app.injector.instanceOf[UsersRepositoryMongo].ensureIndexes)
  }
}
