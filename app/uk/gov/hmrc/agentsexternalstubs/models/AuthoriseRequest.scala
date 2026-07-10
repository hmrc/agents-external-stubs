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

import play.api.libs.json.Reads.*
import play.api.libs.json.*

import scala.reflect.ClassTag

case class AuthoriseRequest(authorise: Seq[Predicate], retrieve: Seq[String])

object AuthoriseRequest {
  given format: Format[AuthoriseRequest] = Json.format[AuthoriseRequest]
  val empty: AuthoriseRequest = new AuthoriseRequest(Seq.empty, Seq.empty)
}

sealed trait Predicate {
  def validate(context: AuthoriseContext): Either[String, Unit]
}

object Predicate {

  val supportedPredicateFormats: Set[PredicateFormat[? <: Predicate]] = Set(
    EnrolmentPredicate,
    AuthProvidersPredicate,
    CredentialStrength,
    ConfidenceLevel,
    AffinityGroup,
    HasNino,
    CredentialRole,
    Alternative
  )

  val supportedKeys = supportedPredicateFormats.map(_.key).mkString(",")

  val predicateFormatByClass: Map[Class[?], PredicateFormat[Predicate]] =
    supportedPredicateFormats.map(p => (p.tag.runtimeClass, p.asInstanceOf[PredicateFormat[Predicate]])).toSeq.toMap

  given reads: Reads[Predicate] = JsObjectReads.flatMap(readsForPredicate)

  def readsForPredicate(json: JsObject): Reads[Predicate] = {
    val keys = json.keys
    supportedPredicateFormats
      .collectFirst {
        case r if keys.contains(r.key) =>
          r.format.map[Predicate](a => a)
      }
      .getOrElse(failedReads(json))
  }

  def failedReads(_json: JsObject) = {
    val _ = _json
    new Reads[Predicate]() {
      override def reads(json: JsValue): JsResult[Predicate] =
        JsError(s"Unsupported predicate ${json.toString}, should be one of [$supportedKeys]")
    }
  }

  given writes: Writes[Seq[Predicate]] = new Writes[Seq[Predicate]] {
    override def writes(predicates: Seq[Predicate]): JsValue = {
      val objects = predicates
        .map { p =>
          val pf = predicateFormatByClass.getOrElse(p.getClass, throw new Exception())
          pf.format.writes(p)
        }
      JsArray(objects)
    }
  }

}

abstract class PredicateFormat[P <: Predicate](val key: String)(using val tag: ClassTag[P]) {
  lazy val format: Format[P]
}

case class EnrolmentPredicate(
  enrolment: String,
  identifiers: Option[Seq[Identifier]] = None,
  delegatedAuthRule: Option[String] = None
) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    (delegatedAuthRule, identifiers) match {
      case (Some(rule), Some(identifierSeq)) =>
        if context.hasDelegatedAuth(rule, identifierSeq) then Right(()) else Left("InsufficientEnrolments")
      case (Some(rule), None) =>
        Left(s"Missing predicate part: delegated $rule enrolment identifiers")
      case (None, _) =>
        if context.providerType == "PrivilegedApplication" then
          context.strideRoles.find(_ == enrolment).map(_ => ()).toRight("InsufficientEnrolments")
        else
          context.principalEnrolments
            .collectFirst {
              case Enrolment(`enrolment`, ii, state, _)
                  if ii.nonEmpty && identifiersMatches(identifiers, ii) && state == Enrolment.ACTIVATED =>
              case Enrolment(`enrolment`, ii, state, _) if ii.isEmpty && state == Enrolment.ACTIVATED =>
            }
            .map(_ => ())
            .toRight("InsufficientEnrolments")
    }

  private def identifiersMatches(expected: Option[Seq[Identifier]], provided: Option[Seq[Identifier]]): Boolean =
    (expected, provided) match {
      case (None, _)            => true
      case (Some(sa), Some(sb)) => sa.forall(i => sb.contains(i))
      case _                    => false
    }
}

object EnrolmentPredicate
    extends PredicateFormat[EnrolmentPredicate]("enrolment")(using ClassTag(classOf[EnrolmentPredicate])) {
  given format: Format[EnrolmentPredicate] = Json.format[EnrolmentPredicate]
}

case class AuthProviders(authProviders: Seq[String]) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    authProviders.contains(context.providerType) match {
      case true  => Right(())
      case false => Left("UnsupportedAuthProvider")
    }
}

object AuthProvidersPredicate
    extends PredicateFormat[AuthProviders]("authProviders")(using ClassTag(classOf[AuthProviders])) {
  given format: Format[AuthProviders] = Json.format[AuthProviders]
}

case class CredentialStrength(credentialStrength: String) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.credentialStrength.contains(credentialStrength) match {
      case true  => Right(())
      case false => Left("IncorrectCredentialStrength")
    }
}

object CredentialStrength
    extends PredicateFormat[CredentialStrength]("credentialStrength")(using ClassTag(classOf[CredentialStrength])) {
  given format: Format[CredentialStrength] = Json.format[CredentialStrength]
}

case class ConfidenceLevel(confidenceLevel: Int) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.confidenceLevel.contains(confidenceLevel) match {
      case true  => Right(())
      case false => Left("InsufficientConfidenceLevel")
    }
}

object ConfidenceLevel extends PredicateFormat[ConfidenceLevel]("confidenceLevel")(using ClassTag(classOf[ConfidenceLevel])) {
  given format: Format[ConfidenceLevel] = Json.format[ConfidenceLevel]

  val Default: Int = 600
}

case class AffinityGroup(affinityGroup: String) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.affinityGroup.contains(affinityGroup) match {
      case true  => Right(())
      case false => Left("UnsupportedAffinityGroup")
    }
}

object AffinityGroup extends PredicateFormat[AffinityGroup]("affinityGroup")(using ClassTag(classOf[AffinityGroup])) {
  given format: Format[AffinityGroup] = Json.format[AffinityGroup]
}

case class HasNino(hasNino: Boolean, nino: Option[String] = None) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.nino.isDefined == hasNino match {
      case false => if hasNino then Left("Nino required but not found") else Left("Nino found but not expected")
      case true =>
        nino match {
          case Some(expected) => if context.nino.exists(_.value == expected) then Right(()) else Left("Nino doesn't match")
          case None           => Right(())
        }
    }
}

object HasNino extends PredicateFormat[HasNino]("nino")(using ClassTag(classOf[HasNino])) {
  given format: Format[HasNino] = Json.format[HasNino]
}

case class CredentialRole(credentialRole: String) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    context.credentialRole.contains(credentialRole) match {
      case true  => Right(())
      case false => Left("UnsupportedCredentialRole")
    }
}

object CredentialRole extends PredicateFormat[CredentialRole]("credentialRole")(using ClassTag(classOf[CredentialRole])) {
  given format: Format[CredentialRole] = Json.format[CredentialRole]
}

case class Alternative(`$or`: Seq[Predicate]) extends Predicate {
  override def validate(context: AuthoriseContext): Either[String, Unit] =
    `$or`.foldLeft[Either[String, Unit]](Left(""))((a, p) => a.fold(_ => p.validate(context), Right.apply))
}

object Alternative extends PredicateFormat[Alternative]("$or")(using ClassTag(classOf[Alternative])) {
  val writes: Writes[Alternative] = Json.writes[Alternative]
  val reads: Reads[Alternative] = new Reads[Alternative] {
    override def reads(json: JsValue): JsResult[Alternative] = json match {
      case obj: JsObject =>
        (obj \ "$or").asOpt[Seq[Predicate]].map(Alternative.apply) match {
          case Some(alt) => JsSuccess(alt)
          case None      => JsError("Expected an array value for $or predicate")
        }
      case _ => JsError("Expected a JSON Object")
    }
  }

  given format: Format[Alternative] = Format(reads, writes)

}
