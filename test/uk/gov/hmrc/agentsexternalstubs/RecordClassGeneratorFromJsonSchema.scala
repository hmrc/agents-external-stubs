package uk.gov.hmrc.agentsexternalstubs
import better.files.File
import play.api.libs.json.{JsArray, JsLookup, JsObject, Json}
import uk.gov.hmrc.agentsexternalstubs.JsonSchema.ObjectDefinition

import scala.io.Source

/**
  * You might want to add some custom properties to the json schema in order to enable
  * custom record features, i.e. index keys and custom generators.
  *
  * "x_uniqueKey": true - this will mark string property as a unique key
  */
object RecordClassGeneratorFromJsonSchema extends App {

  require(args.length >= 3, "Expected args: source sink className")
  val source = args(0)
  val sink = args(1)
  val className = args(2)
  require(source != null && !source.isEmpty)
  require(sink != null && !sink.isEmpty)
  require(className != null && !className.isEmpty)

  val schema = Json.parse(Source.fromFile(source, "utf-8").mkString).as[JsObject]
  val definition = JsonSchema.read(schema)
  val code = RecordCodeRenderer.render(className, definition.asInstanceOf[ObjectDefinition])
  println(code)
  File(sink).write(code)
}

object JsonSchema {

  sealed trait Definition {

    val name: String
    val description: Option[String]

    val path: String
    val isRef: Boolean

    def isMandatory: Boolean
    def isPrimitive: Boolean = true

    def typeName: String = {
      val n = if (isRef) pathToName(path) else name
      n.substring(0, 1).toUpperCase + n.substring(1)
    }

    def variableName: String =
      if (name.charAt(0).isUpper) name.toLowerCase else name
  }

  case class ObjectDefinition(
    name: String,
    path: String,
    properties: Seq[Definition],
    required: Seq[String],
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean)
      extends Definition { override def isPrimitive: Boolean = false }

  case class OneOfDefinition(
    name: String,
    path: String,
    properties: Seq[Definition],
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean)
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
    isMandatory: Boolean
  ) extends Definition

  case class NumberDefinition(
    name: String,
    path: String,
    isRef: Boolean = false,
    description: Option[String] = None,
    isMandatory: Boolean)
      extends Definition

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

  def read(schema: JsObject): Definition = readProperty("", "#", schema, schema, required = Seq(""))

  private def pathToName(path: String): String = {
    val name = path.split("/").last
    if (name.endsWith("Type")) name.dropRight(4) else name
  }

  private def readProperty(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean = false,
    description: Option[String] = None,
    required: Seq[String]): Definition =
    (property \ "type").asOpt[String] match {
      case None =>
        (property \ "$ref").asOpt[String] match {
          case Some(ref) =>
            if (ref.startsWith("#/")) {
              val desc = description.orElse((property \ "description").asOpt[String])
              val jsonLookup = ref.substring(2).split("/").foldLeft[JsLookup](schema)((s, p) => s \ p)
              readProperty(
                name,
                ref,
                jsonLookup.result.as[JsObject],
                schema,
                isRef = true,
                description = desc,
                required)
            } else throw new IllegalStateException(s"Reference format not supported, must start with #/: $ref")
          case None =>
            throw new IllegalStateException(s"Property definition invalid, `type` or `$$ref` expected in  $property")
        }
      case Some(valueType) =>
        val desc = description.orElse((property \ "description").asOpt[String])
        val isMandatory = required.contains(name)
        valueType match {
          case "object"  => readObject(name, path, property, schema, isRef, desc, isMandatory)
          case "string"  => readString(name, path, property, schema, isRef, desc, isMandatory)
          case "number"  => NumberDefinition(name, path, isRef, desc, isMandatory)
          case "boolean" => BooleanDefinition(name, path, isRef, desc, isMandatory)
          case "array"   => readArray(name, path, property, schema, isRef, desc, isMandatory)
        }
    }

  private def readString(
    name: String,
    path: String,
    property: JsObject,
    schema: JsObject,
    isRef: Boolean,
    description: Option[String] = None,
    isMandatory: Boolean) = {

    val pattern = (property \ "pattern").asOpt[String]
    val enum = (property \ "enum").asOpt[Seq[String]]
    val minLength = (property \ "minLength").asOpt[Int]
    val maxLength = (property \ "maxLength").asOpt[Int]
    val isUniqueKey = (property \ "x_uniqueKey").asOpt[Boolean].getOrElse(false)
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
      isMandatory = isMandatory
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

    val required: Seq[String] = (property \ "required").asOpt[Seq[String]].getOrElse(Seq.empty)
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
          isMandatory)
      case None =>
        (property \ "oneOf").asOpt[JsArray] match {
          case Some(array) =>
            val props = array.value.map(p => readProperty(name, path, p.as[JsObject], schema, required = required))
            OneOfDefinition(name, path, properties = props, isRef = isRef, description = description, isMandatory)
          case None =>
            throw new IllegalStateException(
              s"Unsupported object definition, `properties` or `oneOf` expected in $property.")
        }
    }
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
    val itemDefinition = readProperty("item", path, items, schema, required = Seq("item"))
    ArrayDefinition(name, path, itemDefinition, isRef = isRef, description = description, isMandatory = isMandatory)
  }
}

trait JsonSchemaRenderer {
  def render(className: String, definition: JsonSchema.ObjectDefinition): String

  protected def quoted(s: String): String = "\"\"\"" + s + "\"\"\""
}

trait JsonSchemaCodeRenderer extends JsonSchemaRenderer {

  def render(className: String, typeDef: TypeDefinition): String

  import JsonSchema._

  case class TypeDefinition(name: String, definition: ObjectDefinition, subtypes: Seq[TypeDefinition], prefix: String)

  private def typeDefinition(typeName: String, definition: ObjectDefinition, prefix: String = ""): TypeDefinition =
    TypeDefinition(
      typeName,
      definition,
      definition.properties.collect {
        case od: ObjectDefinition => typeDefinition(od.typeName, od, s"${od.typeName}.")
        case oneOf: OneOfDefinition if oneOf.properties.headOption.collect { case _: ObjectDefinition => }.isDefined =>
          val od2 = oneOf.properties.headOption.collect { case o: ObjectDefinition => o }.get
          typeDefinition(oneOf.typeName, od2, s"${od2.typeName}.")
        case a: ArrayDefinition if a.item.isInstanceOf[ObjectDefinition] =>
          typeDefinition(a.item.typeName, a.item.asInstanceOf[ObjectDefinition], s"${a.item.typeName}.")
      },
      prefix
    )

  private def findAndAppendRefTypes(typeDef: TypeDefinition): TypeDefinition = {
    val subtypes = findRefTypes(typeDef.subtypes).map(t => t.copy(prefix = "")).sortBy(_.definition.typeName)
    typeDef.copy(subtypes = subtypes)
  }

  private def findRefTypes(typeDefs: Seq[TypeDefinition]): Seq[TypeDefinition] =
    typeDefs
      .flatMap(t => (if (t.definition.isRef) Seq(t) else Seq.empty) ++ findRefTypes(t.subtypes))
      .map(t => t.definition.path -> t)
      .toMap
      .values
      .toSeq

  final def render(className: String, definition: JsonSchema.ObjectDefinition): String = {
    val typeDef = findAndAppendRefTypes(typeDefinition(className, definition))
    render(className, typeDef)
  }
}

object RecordCodeRenderer extends JsonSchemaCodeRenderer {

  import JsonSchema._

  def render(className: String, typeDef: TypeDefinition): String =
    s"""
       |package uk.gov.hmrc.agentsexternalstubs.models
       |
       |import org.scalacheck.{Arbitrary, Gen}
       |import org.joda.time.LocalDate
       |import play.api.libs.json.{Format, Json}
       |import uk.gov.hmrc.agentsexternalstubs.models.$className._
       |
       |/**
       |  * ----------------------------------------------------------------------------
       |  * This $className code has been generated from json schema
       |  * by {@see uk.gov.hmrc.agentsexternalstubs.RecordCodeRenderer}
       |  * ----------------------------------------------------------------------------
       |  */
       |
       |${generateTypeDefinition(typeDef, isTopLevel = true)}
       |
     """.stripMargin

  private def generateTypeDefinition(typeDef: TypeDefinition, isTopLevel: Boolean): String = {
    val uniqueKey: Option[(String, String)] = generateUniqueKeyOption(typeDef.definition)
    val fieldValidators = generateFieldValidators(typeDef.definition)
    val fieldGenerators = generateFieldGenerators(typeDef.definition)
    val fieldsInitialization = generateGenFieldsInitialization(typeDef.definition)
    val sanitizers = generateSanitizers(typeDef.definition)
    val sanitizerList = generateSanitizerList(typeDef.definition)
    s"""
       |// schema path: ${typeDef.definition.path}
       |case class ${typeDef.name}(
       |  ${generateClassFields(typeDef)}${if (isTopLevel)
         s""",
            |  id: Option[String] = None
            |) extends Record {
            |
            |  override def uniqueKey: Option[String] = ${uniqueKey
              .map(k => s"${k._1}.map(${typeDef.name}.uniqueKey)")
              .getOrElse("None")}
            |  override def lookupKeys: Seq[String] = Seq()
            |  override def withId(id: Option[String]): Record = copy(id = id)
            |}
            |""".stripMargin
       else ")"}
       |
       |object ${typeDef.name} extends RecordUtils[${typeDef.name}] {
       |  ${if (isTopLevel)
         s"""
            |  implicit val arbitrary: Arbitrary[Char] = Arbitrary(Gen.alphaNumChar)
            |  implicit val recordType: RecordMetaData[${typeDef.name}] = RecordMetaData[${typeDef.name}](this)
            |  ${if (uniqueKey.isDefined)
              s"def uniqueKey(key: String): String = s${quoted(s"${uniqueKey.get._2}:$${key.toUpperCase}")}"
            else ""}
            |  import Validator._
         """.stripMargin
       else ""}
       |  override val gen: Gen[${typeDef.name}] = ${if (fieldGenerators.isEmpty) "Gen const"
       else
         s"""for {
            |    $fieldGenerators
            |  } yield""".stripMargin} ${typeDef.name}($fieldsInitialization)
       |  ${typeDef.subtypes
         .filter(!_.definition.isRef || isTopLevel)
         .map(t => generateTypeDefinition(t, isTopLevel = false))
         .mkString("\n")}
       |  override val validate: Validator[${typeDef.name}] = Validator($fieldValidators)
       |  $sanitizers
       |  override val sanitizers: Seq[Update] = Seq($sanitizerList)
       |
       |  implicit val formats: Format[${typeDef.name}] = Json.format[${typeDef.name}]
       |}
       |
     """.stripMargin
  }

  private def generateClassFields(typeDef: TypeDefinition): String =
    typeDef.definition.properties
      .take(22)
      .map(prop =>
        s"""${prop.name}: ${typeOf(prop, typeDef.prefix, typeDef.definition.required.contains(prop.name))}""".stripMargin)
      .mkString(",\n  ")

  private def typeOf(definition: Definition, prefix: String, mandatory: Boolean): String = {
    val typeName = definition match {
      case _: StringDefinition  => "String"
      case _: NumberDefinition  => "Int"
      case _: BooleanDefinition => "Boolean"
      case a: ArrayDefinition   => s"Seq[${a.item.typeName}]"
      case o: ObjectDefinition  => s"${if (o.isRef) prefix else ""}${o.typeName}"
      case o: OneOfDefinition   => s"${if (o.isRef) prefix else ""}${o.typeName}"
    }
    if (mandatory) typeName else s"Option[$typeName] = None"
  }

  private def generateFieldGenerators(definition: ObjectDefinition): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.variableName} <- ${valueGenerator(prop)}""".stripMargin)
      .mkString("\n    ")

  private def generateGenFieldsInitialization(definition: ObjectDefinition): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.name} = ${prop.variableName}""".stripMargin)
      .mkString("\n    ", ",\n    ", "\n  ")

  private def valueGenerator(property: Definition): String = {
    val gen = property match {
      case s: StringDefinition =>
        if (s.enum.isDefined) {
          if (s.enum.get.size == 1) s"""Gen.const("${s.enum.get.head}")"""
          else s"""Gen.oneOf(Seq(${s.enum.get.mkString("\"", "\",\"", "\"")}))"""
        } else if (s.pattern.isDefined) {
          s"""Generator.regex(${quoted(s.pattern.get)})"""
        } else if (s.minLength.isDefined || s.maxLength.isDefined) {
          s"""Generator.stringMinMaxN(${s.minLength.getOrElse(1)},${s.maxLength.getOrElse(256)})"""
        } else {
          "Generator.stringMaxN(256)"
        }
      case n: NumberDefinition  => "Gen.const(1)"
      case b: BooleanDefinition => "Generator.biasedBooleanGen"
      case a: ArrayDefinition   => s"Generator.nonEmptyListOfMaxN(3,${valueGenerator(a.item)})"
      case o: ObjectDefinition  => s"${o.typeName}.gen"
      case o: OneOfDefinition =>
        o.properties.head match {
          case _: ObjectDefinition => s"${o.typeName}.gen"
          case x                   => valueGenerator(x)
        }
    }
    if (property.isMandatory) gen else s"""Generator.biasedOptionGen($gen)"""
  }

  private def generateFieldValidators(definition: ObjectDefinition): String =
    definition.properties
      .take(22)
      .map(prop => valueValidator(prop))
      .collect { case Some(validator) => s"""$validator""".stripMargin }
      .mkString(",\n  ")

  private def valueValidator(property: Definition): Option[String] =
    property match {
      case s: StringDefinition =>
        if (s.enum.isDefined) Some(s"""  check(_.${property.name}.isOneOf(Seq(${s.enum.get
          .mkString("\"", "\",\"", "\"")})), "Invalid ${property.name}, does not match allowed values")""")
        else if (s.pattern.isDefined)
          Some(s"""  check(_.${property.name}.matches(${quoted(s.pattern.get)}), ${quoted(
            s"Invalid ${property.name}, does not matches regex ${s.pattern.get}")})""")
        else if (s.minLength.isDefined && s.maxLength.isDefined)
          Some(
            s"""  check(_.${property.name}.lengthMinMaxInclusive(${s.minLength.get},${s.maxLength.get}), "Invalid length of ${property.name}, should be between ${s.minLength.get} and ${s.maxLength.get} inclusive")""")
        else if (s.minLength.isDefined)
          Some(
            s"""  check(_.${property.name}.lengthMin(${s.minLength.get}),"Invalid length of ${property.name}, minimum length should be ${s.minLength.get}")""")
        else if (s.maxLength.isDefined)
          Some(
            s"""  check(_.${property.name}.lengthMax(${s.maxLength.get}),"Invalid length of ${property.name}, maximum length should be ${s.maxLength.get}")""")
        else None
      case n: NumberDefinition  => None
      case b: BooleanDefinition => None
      case a: ArrayDefinition =>
        (a.item match {
          case o: ObjectDefinition => Some(s"""${o.typeName}.validate""")
          case x                   => valueValidator(x)
        }).map(vv =>
          if (property.isMandatory) s""" checkEach(_.${property.name}, $vv)"""
          else s""" checkEachIfSome(_.${property.name}, $vv)""")

      case o: ObjectDefinition =>
        if (property.isMandatory) Some(s""" checkObject(_.${property.name}, ${property.typeName}.validate)""")
        else Some(s""" checkObjectIfSome(_.${property.name}, ${property.typeName}.validate)""")
      case o: OneOfDefinition => None
    }

  private def generateSanitizerList(definition: ObjectDefinition): String =
    definition.properties
      .filter(!_.isMandatory)
      .take(22)
      .map(prop => s"${prop.name}Sanitizer")
      .mkString("\n    ", ",\n    ", "\n  ")

  private def generateSanitizers(definition: ObjectDefinition): String =
    definition.properties
      .filter(!_.isMandatory)
      .take(22)
      .map(prop => s"""  val ${prop.name}Sanitizer: Update = seed => entity =>
                      |    entity.copy(${prop.name} = entity.${prop.name}.orElse(Generator.get(${valueGenerator(definition)})(seed)))
         """.stripMargin)
      .mkString("\n")

  private def generateUniqueKeyOption(definition: Definition, path: List[Definition] = Nil): Option[(String, String)] =
    definition match {
      case s: StringDefinition => if (s.isUniqueKey) Some(accessorFor(s :: path), s.name) else None
      case o: ObjectDefinition =>
        o.properties.foldLeft[Option[(String, String)]](None)((a, p) => a.orElse(generateUniqueKeyOption(p, o :: path)))
      case _ => None
    }

  private def accessorFor(path: List[Definition], nested: String = "", option: Boolean = false): String = path match {
    case (s: StringDefinition) :: xs =>
      accessorFor(xs, s.name, !s.isMandatory)
    case (o: ObjectDefinition) :: xs =>
      val prefix =
        if (o.name.isEmpty) ""
        else if (o.isMandatory) s"${o.name}."
        else s"${o.name}.${if (option) "flatMap" else "map"}(_."
      val suffix = if (o.name.isEmpty) "" else if (!o.isMandatory) ")" else ""
      accessorFor(xs, prefix + nested + suffix, !o.isMandatory || option)
    case Nil => if (option) nested else s"Some($nested)"
  }
}
