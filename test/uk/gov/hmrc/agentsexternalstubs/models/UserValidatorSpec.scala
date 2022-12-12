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

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.agentsexternalstubs.support.UnitSpec
import uk.gov.hmrc.agentsexternalstubs.support.ValidatedMatchers

import java.time.LocalDate

class UserValidatorSpec extends UnitSpec with ValidatedMatchers {

  "UserValidator" should {
    "validate only when affinityGroup is none or one of [Individual, Organisation, Agent]" in {
      UserValidator(Some(AG.Individual))
        .validate(User("foo"))
        .isValid shouldBe true
      UserValidator(Some(AG.Organisation))
        .validate(User("foo", credentialRole = Some(User.CR.Admin)))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent)).validate(UserGenerator.agent("foo")).isValid shouldBe true

      UserValidator(Some("Foo")).validate(User("foo")).isValid shouldBe false
      UserValidator(None).validate(User("foo")).isValid shouldBe true
    }

    "validate only when confidenceLevel is none, or one of [50,200,250] and user is Individual and NINO is not empty" in {
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(50),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(250),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe true

      UserValidator(Some(AG.Agent))
        .validate(
          User("foo", confidenceLevel = Some(200), nino = Some(Nino("HW827856C")))
        )
        .isValid shouldBe false
      UserValidator(Some(AG.Organisation))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(200),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
      UserValidator(Some(AG.Individual))
        .validate(User("foo", confidenceLevel = Some(200), nino = None))
        .isValid shouldBe false
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(55),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            confidenceLevel = Some(0),
            nino = Some(Nino("HW827856C"))
          )
        )
        .isValid shouldBe false
    }

    "validate only when credentialStrength is none, or one of [weak, strong]" in {
      UserValidator(Some(AG.Individual)).validate(User("foo", credentialStrength = Some("weak"))).isValid shouldBe true
      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialStrength = Some("strong")))
        .isValid shouldBe true
      UserValidator(Some(AG.Individual)).validate(User("foo", credentialStrength = None)).isValid shouldBe true

      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialStrength = Some("very strong")))
        .isValid shouldBe false
      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialStrength = Some("little weak")))
        .isValid shouldBe false
      UserValidator(Some(AG.Individual)).validate(User("foo", credentialStrength = Some(""))).isValid shouldBe false
    }

    "validate only when credentialRole is none, or one of [Admin, User, Assistant] for Individual or Agent" in {
      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialRole = Some(User.CR.User)))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(UserGenerator.agent("foo", credentialRole = User.CR.User))
        .isValid shouldBe true
      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialRole = Some("Assistant")))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(UserGenerator.agent("foo", credentialRole = "Assistant"))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent)).validate(UserGenerator.agent("foo")).isValid shouldBe true
      UserValidator(Some(AG.Individual))
        .validate(User("foo", credentialRole = None))
        .isValid shouldBe true

      UserValidator(Some(AG.Organisation))
        .validate(User("foo", credentialRole = Some("Assistant")))
        .isValid shouldBe false
    }

    "validate only when credentialRole is Admin or User for Organisation" in {
      UserValidator(Some(AG.Organisation))
        .validate(User("foo", credentialRole = Some("Admin")))
        .isValid shouldBe true

      UserValidator(Some(AG.Organisation))
        .validate(User("foo", credentialRole = Some("User")))
        .isValid shouldBe true
      UserValidator(Some(AG.Organisation))
        .validate(User("foo", credentialRole = Some("Assistant")))
        .isValid shouldBe false
    }

    "validate only when nino is none or set for an Individual" in {
      UserValidator(Some(AG.Individual))
        .validate(
          User(
            "foo",
            nino = Some(Nino("HW827856C")),
            confidenceLevel = Some(200)
          )
        )
        .isValid shouldBe true
      UserValidator(Some(AG.Individual)).validate(User("foo", nino = None)).isValid shouldBe true

      UserValidator(Some(AG.Individual))
        .validate(
          User("foo", nino = Some(Nino("HW827856C")), confidenceLevel = None)
        )
        .isValid shouldBe false
    }

    "validate only when dateOfBirth is none or set for an Individual" in {
      val now = LocalDate.now()
      UserValidator(Some(AG.Individual))
        .validate(UserGenerator.individual(userId = "foo", dateOfBirth = "1975-03-29"))
        .isValid shouldBe true

      UserValidator(Some(AG.Organisation))
        .validate(User("foo", dateOfBirth = Some(now)))
        .isValid shouldBe false
    }

    "validate only when address is none or valid" in {
      val user = UserGenerator.agent()
      UserValidator(Some(AG.Agent)).validate(user.copy(address = None)).isValid shouldBe true
      UserValidator(Some(AG.Agent)).validate(user.copy(address = Some(User.Address()))).isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line1 = Some("f")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line1 = Some("f" * 35)))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line2 = Some("f")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line3 = Some("f")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line4 = Some("f")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(postcode = Some("CX12 6BU")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(countryCode = Some("GB")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(countryCode = Some("GB")))))
        .isValid shouldBe true
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(countryCode = None))))
        .isValid shouldBe true

      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(postcode = Some("CX12")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(postcode = Some("")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line1 = Some("")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line2 = Some("")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line3 = Some("")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line4 = Some("")))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line1 = Some("a" * 36)))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line2 = Some("a" * 36)))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line3 = Some("a" * 36)))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(line4 = Some("a" * 36)))))
        .isValid shouldBe false
      UserValidator(Some(AG.Agent))
        .validate(user.copy(address = Some(User.Address(countryCode = Some("")))))
        .isValid shouldBe false
    }
  }
}
