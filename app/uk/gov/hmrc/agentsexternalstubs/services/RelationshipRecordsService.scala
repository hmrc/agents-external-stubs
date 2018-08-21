package uk.gov.hmrc.agentsexternalstubs.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.agentsexternalstubs.models.RelationshipRecord
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RelationshipRecordsService @Inject()(recordsRepository: RecordsRepository) {

  private val MAX_DOCS = 1000

  def findByKey(key: String, planetId: String)(implicit ec: ExecutionContext): Future[List[RelationshipRecord]] =
    recordsRepository.cursor(key, planetId).collect[List](MAX_DOCS)

}
