/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentsexternalstubs.models.identifiers

import play.api.libs.json.Format
import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}

sealed abstract class Service(
  val id: String,
  val invitationIdPrefix: Char,
  val enrolmentKey: String,
  val supportedSuppliedClientIdType: ClientIdType[_ <: TaxIdentifier],
  val supportedClientIdType: ClientIdType[_ <: TaxIdentifier]
) {

  override def toString: String = this.id

  override def equals(that: Any): Boolean =
    that match {
      case that: Service => this.id.equals(that.id)
      case _             => false
    }

}

object Service {

  val HMRCMTDIT = "HMRC-MTD-IT"
  val HMRCPIR = "PERSONAL-INCOME-RECORD"
  val HMRCMTDVAT = "HMRC-MTD-VAT"
  val HMRCTERSORG = "HMRC-TERS-ORG"
  val HMRCTERSNTORG = "HMRC-TERSNT-ORG"
  val HMRCCGTPD = "HMRC-CGT-PD"
  val HMRCPPTORG = "HMRC-PPT-ORG"
  val HMRCCBCORG = "HMRC-CBC-ORG"
  val HMRCCBCNONUKORG = "HMRC-CBC-NONUK-ORG"
  val HMRCPILLAR2ORG = "HMRC-PILLAR2-ORG"
  val HMRCMTDITSUPP = "HMRC-MTD-IT-SUPP"

  case object MtdIt
      extends Service(
        "HMRC-MTD-IT",
        'A',
        "HMRC-MTD-IT",
        NinoType,
        MtdItIdType
      )

  case object PersonalIncomeRecord
      extends Service(
        "PERSONAL-INCOME-RECORD",
        'B',
        "HMRC-NI",
        NinoType,
        NinoType
      )

  case object Vat
      extends Service(
        "HMRC-MTD-VAT",
        'C',
        "HMRC-MTD-VAT",
        VrnType,
        VrnType
      )

  case object Trust
      extends Service(
        "HMRC-TERS-ORG",
        'D',
        "HMRC-TERS-ORG",
        UtrType,
        UtrType
      )

  case object TrustNT
      extends Service(
        "HMRC-TERSNT-ORG",
        'F',
        "HMRC-TERSNT-ORG",
        UrnType,
        UrnType
      )

  case object CapitalGains
      extends Service(
        "HMRC-CGT-PD",
        'E',
        "HMRC-CGT-PD",
        CgtRefType,
        CgtRefType
      )

  case object Ppt
      extends Service(
        "HMRC-PPT-ORG",
        'G',
        "HMRC-PPT-ORG",
        PptRefType,
        PptRefType
      )

  case object Cbc
      extends Service(
        "HMRC-CBC-ORG",
        'H',
        "HMRC-CBC-ORG",
        CbcIdType,
        CbcIdType
      )

  case object CbcNonUk
      extends Service(
        "HMRC-CBC-NONUK-ORG",
        'J',
        "HMRC-CBC-NONUK-ORG",
        CbcIdType,
        CbcIdType
      )

  case object Pillar2
      extends Service(
        "HMRC-PILLAR2-ORG",
        'K',
        "HMRC-PILLAR2-ORG",
        PlrIdType,
        PlrIdType
      )

  case object MtdItSupp
      extends Service(
        "HMRC-MTD-IT-SUPP",
        'L',
        "HMRC-MTD-IT-SUPP",
        NinoType,
        MtdItIdType
      )

  val supportedServices: Seq[Service] = Seq(
    MtdIt,
    Vat,
    PersonalIncomeRecord,
    Trust,
    TrustNT,
    CapitalGains,
    Ppt,
    Cbc,
    CbcNonUk,
    Pillar2,
    MtdItSupp
  )

  def findById(id: String): Option[Service] = supportedServices.find(_.id == id)

  def forId(id: String): Service = findById(id).getOrElse(throw new Exception("Not a valid service id: " + id))

  def apply(id: String): Service = forId(id)
  def unapply(service: Service): Option[String] = Some(service.id)

  val reads = new SimpleObjectReads[Service]("id", Service.apply)
  val writes = new SimpleObjectWrites[Service](_.id)
  implicit val format: Format[Service] = Format(reads, writes)

}

sealed abstract class ClientIdType[+T <: TaxIdentifier](
  val clazz: Class[_],
  val id: String,
  val enrolmentId: String,
  val createUnderlying: String => T
) {
  def isValid(value: String): Boolean
}

object ClientIdType {

  val supportedTypes = Seq(
    NinoType,
    MtdItIdType,
    VrnType,
    UtrType,
    UrnType,
    CgtRefType,
    PptRefType,
    CbcIdType,
    PlrIdType
  )
  def forId(id: String) =
    supportedTypes.find(_.id == id).getOrElse(throw new IllegalArgumentException("Invalid id:" + id))

}

case object NinoType
    extends ClientIdType(
      classOf[NinoWithoutSuffix],
      "ni",
      "NINO",
      NinoWithoutSuffix.apply
    ) {
  override def isValid(value: String): Boolean = NinoWithoutSuffix.isValid(value)
}

case object MtdItIdType
    extends ClientIdType(
      classOf[MtdItId],
      "MTDITID",
      "MTDITID",
      MtdItId.apply
    ) {
  override def isValid(value: String): Boolean = MtdItId.isValid(value)
}

case object VrnType
    extends ClientIdType(
      classOf[Vrn],
      "vrn",
      "VRN",
      Vrn.apply
    ) {
  override def isValid(value: String): Boolean = Vrn.isValid(value)
}

case object UtrType
    extends ClientIdType(
      classOf[Utr],
      "utr",
      "SAUTR",
      Utr.apply
    ) {
  override def isValid(value: String): Boolean = value.matches("^\\d{10}$")
}

case object UrnType
    extends ClientIdType(
      classOf[Urn],
      "urn",
      "URN",
      Urn.apply
    ) {
  override def isValid(value: String): Boolean = value.matches("^([A-Z0-9]{1,15})$")
}

case object CgtRefType
    extends ClientIdType(
      classOf[CgtRef],
      "CGTPDRef",
      "CGTPDRef",
      CgtRef.apply
    ) {
  override def isValid(value: String): Boolean = CgtRef.isValid(value)
}

case object PptRefType
    extends ClientIdType(
      classOf[PptRef],
      "EtmpRegistrationNumber",
      "EtmpRegistrationNumber",
      PptRef.apply
    ) {
  override def isValid(value: String): Boolean = PptRef.isValid(value)
}

case object CbcIdType
    extends ClientIdType(
      classOf[CbcId],
      "cbcId",
      "cbcId",
      CbcId.apply
    ) {
  override def isValid(value: String): Boolean = CbcId.isValid(value)
}

case object PlrIdType
    extends ClientIdType(
      classOf[PlrId],
      "PLRID",
      "PLRID",
      PlrId.apply
    ) {
  override def isValid(value: String): Boolean = PlrId.isValid(value)
}

case class ClientIdentifier[T <: TaxIdentifier](underlying: T) {

  private val clientIdType = ClientIdType.supportedTypes
    .find(_.clazz == underlying.getClass)
    .getOrElse(throw new Exception("Invalid type for clientId " + underlying.getClass.getCanonicalName))

  val value: String = underlying.value
  val typeId: String = clientIdType.id
  val enrolmentId: String = clientIdType.enrolmentId

  override def toString: String = value

}

object ClientIdentifier {

  type ClientId = ClientIdentifier[_ <: TaxIdentifier]

  def apply(
    value: String,
    typeId: String
  ): ClientId = ClientIdType.supportedTypes
    .find(_.id == typeId)
    .getOrElse(throw new IllegalArgumentException("Invalid Client Id Type: " + typeId))
    .createUnderlying(value.replaceAll("\\s", ""))

  implicit def wrap[T <: TaxIdentifier](taxId: T): ClientIdentifier[T] = ClientIdentifier(taxId)

}
