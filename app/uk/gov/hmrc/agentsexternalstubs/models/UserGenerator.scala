package uk.gov.hmrc.agentsexternalstubs.models
import java.util.UUID

import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.smartstub.{Names, Temporal}

object UserGenerator extends Names with Temporal {

  import uk.gov.hmrc.smartstub._

  private implicit val tls: ToLong[String] = new ToLong[String] {
    def asLong(s: String): Long = s.hashCode.toLong
  }

  def name(userId: String): String =
    (for {
      fn <- forename()
      ln <- surname
    } yield s"$fn $ln").seeded(userId).get

  val dateOfBirthLow: java.time.LocalDate = java.time.LocalDate.now().minusYears(100)
  val dateOfBirthHigh: java.time.LocalDate = java.time.LocalDate.now().minusYears(18)

  def dateOfBirth(userId: String): LocalDate =
    date(dateOfBirthLow, dateOfBirthHigh).seeded(userId).map(d => LocalDate.parse(d.toString)).get

  def individual(
    userId: String = UUID.randomUUID().toString,
    confidenceLevel: Int = 50,
    credentialRole: String = "User",
    nino: String = "HW827856C",
    name: String = "John Smith",
    dateOfBirth: String = "1975-03-29"): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Individual),
      confidenceLevel = Some(confidenceLevel),
      credentialRole = Some(credentialRole),
      nino = Some(Nino(nino)),
      name = Some(name),
      dateOfBirth = Some(LocalDate.parse(dateOfBirth, ISODateTimeFormat.date()))
    )

}
