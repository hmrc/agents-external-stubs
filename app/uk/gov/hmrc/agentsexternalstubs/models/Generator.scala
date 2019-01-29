package uk.gov.hmrc.agentsexternalstubs.models
import java.time.format.DateTimeFormatter

import org.scalacheck.Gen
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId, UtrCheck, Vrn}
import uk.gov.hmrc.domain.{Modulus11Check, Modulus23Check, Nino}
import uk.gov.hmrc.smartstub.{Addresses, Companies, Names, Temporal, ToLong}
import wolfendale.scalacheck.regexp.RegexpGen

object Generator extends Names with Temporal with Companies with Addresses {

  implicit val tls: ToLong[String] = new ToLong[String] {
    def asLong(s: String): Long = s.hashCode.toLong
  }

  import uk.gov.hmrc.smartstub._

  def get[T](gen: Gen[T]): String => Option[T] =
    (seed: String) => gen.seeded(seed)

  def shake(seed: String): String = {
    val p = seed.charAt(0).toInt % seed.length
    val s = seed.drop(1) + seed.head
    s.take(p).reverse + s.drop(p)
  }

  def variant(seed: String, i: Int): String = if (i == 0) seed else variant(shake(seed), i - 1)

  def pattern(pattern: String): Gen[String] =
    knownPatterns.getOrElse(pattern, PatternContext(StringContext(pattern)).pattern())
  def patternValue(pat: String, seed: String): String = pattern(pat).seeded(seed).get

  def regex(regex: String): Gen[String] =
    knownRegex.getOrElse(regex, RegexpGen.from(regex).retryUntil(s => s.matches(regex)))

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

  lazy val userID: Gen[String] = pattern"999Z999".gen.map("User" + _)
  def userID(seed: String): String = userID.seeded(seed).get

  lazy val planetID: Gen[String] = for {
    prefix <- Gen.oneOf("Mercury", "Venus", "Earth", "Mars", "Jupiter", "Saturn", "Uranus", "Neptune", "Pluto")
    id     <- pattern"ZZ999"
  } yield prefix + "_" + id
  def planetID(seed: String): String = planetID.seeded(seed).get

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

  object VrnChecksum {

    private def calcCheckSum97(total: Int): String = {
      var x = total
      while (x >= 0) {
        x = x - 97
      }
      if (x > -10) s"0${-x}" else (-x).toString
    }

    private def weightedTotal(reference: String): Int = {
      val weighting = List(8, 7, 6, 5, 4, 3, 2)
      val ref = reference.map(_.asDigit).take(7)
      (ref, weighting).zipped.map(_ * _).sum
    }

    def apply(s: String) = s + calcCheckSum97(weightedTotal(s))
  }

  lazy val vrnGen: Gen[String] = pattern"9999999".gen.map(VrnChecksum.apply).retryUntil(Vrn.isValid)
  def vrn(seed: String): Vrn = vrnGen.map(Vrn.apply).seeded(seed).get

  lazy val eoriGen: Gen[String] = pattern"ZZ999999999999".gen
  def eori(seed: String): String = eoriGen.seeded(seed).get

  def chooseBigDecimal(min: Double, max: Double, multipleOf: Option[Double]): Gen[BigDecimal] =
    Gen
      .chooseNum[Double](min.toDouble, max.toDouble)
      .map(BigDecimal.decimal)
      .map(bd =>
        bd.quot(multipleOf.map(BigDecimal.decimal).getOrElse(1)) * multipleOf.map(BigDecimal.decimal).getOrElse(1))

  object Modulus32 extends Modulus23Check {
    def apply(s: String) = calculateCheckCharacter(s) + s
  }

  lazy val arnGen: Gen[String] = (for {
    b <- pattern"9999999".gen
  } yield Modulus32("ARN" + b)).retryUntil(Arn.isValid)
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
    size         <- Gen.chooseNum[Int](10, 32 - domain.length)
    usernameSize <- Gen.chooseNum[Int](1, size - 3)
    username     <- stringMaxN(usernameSize)
    host         <- stringMaxN(size - usernameSize - 1)
  } yield username + "@" + host + domain
  def email(seed: String): String = emailGen.seeded(seed).get

  case class Address(street: String, town: String, postcode: String)
  lazy val addressGen: Gen[Address] = ukAddress
    .map { case street :: town :: postcode :: Nil => Address(street, town, postcode) }
  def address(userId: String): Address = addressGen.seeded(userId).get

  case class Address4Lines35(line1: String, line2: String, line3: String, line4: String)
  lazy val address4Lines35Gen: Gen[Address4Lines35] = surname
    .flatMap(
      s =>
        ukAddress
          .map {
            case street :: town :: postcode :: Nil =>
              Address4Lines35(street.take(35).replace(";", " "), s"The $s House".take(35), town.take(35), postcode)
        })
    .suchThat(_.line1.matches("""^[A-Za-z0-9 \-,.&'\/()!]{1,35}$"""))

  lazy val tradingNameGen: Gen[String] = company

  lazy val `date_dd/MM/yy` = DateTimeFormatter.ofPattern("dd/MM/yy")
  lazy val `date_dd/MM/yyyy` = DateTimeFormatter.ofPattern("dd/MM/yyyy")
  lazy val `date_yyyy-MM-dd` = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  lazy val `date_MMM` = DateTimeFormatter.ofPattern("MMM")

  lazy val dateDDMMYYGen: Gen[String] = date(1970, 2017).map(_.format(`date_dd/MM/yy`))
  lazy val dateDDMMYYYYGen: Gen[String] = date(1970, 2017).map(_.format(`date_dd/MM/yyyy`))
  lazy val dateYYYYMMDDGen: Gen[String] = date(1970, 2017).map(_.format(`date_yyyy-MM-dd`))
  lazy val shortMonthNameGen: Gen[String] = date(1970, 2017).map(_.format(`date_MMM`).toUpperCase)
  lazy val countryCodeGen: Gen[String] =
    Gen.frequency(95 -> Gen.const("GB"), 5 -> Gen.oneOf(Seq("IE", "DE", "FR", "CA", "AU", "ES", "PL", "NL", "BB")))

  lazy val knownPatterns: Map[String, Gen[String]] = Map(
    "arn"             -> arnGen,
    "utr"             -> utrGen,
    "mtditid"         -> mtdbsaGen,
    "vrn"             -> vrnGen,
    "eori"            -> eoriGen,
    "nino"            -> ninoNoSpacesGen,
    "ninoWithSpaces"  -> ninoWithSpacesGen,
    "email"           -> emailGen,
    "postcode"        -> postcode,
    "phoneNumber"     -> ukPhoneNumber,
    "date:dd/MM/yy"   -> dateDDMMYYGen,
    "date:yyyy-MM-dd" -> dateYYYYMMDDGen,
    "date:MMM"        -> shortMonthNameGen,
    "date:dd/MM/yyyy" -> dateDDMMYYYYGen,
    "countrycode"     -> countryCodeGen
  )

  lazy val knownRegex: Map[String, Gen[String]] = Map(
    ".{0,1}"              -> Gen.oneOf("y", "n"),
    "^[A-Za-z0-9]{0,12}$" -> pattern"Zzz00099900Z"
  )

  object GenOps {
    implicit class GenOps[T](val gen: Gen[T]) extends AnyVal {
      def variant(v: String): Gen[T] = gen.withPerturb(s => s.reseed(s.long._1 + v.hashCode.toLong))
    }
  }

}
