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

import uk.gov.hmrc.agentsexternalstubs.models.SpecialCase.RequestMatch
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.support.AppBaseISpec
import play.api.test.Helpers._

class SpecialCasesRepositoryISpec extends AppBaseISpec {

  lazy val repo = app.injector.instanceOf[SpecialCasesRepository]

  "SpecialCasesRepository" should {
    "create and find an entity" in {
      val planetId = UUID.randomUUID().toString
      val entity = SpecialCase(RequestMatch("/test1"), SpecialCase.Response(404), Some(planetId))
      val key = entity.requestMatch.toKey

      await(repo.upsert(entity, planetId))

      val result = await(repo.findByMatchKey(key, planetId))
      result shouldBe defined
      result.map(_.response.status) shouldBe Some(404)
      result.flatMap(_.id) shouldBe defined
    }

    "find and update an entity by id" in {
      val planetId = UUID.randomUUID().toString
      val entity = SpecialCase(RequestMatch("/test3"), SpecialCase.Response(404), Some(planetId))
      val id = await(repo.upsert(entity, planetId))

      val result = await(repo.findById(id, planetId))
      result shouldBe defined
      result.map(_.response.status) shouldBe Some(404)
      result.flatMap(_.id) shouldBe Some(Id(id))
    }

    "update an entity by key" in {
      val planetId = UUID.randomUUID().toString
      val entity = SpecialCase(RequestMatch("/test3a"), SpecialCase.Response(404), Some(planetId))
      val id = await(repo.upsert(entity, planetId))

      val id2 = await(repo.upsert(entity.copy(response = SpecialCase.Response(444)), planetId))
      id2 shouldBe id

      val result = await(repo.findById(id2, planetId))
      result shouldBe defined
      result.map(_.response.status) shouldBe Some(444)
      result.flatMap(_.id) shouldBe Some(Id(id2))
    }

    "delete an entity" in {
      val planetId = UUID.randomUUID().toString
      val entity = SpecialCase(RequestMatch("/test2"), SpecialCase.Response(404), Some(planetId))
      val key = entity.requestMatch.toKey

      val id = await(repo.upsert(entity, planetId))
      await(repo.findByMatchKey(key, planetId)) shouldBe defined

      await(repo.delete(id, planetId))

      val result1 = await(repo.findByMatchKey(key, planetId))
      result1 shouldBe None
      val result2 = await(repo.findById(id, planetId))
      result2 shouldBe None
    }

    "delete all entities" in {
      val planetId = UUID.randomUUID().toString
      val entity = SpecialCase(RequestMatch("/test2"), SpecialCase.Response(404), Some(planetId))
      val key = entity.requestMatch.toKey

      val id = await(repo.upsert(entity, planetId))
      await(repo.findByMatchKey(key, planetId)) shouldBe defined

      Thread.sleep(100)

      await(repo.deleteAll(System.currentTimeMillis())) should be >= 1L

      val result1 = await(repo.findByMatchKey(key, planetId))
      result1 shouldBe None
      val result2 = await(repo.findById(id, planetId))
      result2 shouldBe None
    }

    "find all entities on the planet" in {
      val planetId = UUID.randomUUID().toString
      val entity1 = SpecialCase(RequestMatch("/test1", "GET"), SpecialCase.Response(200), Some(planetId))
      val entity2 = SpecialCase(RequestMatch("/test2", "POST"), SpecialCase.Response(201), Some(planetId))
      val entity3 = SpecialCase(RequestMatch("/test3", "PUT"), SpecialCase.Response(202), Some(planetId))
      val entity4 = SpecialCase(RequestMatch("/test4", "DELETE"), SpecialCase.Response(204), Some(planetId))

      await(repo.upsert(entity1, planetId))
      await(repo.upsert(entity2, planetId))
      await(repo.upsert(entity3, planetId))
      await(repo.upsert(entity4, planetId))

      val result = await(repo.findByPlanetId(planetId)(100))
      result.size shouldBe 4
      result.map(_.requestMatch.method) should contain.only("GET", "PUT", "POST", "DELETE")
      result.map(_.requestMatch.path) should contain.only("/test1", "/test2", "/test3", "/test4")
    }

  }
}
