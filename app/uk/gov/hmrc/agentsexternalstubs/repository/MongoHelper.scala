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

package uk.gov.hmrc.agentsexternalstubs.repository

import com.mongodb.client.result.UpdateResult
import org.mongodb.scala.result

import scala.concurrent.Future

object MongoHelper {

//  val interpretWriteResult: ((WriteResult, String)) => Future[String] = { case (r, id) =>
//    if (!r.ok || r.code.isDefined || r.n == 0)
//      Future.failed(WriteResult.lastError(r).getOrElse(new Exception("")))
//    else
//      Future.successful(id)
//  }
//
//  val interpretWriteResultUnit: WriteResult => Future[Unit] = { r =>
//    if (!r.ok || r.code.isDefined)
//      Future.failed(WriteResult.lastError(r).getOrElse(new Exception(r.writeErrors.mkString(","))))
//    else
//      Future.successful(())
//  }

  def interpretInsertOneResult: ((result.InsertOneResult, String)) => Future[String] = { case (r, id) =>
    // TODO! No idea how to replicate the 'interpret' logic with the newer Mongo library
    Future.successful(id)
  }

  def interpretUpdateResult: ((result.UpdateResult, String)) => Future[String] = { case (r, id) =>
    // TODO! No idea how to replicate the 'interpret' logic with the newer Mongo library
    Future.successful(id)
  }
  def interpretUpdateResultUnit(r: UpdateResult): Future[Unit] =
    // TODO! No idea how to replicate the 'interpret' logic with the newer Mongo library
    Future.successful(())
}
