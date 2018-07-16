package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class AuthoriseRequestSpec extends UnitSpec {

  "AuthoriseRequest" should {
    "parse empty authorise request" in {
      Json.parse(s"""{
                    |"authorise": [],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest.empty
    }

    "serialize empty authorise request" in {
      Json.toJson(AuthoriseRequest.empty).toString() shouldBe """{"authorise":[],"retrieve":[]}"""
    }

    "parse authorise request with enrolment predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "enrolment": "FOO"
                    | }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(EnrolmentPredicate("FOO")),
        Seq.empty)
    }

    "serialize authorise request with enrolment predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(EnrolmentPredicate("FOO")), Seq.empty))
        .toString() shouldBe """{"authorise":[{"enrolment":"FOO"}],"retrieve":[]}"""
    }

    "parse authorise request with authProviders predicate" in {
      Json.parse(s"""{
                    |"authorise": [
                    | { "authProviders": [
                    |     "FOO"
                    |   ]
                    | }
                    |],
                    |"retrieve": []
                    |}""".stripMargin).as[AuthoriseRequest] shouldBe AuthoriseRequest(
        Seq(AuthProviders(Seq("FOO"))),
        Seq.empty)
    }

    "serialize authorise request with authProviders predicate" in {
      Json
        .toJson(AuthoriseRequest(Seq(AuthProviders(Seq("FOO"))), Seq.empty))
        .toString() shouldBe """{"authorise":[{"authProviders":["FOO"]}],"retrieve":[]}"""
    }
  }

}
