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

import java.util.Base64
import uk.gov.hmrc.agentmtdidentifiers.model.Identifier
import org.scalacheck.{Arbitrary, Gen}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.agentsexternalstubs.models.RegexPatterns.Matcher
import scala.io.Source
import java.io._
import java.nio.charset.StandardCharsets

case class Service(
  name: String,
  description: String,
  affinityGroups: Seq[String],
  identifiers: Seq[Service.Identifier],
  knownFacts: Seq[Service.KnownFact],
  flags: Service.Flags
) {

  def getIdentifier(name: String): Option[Service.Identifier] = identifiers.find(_.name.toUpperCase == name.toUpperCase)

  def getKnownFact(name: String): Option[Service.KnownFact] = knownFacts.find(_.name.toUpperCase == name.toUpperCase)

  val generator: Gen[Enrolment] = {
    val identifiersGen: Gen[Seq[Identifier]] =
      identifiers.map(_.generator.map(Seq(_))).reduce((a, b) => a.flatMap(ia => b.map(ib => ia ++ ib)))
    identifiersGen.map(ii => Enrolment(name, Some(ii)))
  }
}

object Service {

  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)

  case class Identifier(name: String, description: String, regex: String, pattern: Option[String])
      extends IdentifierLike {

    val generator: Gen[uk.gov.hmrc.agentmtdidentifiers.model.Identifier] =
      valueGenerator.map(value =>
        uk.gov.hmrc.agentmtdidentifiers.model.Identifier(
          name,
          if (value.nonEmpty) value else throw new Exception(s"Could not generate value for an identifier $name")
        )
      )
  }

  case class KnownFact(name: String, description: String, regex: String, pattern: Option[String]) extends IdentifierLike

  case class Flags(
    uniqueIdentifiers: Boolean,
    agentExclusive: Boolean,
    agentExcludesPrincipal: Boolean,
    multipleEnrolment: Boolean,
    autoEnrolment: Boolean,
    autoActivation: Boolean
  )

  trait IdentifierLike {

    val regex: String
    val pattern: Option[String]

    lazy val validate: Matcher = RegexPatterns.validate(regex)
    lazy val valueGenerator: Gen[String] = pattern.map(Generator.pattern).getOrElse(Generator.regex(regex))
  }
}

case class Services(services: Seq[Service])

object Services {

  def apply(name: String): Option[Service] = servicesByKey.get(name)

  implicit val f0: Format[Service.Identifier] = Json.format[Service.Identifier]
  implicit val f1: Format[Service.KnownFact] = Json.format[Service.KnownFact]
  implicit val f2: Format[Service.Flags] = Json.format[Service.Flags]
  implicit val f3: Format[Service] = Json.format[Service]
  implicit val f4: Format[Services] = Json.format[Services]

  val services: Seq[Service] = {
    val json = Source
      .fromInputStream(Base64.getMimeDecoder.wrap(this.getClass.getResourceAsStream("/services.b64")), "utf-8")
      .mkString
    Json.parse(json).as[Services].services
  }

  /** Helper method to run from within sbt console e.g. uk.gov.hmrc.agentsexternalstubs.models.Services.decode() */
  def decode() = {
    val json = Json.prettyPrint(Json.toJson(Services(services)))
    val os = new FileOutputStream(new File("conf/services.json"))
    os.write(json.getBytes(StandardCharsets.UTF_8))
    os.close()
  }

  /** Helper method to run from within sbt console */
  def encode() = {
    val is = new FileInputStream(new File("conf/services.json"))
    val json = Source
      .fromInputStream(is)
      .mkString
    is.close()
    val services2 = Json.parse(json).as[Services]
    val json2 = Json.prettyPrint(Json.toJson(services2))
    val os = Base64.getMimeEncoder.wrap(new FileOutputStream(new File("conf/services.b64")))
    os.write(json2.getBytes(StandardCharsets.UTF_8))
    os.close()
  }

  lazy val servicesByKey: Map[String, Service] = services.map(s => (s.name, s)).toMap
  lazy val individualServices: Seq[Service] = services.filter(_.affinityGroups.contains(AG.Individual))
  lazy val organisationServices: Seq[Service] = services.filter(_.affinityGroups.contains(AG.Organisation))
  lazy val agentServices: Seq[Service] = services.filter(_.affinityGroups.contains(AG.Agent))
  lazy val nonAgentServices: Seq[Service] = services.filter(s => !s.affinityGroups.contains(AG.Individual))

}
