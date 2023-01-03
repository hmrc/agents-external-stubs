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
  val ssoPort: Int
  val fileUploadPort: Int
  val fileUploadFrontendPort: Int
  val identityVerification: Int
  val personalDetailsValidation: Int
  val companiesHouseApiProxyPort: Int

  val authCacheEnabled: Boolean
  val specialCasesDisabled: Boolean
  val specialCasesUseTruncatedRequestUriMatch: Boolean
  val preloadRecordsForDefaultUserIds: Boolean
  val clearOldMongoDbDocumentsDaily: Boolean

  val syncToAuthLoginApi: Boolean

  val granPermsTestGenMaxClients: Int
  val granPermsTestGenMaxAgents: Int
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig) extends AppConfig {

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
  val ssoPort: Int = config.getConfInt("sso.port", 0)
  val fileUploadPort: Int = config.getConfInt("file-upload.port", 0)
  val fileUploadFrontendPort: Int = config.getConfInt("file-upload-frontend.port", 0)
  val identityVerification: Int = config.getConfInt("identity-verification.port", 0)
  val personalDetailsValidation: Int = config.getConfInt("personal-details-validation.port", 0)
  override lazy val companiesHouseApiProxyPort: Int = config.getConfInt("companies-house-api-proxy.port", 0)

  val isProxyMode: Boolean = config.getBoolean("features.proxies")
  val authCacheEnabled: Boolean = config.getBoolean("features.authCache")
  val specialCasesDisabled: Boolean = !config.getBoolean("features.specialCases.enabled")
  val specialCasesUseTruncatedRequestUriMatch: Boolean =
    config.getBoolean("features.specialCases.truncate-request-uri-match")
  val preloadRecordsForDefaultUserIds: Boolean = config.getBoolean("features.preloadRecordsForDefaultUserIds")
  val clearOldMongoDbDocumentsDaily: Boolean = config.getBoolean("features.clearOldMongoDbDocumentsDaily")

  val syncToAuthLoginApi: Boolean = config.getBoolean("features.syncToAuthLoginApi")

  val granPermsTestGenMaxClients: Int = config.getInt("gran-perms-test-gen-max-clients")
  val granPermsTestGenMaxAgents: Int = config.getInt("gran-perms-test-gen-max-agents")
}
