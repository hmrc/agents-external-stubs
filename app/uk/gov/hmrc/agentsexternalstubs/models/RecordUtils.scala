package uk.gov.hmrc.agentsexternalstubs.models
import com.github.blemale.scaffeine.Scaffeine
import org.scalacheck.Gen
import uk.gov.hmrc.agentsexternalstubs.models.Validator.Validator

trait RecordUtils[T] {

  type Update = String => T => T

  val gen: Gen[T]

  val validate: Validator[T]

  val sanitizers: Seq[Update]

  private val sanitizedRecordCache = Scaffeine().maximumSize(1000).build[String, T]()
  private val seededRecordCache = Scaffeine().maximumSize(1000).build[String, T]()

  final def seed(s: String): T =
    seededRecordCache
      .get(s, s => Generator.get(gen)(s).getOrElse(throw new Exception(s"Could not seed record with $s")))

  final def sanitize(s: String)(entity: T): T = sanitizers.foldLeft(entity)((u, fx) => fx(s)(u))

  final def generate(s: String): T = sanitizedRecordCache.get(s, s => sanitize(s)(seed(s)))

}
