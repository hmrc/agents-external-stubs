package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import play.api.libs.json.Reads._

case class AuthoriseRequest(authorise: Seq[Predicate], retrieve: Seq[String])

object AuthoriseRequest {
  implicit val reads: Reads[AuthoriseRequest] = Json.reads[AuthoriseRequest]
}

sealed trait Predicate
case class EnrolmentPredicate(enrolment: String) extends Predicate
case class AuthProviders(authProviders: Seq[String]) extends Predicate

object Predicate {

  val supportedPredicates: Seq[Reader[_ <: Predicate]] = Seq(
    EnrolmentPredicate,
    AuthProvidersPredicate
  )
  val supportedKeys = supportedPredicates.map(_.key).mkString(",")

  implicit val reads: Reads[Predicate] = JsObjectReads.flatMap(readsForPredicate)

  def readsForPredicate(json: JsObject): Reads[Predicate] = {
    val keys = json.keys
    supportedPredicates
      .collectFirst {
        case r if keys.contains(r.key) => r.reads.map[Predicate](a => a)
      }
      .getOrElse(failedReads(json))
  }

  def failedReads(json: JsObject) = new Reads[Predicate]() {
    override def reads(json: JsValue): JsResult[Predicate] =
      JsError(s"Unsupported predicate ${json.toString}, should be one of [$supportedKeys]")
  }
}

sealed trait Reader[P] {
  def key: String
  val reads: Reads[P]
}

abstract class PredicateReader[P <: Predicate](val key: String) extends Reader[P]

object EnrolmentPredicate extends PredicateReader[EnrolmentPredicate]("enrolment") {
  val reads = Json.reads[EnrolmentPredicate]
}

object AuthProvidersPredicate extends PredicateReader[AuthProviders]("authProviders") {
  val reads = Json.reads[AuthProviders]
}
