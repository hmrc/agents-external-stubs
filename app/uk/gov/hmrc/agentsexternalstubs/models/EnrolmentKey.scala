/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json._

case class EnrolmentKey(service: String, identifiers: Seq[Identifier]) {
  def isSingle: Boolean = identifiers.size == 1
  lazy val tag = s"$service~${identifiers.sorted.mkString("~")}"
  override def toString: String = tag
}

object EnrolmentKey {

  def apply(s: String): EnrolmentKey =
    parse(s).fold(errors => throw new Exception(s"Invalid enrolmentKey, $errors"), identity)

  def from(service: String, identifiers: (String, String)*): EnrolmentKey =
    EnrolmentKey(service, identifiers.map { case (k, v) => Identifier(k, v) })

  def parse(s: String): Either[String, EnrolmentKey] = {
    val parts = s.split("~")
    if (parts.nonEmpty && parts.size >= 3 && parts.size % 2 == 1) {
      val service = parts.head
      val identifiers = parts.tail.sliding(2, 2).map(a => Identifier(a(0), a(1))).toSeq
      Right(EnrolmentKey(service, identifiers)).right.flatMap(validateService).right.flatMap(validateIdentifiers)
    } else Left("INVALID_ENROLMENT_KEY")
  }

  def validateService(ek: EnrolmentKey): Either[String, EnrolmentKey] =
    if (ek.service.nonEmpty) Services(ek.service).map(_ => Right(ek)).getOrElse(Left("INVALID_SERVICE"))
    else Left("INVALID_SERVICE")

  def validateIdentifiers(ek: EnrolmentKey): Either[String, EnrolmentKey] = Services(ek.service) match {
    case None => Left("INVALID_SERVICE")
    case Some(service) =>
      ek.identifiers
        .foldLeft[Either[String, Unit]](Right(()))((a, i) => a.right.flatMap(_ => validateIdentifier(i, service)))
        .right
        .flatMap(_ => if (ek.identifiers == ek.identifiers.sorted) Right(ek) else Left("INVALID_IDENTIFIERS"))
  }

  def validateIdentifier(identifier: Identifier, service: Service): Either[String, Unit] =
    if (identifier.key.nonEmpty && identifier.value.nonEmpty && identifier.key.length <= 40 && identifier.value.length <= 50)
      service.getIdentifier(identifier.key) match {
        case None => Left("INVALID_IDENTIFIERS")
        case Some(serviceIdentifier) =>
          serviceIdentifier
            .validate(identifier.value)
            .fold(
              _ => Left("INVALID_IDENTIFIERS"),
              _ => Right(())
            )
      } else Left("INVALID_IDENTIFIERS")

  implicit val writes: Writes[EnrolmentKey] = new Writes[EnrolmentKey] {
    override def writes(ek: EnrolmentKey): JsValue = JsString(ek.toString)
  }

  implicit val reads: Reads[EnrolmentKey] = new Reads[EnrolmentKey] {
    override def reads(json: JsValue): JsResult[EnrolmentKey] = json match {
      case JsString(value) => parse(value).fold(JsError.apply, JsSuccess.apply(_))
      case _               => JsError("STRING_VALUE_EXPECTED")
    }
  }

}
