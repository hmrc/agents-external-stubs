package uk.gov.hmrc.agentsexternalstubs.models
import java.time.format.DateTimeFormatter

import org.scalacheck.Gen
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, UtrCheck}
import uk.gov.hmrc.domain.{Modulus11Check, Nino, Vrn}
import uk.gov.hmrc.smartstub.{Addresses, Companies, Names, Temporal, ToLong}
import wolfendale.scalacheck.regexp.RegexpGen

trait Generator extends Names with Temporal with Companies with Addresses {

  implicit val tls: ToLong[String] = new ToLong[String] {
    def asLong(s: String): Long = s.hashCode.toLong
  }

  import uk.gov.hmrc.smartstub._

  def get[T](gen: Gen[T]): String => Option[T] =
    (seed: String) => gen.seeded(seed)

  def pattern(pattern: String): Gen[String] =
    knownPatterns.getOrElse(pattern, PatternContext(StringContext(pattern)).pattern())

  def regex(regex: String): Gen[String] =
    knownPatterns.getOrElse(regex, RegexpGen.from(regex).retryUntil(s => s.matches(regex)))

  def toJodaDate(date: java.time.LocalDate): org.joda.time.LocalDate = org.joda.time.LocalDate.parse(date.toString)

  lazy val booleanGen: Gen[Boolean] = Gen.frequency(80 -> Gen.const(true), 20 -> Gen.const(false))

  case class OptionGenStrategy(someFrequency: Int)
  val AlwaysSome = OptionGenStrategy(100)
  val AlwaysNone = OptionGenStrategy(0)
  val MostlySome = OptionGenStrategy(95)

  def optionGen[T](gen: Gen[T])(implicit os: OptionGenStrategy = MostlySome): Gen[Option[T]] =
    Gen.frequency(os.someFrequency -> gen.map(Some(_)), (100 - os.someFrequency) -> Gen.const(None))

  def biasedOptionGen[T](gen: Gen[T]): Gen[Option[T]] = optionGen(gen)(MostlySome)

  def nonEmptyListOfMaxN[T](max: Int, gen: Gen[T]): Gen[List[T]] =
    for {
      size <- Gen.chooseNum(1, max)
      list <- Gen.listOfN(size, gen)
    } yield list

  lazy val ninoWithSpacesGen: Gen[String] =
    Enumerable.instances.ninoEnum.gen.map(n => if (Nino.isValid(n)) n else "AB" + n.drop(2))
  def ninoWithSpaces(seed: String): Nino =
    ninoWithSpacesGen.seeded(seed).map(n => if (Nino.isValid(n)) Nino.apply(n) else ninoWithSpaces("_" + seed)).get

  lazy val ninoNoSpacesGen: Gen[String] =
    Enumerable.instances.ninoEnumNoSpaces.gen.map(n => if (Nino.isValid(n)) n else "AB" + n.drop(2))
  def ninoNoSpaces(seed: String): Nino =
    ninoNoSpacesGen
      .seeded(seed)
      .map(n => if (RegexPatterns.validNinoNoSpaces(n).isRight) Nino.apply(n) else ninoNoSpaces("_" + seed))
      .get

  lazy val mtdbsaGen: Gen[String] = pattern"ZZZZ99999999999".gen
  def mtdbsa(seed: String): MtdItId = mtdbsaGen.map(MtdItId.apply).seeded(seed).get

  object Modulus11 extends Modulus11Check {
    def apply(s: String) = calculateCheckCharacter(s) + s
  }

  lazy val utrGen: Gen[String] = pattern"999999999".gen.map(Modulus11.apply).retryUntil(UtrCheck.isValid)
  def utr(seed: String): String = utrGen.seeded(seed).get

  lazy val vrnGen: Gen[String] = pattern"999999999".gen
  def vrn(seed: String): Vrn = vrnGen.map(Vrn.apply).seeded(seed).get

  def chooseBigDecimal(min: Double, max: Double, multipleOf: Option[Double]): Gen[BigDecimal] =
    Gen
      .chooseNum[Double](min.toDouble, max.toDouble)
      .map(BigDecimal.decimal)
      .map(bd =>
        bd.quot(multipleOf.map(BigDecimal.decimal).getOrElse(1)) * multipleOf.map(BigDecimal.decimal).getOrElse(1))

  lazy val arnGen: Gen[String] = for {
    a <- pattern"Z".gen
    b <- pattern"9999999".gen
  } yield a + "ARN" + b
  def arn(seed: String): Arn = arnGen.map(Arn.apply).seeded(seed).get

  lazy val safeIdGen: Gen[String] = pattern"Z0009999999999".gen.map("X" + _)

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

  lazy val emailGen: Gen[String] = for {
    domain       <- Gen.oneOf(".com", ".co.uk", ".uk", ".eu", ".me")
    size         <- Gen.chooseNum[Int](10, 132 - domain.length)
    usernameSize <- Gen.chooseNum[Int](1, size - 3)
    username     <- stringMaxN(usernameSize)
    host         <- stringMaxN(size - usernameSize - 1)
  } yield username + "@" + host + domain

  case class Address(street: String, town: String, postcode: String)
  lazy val addressGen: Gen[Address] = ukAddress
    .map { case street :: town :: postcode :: Nil => Address(street, town, postcode) }
  def address(userId: String): Address = addressGen.seeded(userId).get

  case class Address4Lines35(line1: String, line2: String, line3: String, line4: String)
  lazy val address4Lines35Gen: Gen[Address4Lines35] = ukAddress
    .map {
      case street :: town :: postcode :: Nil =>
        Address4Lines35(street.take(35).replace(";", " "), "The House", town.take(35), postcode)
    }

  lazy val tradingNameGen: Gen[String] = company.map(_.split(" ").mkString(" "))

  lazy val `date_dd/MM/yy` = DateTimeFormatter.ofPattern("dd/MM/yy")
  lazy val `date_yyyy-MM-dd` = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  lazy val `date_MMM` = DateTimeFormatter.ofPattern("MMM")

  lazy val dateDDMMYYGen: Gen[String] = date(1970, 2017).map(_.format(`date_dd/MM/yy`))
  lazy val dateYYYYMMDDGen: Gen[String] = date(1970, 2017).map(_.format(`date_yyyy-MM-dd`))
  lazy val shortMonthNameGen: Gen[String] = date(1970, 2017).map(_.format(`date_MMM`).toUpperCase)

  lazy val knownPatterns: Map[String, Gen[String]] = Map(
    "arn"             -> arnGen,
    "utr"             -> utrGen,
    "mtditid"         -> mtdbsaGen,
    "vrn"             -> vrnGen,
    "nino"            -> ninoNoSpacesGen,
    "ninoWithSpaces"  -> ninoWithSpacesGen,
    "email"           -> emailGen,
    "postcode"        -> postcode,
    "phoneNumber"     -> ukPhoneNumber,
    "date:dd/MM/yy"   -> dateDDMMYYGen,
    "date:yyyy-MM-dd" -> dateYYYYMMDDGen,
    "date:MMM"        -> shortMonthNameGen
  )

}

object Generator extends Generator {

  object GenOps {
    implicit class GenOps[T](val gen: Gen[T]) extends AnyVal {
      def variant(v: String): Gen[T] = gen.withPerturb(s => s.reseed(s.long._1 + v.hashCode.toLong))
    }
  }

}
