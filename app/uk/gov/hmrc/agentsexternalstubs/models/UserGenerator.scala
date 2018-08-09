package uk.gov.hmrc.agentsexternalstubs.models
import java.util.UUID

import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import org.scalacheck.Gen
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.smartstub.{Companies, Names, Temporal}

import scala.util.control.NonFatal

object UserGenerator extends Names with Temporal with Companies {

  import uk.gov.hmrc.smartstub._

  private implicit val tls: ToLong[String] = new ToLong[String] {
    def asLong(s: String): Long = s.hashCode.toLong
  }

  def nameForIndividual(userId: String): String =
    (for {
      fn <- forename()
      ln <- surname
    } yield s"$fn $ln").seeded(userId).get

  def nameForAgent(userId: String): String =
    (for {
      fn <- forename()
      ln <- surname
    } yield s"$fn $ln").seeded(userId + "_agent").get

  def nameForOrganisation(userId: String): String = company.seeded(userId).get

  private final val dateOfBirthLow: java.time.LocalDate = java.time.LocalDate.now().minusYears(100)
  private final val dateOfBirthHigh: java.time.LocalDate = java.time.LocalDate.now().minusYears(18)

  def dateOfBirth(userId: String): LocalDate =
    date(dateOfBirthLow, dateOfBirthHigh).seeded(userId).map(d => LocalDate.parse(d.toString)).get

  private final val ninoGen = Enumerable.instances.ninoEnum.gen
  def nino(userId: String): Nino =
    ninoGen.seeded(userId).map(n => try { Nino.apply(n) } catch { case NonFatal(_) => Nino("HW 82 78 56 C") }).get

  private final val groupIdGen = pattern"9Z9Z-Z9Z9-9Z9Z-Z9Z9".gen
  def groupId(userId: String): String = groupIdGen.seeded(userId).get

  private final val agentCodeGen = pattern"ZZZZZZ999999".gen
  def agentCode(userId: String): String = agentCodeGen.seeded(userId).get

  def agentFriendlyName(userId: String): String =
    (for {
      ln <- surname
      suffix <- Gen.oneOf(
                 " Accountants",
                 " and Company",
                 " & Co",
                 " Professional Services",
                 " Accountancy",
                 " Chartered Accountants & Business Advisers",
                 " Group of Accountants",
                 " Professional",
                 " & " + ln
               )
    } yield s"$ln$suffix").seeded(userId + "_agent").get

  def individual(
    userId: String = UUID.randomUUID().toString,
    confidenceLevel: Int = 50,
    credentialRole: String = User.CR.User,
    nino: String = null,
    name: String = null,
    dateOfBirth: String = null): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Individual),
      confidenceLevel = Some(confidenceLevel),
      credentialRole = Some(credentialRole),
      nino = Option(nino).map(Nino.apply).orElse(Option(UserGenerator.nino(userId))),
      name = Option(name).orElse(Option(UserGenerator.nameForIndividual(userId))),
      dateOfBirth = Option(dateOfBirth)
        .map(LocalDate.parse(_, ISODateTimeFormat.date()))
        .orElse(Option(UserGenerator.dateOfBirth(userId)))
    )

  def agent(
    userId: String = UUID.randomUUID().toString,
    credentialRole: Option[String] = Some(User.CR.User),
    name: String = null,
    agentCode: String = null,
    agentFriendlyName: String = null,
    delegatedEnrolments: Seq[Enrolment] = Seq.empty): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Agent),
      credentialRole = credentialRole,
      name = Option(name).orElse(Option(UserGenerator.nameForAgent(userId))),
      agentCode = Option(agentCode).orElse(Option(UserGenerator.agentCode(userId))),
      agentFriendlyName = Option(agentFriendlyName).orElse(Option(UserGenerator.agentFriendlyName(userId))),
      delegatedEnrolments = delegatedEnrolments
    )

  def organisation(userId: String = UUID.randomUUID().toString, name: String = null): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Organisation),
      name = Option(name).orElse(Option(UserGenerator.nameForOrganisation(userId)))
    )

}
