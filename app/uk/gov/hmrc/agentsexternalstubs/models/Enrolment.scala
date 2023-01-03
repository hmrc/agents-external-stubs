/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._

case class Enrolment(
  key: String,
  identifiers: Option[Seq[Identifier]] = None,
  state: String = Enrolment.ACTIVATED,
  friendlyName: Option[String] = None
) {

  lazy val toEnrolmentKey: Option[EnrolmentKey] = identifiers.map(ii => EnrolmentKey(key, ii))
  lazy val toEnrolmentKeyTag: Option[String] = toEnrolmentKey.map(_.tag)

  def description: String =
    s"enrolment for service $key${identifiers.map(_.map(i => s"${i.key.toUpperCase} ${i.value}").mkString(" and ")).map(x => s" with identifier $x").getOrElse("")}"

  def matches(ek: EnrolmentKey): Boolean = toEnrolmentKeyTag.contains(ek.tag)

  def isActivated: Boolean = state == Enrolment.ACTIVATED

  def identifierValueOf(identifierName: String): Option[String] =
    identifiers.flatMap(_.find(_.key == identifierName)).map(_.value)
}

object Enrolment {

  val ACTIVATED = "Activated"

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
                s"Expected ${service.identifiers.size} identifiers while got ${identifiers.size}"
              )
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
                s"Identifier's ${identifier.key} value does not match expected pattern ${serviceIdentifier.regex}"
              ),
            _ => Valid(())
          )
    }

  import play.api.libs.functional.syntax._

  val reads: Reads[Enrolment] = ((JsPath \ "key").read[String] and
    (JsPath \ "identifiers").readNullable[Seq[Identifier]] and
    (JsPath \ "state").readNullable[String].map(_.getOrElse(Enrolment.ACTIVATED)) and
    (JsPath \ "friendlyName").readNullable[String])((k, ii, s, fn) => Enrolment.apply(k, ii, s, fn))

  val writes: Writes[Enrolment] = Json.writes[Enrolment]

  implicit val format: Format[Enrolment] = Format(reads, writes)

}
