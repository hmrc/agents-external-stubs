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
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import uk.gov.hmrc.agentsexternalstubs.controllers.AuthStubController.Authorise
import uk.gov.hmrc.agentsexternalstubs.models.Retrieve.MaybeResponse
import uk.gov.hmrc.agentsexternalstubs.models._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AuthorisationCache {

  type AuthCache = Cache[String, ConcurrentHashMap[AuthoriseRequest, MaybeResponse]]

  private val authPlanetCache: AuthCache = Scaffeine()
    .maximumSize(1000)
    .expireAfterAccess(60.seconds)
    .build()

  private def isEligibleToCache(authoriseRequest: AuthoriseRequest): Boolean = !authoriseRequest.authorise.exists {
    case EnrolmentPredicate(_, _, Some(_)) => true; case _ => false // check for delegated auth rule present
  }

  def get(authenticatedSession: AuthenticatedSession, authoriseRequest: AuthoriseRequest): Option[MaybeResponse] = {
    val key = s"${authenticatedSession.userId}@${authenticatedSession.planetId}"
    if (isEligibleToCache(authoriseRequest))
      authPlanetCache
        .getIfPresent(key)
        .flatMap(m => Option(m.get(authoriseRequest)))
    else None
  }

  def put(
    authenticatedSession: AuthenticatedSession,
    authoriseRequest: AuthoriseRequest,
    maybeResponse: MaybeResponse
  ): Unit = {
    val key = s"${authenticatedSession.userId}@${authenticatedSession.planetId}"
    if (isEligibleToCache(authoriseRequest))
      authPlanetCache
        .get(key, _ => new ConcurrentHashMap[AuthoriseRequest, Retrieve.MaybeResponse]())
        .put(authoriseRequest, maybeResponse)
    else ()
  }

  def updateResultsFor(
    user: User,
    group: Option[Group],
    userService: UsersService,
    groupsService: GroupsService,
    planetId: String
  )(implicit ec: ExecutionContext): Unit = {
    val key = s"${user.userId}@$planetId"
    authPlanetCache
      .get(key, _ => new ConcurrentHashMap[AuthoriseRequest, Retrieve.MaybeResponse]())
      .replaceAll(new BiFunction[AuthoriseRequest, MaybeResponse, MaybeResponse] {
        override def apply(authoriseRequest: AuthoriseRequest, u: MaybeResponse): MaybeResponse =
          Authorise
            .prepareAuthoriseResponse(
              SimplifiedAuthoriseContext(authoriseRequest, user, group, userService, groupsService, Some(planetId))
            )
      })

  }

  def destroyPlanet(planetId: String): Unit = {
    val keySuffix = s"@$planetId"
    val keys = authPlanetCache.asMap().keys.filter(_.endsWith(keySuffix))
    authPlanetCache.invalidateAll(keys)
  }

}

case class SimplifiedAuthoriseContext(
  request: AuthoriseRequest,
  user: User,
  group: Option[Group],
  userService: UsersService,
  groupsService: GroupsService,
  planetId: Option[String]
) extends AuthoriseUserContext(user, group) {

  override def userId: String = user.userId
  override def providerType: String = "GovernmentGateway"
  override def hasDelegatedAuth(rule: String, identifiers: Seq[Identifier]): Boolean = false
}
