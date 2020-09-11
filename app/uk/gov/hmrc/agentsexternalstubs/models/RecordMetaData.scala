/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models

import scala.reflect.ClassTag

trait RecordMetaData[T <: Record] {

  val typeName: String
  val fieldNames: Seq[String]
  val utils: RecordUtils[T]
}

object RecordMetaData {

  def apply[T <: Record](utilities: RecordUtils[T])(implicit classTag: ClassTag[T]): RecordMetaData[T] = {

    val properties =
      classTag.runtimeClass.getDeclaredFields
        .map(_.getName.replace("$minus", "-"))
        .toSet
        .-("id")
        .+(Record.ID)
        .+(Record.TYPE)
        .toSeq

    new RecordMetaData[T] {
      override val typeName: String = classTag.runtimeClass.getSimpleName
      override val fieldNames: Seq[String] = properties
      override val utils: RecordUtils[T] = utilities
    }
  }
}
