package uk.gov.hmrc.agentsexternalstubs.wiring

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agentsexternalstubs.models.{BusinessDetailsRecord, BusinessPartnerRecord, UserIdGenerator, VatCustomerInformationRecord}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PreloadData @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext) {

  if (appConfig.preloadRecordsForDefaultUserIds) Future {
    Logger(getClass).info("Pre-loading records for default user ids")
    UserIdGenerator.defaultUserIds.foreach(userId => {
      VatCustomerInformationRecord.generate(userId)
      BusinessDetailsRecord.generate(userId)
      BusinessPartnerRecord.generate(userId)
    })

  }

}
