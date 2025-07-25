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

package uk.gov.hmrc.agentsexternalstubs.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.agentsexternalstubs.controllers.CitizenDetailsStubController.{GetCitizenResponse, GetDesignatoryDetailsBasicResponse, GetDesignatoryDetailsResponse}
import uk.gov.hmrc.agentsexternalstubs.models.{AG, AuthenticatedSession, Group, User, UserGenerator}
import uk.gov.hmrc.agentsexternalstubs.services.{AuthenticationService, GroupsService, UsersService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentsexternalstubs.models.BusinessPartnerRecord.Common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CitizenDetailsStubController @Inject() (
  val authenticationService: AuthenticationService,
  usersService: UsersService,
  groupsService: GroupsService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc) with CurrentSession {

  def getCitizen(idName: String, taxId: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      idName match {
        case "nino" =>
          Nino.isValid(taxId) match {
            case false => badRequestF("INVALID_NINO", s"Provided NINO $taxId is not valid")
            case true =>
              usersService.findByNino(taxId, session.planetId).map {
                case None       => notFound("CITIZEN_RECORD_NOT_FOUND", s"Citizen record for $idName=$taxId not found")
                case Some(user) => Ok(RestfulResponse(GetCitizenResponse.from(user)))
              }
          }
        case "sautr" =>
          if (taxId.matches(Common.utrPattern)) {
            usersService.findByUtr(taxId, session.planetId).map {
              case None       => notFound("CITIZEN_RECORD_NOT_FOUND", s"Citizen record for $idName=$taxId not found")
              case Some(user) => Ok(RestfulResponse(GetCitizenResponse.from(user)))
            }
          } else badRequestF("INVALID_UTR", s"Provided SAUTR $taxId is not valid")
        case _ => badRequestF("TAX_IDENTIFIER_NOT_SUPPORTED", s"tax identifier $idName not supported")
      }
    }(SessionRecordNotFound)
  }

  def getDesignatoryDetails(nino: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      Nino.isValid(nino) match {
        case false => badRequestF("INVALID_NINO", s"Provided NINO $nino is not valid")
        case true =>
          for {
            maybeUser <- usersService.findByNino(nino, session.planetId)
            maybeGroup <-
              maybeUser
                .flatMap(_.groupId)
                .fold(Future.successful(Option.empty[Group]))(gid => groupsService.findByGroupId(gid, session.planetId))
          } yield (maybeUser, maybeGroup) match {
            case (Some(user), Some(group)) =>
              Ok(RestfulResponse(GetDesignatoryDetailsResponse.from(user, group.affinityGroup)))
            case _ => notFound("NOT_FOUND", s"Citizen details are not found for $nino")
          }
      }
    }(SessionRecordNotFound)
  }

  def getDesignatoryDetailsBasic(nino: String): Action[AnyContent] = Action.async { implicit request =>
    withCurrentSession { session =>
      Nino.isValid(nino) match {
        case false => badRequestF("INVALID_NINO", s"Provided NINO $nino is not valid")
        case true =>
          usersService.findByNino(nino, session.planetId).map {
            case None       => notFound("NOT_FOUND", s"Citizen details are not found for $nino")
            case Some(user) => Ok(RestfulResponse(GetDesignatoryDetailsBasicResponse.from(user, session)))
          }
      }
    }(SessionRecordNotFound)
  }

}

object CitizenDetailsStubController {

  /** {
    *   "name": {
    *     "current": {
    *       "firstName": "John",
    *       "lastName": "Smith"
    *     },
    *     "previous": []
    *   },
    *   "ids": {
    *     "nino": "AA055075C"
    *   },
    *   "dateOfBirth": "11121971"
    * }
    */
  case class GetCitizenResponse(
    name: GetCitizenResponse.Names,
    ids: GetCitizenResponse.Ids,
    dateOfBirth: Option[String],
    deceased: Boolean = false
  )

  object GetCitizenResponse {

    case class Name(firstName: String, lastName: Option[String] = None)
    case class Names(current: Name, previous: Seq[Name] = Seq.empty)
    case class Ids(nino: Option[Nino], sautr: Option[Utr])

    implicit val formats1: Format[Name] = Json.format[Name]
    implicit val formats2: Format[Names] = Json.format[Names]
    implicit val formats3: Format[Ids] = Json.format[Ids]
    implicit val formats4: Format[GetCitizenResponse] = Json.format[GetCitizenResponse]

    private def convertName(name: Option[String]): Name =
      name
        .map { n =>
          val nameParts = n.split(" ")
          val (fn, ln) = if (nameParts.length > 1) {
            (nameParts.init.mkString(" "), Some(nameParts.last))
          } else (nameParts.headOption.getOrElse("John"), Some("Doe"))
          Name(fn, ln)
        }
        .getOrElse(Name("John", Some("Doe")))

    def from(user: User): GetCitizenResponse =
      GetCitizenResponse(
        name = Names(current = convertName(user.name)),
        ids = Ids(nino = user.nino, sautr = user.utr.map(Utr(_))),
        dateOfBirth = user.dateOfBirth.map(_.format(DateTimeFormatter.ofPattern("ddMMyyyy"))),
        deceased = user.isDeceased
      )

  }

  /** {
    *   "etag" : "115",
    *   "person" : {
    *     "firstName" : "HIPPY",
    *     "middleName" : "T",
    *     "lastName" : "NEWYEAR",
    *     "title" : "Mr",
    *     "honours": "BSC",
    *     "sex" : "M",
    *     "dateOfBirth" : "1952-04-01",
    *     "nino" : "TW189213B",
    *     "deceased" : false
    *   },
    *   "address" : {
    *     "line1" : "26 FARADAY DRIVE",
    *     "line2" : "PO BOX 45",
    *     "line3" : "LONDON",
    *     "postcode" : "CT1 1RQ",
    *     "startDate": "2009-08-29",
    *     "country" : "GREAT BRITAIN",
    *     "type" : "Residential"
    *   }
    * }
    */
  case class GetDesignatoryDetailsResponse(
    etag: String,
    person: Option[GetDesignatoryDetailsResponse.Person],
    address: Option[GetDesignatoryDetailsResponse.Address]
  )

  object GetDesignatoryDetailsResponse {

    def from(user: User, affinityGroup: String): GetDesignatoryDetailsResponse =
      GetDesignatoryDetailsResponse(
        user.userId.reverse,
        affinityGroup match {
          case AG.Individual | AG.Agent =>
            Some(
              Person(
                firstName = user.firstName,
                lastName = user.lastName,
                sex = Some(UserGenerator.sex(user.userId)),
                nino = user.nino,
                dateOfBirth = user.dateOfBirth,
                deceased = user.isDeceased
              )
            )
          case _ => None
        },
        user.address.map(a =>
          Address(
            line1 = a.line1,
            line2 = a.line2,
            line3 = a.line3,
            postcode = a.postcode,
            country = a.countryCode.map {
              case "GB" => "GREAT BRITAIN"
              case "PG" => "PAPUA NEW GUINEA"
              case cc   => cc
            }
          )
        )
      )

    case class Person(
      firstName: Option[String] = None,
      lastName: Option[String] = None,
      sex: Option[String] = None,
      nino: Option[Nino] = None,
      dateOfBirth: Option[LocalDate],
      deceased: Boolean = false
    )

    case class Address(
      line1: Option[String] = None,
      line2: Option[String] = None,
      line3: Option[String] = None,
      postcode: Option[String] = None,
      startDate: Option[String] = None,
      country: Option[String] = None,
      `type`: String = "Residential"
    )

    implicit val format1: OFormat[Person] = Json.format[Person]
    implicit val format2: OFormat[Address] = Json.format[Address]
    implicit val format3: OFormat[GetDesignatoryDetailsResponse] = Json.format[GetDesignatoryDetailsResponse]
  }

  /** {
    *   "etag" : "115",
    *   "firstName" : "HIPPY",
    *   "lastName" : "NEWYEAR",
    *   "title" : "Mr",
    *   "nino" : "TW189213B",
    *   "deceased" : false
    * }
    */
  case class GetDesignatoryDetailsBasicResponse(
    etag: String,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    nino: Option[Nino] = None,
    deceased: Boolean = false
  )

  object GetDesignatoryDetailsBasicResponse {
    def from(user: User, session: AuthenticatedSession): GetDesignatoryDetailsBasicResponse =
      GetDesignatoryDetailsBasicResponse(
        etag = user.userId.reverse,
        firstName = user.firstName,
        lastName = user.lastName,
        nino = user.nino,
        deceased = user.isDeceased
      )

    implicit val format1: OFormat[GetDesignatoryDetailsBasicResponse] = Json.format[GetDesignatoryDetailsBasicResponse]
  }
}
