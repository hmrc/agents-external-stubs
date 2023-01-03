/*
 * Copyright 2023 HM Revenue & Customs
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

import cats.data.Validated.Valid
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec

class GroupUsersValidatorSpec extends UnitSpec {

  "GroupValidator" should {
    "validate empty group" in {
      GroupUsersValidator.validate(Seq()) shouldBe Valid(())
    }
    "validate group with users without affinity" in {
      GroupUsersValidator.validate(Seq(User("foo", credentialRole = Some("Admin")))) shouldBe Valid(())
      GroupUsersValidator.validate(Seq(User("foo", credentialRole = Some("Admin")), User("bar"))) shouldBe Valid(())
    }
    "validate only when group is empty or have one and at most one Admin" in {
      GroupUsersValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"))) shouldBe Valid(())
      GroupUsersValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"), User("foo"))) shouldBe Valid(
        ()
      )
      GroupUsersValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"))) shouldBe Valid(())
      GroupUsersValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), User("foo"))) shouldBe Valid(())

      GroupUsersValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "User"))
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(
          UserGenerator.individual(credentialRole = "Admin"),
          UserGenerator.individual(credentialRole = "User"),
          UserGenerator.individual(credentialRole = "Assistant")
        )
      ) shouldBe Valid(())

      GroupUsersValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "User")
        )
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "Assistant")
        )
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "User"),
          UserGenerator.agent(groupId = "A", credentialRole = "Assistant")
        )
      ) shouldBe Valid(())

      GroupUsersValidator.validate(Seq(UserGenerator.individual(credentialRole = "User"))).isInvalid shouldBe true
    }
    "validate only if group have at most one Organisation" in {
      GroupUsersValidator.validate(Seq(UserGenerator.organisation())) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "User"))
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())

      GroupUsersValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.organisation()))
        .isInvalid shouldBe true
      GroupUsersValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.organisation(), UserGenerator.individual()))
        .isInvalid shouldBe true
    }
    "validate only if group is not only consisting of Assistants" in {
      GroupUsersValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())
      GroupUsersValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())

      GroupUsersValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "Assistant")))
        .isInvalid shouldBe true
      GroupUsersValidator
        .validate(
          Seq(
            UserGenerator.individual(credentialRole = "Assistant"),
            UserGenerator.individual(credentialRole = "Assistant")
          )
        )
        .isInvalid shouldBe true
    }
  }

}
