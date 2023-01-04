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
package uk.gov.hmrc.agentsexternalstubs.repository

import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec

import java.util.UUID

class AuthenticatedSessionsRepositoryISpec extends AppBaseISpec {

  lazy val repo: AuthenticatedSessionsRepository = app.injector.instanceOf[AuthenticatedSessionsRepository]

  "store" should {
    "store a session" in {
      val planetId = UUID.randomUUID().toString
      val authToken = UUID.randomUUID().toString

      await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foobar", authToken, "bla", planetId)))

      val result = await(repo.collection.find(Filters.equal("planetId", planetId)).toFuture)

      result.size shouldBe 1
      result.head.authToken shouldBe authToken
      result.head.userId shouldBe "foobar"

    }

    "not allow duplicate sessions to be created for the same authToken" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foo", "bar", "bla", planetId)))

      val e = intercept[MongoWriteException] {
        await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foo", "bar", "ala", planetId)))
      }

      e.getMessage() should include("E11000")
    }
  }
}
