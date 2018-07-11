package uk.gov.hmrc.agentsexternalstubs.binders

import uk.gov.hmrc.agentmtdidentifiers.model.Utr

object UrlBinders {
  implicit val utrBinder = new SimpleObjectBinder[Utr](Utr.apply, _.value)
}
