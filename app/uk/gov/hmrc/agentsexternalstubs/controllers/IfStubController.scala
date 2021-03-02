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

package uk.gov.hmrc.agentsexternalstubs.controllers

import org.joda.time.LocalDate
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Constraints, Invalid, Valid}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentsexternalstubs.controllers.DesStubController.{AuthoriseRequest, AuthoriseResponse}
import uk.gov.hmrc.agentsexternalstubs.models.TrustDetailsResponse.getErrorResponseFor
import uk.gov.hmrc.agentsexternalstubs.models._
import uk.gov.hmrc.agentsexternalstubs.services._
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IfStubController @Inject() (
  val authenticationService: AuthenticationService,
  relationshipRecordsService: RelationshipRecordsService,
  businessPartnerRecordsService: BusinessPartnerRecordsService,
  cc: ControllerComponents
)(implicit usersService: UsersService, executionContext: ExecutionContext)
    extends BackendController(cc) with DesCurrentSession {

  import IfStubController._

  def getRelationship(
    arn: Option[String],
    agent: Boolean,
    regime: String
  ): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      GetRelationships.form.bindFromRequest.fold(
        hasErrors => badRequestF("INVALID_SUBMISSION", hasErrors.errors.map(_.message).mkString(", ")),
        query =>
          relationshipRecordsService
            .findByQuery(query, session.planetId)
            .flatMap { records =>
              def checkSuspension(arn: Arn): Future[Result] =
                businessPartnerRecordsService.getBusinessPartnerRecord(arn, session.planetId) map { case Some(bpr) =>
                  bpr.suspensionDetails match {
                    case Some(sd) =>
                      if (sd.suspendedRegimes.contains(regime))
                        forbidden("AGENT_SUSPENDED", "The remote endpoint has indicated that the agent is suspended")
                      else Ok(Json.toJson(GetRelationships.Response.from(records)))

                    case None => Ok(Json.toJson(GetRelationships.Response.from(records)))
                  }
                }

              records.headOption match {
                case Some(r) =>
                  checkSuspension(Arn(r.arn))

                case None =>
                  if (agent) {
                    checkSuspension(Arn(arn.getOrElse(throw new Exception("agent must have arn"))))
                  } else Future successful Ok(Json.toJson(GetRelationships.Response.from(records)))
              }
            }
      )
    }(SessionRecordNotFound)
  }

  private val HMRC_TERS_ORG = "HMRC-TERS-ORG"
  private val HMRC_TERSNT_ORG = "HMRC-TERSNT-ORG"

  def getTrustKnownFactsUTR(utr: String): Action[AnyContent] =
    getTrustsKnownFacts(utr, RegexPatterns.validUtr, HMRC_TERS_ORG, "SAUTR")

  def getTrustKnownFactsURN(urn: String): Action[AnyContent] =
    getTrustsKnownFacts(urn, RegexPatterns.validUrn, HMRC_TERSNT_ORG, "URN")

  private def getTrustsKnownFacts(
    id: String,
    validation: RegexPatterns.Matcher,
    service: String,
    key: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withCurrentSession { session =>
        validation(id)
          .fold(
            error => badRequestF(error),
            taxIdentifier => {

              val enrolmentKey = EnrolmentKey(service, Seq(Identifier(key, taxIdentifier)))
              usersService
                .findByPrincipalEnrolmentKey(enrolmentKey, session.planetId)
                .map {
                  case Some(record) =>
                    val maybeUtr =
                      extractEnrolmentValue(HMRC_TERS_ORG)(record)
                    val maybeUrn =
                      extractEnrolmentValue(HMRC_TERSNT_ORG)(record)
                    val trustDetails = TrustDetailsResponse(
                      TrustDetails(
                        maybeUtr,
                        maybeUrn,
                        record.name.getOrElse(""),
                        TrustAddress(record.user.address),
                        "TERS"
                      )
                    )
                    Ok(Json.toJson(trustDetails))
                  case None => getErrorResponseFor(id)
                }
            }
          )
      }(SessionRecordNotFound)
    }

  private def extractEnrolmentValue(serviceKey: String)(record: User) =
    record.principalEnrolments
      .find(_.key == serviceKey)
      .flatMap(_.toEnrolmentKeyTag)
      .map(_.split('~').takeRight(1).mkString)

  val authoriseOrDeAuthoriseRelationship: Action[JsValue] = Action.async(parse.tolerantJson) { implicit request =>
    withCurrentSession { session =>
      withPayload[CreateUpdateAgentRelationshipPayload] { payload =>
        CreateUpdateAgentRelationshipPayload
          .validate()(payload)
          .fold(
            error => badRequestF("INVALID_SUBMISSION", error.mkString(", ")),
            _ =>
              if (payload.authorisation.action == "Authorise")
                relationshipRecordsService
                  .authorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
              else
                relationshipRecordsService
                  .deAuthorise(AuthoriseRequest.toRelationshipRecord(payload), session.planetId)
                  .map(_ => Ok(Json.toJson(AuthoriseResponse())))
          )
      }
    }(SessionRecordNotFound)
  }
}

object IfStubController {

  object GetRelationships {

    private val queryConstraint: Constraint[RelationshipRecordQuery] = Constraint(q =>
      if (q.agent && q.arn.isEmpty) Invalid("Missing arn")
      else if (!q.agent && q.refNumber.isEmpty) Invalid("Missing referenceNumber")
      else if ((!q.activeOnly || q.to.isDefined) && q.from.isEmpty) Invalid("Missing from date")
      else if (!q.activeOnly && q.to.isEmpty) Invalid("Missing to date")
      else if ((q.regime == "VATC" || q.regime == "CGT") && q.relationship.isEmpty)
        Invalid(s"relationship type is mandatory for ${q.regime} regime")
      else if ((q.regime == "VATC" || q.regime == "CGT") && q.authProfile.isEmpty)
        Invalid(s"auth profile is mandatory for ${q.regime} regime")
      else Valid
    )

    val form: Form[RelationshipRecordQuery] = Form[RelationshipRecordQuery](
      mapping(
        "regime" -> nonEmptyText.verifying(Constraints.pattern("^[A-Z]{3,10}$".r, "regime", "Invalid regime")),
        "arn"    -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validArn, "arn"))),
        "idtype" -> default(
          nonEmptyText.verifying(Constraints.pattern("^[A-Z]{1,6}$".r, "idtype", "Invalid idtype")),
          "none"
        ),
        "referenceNumber" -> optional(
          nonEmptyText.verifying(
            Constraints.pattern(RegexPatterns.validUrnPattern.r, "referenceNumber", "Invalid referenceNumber")
          )
        ),
        "active-only" -> boolean,
        "agent"       -> boolean,
        "from" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "from")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString)),
        "to" -> optional(nonEmptyText.verifying(MoreConstraints.pattern(RegexPatterns.validDate, "to")))
          .transform[Option[LocalDate]](_.map(LocalDate.parse), Option(_).map(_.toString)),
        "relationship" -> optional(nonEmptyText.verifying("invalid relationship type", _ == "ZA01")),
        "auth-profile" -> optional(nonEmptyText.verifying("invalid auth profile", _ == "ALL00001"))
      )(RelationshipRecordQuery.apply)(RelationshipRecordQuery.unapply).verifying(queryConstraint)
    )

    case class Individual(firstName: String, lastName: String)

    case class Organisation(organisationName: String)

    case class Relationship(
      referenceNumber: String,
      agentReferenceNumber: String,
      dateFrom: LocalDate,
      dateTo: Option[LocalDate] = None,
      contractAccountCategory: String,
      activity: Option[String] = None,
      relationshipType: Option[String] = None,
      authProfile: Option[String] = None,
      individual: Option[Individual] = None,
      organisation: Option[Organisation] = None
    )

    object Relationship {
      import play.api.libs.json.JodaWrites._
      import play.api.libs.json.JodaReads._

      implicit val writes1: Writes[Individual] = Json.writes[Individual]
      implicit val writes2: Writes[Organisation] = Json.writes[Organisation]
      implicit val writes3: Writes[Relationship] = Json.writes[Relationship]

      def from(record: RelationshipRecord): Relationship = Relationship(
        referenceNumber = record.refNumber,
        agentReferenceNumber = record.arn,
        dateFrom = record.startDate.getOrElse(throw new Exception("Missing startDate of relationship")),
        dateTo = record.endDate,
        contractAccountCategory = "33", // magic number!
        relationshipType = record.relationshipType,
        authProfile = record.authProfile,
        individual = decideIndividual(record),
        organisation = decideOrganisation(record)
      )

      def decideIndividual(record: RelationshipRecord): Option[Individual] =
        if (record.regime == "ITSA") {
          val nameParts: Array[String] =
            UserGenerator.nameForIndividual(record.idType + "/" + record.refNumber).split(" ")
          Some(Individual(nameParts.init.mkString(" "), nameParts.last))
        } else None

      def decideOrganisation(record: RelationshipRecord): Option[Organisation] =
        if (record.regime != "ITSA")
          Some(Organisation(UserGenerator.nameForOrganisation(record.idType + "/" + record.refNumber)))
        else None
    }

    case class Response(relationship: Seq[Relationship])

    object Response {
      implicit val writes: Writes[Response] = Json.writes[Response]

      def from(records: Seq[RelationshipRecord]): Response =
        Response(relationship = records.map(Relationship.from))
    }
  }

  case class Response(safeId: String, agentRegistrationNumber: String)

  object Response {
    implicit val writes: Writes[Response] = Json.writes[Response]
  }

}
