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
import uk.gov.hmrc.agentsexternalstubs.support.BaseUnitSpec

import java.time.LocalDate

class UserSanitizerSpec extends BaseUnitSpec {

  "UserSanitizer" should {
    "add missing name to the Individual" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).name shouldBe Some(
        "Kaylee Phillips"
      )
      UserSanitizer(Some(AG.Individual)).sanitize(User("boo")).name shouldBe Some(
        "Nicholas Isnard"
      )
    }

    "add missing name to the Agent" in {
      UserSanitizer(Some(AG.Agent)).sanitize(User("foo")).name shouldBe Some("Kaylee Hastings")
      UserSanitizer(Some(AG.Agent)).sanitize(User("boo")).name shouldBe Some("Nicholas Bates")
    }

    "add missing name to the Organisation" in {
      UserSanitizer(Some(AG.Organisation)).sanitize(User("boo")).name shouldBe Some(
        "Markets"
      )
      UserSanitizer(Some(AG.Organisation)).sanitize(User("zoo")).name shouldBe Some(
        "Honder Energy Swissa Ltd."
      )
    }

    "add missing dateOfBirth to the Individual" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).dateOfBirth.isDefined shouldBe true
      UserSanitizer(Some(AG.Individual)).sanitize(User("boo")).dateOfBirth.isDefined shouldBe true
    }

    "add missing NINO to the Individual" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).nino shouldBe Some(
        Nino("XC 93 60 45 D")
      )
      UserSanitizer(Some(AG.Individual)).sanitize(User("boo")).nino shouldBe Some(
        Nino("AB 61 73 12 C")
      )
    }

    "allow NINO for Business" in {
      UserSanitizer(Some(AG.Organisation))
        .sanitize(User("foo", nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe Some(Nino("XC 93 60 45 D"))
      UserSanitizer(Some(AG.Agent))
        .sanitize(User("foo", nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe Some(Nino("XC 93 60 45 D"))
      UserSanitizer(Some(AG.Individual))
        .sanitize(User("foo", nino = Some(Nino("XC 93 60 45 D"))))
        .nino shouldBe Some(Nino("XC 93 60 45 D"))
    }

    "add missing ConfidenceLevel to the Individual" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).confidenceLevel shouldBe Some(250)
      UserSanitizer(Some(AG.Individual)).sanitize(User("boo")).confidenceLevel shouldBe Some(250)
    }

    "remove ConfidenceLevel if not Individual" in {
      UserSanitizer(Some(AG.Organisation))
        .sanitize(User("foo", confidenceLevel = Some(100)))
        .confidenceLevel shouldBe None
      UserSanitizer(Some(AG.Agent))
        .sanitize(User("foo", confidenceLevel = Some(100)))
        .confidenceLevel shouldBe None
    }

    "add or remove CredentialRole when appropriate" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).credentialRole shouldBe Some(
        User.CR.User
      )
      UserSanitizer(Some(AG.Agent)).sanitize(User("foo")).credentialRole shouldBe Some(
        User.CR.User
      )
      UserSanitizer(Some(AG.Organisation)).sanitize(User("foo")).credentialRole shouldBe Some(
        User.CR.Admin
      )
      UserSanitizer(None).sanitize(User("foo")).credentialRole shouldBe None
      UserSanitizer(Some(AG.Organisation))
        .sanitize(User("foo", credentialRole = Some(User.CR.User)))
        .credentialRole shouldBe Some(User.CR.User)
      UserSanitizer(Some(AG.Organisation))
        .sanitize(User("foo", credentialRole = Some(User.CR.Admin)))
        .credentialRole shouldBe Some(User.CR.Admin)
      UserSanitizer(None)
        .sanitize(User("foo", credentialRole = Some(User.CR.User)))
        .credentialRole shouldBe None
    }

    "allow DateOfBirth for Business" in {
      val now = LocalDate.now()
      UserSanitizer(Some(AG.Individual))
        .sanitize(User("foo", dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
      UserSanitizer(Some(AG.Organisation))
        .sanitize(User("foo", dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
      UserSanitizer(Some(AG.Agent))
        .sanitize(User("foo", dateOfBirth = Some(now)))
        .dateOfBirth shouldBe Some(now)
    }

    "add missing GroupIdentifier" in {
      UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).groupId.isDefined shouldBe true
      UserSanitizer(Some(AG.Agent)).sanitize(User("foo")).groupId.isDefined shouldBe true
      UserSanitizer(Some(AG.Organisation)).sanitize(User("foo")).groupId.isDefined shouldBe true
      UserSanitizer(None).sanitize(User("foo")).groupId.isDefined shouldBe false
    }

    "remove irrelevant data if STRIDE role present" in {
      val sanitized = UserSanitizer(None)
        .sanitize(
          User(
            "foo",
            strideRoles = Seq("FOO")
          )
        )
      sanitized.strideRoles shouldBe Seq("FOO")
      sanitized.nino shouldBe None
      sanitized.credentialRole shouldBe None
      sanitized.credentialStrength shouldBe None
      sanitized.confidenceLevel shouldBe None
    }

    "add missing address" in {
      val address = UserSanitizer(Some(AG.Individual)).sanitize(User("foo")).address
      address shouldBe defined
      address.get.postcode shouldBe defined
      address.get.line1 shouldBe defined
      address.get.line2 shouldBe defined
      address.get.countryCode shouldBe defined
    }

    "add missing address fields" in {
      val address =
        UserSanitizer(Some(AG.Individual))
          .sanitize(User("foo", address = Some(User.Address(line1 = Some("foo")))))
          .address
      address shouldBe defined
      address.get.postcode shouldBe defined
      address.get.line1 shouldBe defined
      address.get.line2 shouldBe defined
      address.get.countryCode shouldBe defined
    }
  }

}
