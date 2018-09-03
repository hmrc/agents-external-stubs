package uk.gov.hmrc.agentsexternalstubs.models

import play.api.libs.json._
import uk.gov.hmrc.agentsexternalstubs.models.CreateUpdateAgentRelationshipPayload._

/**
  * ----------------------------------------------------------------------------
  * This CreateUpdateAgentRelationshipPayload code has been generated from json schema
  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
  * ----------------------------------------------------------------------------
  */
case class CreateUpdateAgentRelationshipPayload(
  acknowledgmentReference: String,
  refNumber: String,
  idType: Option[String] = None,
  agentReferenceNumber: String,
  regime: String,
  authorisation: Authorisation,
  relationshipType: Option[String] = None,
  authProfile: Option[String] = None)

object CreateUpdateAgentRelationshipPayload {

  import Validator._

  sealed trait Authorisation { def action: String }

  object Authorisation {

    val validate: Validator[Authorisation] = {
      case x: Authorise   => Authorise.validate(x)
      case x: Deauthorise => Deauthorise.validate(x)
    }

    implicit val reads: Reads[Authorisation] = new Reads[Authorisation] {
      override def reads(json: JsValue): JsResult[Authorisation] = {
        val r0 =
          Authorise.formats.reads(json).flatMap(e => Authorise.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))
        val r1 = r0.orElse(
          Deauthorise.formats.reads(json).flatMap(e => Deauthorise.validate(e).fold(_ => JsError(), _ => JsSuccess(e))))
        r1.orElse(
          aggregateErrors(
            JsError("Could not match json object to any variant of Authorisation, i.e. Authorise, Deauthorise"),
            r0,
            r1))
      }

      private def aggregateErrors[T](errors: JsResult[T]*): JsError =
        errors.foldLeft(JsError())((a, r) =>
          r match {
            case e: JsError => JsError(a.errors ++ e.errors)
            case _          => a
        })
    }

    implicit val writes: Writes[Authorisation] = new Writes[Authorisation] {
      override def writes(o: Authorisation): JsValue = o match {
        case x: Authorise   => Authorise.formats.writes(x)
        case x: Deauthorise => Deauthorise.formats.writes(x)
      }
    }

  }

  case class Authorise(override val action: String, isExclusiveAgent: Boolean) extends Authorisation

  object Authorise {

    val validate: Validator[Authorise] = Validator(
      check(_.action.isOneOf(Common.actionEnum1), "Invalid action, does not match allowed values"))

    implicit val formats: Format[Authorise] = Json.format[Authorise]

  }

  case class Deauthorise(override val action: String) extends Authorisation

  object Deauthorise {

    val validate: Validator[Deauthorise] = Validator(
      check(_.action.isOneOf(Common.actionEnum0), "Invalid action, does not match allowed values"))

    implicit val formats: Format[Deauthorise] = Json.format[Deauthorise]

  }

  val validate: Validator[CreateUpdateAgentRelationshipPayload] = Validator(
    check(
      _.acknowledgmentReference.matches(Common.acknowledgmentReferencePattern),
      s"""Invalid acknowledgmentReference, does not matches regex ${Common.acknowledgmentReferencePattern}"""
    ),
    check(
      _.refNumber.matches(Common.refNumberPattern),
      s"""Invalid refNumber, does not matches regex ${Common.refNumberPattern}"""),
    check(
      _.idType.matches(Common.idTypePattern),
      s"""Invalid idType, does not matches regex ${Common.idTypePattern}"""),
    check(
      _.agentReferenceNumber.matches(Common.agentReferenceNumberPattern),
      s"""Invalid agentReferenceNumber, does not matches regex ${Common.agentReferenceNumberPattern}"""
    ),
    check(
      _.regime.matches(Common.regimePattern),
      s"""Invalid regime, does not matches regex ${Common.regimePattern}"""),
    checkObject(_.authorisation, Authorisation.validate),
    check(
      _.relationshipType.isOneOf(Common.relationshipTypeEnum),
      "Invalid relationshipType, does not match allowed values"),
    check(_.authProfile.isOneOf(Common.authProfileEnum), "Invalid authProfile, does not match allowed values")
  )

  implicit val formats: Format[CreateUpdateAgentRelationshipPayload] = Json.format[CreateUpdateAgentRelationshipPayload]
  object Common {
    val relationshipTypeEnum = Seq("ZA01", "ZA02")
    val actionEnum0 = Seq("De-Authorise")
    val authProfileEnum =
      Seq("ALL00001", "CORR0001", "EDIT0001", "FILE0001", "FILE0002", "FILE0003", "FILE0004", "VIEW0001")
    val refNumberPattern = """^[0-9A-Za-z]{1,15}$"""
    val acknowledgmentReferencePattern = """^\S{1,32}$"""
    val agentReferenceNumberPattern = """^[A-Z](ARN)[0-9]{7}$"""
    val regimePattern = """^[A-Z]{3,10}$"""
    val actionEnum1 = Seq("Authorise")
    val idTypePattern = """^[0-9A-Za-z]{1,6}$"""
  }
}
