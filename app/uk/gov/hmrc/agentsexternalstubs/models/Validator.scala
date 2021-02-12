/*
 * Copyright 2021 HM Revenue & Customs
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
import cats.Semigroup
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object Validator {

  import Implicits._

  type Validator[T] = T => Validated[List[String], Unit]

  def apply[T](constraints: Validator[T]*): Validator[T] =
    (entity: T) =>
      constraints
        .foldLeft[Validated[List[String], Unit]](Valid(()))((v, fx) =>
          v.combine(fx(entity).leftMap(le => le.map(e => if (e.contains(" in ")) e else s"$e in $entity")))
        )

  def alternatively[T](constraints: Validator[T]*): Validator[T] =
    (entity: T) =>
      constraints
        .foldLeft[Validated[List[String], Unit]](Valid(()))((v, fx) => v.orElse(fx(entity)))

  def product[A, B](constraintA: Validator[A], constraintB: Validator[B]): Validator[(A, B)] =
    (entity: (A, B)) => constraintA(entity._1).combine(constraintB(entity._2))

  def product[A, B, C](
    constraintA: Validator[A],
    constraintB: Validator[B],
    constraintC: Validator[C]
  ): Validator[(A, B, C)] =
    (entity: (A, B, C)) => constraintA(entity._1).combine(constraintB(entity._2)).combine(constraintC(entity._3))

  private type SimpleValidator[T] = T => Validated[String, Unit]
  def validate[T](constraints: SimpleValidator[T]*): Validator[T] =
    (entity: T) =>
      constraints
        .foldLeft[Validated[List[String], Unit]](Valid(()))((v, fx) => v.combine(fx(entity).leftMap(_ :: Nil)))

  def check[T](test: T => Boolean, error: String): Validator[T] =
    (entity: T) => Validated.cond(test(entity), (), error :: Nil)

  def checkFromEither[T](test: T => Either[String, Any], error: String): Validator[T] =
    (entity: T) => Validated.fromEither(test(entity).map(_ => ()).left.map(_ :: Nil))

  def checkFromOption[T](test: T => Option[Any], error: String): Validator[T] =
    (entity: T) => Validated.fromOption(test(entity).map(_ => ()), "Some expected but got None" :: Nil)

  def checkObject[T, E](element: T => E, validator: Validator[E]): Validator[T] =
    (entity: T) => validator(element(entity))

  def checkProperty[T, E](element: T => E, validator: Validator[E]): Validator[T] =
    (entity: T) => validator(element(entity))

  def checkObjectIfSome[T, E](
    element: T => Option[E],
    validator: Validator[E],
    isValidIfNone: Boolean = true
  ): Validator[T] =
    (entity: T) =>
      element(entity)
        .map(validator)
        .getOrElse(if (isValidIfNone) Valid(()) else Invalid(List("Some value expected but got None")))

  def checkIfSome[T, E](element: T => Option[E], validator: Validator[E], isValidIfNone: Boolean = true): Validator[T] =
    (entity: T) =>
      element(entity)
        .map(validator)
        .getOrElse(if (isValidIfNone) Valid(()) else Invalid(List("Some value expected but got None")))

  def checkEach[T, E](elements: T => Seq[E], validator: Validator[E]): Validator[T] =
    (entity: T) => elements(entity).map(validator).reduce(_.combine(_))

  def checkEachIfSome[T, E](
    extract: T => Option[Seq[E]],
    validator: Validator[E],
    isValidIfNone: Boolean = true
  ): Validator[T] =
    (entity: T) =>
      extract(entity)
        .map(
          _.map(validator)
            .foldLeft[Validated[List[String], Unit]](Valid(()))((a, b) => a.combine(b))
        )
        .getOrElse(if (isValidIfNone) Valid(()) else Invalid(List("Some sequence expected but got None")))

  def checkIfAtLeastOneIsDefined[T](alternatives: Seq[T => Option[Any]], expectations: String): Validator[T] =
    (entity: T) =>
      if (alternatives.exists(f => f(entity).isDefined)) Valid(())
      else Invalid(List(s"One of an alternative values $expectations must be defined"))

  def checkIfOnlyOneSetIsDefined[T](alternatives: Seq[Set[T => Option[Any]]], expectations: String): Validator[T] =
    (entity: T) => {
      val definedSetCount = alternatives.map(_.map(f => f(entity).isDefined).reduce(_ && _)).count(_ == true)
      if (definedSetCount == 0)
        Invalid(List(s"At least one of an alternative property sets $expectations must be defined or true"))
      else if (definedSetCount > 1)
        Invalid(List(s"Only one of an alternative property sets $expectations can be defined or true"))
      else Valid(())
    }

  implicit class StringMatchers(val value: String) extends AnyVal {
    def lengthMinMaxInclusive(min: Int, max: Int): Boolean = value != null && value.length >= min && value.length <= max
    def lengthMin(min: Int): Boolean = value != null && value.length >= min
    def lengthMax(max: Int): Boolean = value != null && value.length <= max
    def isRight(test: String => Either[String, _]): Boolean = test(value).isRight
    def isTrue(test: String => Boolean): Boolean = test(value)
    def isOneOf(seq: Seq[String]): Boolean = seq.contains(value)
  }

  implicit class OptionalStringMatchers(val value: Option[String]) extends AnyVal {
    def lengthMinMaxInclusive(min: Int, max: Int): Boolean =
      value.forall(v => v != null && v.length >= min && v.length <= max)
    def lengthMin(min: Int): Boolean =
      value.forall(v => v != null && v.length >= min)
    def lengthMax(max: Int): Boolean =
      value.forall(v => v != null && v.length <= max)
    def isRight(test: String => Either[String, _]): Boolean = value.forall(test(_).isRight)
    def isTrue(test: String => Boolean): Boolean = value.forall(test(_))
    def matches(regex: String): Boolean = value.forall(_.matches(regex))
    def isOneOf(seq: Seq[String]): Boolean = value.forall(seq.contains)
    def isOneOf(set: Set[String]): Boolean = value.forall(set.apply)
  }

  implicit class BigDecimalMatchers(val value: BigDecimal) extends AnyVal {
    def inRange(min: BigDecimal, max: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value != null && value <= max && value >= min && multipleOf.forall(a => (value % a).abs < 0.0001)
    def lteq(max: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value != null && value <= max && multipleOf.forall(a => (value % a).abs < 0.0001)
    def gteq(min: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value != null && value >= min && multipleOf.forall(a => (value % a).abs < 0.0001)
  }

  implicit class OptionalBigDecimalMatchers(val value: Option[BigDecimal]) extends AnyVal {
    def inRange(min: BigDecimal, max: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value.forall(v => v != null && v <= max && v >= min && multipleOf.forall(a => (v % a).abs < 0.0001))
    def lteq(max: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value.forall(v => v != null && v <= max && multipleOf.forall(a => (v % a).abs < 0.0001))
    def gteq(min: BigDecimal, multipleOf: Option[BigDecimal] = None): Boolean =
      value.forall(v => v != null && v >= min && multipleOf.forall(a => (v % a).abs < 0.0001))
  }

  implicit class BooleanOps(val value: Boolean) extends AnyVal {
    def map[T](f: Unit => T): Option[T] = if (value) Some(f(())) else None
    def orElse(b: => Boolean): Boolean = value || b
    def asOption: Option[Unit] = if (value) Some(()) else None
    def isDefined: Boolean = value
  }

  object Implicits {

    implicit val listSemigroup: Semigroup[List[String]] = Semigroup.instance(_ ++ _)
    implicit val unitSemigroup: Semigroup[Unit] = Semigroup.instance((_, _) => ())
    implicit val stringSemigroup: Semigroup[String] = Semigroup.instance(_ + _)
  }
}
