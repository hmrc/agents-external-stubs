package uk.gov.hmrc.agentsexternalstubs.repository
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

object MongoHelper {

  val interpretWriteResult: WriteResult => Future[Unit] = r =>
    if (!r.ok || r.code.isDefined || r.n == 0)
      Future.failed(WriteResult.lastError(r).getOrElse(new Exception("")))
    else Future.successful(())

}
