package uk.gov.hmrc.agentsexternalstubs.models
import java.util.UUID

import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import org.scalacheck.Gen
import uk.gov.hmrc.domain.Nino

object UserGenerator extends Generator {

  import uk.gov.hmrc.smartstub._

  def nameForIndividual(userId: String): String =
    (for {
      fn <- forename()
      ln <- surname
    } yield s"$fn $ln").seeded(userId).get

  def nameForAgent(seed: String): String = nameForAgent(seed, seed)

  val nameForAgentGen: Gen[String] = for { f <- forename(); s <- surname } yield s"$f $s"
  def nameForAgent(userId: String, groupId: String): String =
    s"${forename().seeded(userId).getOrElse("Agent")} ${surname.seeded(groupId).getOrElse("Cooper")}"

  def nameForOrganisation(userId: String): String = company.seeded(userId).get

  private final val dateOfBirthLow: java.time.LocalDate = java.time.LocalDate.now().minusYears(100)
  private final val dateOfBirthHigh: java.time.LocalDate = java.time.LocalDate.now().minusYears(18)

  def dateOfBirth(userId: String): LocalDate =
    date(dateOfBirthLow, dateOfBirthHigh).seeded(userId).map(d => LocalDate.parse(d.toString)).get

  case class GeneratedAddress(street: String, town: String, postcode: String)
  def address(userId: String): GeneratedAddress =
    ukAddress
      .map { case street :: town :: postcode :: Nil => GeneratedAddress(street, town, postcode) }
      .seeded(userId)
      .get

  private final val groupIdGen = pattern"9Z9Z-Z9Z9-9Z9Z-Z9Z9".gen
  def groupId(userId: String): String = groupIdGen.seeded(userId).get

  private final val agentCodeGen = pattern"ZZZZZZ999999".gen
  def agentCode(userId: String): String = agentCodeGen.seeded(userId).get

  private final val agentIdGen = pattern"999999".gen
  def agentId(userId: String): String = agentIdGen.seeded(userId).get

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
    dateOfBirth: String = null,
    groupId: String = null): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Individual),
      confidenceLevel = Option(confidenceLevel),
      credentialRole = Option(credentialRole),
      nino = Option(nino).map(Nino.apply).orElse(Option(UserGenerator.ninoWithSpaces(userId))),
      name = Option(name).orElse(Option(UserGenerator.nameForIndividual(userId))),
      dateOfBirth = Option(dateOfBirth)
        .map(LocalDate.parse(_, ISODateTimeFormat.date()))
        .orElse(Option(UserGenerator.dateOfBirth(userId))),
      groupId = Option(groupId).orElse(Option(UserGenerator.groupId(userId)))
    )

  def agent(
    userId: String = UUID.randomUUID().toString,
    credentialRole: String = null,
    name: String = null,
    agentCode: String = null,
    agentFriendlyName: String = null,
    agentId: String = null,
    delegatedEnrolments: Seq[Enrolment] = Seq.empty,
    groupId: String = null): User = {
    val gid = Option(groupId).getOrElse(UserGenerator.groupId(userId))
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Agent),
      credentialRole = Option(credentialRole),
      name = Option(name).orElse(Option(UserGenerator.nameForAgent(userId, gid))),
      agentCode = Option(agentCode).orElse(Option(UserGenerator.agentCode(gid))),
      agentFriendlyName = Option(agentFriendlyName).orElse(Option(UserGenerator.agentFriendlyName(gid))),
      agentId = Option(agentId).orElse(Option(UserGenerator.agentId(gid))),
      delegatedEnrolments = delegatedEnrolments,
      groupId = Option(gid)
    )
  }

  def organisation(userId: String = UUID.randomUUID().toString, name: String = null, groupId: String = null): User =
    User(
      userId = userId,
      affinityGroup = Some(User.AG.Organisation),
      name = Option(name).orElse(Option(UserGenerator.nameForOrganisation(userId))),
      groupId = Option(groupId).orElse(Option(UserGenerator.groupId(userId))),
      credentialRole = Some(User.CR.Admin)
    )

}
