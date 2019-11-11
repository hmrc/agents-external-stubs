package uk.gov.hmrc.agentsexternalstubs.binders

import play.api.mvc.PathBindable
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentsexternalstubs.models.EnrolmentKey

object UrlBinders {

  implicit val enrolmentKeyBinder: PathBindable[EnrolmentKey] = new PathBindable[EnrolmentKey] {
    override def bind(key: String, value: String): Either[String, EnrolmentKey] = EnrolmentKey.parse(value)
    override def unbind(key: String, value: EnrolmentKey): String = value.toString
  }
  implicit object ArnBinder extends SimpleObjectBinder[Arn](Arn.apply, _.value)
}
