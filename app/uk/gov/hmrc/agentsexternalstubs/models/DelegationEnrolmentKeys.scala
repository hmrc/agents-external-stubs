/*
 * Copyright 2024 HM Revenue & Customs
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

sealed trait DelegationEnrolmentKeys {
  def primaryEnrolmentKey: EnrolmentKey
  def delegatedEnrolmentKey: EnrolmentKey
  def delegationEnrolments: Seq[EnrolmentKey]
  def isPrimary: Boolean
}

object DelegationEnrolmentKeys {
  def apply(enrolmentKey: EnrolmentKey): DelegationEnrolmentKeys =
    enrolmentKey.service match {
      case "HMRC-MTD-IT-SUPP" => Secondary(EnrolmentKey("HMRC-MTD-IT", enrolmentKey.identifiers), enrolmentKey)
      case _                  => Primary(enrolmentKey)
    }

  final case class Primary(primaryEnrolmentKey: EnrolmentKey) extends DelegationEnrolmentKeys {
    override def isPrimary: Boolean = true
    override def delegatedEnrolmentKey: EnrolmentKey = primaryEnrolmentKey
    override def delegationEnrolments: Seq[EnrolmentKey] = Seq(primaryEnrolmentKey)

  }
  final case class Secondary(primaryEnrolmentKey: EnrolmentKey, secondaryEnrolmentKey: EnrolmentKey)
      extends DelegationEnrolmentKeys {
    override def isPrimary: Boolean = false
    override def delegatedEnrolmentKey: EnrolmentKey = secondaryEnrolmentKey
    override def delegationEnrolments: Seq[EnrolmentKey] = Seq(primaryEnrolmentKey, secondaryEnrolmentKey)
  }

}
