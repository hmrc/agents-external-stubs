package uk.gov.hmrc.agentsexternalstubs

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
case class TcpProxiesConfig @Inject()(
  @Named("proxies.start") startProxies: String,
  @Named("auth.port") authPort: Int,
  @Named("citizen-details.port") citizenDetailsPort: Int,
  @Named("users-groups-search.port") usersGroupsSearchPort: Int,
  @Named("enrolment-store-proxy.port") enrolmentStoreProxyPort: Int,
  @Named("tax-enrolments.port") taxEnrolmentsPort: Int)

@Singleton
class TcpProxies @Inject()(tcpProxiesConfig: TcpProxiesConfig, @Named("http.port") httpPort: String)(
  implicit system: ActorSystem,
  materializer: Materializer) {

  if (tcpProxiesConfig.startProxies == "true") {
    Logger(getClass).info("Starting local TCP proxies ...")

    implicit val ec: ExecutionContext = system.dispatcher

    val agentsExternalStubsPort = Try(httpPort.toInt).toOption.getOrElse(9009)

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

    startProxy(tcpProxiesConfig.authPort, "auth")
    startProxy(tcpProxiesConfig.citizenDetailsPort, "citizen-details")
    startProxy(tcpProxiesConfig.usersGroupsSearchPort, "users-groups-search")
    startProxy(tcpProxiesConfig.enrolmentStoreProxyPort, "enrolment-store-proxy")

  } else {
    Logger(getClass).info("TCP proxying feature is switched off")
  }

}
