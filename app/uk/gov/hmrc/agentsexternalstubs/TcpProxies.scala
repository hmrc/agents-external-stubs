package uk.gov.hmrc.agentsexternalstubs

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TcpProxies @Inject()(appConfig: AppConfig)(implicit system: ActorSystem, materializer: Materializer) {

  if (appConfig.isProxyMode) {
    Logger(getClass).info("Starting local TCP proxies ...")

    implicit val ec: ExecutionContext = system.dispatcher

    val agentsExternalStubsPort = Try(appConfig.httpPort.toInt).toOption.getOrElse(9009)

    val tcpOutgoingConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
      Tcp().outgoingConnection("localhost", agentsExternalStubsPort)

    val tcpProxy = Flow[ByteString].via(tcpOutgoingConnection)

    def startProxy(port: Int, serviceName: String): Future[Unit] =
      Tcp(system)
        .bindAndHandle(tcpProxy, interface = "localhost", port = port)
        .map(s => Logger(getClass).info(s"Listening for $serviceName requests on ${s.localAddress}"))
        .recover {
          case e: Exception =>
            Logger(getClass).error(s"Could not start TCP proxy for $serviceName requests on $port because of $e")
        }

    Future
      .sequence(
        Seq(
          startProxy(appConfig.authPort, "auth"),
          startProxy(appConfig.citizenDetailsPort, "citizen-details"),
          startProxy(appConfig.userDetailsPort, "user-details"),
          startProxy(appConfig.usersGroupsSearchPort, "users-groups-search"),
          startProxy(appConfig.enrolmentStoreProxyPort, "enrolment-store-proxy"),
          startProxy(appConfig.taxEnrolmentsPort, "tax-enrolments"),
          startProxy(appConfig.niExemptionRegistrationPort, "ni-exemption-registration"),
          startProxy(appConfig.desPort, "des"),
          startProxy(appConfig.dataStreamPort, "datastream")
        ))
      .map(_ => Logger(getClass).info("All proxies have started."))

  } else {
    Logger(getClass).info("TCP proxying feature is switched off")
  }

}
