package uk.gov.hmrc.agentsexternalstubs.models
import java.util.concurrent.ConcurrentHashMap

import scala.util.Random

object UserIdGenerator {

  val userIdSeries = new ConcurrentHashMap[String, Iterator[String]]

  val defaultUserIds: Seq[String] = Seq(
    "John",
    "William",
    "James",
    "Charles",
    "George",
    "Frank",
    "Joseph",
    "Thomas",
    "Henry",
    "Robert",
    "Edward",
    "Harry",
    "Walter",
    "Arthur",
    "Fred",
    "Albert",
    "Samuel",
    "David",
    "Louis",
    "Joe",
    "Charlie",
    "Clarence",
    "Richard",
    "Andrew",
    "Daniel",
    "Ernest",
    "Will",
    "Jesse",
    "Oscar",
    "Lewis",
    "Peter",
    "Benjamin",
    "Frederick",
    "Willie",
    "Alfred",
    "Sam",
    "Roy",
    "Herbert",
    "Jacob",
    "Tom",
    "Elmer",
    "Carl",
    "Lee",
    "Howard",
    "Martin",
    "Michael",
    "Bert",
    "Herman",
    "Jim",
    "Francis",
    "Megan",
    "Haley",
    "Mya",
    "Michelle",
    "Molly",
    "Stephanie",
    "Nicole",
    "Jenna",
    "Natalia",
    "Sadie",
    "Jada",
    "Serenity",
    "Lucy",
    "Ruby",
    "Eva",
    "Kennedy",
    "Rylee",
    "Jayla",
    "Naomi",
    "Rebecca",
    "Lydia",
    "Daniela",
    "Bella",
    "Keira",
    "Adriana",
    "Lilly",
    "Hayden",
    "Miley",
    "Katie",
    "Jade",
    "Jordan",
    "Gabriela",
    "Amy",
    "Angela",
    "Melissa",
    "Valerie",
    "Giselle",
    "Diana",
    "Amanda",
    "Kate",
    "Laila",
    "Reagan",
    "Jordyn",
    "Kylee",
    "Danielle",
    "Briana",
    "Marley",
    "Leslie",
    "Kendall",
    "Catherine",
    "Liliana"
  )

  private def nextUserId: String = Generator.userID(Random.nextString(8))

  private def shuffle(strings: Seq[String]): Seq[String] =
    strings.zip(Stream.continually(Random.nextInt())).sortBy(_._2).map(_._1)

  def nextUserIdFor(planetId: String): String =
    Option(userIdSeries.get(planetId)) match {
      case Some(iterator) => if (iterator.hasNext) iterator.next() else nextUserId
      case None =>
        val iterator = new UserIdIterator(shuffle(defaultUserIds))
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
