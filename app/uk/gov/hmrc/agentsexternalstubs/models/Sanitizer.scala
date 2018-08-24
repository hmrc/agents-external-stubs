package uk.gov.hmrc.agentsexternalstubs.models

trait Sanitizer[T] {

  type Update = T => T

  val sanitizers: Seq[Update]

  def seed(s: String): T

  final def sanitize(entity: T): T = sanitizers.foldLeft(entity)((u, fx) => fx(u))

  final def generate(s: String): T = sanitize(seed(s))
}
