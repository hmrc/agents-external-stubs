package uk.gov.hmrc.agentsexternalstubs.models
import java.util.concurrent.ConcurrentHashMap

import scala.util.Random

object UserIdGenerator {

  val userIdSeries = new ConcurrentHashMap[String, Iterator[String]]

  val defaultUserIds: Seq[String] = Seq(
    "John",
    "Marianne",
    "William",
    "Sara",
    "James",
    "Kay",
    "Charles",
    "Debora",
    "George",
    "Melody",
    "Frank",
    "Heidi",
    "Joseph",
    "Shelly",
    "Thomas",
    "Tammie",
    "Henry",
    "Jody",
    "Robert",
    "Shelley",
    "Edward",
    "Sheri",
    "Harry",
    "Constance",
    "Walter",
    "Gayle",
    "Arthur",
    "Deanna",
    "Fred",
    "Jamie",
    "Albert",
    "Roxanne",
    "Samuel",
    "Grace",
    "David",
    "Ramona",
    "Louis",
    "Karla",
    "Joe",
    "Annie",
    "Charlie",
    "Dianna"
  )

  private def nextUserId: String = Generator.userID(Random.nextString(8))

  def nextUserIdFor(planetId: String): String =
    Option(userIdSeries.get(planetId)) match {
      case Some(iterator) => if (iterator.hasNext) iterator.next() else nextUserId
      case None =>
        val iterator = new UserIdIterator(defaultUserIds)
        userIdSeries.put(planetId, iterator)
        iterator.next()
    }

  def destroyPlanetId(planetId: String): Unit = userIdSeries.remove(planetId)

  private class UserIdIterator(userIds: Seq[String]) extends Iterator[String] {
    @volatile var i = 0
    override def hasNext: Boolean = i < userIds.length - 1
    override def next(): String = {
      val id = userIds(i)
      i = i + 1
      id
    }
  }

}
