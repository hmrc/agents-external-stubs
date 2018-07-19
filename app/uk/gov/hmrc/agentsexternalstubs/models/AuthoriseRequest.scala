package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.reflect.ClassTag

case class AuthoriseRequest(authorise: Seq[Predicate], retrieve: Seq[String])

object AuthoriseRequest {
  implicit val format: Format[AuthoriseRequest] = Json.format[AuthoriseRequest]
  val empty: AuthoriseRequest = new AuthoriseRequest(Seq.empty, Seq.empty)
}

sealed trait Predicate {
  def validate(context: AuthoriseContext): Either[String, Unit]
}

object Predicate {

  val supportedPredicateFormats: Set[PredicateFormat[_ <: Predicate]] = Set(
    EnrolmentPredicate,
    AuthProvidersPredicate,
    CredentialStrength,
    ConfidenceLevel,
    AffinityGroup,
    HasNino
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

case class EnrolmentPredicate(enrolment: String, identifiers: Option[Seq[Identifier]] = None) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.principalEnrolments
      .collectFirst {
        case Enrolment(`enrolment`, ii) if identifiersMatches(identifiers, ii) =>
      }
      .toRight("InsufficientEnrolments")

  private def identifiersMatches(expected: Option[Seq[Identifier]], provided: Option[Seq[Identifier]]): Boolean =
    (expected, provided) match {
      case (None, _)            => true
      case (Some(sa), Some(sb)) => sa.forall(i => sb.contains(i))
      case _                    => false
    }
}

object EnrolmentPredicate extends PredicateFormat[EnrolmentPredicate]("enrolment") {
  implicit val format: Format[EnrolmentPredicate] = Json.format[EnrolmentPredicate]
}

case class AuthProviders(authProviders: Seq[String]) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    authProviders.contains(context.providerType) match {
      case true  => Right(())
      case false => Left("UnsupportedAuthProvider")
    }
}

object AuthProvidersPredicate extends PredicateFormat[AuthProviders]("authProviders") {
  implicit val format: Format[AuthProviders] = Json.format[AuthProviders]
}

case class CredentialStrength(credentialStrength: String) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.credentialStrength.contains(credentialStrength) match {
      case true  => Right(())
      case false => Left("IncorrectCredentialStrength")
    }
}

object CredentialStrength extends PredicateFormat[CredentialStrength]("credentialStrength") {
  implicit val format: Format[CredentialStrength] = Json.format[CredentialStrength]
}

case class ConfidenceLevel(confidenceLevel: Int) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.confidenceLevel == confidenceLevel match {
      case true  => Right(())
      case false => Left("InsufficientConfidenceLevel")
    }
}

object ConfidenceLevel extends PredicateFormat[ConfidenceLevel]("confidenceLevel") {
  implicit val format: Format[ConfidenceLevel] = Json.format[ConfidenceLevel]
}

case class AffinityGroup(affinityGroup: String) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.affinityGroup.contains(affinityGroup) match {
      case true  => Right(())
      case false => Left("UnsupportedAffinityGroup")
    }
}

object AffinityGroup extends PredicateFormat[AffinityGroup]("affinityGroup") {
  implicit val format: Format[AffinityGroup] = Json.format[AffinityGroup]
}

case class HasNino(hasNino: Boolean, nino: Option[String] = None) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.nino.isDefined == hasNino match {
      case false => if (hasNino) Left("Nino required but not found") else Left("Nino found but not expected")
      case true =>
        nino match {
          case Some(expected) => if (context.nino.exists(_.value == expected)) Right(()) else Left("Nino doesn't match")
          case None           => Right(())
        }
    }
}

object HasNino extends PredicateFormat[HasNino]("nino") {
  implicit val format: Format[HasNino] = Json.format[HasNino]
}
