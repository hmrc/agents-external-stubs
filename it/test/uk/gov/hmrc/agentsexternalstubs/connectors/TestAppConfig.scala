/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.connectors
import uk.gov.hmrc.agentsexternalstubs.wiring.AppConfig

case class TestAppConfig(wireMockBaseUrl: String, wireMockPort: Int) extends AppConfig {

  override val appName: String = "agents-external-stubs"
  override val syncUsersAllPlanets: Boolean = true

  override val authUrl: String = wireMockBaseUrl
  override val citizenDetailsUrl: String = wireMockBaseUrl
  override val usersGroupsSearchUrl: String = wireMockBaseUrl
  override val enrolmentStoreProxyUrl: String = wireMockBaseUrl
  override val taxEnrolmentsUrl: String = wireMockBaseUrl
  override val desUrl: String = wireMockBaseUrl
  override val niExemptionRegistrationUrl: String = wireMockBaseUrl
  override val authLoginApiUrl: String = wireMockBaseUrl
  override val agentAccessControlUrl: String = wireMockBaseUrl
  override val apiPlatformTestUserUrl: String = wireMockBaseUrl
  override val agentPermissionsUrl: String = wireMockBaseUrl
  override val agentUserClientDetailsUrl: String = wireMockBaseUrl

  override val isProxyMode: Boolean = false
  override val httpPort: Int = wireMockPort

  override val authPort: Int = wireMockPort
  override val userDetailsPort: Int = wireMockPort
  override val citizenDetailsPort: Int = wireMockPort
  override val usersGroupsSearchPort: Int = wireMockPort
  override val enrolmentStoreProxyPort: Int = wireMockPort
  override val taxEnrolmentsPort: Int = wireMockPort
  override val niExemptionRegistrationPort: Int = wireMockPort
  override val desPort: Int = wireMockPort
  override val dataStreamPort: Int = wireMockPort
  override val ssoPort: Int = wireMockPort
  override val fileUploadFrontendPort: Int = wireMockPort
  override val fileUploadPort: Int = wireMockPort
  override val authCacheEnabled: Boolean = true
  override val specialCasesDisabled: Boolean = false
  override val specialCasesUseTruncatedRequestUriMatch: Boolean = false
  override val preloadRecordsForDefaultUserIds: Boolean = false
  override val clearOldMongoDbDocumentsDaily: Boolean = false

  override val identityVerification: Int = wireMockPort
  override val personalDetailsValidation: Int = wireMockPort
  override val companiesHouseApiProxyPort: Int = wireMockPort

  override val syncToAuthLoginApi: Boolean = false

  override val granPermsTestGenMaxClients: Int = 10
  override val granPermsTestGenMaxAgents: Int = 2
}
