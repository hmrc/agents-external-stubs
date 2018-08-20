package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object GroupValidator {

  /*
      Group and User constraints:
        - Group MUST have one and at most one Admin
        - Group CAN have at most one Organisation
        - Group MAY NOT consist of Assistants only
        - Group CAN have at most 100 users
        - Organisation MUST be an Admin in the group
        - Agent MAY NOT be in the group with Organisation and Individuals
        - All Agents in the group MUST have the same AgentCode
   */

  type GroupConstraint = Seq[User] => Validated[String, Unit]

  val groupMustHaveOneAndAtMostOneAdmin: GroupConstraint = users =>
    if (users.isEmpty || users.count(_.isAdmin) == 1) Valid(())
    else Invalid("Group MUST have one and at most one Admin")

  val groupCanHaveAtMostOneOrganisation: GroupConstraint = users =>
    if (users.count(_.isOrganisation) <= 1) Valid(()) else Invalid("Group CAN have at most one Organisation")

  val groupMayNotHaveOnlyAssistants: GroupConstraint = users =>
    if (users.isEmpty || !users.forall(_.isAssistant)) Valid(())
    else Invalid("Group MAY NOT consist of Assistants only")

  val groupCanHaveAtMost100Users: GroupConstraint = users =>
    if (users.size <= 100) Valid(()) else Invalid("Group CAN have at most 100 users")

  val organisationMustBeAnAdminInTheGroup: GroupConstraint = users =>
    if (users.filter(_.isOrganisation).forall(_.isAdmin)) Valid(())
    else Invalid("Organisation MUST be an Admin in the group")

  val agentMayNotBeInTheGroupWithOrganisationAndIndividuals: GroupConstraint = users =>
    if (users.isEmpty || (users.filter(_.affinityGroup.isDefined).partition(_.isAgent) match {
          case (a, na) => a.isEmpty || na.isEmpty
        })) Valid(())
    else Invalid("Agents MAY NOT be in the group with Organisation and Individuals")

  val allAgentsInTheGroupMustShareTheSameAgentCode: GroupConstraint = users =>
    if (users.filter(_.isAgent).map(_.agentCode).distinct.size <= 1) Valid(())
    else Invalid("All Agents in the group MUST share the same AgentCode")

  private val constraints: Seq[GroupConstraint] =
    Seq(
      groupMustHaveOneAndAtMostOneAdmin,
      groupCanHaveAtMostOneOrganisation,
      groupMayNotHaveOnlyAssistants,
      groupCanHaveAtMost100Users,
      organisationMustBeAnAdminInTheGroup,
      agentMayNotBeInTheGroupWithOrganisationAndIndividuals,
      allAgentsInTheGroupMustShareTheSameAgentCode
    )

  val validate: Seq[User] => Validated[List[String], Unit] = Validate(constraints)

}
