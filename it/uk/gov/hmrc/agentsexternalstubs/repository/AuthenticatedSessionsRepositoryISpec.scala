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

import java.util.UUID

import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.agentsexternalstubs.models.AuthenticatedSession
import uk.gov.hmrc.agentsexternalstubs.support.{AppBaseISpec, MongoDB}
import play.api.test.Helpers._

import scala.concurrent.Future
import scala.util.Random

class AuthenticatedSessionsRepositoryISpec extends AppBaseISpec with MongoDB {

  lazy val repo: AuthenticatedSessionsRepository = app.injector.instanceOf[AuthenticatedSessionsRepository]

  "store" should {
    "store a session" in {
      val planetId = UUID.randomUUID().toString
      val authToken = UUID.randomUUID().toString

      await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foobar", authToken, "bla", planetId)))

      val result = await(repo.find("planetId" -> planetId))

      result.size shouldBe 1
      result.head.authToken shouldBe authToken
      result.head.userId shouldBe "foobar"

    }

    "not allow duplicate sessions to be created for the same authToken" in {
      val planetId = UUID.randomUUID().toString
      await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foo", "bar", "bla", planetId)))

      val e = intercept[DatabaseException] {
        await(repo.create(AuthenticatedSession(UUID.randomUUID().toString, "foo", "bar", "ala", planetId)))
      }

      e.getMessage() should include("E11000")
    }

    "delete all sessions" in {
      await(
        Future.sequence(
          Stream
            .continually(Random.nextString(32))
            .take(100)
            .map(seed =>
              repo.create(
                AuthenticatedSession(UUID.randomUUID().toString, seed, UUID.randomUUID().toString, seed, seed)
              )
            )
        )
      )

      await(repo.count) should be >= 100
      await(repo.deleteAll(System.currentTimeMillis()))
      await(repo.count) shouldBe 0
    }

    "delete all sessions created before some datetime" in {
      def fixture: Future[Unit] =
        Future
          .sequence(
            Stream
              .continually(Random.nextString(32))
              .take(50)
              .map(seed =>
                repo.create(
                  AuthenticatedSession(UUID.randomUUID().toString, seed, UUID.randomUUID().toString, seed, seed)
                )
              )
          )
          .map(_.reduce((_, _) => ()))

      await(fixture)
      val t0 = System.currentTimeMillis()
      await(fixture)

      await(repo.count) should be >= 100
      await(repo.deleteAll(t0)) should be >= 50
      await(repo.count) shouldBe 50
    }
  }
}
