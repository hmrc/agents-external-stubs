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

package uk.gov.hmrc.agentsexternalstubs.services

import akka.actor.{Actor, Props}
import uk.gov.hmrc.agentsexternalstubs.models.User

import scala.concurrent.ExecutionContext

object UserCreationActor {

  def props(usersService: UsersService)(implicit executionContext: ExecutionContext): Props =
    Props(new UserCreationActor(usersService))

  case class PlanetUser(planetId: String, user: User)
}

class UserCreationActor(usersService: UsersService)(implicit executionContext: ExecutionContext) extends Actor {

  import UserCreationActor._

  def receive: Receive = { case PlanetUser(planetId: String, user: User) =>
    usersService.createUser(user, planetId)
  }
}
