package uk.gov.hmrc.agentsexternalstubs

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Tcp}
import akka.util.ByteString
import javax.inject.{Inject, Named, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TcpProxies @Inject()(
  @Named("proxies.start") startProxies: String,
  @Named("auth.port") authPort: Int,
  @Named("citizen-details.port") citizenDetailsPort: Int,
  @Named("http.port") httpPort: String)(implicit system: ActorSystem, materializer: Materializer) {

  if (startProxies == "true") {
    println("Starting local TCP proxies ...")

    implicit val ec: ExecutionContext = system.dispatcher

    val agentsExternalStubsPort = Try(httpPort.toInt).toOption.getOrElse(9009)

    val tcpOutgoingConnection: Flow[ByteString, ByteString, Future[Tcp.OutgoingConnection]] =
      Tcp().outgoingConnection("localhost", agentsExternalStubsPort)

    val tcpProxy = Flow[ByteString].via(tcpOutgoingConnection)

    startProxy(authPort, "auth")
    startProxy(citizenDetailsPort, "citizen-details")

    def startProxy(port: Int, serviceName: String): Future[Unit] =
      Tcp(system)
        .bindAndHandle(tcpProxy, interface = "localhost", port = port)
        .map(s => println(s"Listening for $serviceName requests on ${s.localAddress}"))
        .recover {
          case e: Exception =>
            println(s"Could not start tcp proxy for $serviceName requests on $port because of $e")
        }

  } else {
    println("TCP proxies feature is switched off")
  }

}
