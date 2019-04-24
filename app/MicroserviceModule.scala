import com.google.inject.AbstractModule
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.agentsexternalstubs.TcpProxies
import uk.gov.hmrc.agentsexternalstubs.wiring.{ClearDatabase, PreloadData}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

class MicroserviceModule(environment: Environment, configuration: Configuration) extends AbstractModule {

  def configure(): Unit = {
    Logger(getClass).info(s"Starting microservice agents-external-stubs in mode : ${environment.mode}")

    bind(classOf[HttpGet]).to(classOf[DefaultHttpClient])
    bind(classOf[HttpPost]).to(classOf[DefaultHttpClient])

    bind(classOf[TcpProxies]).asEagerSingleton()
    bind(classOf[PreloadData]).asEagerSingleton()
    bind(classOf[ClearDatabase]).asEagerSingleton()
  }
}
