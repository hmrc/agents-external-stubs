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

package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object GroupUsersValidator {

  /*
      Group and User constraints:
        - Group MUST have one and at most one Admin
        - Group MAY NOT consist of Assistants only
        - Group CAN have at most 100 users
   */

  type GroupConstraint = Seq[User] => Validated[String, Unit]

  val groupMustHaveOneAndAtMostOneAdmin: GroupConstraint = users =>
    if (users.isEmpty || users.count(_.isAdmin) == 1) Valid(())
    else Invalid("Group MUST have one and at most one Admin")

  val groupMayNotHaveOnlyAssistants: GroupConstraint = users =>
    if (users.isEmpty || !users.forall(_.isAssistant)) Valid(())
    else Invalid("Group MAY NOT consist of Assistants only")

  private val constraints: Seq[GroupConstraint] =
    Seq(
      groupMustHaveOneAndAtMostOneAdmin,
      groupMayNotHaveOnlyAssistants
    )

  val validate: Seq[User] => Validated[List[String], Unit] = Validator.validate(constraints: _*)

}
