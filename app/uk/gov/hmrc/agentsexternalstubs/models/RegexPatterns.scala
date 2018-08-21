package uk.gov.hmrc.agentsexternalstubs.models
import scala.util.matching.Regex

object RegexPatterns {

  val nino = "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r
  val arn = "^[A-Z]ARN[0-9]{7}$".r
  val utr = "^[0-9]{10}$".r
  val mtdbsa = "^[A-Z0-9]{1,16}$".r

  val `date_yyyy-MM-dd` =
    "^(((19|20)([2468][048]|[13579][26]|0[48])|2000)[-]02[-]29|((19|20)[0-9]{2}[-](0[469]|11)[-](0[1-9]|1[0-9]|2[0-9]|30)|(19|20)[0-9]{2}[-](0[13578]|1[02])[-](0[1-9]|[12][0-9]|3[01])|(19|20)[0-9]{2}[-]02[-](0[1-9]|1[0-9]|2[0-8])))$".r

  def validate(regex: Regex)(value: String): Either[String, String] = value match {
    case regex(s) => Right(s)
    case _        => Left(s"Supplied value $value does not match pattern ${regex.pattern.toString}")
  }

}
