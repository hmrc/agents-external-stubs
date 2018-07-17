package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import play.api.libs.json.Reads._
import uk.gov.hmrc.agentsexternalstubs.services.RetrievalService

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

case class AuthoriseRequest(authorise: Seq[Predicate], retrieve: Seq[String])

object AuthoriseRequest {
  implicit val format: Format[AuthoriseRequest] = Json.format[AuthoriseRequest]
  val empty: AuthoriseRequest = new AuthoriseRequest(Seq.empty, Seq.empty)
}

sealed trait Predicate {
  def validate(retrievalService: RetrievalService, authenticatedSession: AuthenticatedSession)(
    implicit ex: ExecutionContext): Future[Either[String, Unit]]
}

case class EnrolmentPredicate(enrolment: String) extends Predicate {
  override def validate(retrievalService: RetrievalService, authenticatedSession: AuthenticatedSession)(
    implicit ex: ExecutionContext): Future[Either[String, Unit]] =
    for {
      enrolments <- retrievalService.principalEnrolments(authenticatedSession.userId)
      authorised = enrolments.collectFirst {
        case Enrolment(`enrolment`, _) =>
      }
    } yield authorised.toRight("InsufficientEnrolments")
}

case class AuthProviders(authProviders: Seq[String]) extends Predicate {
  override def validate(retrievalService: RetrievalService, authenticatedSession: AuthenticatedSession)(
    implicit ex: ExecutionContext): Future[Either[String, Unit]] =
    Future.successful(authProviders.contains(authenticatedSession.providerType) match {
      case true  => Right(())
      case false => Left("UnsupportedAuthProvider")
    })
}

object Predicate {

  val supportedPredicateFormats: Set[PredicateFormat[_ <: Predicate]] = Set(
    EnrolmentPredicateFormat,
    AuthProvidersPredicateFormat
  )

  val supportedKeys = supportedPredicateFormats.map(_.key).mkString(",")

  val predicateFormatByClass: Map[Class[_], PredicateFormat[Predicate]] =
    supportedPredicateFormats.map(p => (p.tag.runtimeClass, p.asInstanceOf[PredicateFormat[Predicate]])).toSeq.toMap

  implicit val reads: Reads[Predicate] = JsObjectReads.flatMap(readsForPredicate)

  def readsForPredicate(json: JsObject): Reads[Predicate] = {
    val keys = json.keys
    supportedPredicateFormats
      .collectFirst {
        case r if keys.contains(r.key) => r.format.map[Predicate](a => a)
      }
      .getOrElse(failedReads(json))
  }

  def failedReads(json: JsObject) = new Reads[Predicate]() {
    override def reads(json: JsValue): JsResult[Predicate] =
      JsError(s"Unsupported predicate ${json.toString}, should be one of [$supportedKeys]")
  }

  implicit val writes: Writes[Seq[Predicate]] = new Writes[Seq[Predicate]] {
    override def writes(predicates: Seq[Predicate]): JsValue = {
      val objects = predicates
        .map(p => {
          val pf = predicateFormatByClass.getOrElse(p.getClass, throw new Exception())
          pf.format.writes(p)
        })
      JsArray(objects)
    }
  }

}

abstract class PredicateFormat[P <: Predicate](val key: String)(implicit val tag: ClassTag[P]) {
  val format: Format[P]
}

object EnrolmentPredicateFormat extends PredicateFormat[EnrolmentPredicate]("enrolment") {
  val format = Json.format[EnrolmentPredicate]
}

object AuthProvidersPredicateFormat extends PredicateFormat[AuthProviders]("authProviders") {
  val format = Json.format[AuthProviders]
}
