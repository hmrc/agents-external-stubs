package uk.gov.hmrc.agentsexternalstubs.controllers
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.agentsexternalstubs.models.RegexPatterns

object MoreConstraints {

  def pattern(matcher: RegexPatterns.Matcher, field: String): Constraint[String] =
    Constraint(
      matcher(_).fold(e => Invalid(s"Invalid value of the `$field`, does not follow regex pattern"), _ => Valid))

}
