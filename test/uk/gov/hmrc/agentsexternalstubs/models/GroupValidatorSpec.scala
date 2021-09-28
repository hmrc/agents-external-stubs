/*
 * Copyright 2021 HM Revenue & Customs
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

class GroupValidatorSpec extends UnitSpec {

  "GroupValidator" should {
    "validate empty group" in {
      GroupValidator.validate(Seq()) shouldBe Valid(())
    }
    "validate group with users without affinity" in {
      GroupValidator.validate(Seq(User("foo", credentialRole = Some("Admin")))) shouldBe Valid(())
      GroupValidator.validate(Seq(User("foo", credentialRole = Some("Admin")), User("bar"))) shouldBe Valid(())
    }
    "validate only when group is empty or have one and at most one Admin" in {
      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"))) shouldBe Valid(())
      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "Admin"), User("foo"))) shouldBe Valid(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"))) shouldBe Valid(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), User("foo"))) shouldBe Valid(())

      GroupValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "User"))
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(
          UserGenerator.individual(credentialRole = "Admin"),
          UserGenerator.individual(credentialRole = "User"),
          UserGenerator.individual(credentialRole = "Assistant")
        )
      ) shouldBe Valid(())

      GroupValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "User")
        )
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "Assistant")
        )
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin"),
          UserGenerator.agent(groupId = "A", credentialRole = "User"),
          UserGenerator.agent(groupId = "A", credentialRole = "Assistant")
        )
      ) shouldBe Valid(())

      GroupValidator.validate(Seq(UserGenerator.individual(credentialRole = "User"))).isInvalid shouldBe true
    }
    "validate only if group have at most one Organisation" in {
      GroupValidator.validate(Seq(UserGenerator.organisation())) shouldBe Valid(())
      GroupValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "User"))
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())

      GroupValidator.validate(Seq(UserGenerator.organisation(), UserGenerator.organisation())).isInvalid shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.organisation(), UserGenerator.individual()))
        .isInvalid shouldBe true
    }
    "validate only if group is not only consisting of Assistants" in {
      GroupValidator.validate(
        Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())
      GroupValidator.validate(
        Seq(UserGenerator.organisation(), UserGenerator.individual(credentialRole = "Assistant"))
      ) shouldBe Valid(())

      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "Assistant")))
        .isInvalid shouldBe true
      GroupValidator
        .validate(
          Seq(
            UserGenerator.individual(credentialRole = "Assistant"),
            UserGenerator.individual(credentialRole = "Assistant")
          )
        )
        .isInvalid shouldBe true
    }
    "validate if agents are not in the group with Organisation and Individuals" in {
      GroupValidator.validate(Seq(UserGenerator.agent(groupId = "A", credentialRole = "Admin"))) shouldBe Valid(())
      GroupValidator.validate(
        Seq(UserGenerator.agent(groupId = "A", credentialRole = "Admin"), UserGenerator.agent(groupId = "A"))
      ) shouldBe Valid(())
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin"), User("foo"))) shouldBe Valid(())

      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "User"), UserGenerator.agent(credentialRole = "Admin")))
        .isInvalid shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.individual(credentialRole = "Admin"), UserGenerator.agent(credentialRole = "User")))
        .isInvalid shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.agent(credentialRole = "User")))
        .isInvalid shouldBe true
      GroupValidator
        .validate(Seq(UserGenerator.organisation(), UserGenerator.agent(credentialRole = "Assistant")))
        .isInvalid shouldBe true
    }
    "validate if all Agents in the group share the same agentCode" in {
      GroupValidator.validate(Seq(UserGenerator.agent(credentialRole = "Admin", agentCode = "A"))) shouldBe Valid(())
      GroupValidator.validate(
        Seq(
          UserGenerator.agent(groupId = "A", credentialRole = "Admin", agentCode = "A"),
          UserGenerator.agent(groupId = "A", credentialRole = "User", agentCode = "A")
        )
      ) shouldBe Valid(())
    }
  }

}
