package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.{Format, Json}

case class Enrolment(key: String, identifiers: Option[Seq[Identifier]] = None) {

  lazy val toEnrolmentKey: Option[EnrolmentKey] = identifiers.map(ii => EnrolmentKey(key, ii))
  lazy val toEnrolmentKeyTag: Option[String] = toEnrolmentKey.map(_.tag)

  def description: String =
    s"enrolment for service $key${identifiers.map(_.map(i => s"${i.key.toUpperCase} ${i.value}").mkString(" and ")).map(x => s" with identifier $x").getOrElse("")}"

  def matches(ek: EnrolmentKey): Boolean = toEnrolmentKeyTag.contains(ek.tag)
}

object Enrolment {

  implicit val format: Format[Enrolment] = Json.format[Enrolment]

  def from(ek: EnrolmentKey): Enrolment =
    Enrolment(ek.service, if (ek.identifiers.isEmpty) None else Some(ek.identifiers))

  def apply(key: String, identifierKey: String, identifierValue: String): Enrolment =
    Enrolment(key, Some(Seq(Identifier(identifierKey, identifierValue))))

  import Validator.Implicits._

  val validate: Enrolment => Validated[String, Unit] = e => {
    e.identifiers match {
      case None => Valid(())
      case Some(identifiers) =>
        Services(e.key) match {
          case None => Invalid(s"Unknown service ${e.key}")
          case Some(service) =>
            Validated
              .cond(
                service.identifiers.size == identifiers.size,
                (),
                s"Expected ${service.identifiers.size} identifiers while got ${identifiers.size}")
              .andThen(_ => identifiers.map(i => validateIdentifier(i, service)).reduce(_ combine _))
        }
    }
  }

  def validateIdentifier(identifier: Identifier, service: Service): Validated[String, Unit] =
    service.getIdentifier(identifier.key) match {
      case None => Invalid(s"Service ${service.name} does not allow for ${identifier.key} identifiers")
      case Some(serviceIdentifier) =>
        serviceIdentifier
          .validate(identifier.value)
          .fold(
            _ =>
              Invalid(
                s"Identifier's ${identifier.key} value does not match expected pattern ${serviceIdentifier.regex}"),
            _ => Valid(())
          )
    }

}
