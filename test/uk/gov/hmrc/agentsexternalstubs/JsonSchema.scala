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

package uk.gov.hmrc.agentsexternalstubs
import play.api.libs.json.{JsArray, JsLookup, JsObject}

/** You might want to add some custom properties to the json schema in order to enable
  * custom record features, i.e. index keys and custom generators.
  *
  * "x_uniqueKey": true - this will mark string property as a unique key
  * "x_key": true - this will mark string property as a lookup key
  * "x_gen": "xxx" - this will set a custom generator reference for the property
  */
object JsonSchema {

  def read(schema: JsObject): Definition = readProperty("", "#", schema, schema, required = Seq(""))

  sealed trait Definition {

    val name: String
    val description: Option[String]

    val path: String
    val isRef: Boolean

    def isMandatory: Boolean
    def shallBeValidated: Boolean

    val isPrimitive: Boolean = true
    val isBoolean: Boolean = false

    final def typeName: String = {
      val n = if (isRef) pathToName(path) else name
      if (n.isEmpty) name else n.substring(0, 1).toUpperCase + n.substring(1)
    }

    final def nameWithFirstCharUpper: String =
      normalizedName(if (name.isEmpty) name else name.substring(0, 1).toUpperCase + name.substring(1))

    final def normalizedName(name: String): String = name.replace("-", "_")

    final def variableName: String =
      normalizedName(if (name.charAt(0).isUpper) name.toLowerCase else name)

    final def pathLastPart: String = path.split("/").last

    private final def pathToName(path: String): String = {
      val name = pathLastPart
      if (name.endsWith("Type")) name.dropRight(4) else name
    }
  }

  case class ObjectDefinition(
    name: String,
    path: String,
    properties: Seq[Definition],
    required: Seq[String],
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean,
    alternatives: Seq[Set[String]] = Seq.empty
  ) extends Definition {
    override val isPrimitive: Boolean = false
    override def shallBeValidated: Boolean = true
  }

  case class OneOfDefinition(
    name: String,
    path: String,
    variants: Seq[Definition],
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean,
    alternatives: Seq[Set[String]] = Seq.empty
  ) extends Definition {
    override def shallBeValidated: Boolean = variants.nonEmpty
  }

  case class StringDefinition(
    name: String,
    path: String,
    isRef: Boolean = false,
    description: Option[String] = None,
    pattern: Option[String] = None,
    enum: Option[Seq[String]] = None,
    minLength: Option[Int] = None,
    maxLength: Option[Int] = None,
    isUniqueKey: Boolean = false,
    isKey: Boolean = false,
    customGenerator: Option[String] = None,
    isMandatory: Boolean
  ) extends Definition {
    override def shallBeValidated: Boolean =
      pattern.isDefined || enum.isDefined || minLength.isDefined || maxLength.isDefined
  }

  case class NumberDefinition(
    name: String,
    path: String,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean,
    customGenerator: Option[String] = None,
    minimum: Option[BigDecimal] = None,
    maximum: Option[BigDecimal] = None,
    multipleOf: Option[BigDecimal] = None
  ) extends Definition {
    override def shallBeValidated: Boolean = maximum.isDefined || minimum.isDefined || multipleOf.isDefined
  }

  case class BooleanDefinition(
    name: String,
    path: String,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean
  ) extends Definition {
    override val isBoolean: Boolean = true
    override def shallBeValidated: Boolean = false
  }

  case class ArrayDefinition(
    name: String,
    path: String,
    item: Definition,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean,
    minItems: Option[Int] = None,
    maxItems: Option[Int] = None
  ) extends Definition {
    override val isPrimitive: Boolean = false
    override def shallBeValidated: Boolean = item.shallBeValidated || minItems.isDefined || maxItems.isDefined
  }

  private def readProperty(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean = false,
    description: Option[String] = None,
    required: Seq[String]
  ): Definition = {

    val desc = description.orElse((property \ "description").asOpt[String])

    def read(valueType: String, isMandatory: Boolean): Definition = valueType match {
      case "object"  => readObject(name, path, property, schema, isRef, desc, isMandatory)
      case "string"  => readString(name, path, property, schema, isRef, desc, isMandatory)
      case "number"  => readNumber(name, path, property, schema, isRef, desc, isMandatory)
      case "boolean" => BooleanDefinition(name, path, isRef, desc, isMandatory = true)
      case "array"   => readArray(name, path, property, schema, isRef, desc, isMandatory)
    }

    (property \ "type").asOpt[String] match {
      case Some(valueType) =>
        val isMandatory = required.contains(name)
        read(valueType, isMandatory)
      case None =>
        (property \ "type").asOpt[Seq[String]] match {
          case Some(types) =>
            val types1 = types.filterNot(_ == "null").toSet
            val isMandatory = !types.contains("null") && required.contains(name)
            if (types1.size == 1) {
              read(types1.head, isMandatory)
            } else {
              val variants = types1.map(read(_, isMandatory)).toSeq
              OneOfDefinition(name, path, variants, isRef, description, isMandatory)
            }
          case None =>
            (property \ "$ref").asOpt[String] match {
              case Some(ref) =>
                if (ref.startsWith("#/")) {
                  val jsonLookup = ref.substring(2).split("/").foldLeft[JsLookup](schema)((s, p) => s \ p)
                  val r = readProperty(
                    name,
                    ref,
                    jsonLookup.result.as[JsObject],
                    schema,
                    isRef = true,
                    description = desc,
                    required
                  )
                  r
                } else throw new IllegalStateException(s"Reference format not supported, must start with #/: $ref")
              case None =>
                readOneOf(
                  name,
                  path,
                  property,
                  schema,
                  isRef,
                  description,
                  required.contains(name),
                  required,
                  Seq.empty
                )
            }
        }
    }
  }

  private def readString(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean
  ): Definition = {

    val pattern = (property \ "pattern").asOpt[String]
    val enum = (property \ "enum").asOpt[Seq[String]]
    val minLength = (property \ "minLength").asOpt[Int]
    val maxLength = (property \ "maxLength").asOpt[Int]
    val isUniqueKey = (property \ "x_uniqueKey").asOpt[Boolean].getOrElse(false)
    val isKey = (property \ "x_key").asOpt[Boolean].getOrElse(false)
    val customGenerator = (property \ "x_gen").asOpt[String]

    StringDefinition(
      name,
      path,
      isRef,
      description = description,
      pattern = pattern,
      enum = enum,
      minLength = minLength,
      maxLength = maxLength,
      isUniqueKey = isUniqueKey,
      isKey = isKey,
      customGenerator = customGenerator,
      isMandatory = isMandatory
    )
  }

  private def readNumber(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean
  ): Definition = {

    val minimum = (property \ "minimum").asOpt[BigDecimal]
    val maximum = (property \ "maximum").asOpt[BigDecimal]
    val multipleOf = (property \ "multipleOf").asOpt[BigDecimal]
    val customGenerator = (property \ "x_gen").asOpt[String]

    NumberDefinition(
      name,
      path,
      isRef,
      description = description,
      customGenerator = customGenerator,
      isMandatory = isMandatory,
      minimum = minimum,
      maximum = maximum,
      multipleOf = multipleOf
    )
  }

  private def readObject(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean
  ): Definition = {

    val (required, alternatives) = readRequiredProperty(property)
    (property \ "properties").asOpt[JsObject] match {
      case Some(properties) =>
        val props = properties.fields
          .map(_._1)
          .distinct
          .map(p =>
            readProperty(p, s"$path/$p", (property \ "properties" \ p).as[JsObject], schema, required = required)
          )
        ObjectDefinition(
          name,
          path,
          properties = props,
          required,
          isRef = isRef,
          description = description,
          isMandatory,
          alternatives = alternatives
        )
      case None =>
        readOneOf(name, path, property, schema, isRef, description, isMandatory, required, alternatives)
    }
  }

  private def readRequiredProperty(property: JsObject): (Seq[String], Seq[Set[String]]) =
    (property \ "required")
      .asOpt[Seq[String]]
      .map(s => (s, Seq.empty))
      .orElse(
        (property \ "oneOf")
          .asOpt[Seq[JsObject]]
          .map { s =>
            val names = s.map(o => (o \ "required").asOpt[Set[String]].getOrElse(Set.empty))
            val required = names.reduce(_ intersect _)
            val variants = names.map(_ -- required)
            (required.toSeq, variants)
          }
      )
      .getOrElse((Seq.empty, Seq.empty))

  private def readOneOf(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean,
    required: Seq[String],
    alternatives: Seq[Set[String]]
  ): Definition = (property \ "oneOf").asOpt[JsArray] match {
    case Some(array) =>
      val props = array.value.map(p => readProperty(name, path, p.as[JsObject], schema, required = required))
      OneOfDefinition(name, path, variants = props, isRef = isRef, description = description, isMandatory, alternatives)
    case None =>
      throw new IllegalStateException(s"Unsupported object definition, `properties` or `oneOf` expected in $property.")
  }

  private def readArray(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean
  ): Definition = {

    val items = (property \ "items").as[JsObject]
    val minItems = (property \ "minItems").asOpt[Int]
    val maxItems = (property \ "maxItems").asOpt[Int]
    val itemName =
      if (name.endsWith("Array")) name.substring(0, name.length - 5)
      else if (name.endsWith("List")) name.substring(0, name.length - 4)
      else name
    val itemDefinition = readProperty(itemName, path, items, schema, required = Seq(name))
    ArrayDefinition(
      name,
      path,
      itemDefinition,
      isRef = isRef,
      description = description,
      isMandatory = isMandatory,
      minItems = minItems,
      maxItems = maxItems
    )
  }
}
