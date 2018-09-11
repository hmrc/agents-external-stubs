package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._

case class KnownFacts(enrolmentKey: EnrolmentKey, verifiers: Seq[KnownFact], planetId: String) {
  override def toString: String = s"$enrolmentKey~${verifiers.sorted.mkString("~")}"
}

object KnownFacts {

  final val UNIQUE_KEY = "_unique_key"
  final val VERIFIERS_KEYS = "_verifiers_keys"

  type Transformer = JsObject => JsObject

  def uniqueKey(enrolmentKey: String, planetId: String): String = s"$enrolmentKey@$planetId"
  def verifierKey(knownFact: KnownFact, planetId: String): String = s"$knownFact@$planetId"

  private def planetIdOf(json: JsObject): String =
    (json \ "planetId").asOpt[String].getOrElse("hmrc")

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
            s"Known facts verifiers must represent distinct keys, unlike $kf")
          .andThen(_ => kf.verifiers.map(v => validateVerifier(v, service)).reduce(_ combine _))
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
}
