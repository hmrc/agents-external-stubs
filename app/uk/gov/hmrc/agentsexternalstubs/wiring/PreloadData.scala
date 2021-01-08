/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessDetailsRecord, BusinessPartnerRecord, UserIdGenerator, VatCustomerInformationRecord}

import scala.concurrent.ExecutionContext

@Singleton
class PreloadData @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext) {

  if (appConfig.preloadRecordsForDefaultUserIds) {
    Logger(getClass).info("Pre-loading records for default user ids")
    UserIdGenerator.defaultUserIds.map(_.foreach(userId => {
      VatCustomerInformationRecord.generate(userId)
      BusinessDetailsRecord.generate(userId)
      BusinessPartnerRecord.generate(userId)
    }))

  }

}
