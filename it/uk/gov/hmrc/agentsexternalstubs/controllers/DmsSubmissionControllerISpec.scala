package uk.gov.hmrc.agentsexternalstubs.controllers

import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._
import uk.gov.hmrc.agentsexternalstubs.support.ServerBaseISpec

class DmsSubmissionControllerISpec extends ServerBaseISpec {

  "POST /dms-submission/submit" should {
    "return 202" in {

      val ws: WSClient = app.injector.instanceOf[WSClient]

      val result = ws.url(s"$url/dms-submission/submit").post(Json.obj())

      await(result).status shouldBe ACCEPTED

    }
  }
}
