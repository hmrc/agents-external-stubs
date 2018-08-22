package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.BusinessDetailsRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsRecordsService @Inject()(recordsRepository: RecordsRepository) {

  def findByKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[List[BusinessDetailsRecord]] =
    recordsRepository.cursor[BusinessDetailsRecord](key, planetId).collect[List](1000)

}
