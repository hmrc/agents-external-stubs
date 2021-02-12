/*
 * Copyright 2021 HM Revenue & Customs
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

case class KnownFacts(enrolmentKey: EnrolmentKey, verifiers: Seq[KnownFact], planetId: Option[String] = None) {

  override def toString: String = s"$enrolmentKey~${verifiers.sorted.mkString("~")}"

  def getVerifierValue(key: String): Option[String] = verifiers.find(_.key == key).map(_.value)

  def applyProperties(properties: Map[String, String]): KnownFacts =
    copy(verifiers = verifiers.map(kf => properties.get(kf.key).map(value => KnownFact(kf.key, value)).getOrElse(kf)))
}

object KnownFacts {

  val UNIQUE_KEY = "_unique_key"
  val VERIFIERS_KEYS = "_verifiers_keys"

  type Transformer = JsObject => JsObject

  def uniqueKey(enrolmentKey: String, planetId: String): String = s"$enrolmentKey@$planetId"
  def verifierKey(knownFact: KnownFact, planetId: String): String = s"$knownFact@$planetId"

  private def planetIdOf(json: JsObject): String =
    (json \ "planetId").asOpt[String].getOrElse(Planet.DEFAULT)

  private final val addUniqueKey: Transformer = json => {
    val enrolmentKey = (json \ "enrolmentKey").as[String]
    val planetId = planetIdOf(json)
    json + ((UNIQUE_KEY, JsString(uniqueKey(enrolmentKey, planetId))))
  }

  private final val addVerifiersKeys: Transformer = json => {
    val verifiers = (json \ "verifiers").as[Seq[KnownFact]]
    if (verifiers.isEmpty) json
    else {
      val planetId = planetIdOf(json)
      val keys = verifiers.map(key => verifierKey(key, planetId))
      if (keys.isEmpty) json else json + ((VERIFIERS_KEYS, JsArray(keys.map(JsString))))
    }
  }

  implicit val reads: Reads[KnownFacts] = Json.reads[KnownFacts]
  implicit val writes: OWrites[KnownFacts] = Json
    .writes[KnownFacts]
    .transform(addUniqueKey.andThen(addVerifiersKeys))

  val formats = Format(reads, writes)

  import Validator.Implicits._

  val validate: KnownFacts => Validated[String, Unit] = kf =>
    Services(kf.enrolmentKey.service) match {
      case None => Invalid(s"Unknown service ${kf.enrolmentKey.service}")
      case Some(service) =>
        Validated
          .cond(
            kf.verifiers.size == kf.verifiers.map(_.key).distinct.size,
            (),
            s"Known facts verifiers must represent distinct keys, unlike $kf"
          )
          .andThen(_ =>
            if (kf.verifiers.nonEmpty) kf.verifiers.map(v => validateVerifier(v, service)).reduce(_ combine _)
            else Valid(())
          )
    }

  def validateVerifier(knownFact: KnownFact, service: Service): Validated[String, Unit] =
    service.getKnownFact(knownFact.key) match {
      case None => Invalid(s"Service ${service.name} does not know about ${knownFact.key} verifier")
      case Some(serviceKnownFact) =>
        serviceKnownFact
          .validate(knownFact.value)
          .fold(
            _ =>
              Invalid(s"KnownFact's ${knownFact.key} value does not match expected pattern ${serviceKnownFact.regex}"),
            _ => Valid(())
          )
    }

  def generate(
    enrolmentKey: EnrolmentKey,
    seed: String,
    alreadyKnownFacts: String => Option[String]
  ): Option[KnownFacts] =
    Services(enrolmentKey.service).map { s =>
      val verifiers =
        s.knownFacts
          .map(kf =>
            alreadyKnownFacts(kf.name)
              .orElse(Generator.get(kf.valueGenerator)(seed))
              .map(value => KnownFact(kf.name, value))
          )
          .collect { case Some(x) =>
            x
          }
      KnownFacts(enrolmentKey, verifiers)
    }

  val knownFactSanitizer: Service => String => KnownFact => KnownFact = service =>
    seed =>
      entity =>
        service.getKnownFact(entity.key) match {
          case None => entity
          case Some(knownFact) =>
            if (entity.value.isEmpty || !entity.value.matches(knownFact.regex))
              entity.copy(value = Generator.get(knownFact.valueGenerator)(seed).getOrElse(""))
            else entity
        }

  val verifiersSanitizer: String => KnownFacts => KnownFacts = seed =>
    entity => {
      Services(entity.enrolmentKey.service) match {
        case None => entity
        case Some(service) =>
          val verifiers =
            service.knownFacts.map(kf => entity.verifiers.find(_.key == kf.name).getOrElse(KnownFact(kf.name, "")))
          entity.copy(verifiers = verifiers.map(knownFactSanitizer(service)(seed)))
      }
    }

  def sanitize(s: String)(entity: KnownFacts): KnownFacts =
    Seq(verifiersSanitizer).foldLeft(entity)((u, fx) => fx(s)(u))
}
