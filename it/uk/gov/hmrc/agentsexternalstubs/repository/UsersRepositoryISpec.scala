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

import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.agentsexternalstubs.models.{Enrolment, Identifier, User}
import uk.gov.hmrc.agentsexternalstubs.support.MongoApp
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class UsersRepositoryISpec extends UnitSpec with OneAppPerSuite with MongoApp {

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
      )

  override implicit lazy val app: Application = appBuilder.build()

  def repo: UsersRepository = app.injector.instanceOf[UsersRepository]

  override def beforeEach() {
    super.beforeEach()
    await(repo.ensureIndexes)
  }

  "create" should {
    "create a simple user" in {
      await(repo.create(User("foo")))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "foo"

    }

    "create a user with simple principal enrolment" in {
      await(repo.create(User("889foo", principalEnrolments = Seq(Enrolment("foobar")))))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "889foo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar"))
    }

    "create multiple users with the same principal enrolment" in {
      await(repo.create(User("1foo", principalEnrolments = Seq(Enrolment("foobar")))))
      await(repo.create(User("foo2", principalEnrolments = Seq(Enrolment("foobar")))))
      await(repo.create(User("3oo", principalEnrolments = Seq(Enrolment("foobar")))))

      val result = await(repo.find())

      result.size shouldBe 3
      result.flatMap(_.principalEnrolments).toSet shouldBe Set(Enrolment("foobar"))
    }

    "create a user with single principal enrolment" in {
      await(repo.create(User("abcfoo", principalEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123"))))))))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "create a user with multiple principal enrolments" in {
      await(
        repo.create(
          User(
            "foo888",
            principalEnrolments = Seq(
              Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
              Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345"))))
            ))))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "foo888"
      result.head.principalEnrolments shouldBe
        Seq(
          Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))),
          Enrolment("barefoot", Some(Seq(Identifier("foo", "foo345")))))
    }

    "create a user with single delegated enrolment" in {
      await(repo.create(User("abcfoo", delegatedEnrolments = Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123"))))))))

      val result = await(repo.find())

      result.size shouldBe 1
      result.head.userId shouldBe "abcfoo"
      result.head.principalEnrolments shouldBe Seq.empty
      result.head.delegatedEnrolments shouldBe Seq(Enrolment("foobar", Some(Seq(Identifier("bar", "boo123")))))
    }

    "not allow duplicate users to be created for the same userId" in {
      await(repo.create(User("foo")))

      val e = intercept[DatabaseException] {
        await(repo.create(User("foo")))
      }

      e.getMessage() should include("E11000")
    }
  }
}
