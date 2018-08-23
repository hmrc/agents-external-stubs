package uk.gov.hmrc.agentsexternalstubs

object syntax {

  implicit class |>[A](val value: A) extends AnyVal {
    def |>[B](fx: Function[A, B]): B = fx.apply(value)
  }

}
