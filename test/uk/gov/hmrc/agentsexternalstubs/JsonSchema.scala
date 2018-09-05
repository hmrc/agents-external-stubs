package uk.gov.hmrc.agentsexternalstubs
import play.api.libs.json.{JsArray, JsLookup, JsObject}

/**
  * You might want to add some custom properties to the json schema in order to enable
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
    def isPrimitive: Boolean = true

    final def typeName: String = {
      val n = if (isRef) pathToName(path) else name
      if (n.isEmpty) name else n.substring(0, 1).toUpperCase + n.substring(1)
    }

    final def nameWithFirstCharUpper: String =
      if (name.isEmpty) name else name.substring(0, 1).toUpperCase + name.substring(1)

    final def variableName: String =
      if (name.charAt(0).isUpper) name.toLowerCase else name

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
    alternatives: Seq[String] = Seq.empty)
      extends Definition { override def isPrimitive: Boolean = false }

  case class OneOfDefinition(
    name: String,
    path: String,
    variants: Seq[Definition],
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean,
    alternatives: Seq[String] = Seq.empty)
      extends Definition

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
  ) extends Definition

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
  ) extends Definition

  case class BooleanDefinition(
    name: String,
    path: String,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean)
      extends Definition

  case class ArrayDefinition(
    name: String,
    path: String,
    item: Definition,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean)
      extends Definition { override def isPrimitive: Boolean = false }

  private def readProperty(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean = false,
    description: Option[String] = None,
    required: Seq[String]): Definition = {
    val desc = description.orElse((property \ "description").asOpt[String])
    (property \ "type").asOpt[String] match {
      case Some(valueType) =>
        val isMandatory = required.contains(name)
        valueType match {
          case "object"  => readObject(name, path, property, schema, isRef, desc, isMandatory)
          case "string"  => readString(name, path, property, schema, isRef, desc, isMandatory)
          case "number"  => readNumber(name, path, property, schema, isRef, desc, isMandatory)
          case "boolean" => BooleanDefinition(name, path, isRef, desc, isMandatory)
          case "array"   => readArray(name, path, property, schema, isRef, desc, isMandatory)
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
                required)
              r
            } else throw new IllegalStateException(s"Reference format not supported, must start with #/: $ref")
          case None =>
            readOneOf(name, path, property, schema, isRef, description, required.contains(name), required, Seq.empty)
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
    isMandatory: Boolean): Definition = {

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
    isMandatory: Boolean): Definition = {

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
    isMandatory: Boolean): Definition = {

    val (required, alternatives) = readRequiredProperty(property)
    (property \ "properties").asOpt[JsObject] match {
      case Some(properties) =>
        val props = properties.fields
          .map(_._1)
          .distinct
          .map(p =>
            readProperty(p, s"$path/$p", (property \ "properties" \ p).as[JsObject], schema, required = required))
        ObjectDefinition(
          name,
          path,
          properties = props,
          required,
          isRef = isRef,
          description = description,
          isMandatory,
          alternatives = alternatives)
      case None =>
        readOneOf(name, path, property, schema, isRef, description, isMandatory, required, alternatives)
    }
  }

  private def readRequiredProperty(property: JsObject): (Seq[String], Seq[String]) =
    (property \ "required")
      .asOpt[Seq[String]]
      .map(s => (s, Seq.empty))
      .orElse(
        (property \ "oneOf")
          .asOpt[Seq[JsObject]]
          .map(s => {
            val names = s.map(o => (o \ "required").asOpt[Set[String]].getOrElse(Set.empty))
            val intersection = names.reduce(_ intersect _)
            val sum = names.reduce(_ ++ _)
            (intersection.toSeq, (sum -- intersection).toSeq)
          })
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
    alternatives: Seq[String]): Definition = (property \ "oneOf").asOpt[JsArray] match {
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
    isMandatory: Boolean): Definition = {

    val items = (property \ "items").as[JsObject]
    val itemDefinition = readProperty(name, path, items, schema, required = Seq(name))
    ArrayDefinition(name, path, itemDefinition, isRef = isRef, description = description, isMandatory = isMandatory)
  }
}
