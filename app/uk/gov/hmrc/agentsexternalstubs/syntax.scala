package uk.gov.hmrc.agentsexternalstubs

object syntax {

  implicit class |>[A](value: A) {
    def |>[B](fx: Function[A, B]): B = fx.apply(value)
  }

}
