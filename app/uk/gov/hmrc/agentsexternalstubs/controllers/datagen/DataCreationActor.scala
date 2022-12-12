/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.controllers.datagen

import akka.actor.{Actor, Props}
import play.api.Logging
import play.api.libs.json.{JsArray, JsString}
import uk.gov.hmrc.agentsexternalstubs.models.{Group, Record, User}
import uk.gov.hmrc.agentsexternalstubs.repository.{GroupsRepositoryMongo, JsonAbuse, RecordsRepositoryMongo, UsersRepositoryMongo}

import scala.concurrent.ExecutionContext

case class UserCreationPayload(user: User, planetId: String)
case class RecordCreationPayload(record: Record, planetId: String)
case class GroupCreationPayload(group: Group, planetId: String)

object DataCreationActor {
  def props(
    usersRepository: UsersRepositoryMongo,
    recordsRepository: RecordsRepositoryMongo,
    groupsRepository: GroupsRepositoryMongo
  )(implicit
    executionContext: ExecutionContext
  ): Props = Props(new DataCreationActor(usersRepository, recordsRepository, groupsRepository))

}

class DataCreationActor(
  usersRepository: UsersRepositoryMongo,
  recordsRepository: RecordsRepositoryMongo,
  groupsRepository: GroupsRepositoryMongo
)(implicit
  executionContext: ExecutionContext
) extends Actor with Logging {

  import uk.gov.hmrc.agentsexternalstubs.syntax.|>

  override def receive: Receive = {
    case UserCreationPayload(user: User, planetId: String) =>
      usersRepository
        .create(user, planetId)
        .recover { case ex =>
          logger.error(s"Could not create user ${user.userId} of $planetId: ${ex.getMessage}")
        }
    case RecordCreationPayload(entity: Record, planetId: String) =>
      recordsRepository
        .rawStore(recordAsJson(entity, planetId))
        .map(id => logger.debug(s"Created record of id $id"))
        .recover { case ex =>
          logger.error(s"Could not create record for ${entity.uniqueKey} of $planetId: ${ex.getMessage}")
        }
    case GroupCreationPayload(group: Group, planetId: String) =>
      groupsRepository
        .create(group, planetId)
        .recover { case ex =>
          logger.error(s"Could not create group ${group.groupId} of $planetId: ${ex.getMessage}")
        }

  }

  private def recordAsJson(record: Record, planetId: String): JsonAbuse[Record] = {
    val PLANET_ID = "_planetId"
    val UNIQUE_KEY = "_uniqueKey"
    val KEYS = "_keys"
    val TYPE = "_record_type"
    val typeName = Record.typeOf(record)

    JsonAbuse(record)
      .addField(PLANET_ID, JsString(planetId))
      .addField(TYPE, JsString(typeName))
      .addField(
        KEYS,
        JsArray(
          record.uniqueKey
            .map(key => record.lookupKeys :+ key)
            .getOrElse(record.lookupKeys)
            .map(key => JsString(keyOf(key, planetId, typeName)))
        )
      )
      .|> { obj =>
        record.uniqueKey
          .map(uniqueKey => obj.addField(UNIQUE_KEY, JsString(keyOf(uniqueKey, planetId, typeName))))
          .getOrElse(obj)
      }
  }

  private def keyOf[T <: Record](key: String, planetId: String, recordType: String): String =
    s"$recordType:${key.replace(" ", "").toLowerCase}@$planetId"

}
