package uk.gov.hmrc.agentsexternalstubs.models
import cats.data.Validated.Valid
import cats.data.{NonEmptyList, Validated}
import cats.{Semigroup, SemigroupK}

object Validate {

  private implicit val nelS: Semigroup[NonEmptyList[String]] =
    SemigroupK[NonEmptyList].algebra[String]

  private implicit val unitS: Semigroup[Unit] = Semigroup.instance((_, _) => ())

  def apply[T](constraints: Seq[T => Validated[String, Unit]]): T => Validated[List[String], Unit] =
    (entity: T) =>
      constraints
        .foldLeft[Validated[NonEmptyList[String], Unit]](Valid(()))((v, fx) => v.combine(fx(entity).toValidatedNel))
        .leftMap(_.toList)

  def constraints[T](constraints: (T => Boolean, String)*): T => Validated[List[String], Unit] =
    Validate(constraints.map {
      case (test, error) =>
        (entity: T) =>
          Validated.cond(test(entity), (), error)
    })

}
