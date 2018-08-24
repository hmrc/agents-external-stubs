package uk.gov.hmrc.agentsexternalstubs.models
import java.util.Base64

import play.api.libs.json.{Format, Json, Reads}

import scala.io.Source

case class Service(
  name: String,
  description: String,
  affinityGroups: Seq[String],
  identifier: Service.Identifier,
  knownFacts: Seq[Service.KnownFact],
  flags: Service.Flags)

object Service {
  case class Identifier(name: String, description: String, regex: String)
  case class KnownFact(name: String, description: String, regex: String)

  case class Flags(
    uniqueIdentifiers: Boolean,
    agentExclusive: Boolean,
    agentExcludesPrincipal: Boolean,
    multipleEnrolment: Boolean,
    autoEnrolment: Boolean,
    autoActivation: Boolean)
}

case class Services(services: Seq[Service])

object Services {

  implicit val f0: Format[Service.Identifier] = Json.format[Service.Identifier]
  implicit val f1: Format[Service.KnownFact] = Json.format[Service.KnownFact]
  implicit val f2: Format[Service.Flags] = Json.format[Service.Flags]
  implicit val f3: Format[Service] = Json.format[Service]
  implicit val f4: Format[Services] = Json.format[Services]

  val services: Seq[Service] = {
    val json = Source
      .fromInputStream(Base64.getDecoder.wrap(this.getClass.getResourceAsStream("/services.b64")), "utf-8")
      .mkString
    Json.parse(json).as[Services].services
  }

  lazy val servicesByKey: Map[String, Service] = services.map(s => (s.name, s)).toMap
  lazy val individualServices: Seq[Service] = services.filter(_.affinityGroups.contains(User.AG.Individual))
  lazy val organisationServices: Seq[Service] = services.filter(_.affinityGroups.contains(User.AG.Organisation))
  lazy val agentServices: Seq[Service] = services.filter(_.affinityGroups.contains(User.AG.Agent))

}
