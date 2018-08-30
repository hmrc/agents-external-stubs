package uk.gov.hmrc.agentsexternalstubs.models
import java.time.format.DateTimeFormatter

import org.scalacheck.Gen
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}
import uk.gov.hmrc.domain.{Nino, Vrn}
import uk.gov.hmrc.smartstub.{Addresses, Companies, Names, Temporal, ToLong}

trait Generator extends Names with Temporal with Companies with Addresses {

  implicit val tls: ToLong[String] = new ToLong[String] {
    def asLong(s: String): Long = s.hashCode.toLong
  }

  import uk.gov.hmrc.smartstub._

  def get[T](gen: Gen[T]): String => T = (seed: String) => gen.seeded(seed).get

  def pattern(pattern: String): Gen[String] =
    knownPatterns.getOrElse(pattern, PatternContext(StringContext(pattern)).pattern())

  def toJodaDate(date: java.time.LocalDate): org.joda.time.LocalDate = org.joda.time.LocalDate.parse(date.toString)

  val ninoWithSpacesGen: Gen[String] =
    Enumerable.instances.ninoEnum.gen.map(n => if (Nino.isValid(n)) n else "AB" + n.drop(2))
  def ninoWithSpaces(seed: String): Nino =
    ninoWithSpacesGen.seeded(seed).map(n => if (Nino.isValid(n)) Nino.apply(n) else ninoWithSpaces("_" + seed)).get

  val ninoNoSpacesGen: Gen[String] =
    Enumerable.instances.ninoEnumNoSpaces.gen.map(n => if (Nino.isValid(n)) n else "AB" + n.drop(2))
  def ninoNoSpaces(seed: String): Nino =
    ninoNoSpacesGen
      .seeded(seed)
      .map(n => if (RegexPatterns.validNinoNoSpaces(n).isRight) Nino.apply(n) else ninoNoSpaces("_" + seed))
      .get

  val mtdbsaGen: Gen[String] = pattern"ZZZZ99999999999".gen
  def mtdbsa(seed: String): MtdItId = mtdbsaGen.map(MtdItId.apply).seeded(seed).get

  val utrGen: Gen[String] = pattern"9999999999".gen
  def utr(seed: String): String = utrGen.seeded(seed).get

  val vrnGen: Gen[String] = pattern"ZZZZ99999999999".gen
  def vrn(seed: String): Vrn = vrnGen.map(Vrn.apply).seeded(seed).get

  val arnGen: Gen[String] = for {
    a <- pattern"Z".gen
    b <- pattern"9999999".gen
  } yield a + "ARN" + b
  def arn(seed: String): Arn = arnGen.map(Arn.apply).seeded(seed).get

  def stringN(size: Int, charGen: Gen[Char] = Gen.alphaNumChar): Gen[String] =
    Gen.listOfN(size, charGen).map(l => String.valueOf(l.toArray))

  def stringMaxN(max: Int, charGen: Gen[Char] = Gen.alphaNumChar): Gen[String] =
    for {
      size   <- Gen.chooseNum(1, max)
      string <- stringN(size, charGen)
    } yield string

  def stringMinMaxN(min: Int, max: Int, charGen: Gen[Char] = Gen.alphaNumChar): Gen[String] =
    for {
      size   <- Gen.chooseNum(min, max)
      string <- stringN(size, charGen)
    } yield string

  val emailGen: Gen[String] = for {
    domain       <- Gen.oneOf(".com", ".co.uk", ".uk", ".eu", ".me")
    size         <- Gen.chooseNum[Int](10, 132 - domain.length)
    usernameSize <- Gen.chooseNum[Int](1, size - 3)
    username     <- stringMaxN(usernameSize)
    host         <- stringMaxN(size - usernameSize - 1)
  } yield username + "@" + host + domain

  val `date_dd/mm/yy` = DateTimeFormatter.ofPattern("dd/MM/yy")
  val `date_MMM` = DateTimeFormatter.ofPattern("MMM")

  val knownPatterns: Map[String, Gen[String]] = Map(
    "nino"           -> ninoNoSpacesGen,
    "ninoWithSpaces" -> ninoWithSpacesGen,
    "email"          -> emailGen,
    "postcode"       -> postcode,
    "phoneNumber"    -> ukPhoneNumber,
    "date:dd/MM/yy"  -> date(1970, 2017).map(_.format(`date_dd/mm/yy`)),
    "date:MMM"       -> date(1970, 2017).map(_.format(`date_MMM`).toUpperCase)
  )

}

object Generator extends Generator
