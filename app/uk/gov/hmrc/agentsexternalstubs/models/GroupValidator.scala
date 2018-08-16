package uk.gov.hmrc.agentsexternalstubs.models

object GroupValidator {

  /*
      Group and User constraints:
        - Group MUST have one and at most one Admin
        - Group CAN have at most one Organisation
        - Group MAY NOT consist of Assistants only
        - Group CAN have at most 100 users
        - Organisation MUST be an Admin in the group
        - Agent MAY NOT be in the group with Organisation and Individuals
   */

  type GroupConstraint = Seq[User] => Either[String, Unit]

  val groupMustHaveOneAndAtMostOneAdmin: GroupConstraint = users =>
    if (users.isEmpty || users.count(_.isAdmin) == 1) Right(()) else Left("Group MUST have one and at most one Admin")

  val groupCanHaveAtMostOneOrganisation: GroupConstraint = users =>
    if (users.count(_.isOrganisation) <= 1) Right(()) else Left("Group CAN have at most one Organisation")

  val groupMayNotHaveOnlyAssistants: GroupConstraint = users =>
    if (users.isEmpty || !users.forall(_.isAssistant)) Right(()) else Left("Group MAY NOT consist of Assistants only")

  val groupCanHaveAtMost100Users: GroupConstraint = users =>
    if (users.size <= 100) Right(()) else Left("Group CAN have at most 100 users")

  val organisationMustBeAnAdminInTheGroup: GroupConstraint = users =>
    if (users.filter(_.isOrganisation).forall(_.isAdmin)) Right(())
    else Left("Organisation MUST be an Admin in the group")

  val agentMayNotBeInTheGroupWithOrganisationAndIndividuals: GroupConstraint = users =>
    if (users.isEmpty || (users.filter(_.affinityGroup.isDefined).partition(_.isAgent) match {
          case (a, na) => a.isEmpty || na.isEmpty
        })) Right(())
    else Left("Agents MAY NOT be in the group with Organisation and Individuals")

  val constraints: Seq[GroupConstraint] =
    Seq(
      groupMustHaveOneAndAtMostOneAdmin,
      groupCanHaveAtMostOneOrganisation,
      groupMayNotHaveOnlyAssistants,
      groupCanHaveAtMost100Users,
      organisationMustBeAnAdminInTheGroup,
      agentMayNotBeInTheGroupWithOrganisationAndIndividuals
    )

  def validate(users: Seq[User]): Either[String, Unit] =
    constraints.foldLeft[Either[String, Unit]](Right(()))((a, c) => a.right.flatMap(_ => c(users)))

}
