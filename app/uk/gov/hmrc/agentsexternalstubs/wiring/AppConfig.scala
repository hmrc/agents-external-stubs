package uk.gov.hmrc.agentsexternalstubs.wiring
import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {

  val appName: String
  val syncUsersAllPlanets: Boolean

  // External services we stub
  val authUrl: String
  val citizenDetailsUrl: String
  val usersGroupsSearchUrl: String
  val enrolmentStoreProxyUrl: String
  val taxEnrolmentsUrl: String
  val desUrl: String
  val niExemptionRegistrationUrl: String

  // External services we connect to
  val authLoginApiUrl: String
  val agentAccessControlUrl: String
  val apiPlatformTestUserUrl: String

  // Proxies config
  val isProxyMode: Boolean
  val httpPort: Int

  val authPort: Int
  val userDetailsPort: Int
  val citizenDetailsPort: Int
  val usersGroupsSearchPort: Int
  val enrolmentStoreProxyPort: Int
  val taxEnrolmentsPort: Int
  val niExemptionRegistrationPort: Int
  val desPort: Int
  val dataStreamPort: Int

  val authCacheEnabled: Boolean

}

@Singleton
class AppConfigImpl @Inject()(config: ServicesConfig) extends AppConfig {

  val appName: String = config.getString("appName")
  val syncUsersAllPlanets: Boolean = config.getConfBool("api-platform-test-user.sync-users-all-planets", false)

  // External services we stub
  val authUrl: String = config.baseUrl("auth")
  val citizenDetailsUrl: String = config.baseUrl("citizen-details")
  val usersGroupsSearchUrl: String = config.baseUrl("users-groups-search")
  val enrolmentStoreProxyUrl: String = config.baseUrl("enrolment-store-proxy")
  val taxEnrolmentsUrl: String = config.baseUrl("tax-enrolments")
  val desUrl: String = config.baseUrl("des")
  val niExemptionRegistrationUrl: String = config.baseUrl("ni-exemption-registration")

  // External services we connect to
  val authLoginApiUrl: String = config.baseUrl("auth-login-api")
  val agentAccessControlUrl: String = config.baseUrl("agent-access-control")
  val apiPlatformTestUserUrl: String = config.baseUrl("api-platform-test-user")

  // Proxies config

  val isProxyMode: Boolean = config.getBoolean("proxies.start")
  val httpPort: Int = config.getInt("http.port")

  val authPort: Int = config.getConfInt("auth.port", 0)
  val userDetailsPort: Int = config.getConfInt("user-details.port", 0)
  val citizenDetailsPort: Int = config.getConfInt("citizen-details.port", 0)
  val usersGroupsSearchPort: Int = config.getConfInt("users-groups-search.port", 0)
  val enrolmentStoreProxyPort: Int = config.getConfInt("enrolment-store-proxy.port", 0)
  val taxEnrolmentsPort: Int = config.getConfInt("tax-enrolments.port", 0)
  val niExemptionRegistrationPort: Int = config.getConfInt("ni-exemption-registration.port", 0)
  val desPort: Int = config.getConfInt("des.port", 0)
  val dataStreamPort: Int = config.getInt("auditing.consumer.baseUri.port")

  val authCacheEnabled: Boolean = config.getBoolean("authCache.enabled")
}
