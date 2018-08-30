package uk.gov.hmrc.agentsexternalstubs.services
import play.api.libs.json.Reads
import uk.gov.hmrc.agentsexternalstubs.models.{Record, RecordMetaData}
import uk.gov.hmrc.agentsexternalstubs.repository.RecordsRepository
import uk.gov.hmrc.http.BadRequestException

import scala.concurrent.{ExecutionContext, Future}

trait RecordsService {

  def recordsRepository: RecordsRepository

  protected def findByKey[T <: Record](
    key: String,
    planetId: String)(implicit ec: ExecutionContext, recordType: RecordMetaData[T], reads: Reads[T]): Future[List[T]] =
    recordsRepository.cursor[T](key, planetId).collect[List](1000)
}
