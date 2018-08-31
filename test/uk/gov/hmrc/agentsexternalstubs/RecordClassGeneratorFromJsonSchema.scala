package uk.gov.hmrc.agentsexternalstubs
import better.files.File
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.agentsexternalstubs.JsonSchema.ObjectDefinition

import scala.io.Source

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
  File(sink).write(code)
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
        case od: ObjectDefinition => Seq(typeDefinition(od.typeName, od, s"${od.typeName}."))
        case oneOf: OneOfDefinition if oneOf.variants.collect { case _: ObjectDefinition => }.nonEmpty =>
          oneOf.variants
            .collect { case o: ObjectDefinition => o }
            .map(od2 => typeDefinition(od2.typeName, od2, s"${od2.typeName}."))
        case a: ArrayDefinition if a.item.isInstanceOf[ObjectDefinition] =>
          Seq(typeDefinition(a.item.typeName, a.item.asInstanceOf[ObjectDefinition], s"${a.item.typeName}."))
      }.flatten,
      prefix
    )

  private def findAndAppendRefTypes(typeDef: TypeDefinition): TypeDefinition = {
    val refTypesMap: Map[String, TypeDefinition] = findRefTypes(typeDef)
      .map(t => t.copy(prefix = ""))
      .map(t => t.definition.path -> t)
      .toMap

    val commonRefTypes = refTypesMap.values.toSeq.filterNot(_.definition == typeDef.definition)

    val typeDef1: TypeDefinition = removeRefSubTypes(typeDef)
    typeDef.copy(subtypes = (commonRefTypes ++ typeDef1.subtypes).sortBy(_.definition.typeName))
  }

  private def findRefTypes(typeDef: TypeDefinition): Seq[TypeDefinition] =
    (if (typeDef.definition.isRef) Seq(typeDef) else Seq.empty) ++ typeDef.subtypes.flatMap(findRefTypes)

  private def removeRefSubTypes(typeDef: TypeDefinition): TypeDefinition =
    typeDef.copy(
      subtypes = typeDef.subtypes.filter(!_.definition.isRef).map(removeRefSubTypes)
    )

  final def render(className: String, definition: JsonSchema.ObjectDefinition): String = {
    val typeDef = findAndAppendRefTypes(typeDefinition(className, definition))
    render(className, typeDef)
  }
}

object RecordCodeRenderer extends JsonSchemaCodeRenderer {

  import JsonSchema._

  case class Context(
    uniqueKey: Option[(String, String)],
    keys: Seq[(String, String)],
    commonVals: Map[String, String]) {
    def reference(s: String): String = commonVals.get(s).map(n => s"Common.$n").getOrElse(s)
  }

  object Context {

    def apply(definition: Definition): Context = {

      val uniqueKey = findUniqueKey(definition)
      val keys = findKeys(definition)

      val externalizedStrings = mapCommonVals(definition, Map.empty.withDefaultValue(Nil))
        .mapValues(list => {
          list.map(_.replaceAll("\\d", "")).distinct.minBy(_.length)
        })
        .groupBy { case (_, v) => v }
        .mapValues(m => if (m.size <= 1) m else m.toSeq.zipWithIndex.map { case ((k, v), i) => (k, v + i) }.toMap)
        .foldLeft[Map[String, String]](Map())((a, v) => a ++ v._2)

      Context(uniqueKey, keys, externalizedStrings)
    }

    private def findUniqueKey(definition: Definition, path: List[Definition] = Nil): Option[(String, String)] =
      definition match {
        case s: StringDefinition => if (s.isUniqueKey) Some(accessorFor(s :: path), s.name) else None
        case o: ObjectDefinition =>
          o.properties.foldLeft[Option[(String, String)]](None)((a, p) => a.orElse(findUniqueKey(p, o :: path)))
        case _ => None
      }

    private def findKeys(definition: Definition, path: List[Definition] = Nil): Seq[(String, String)] =
      definition match {
        case s: StringDefinition => if (s.isKey) Seq((accessorFor(s :: path), s.name)) else Seq.empty
        case o: ObjectDefinition =>
          o.properties.map(findKeys(_, o :: path)).reduce(_ ++ _)
        case _ => Seq.empty
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
      case Nil => if (option) nested else s"Option($nested)"
    }

    private def externalizePattern(s: StringDefinition, map: Map[String, List[String]]): Map[String, List[String]] =
      s.pattern
        .map(p => {
          val key = quoted(p)
          map
            .get(key)
            .map(list => map.updated(key, s"${s.name}Pattern" :: list))
            .getOrElse(map.+(key -> List(s"${s.name}Pattern")))
        })
        .getOrElse(map)

    private def externalizeEnum(s: StringDefinition, map: Map[String, List[String]]): Map[String, List[String]] =
      s.enum
        .map(e => {
          val key = s"""Seq(${e.mkString("\"", "\",\"", "\"")})"""
          map
            .get(key)
            .map(list => map.updated(key, s"${s.name}Enum" :: list))
            .getOrElse(map.+(key -> List(s"${s.name}Enum")))
        })
        .getOrElse(map)

    private def mapCommonVals(definition: Definition, map: Map[String, List[String]]): Map[String, List[String]] =
      definition match {
        case s: StringDefinition => externalizePattern(s, map) ++ externalizeEnum(s, map)
        case o: ObjectDefinition =>
          o.properties.foldLeft(map)((m, p) => mapCommonVals(p, m))
        case o: OneOfDefinition =>
          o.variants.foldLeft(map)((m, p) => mapCommonVals(p, m))
        case a: ArrayDefinition =>
          mapCommonVals(a.item, map)
        case _ => map
      }
  }

  def render(className: String, typeDef: TypeDefinition): String = {
    val context = Context(typeDef.definition)
    s"""
       |package uk.gov.hmrc.agentsexternalstubs.models
       |
       |import org.scalacheck.{Arbitrary, Gen}
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
       |${generateTypeDefinition(typeDef, isTopLevel = true, context)}
       |
     """.stripMargin
  }

  private def generateTypeDefinition(typeDef: TypeDefinition, isTopLevel: Boolean, context: Context): String = {
    val classFields = generateClassFields(typeDef)
    val fieldValidators = generateFieldValidators(typeDef.definition, context)
    val fieldGenerators = generateFieldGenerators(typeDef.definition, context)
    val fieldsInitialization = generateGenFieldsInitialization(typeDef.definition)
    val sanitizers = generateSanitizers(typeDef.definition, context)
    val sanitizerList = generateSanitizerList(typeDef.definition)
    val subtypesDefinitions: String = typeDef.subtypes
      .filter(!_.definition.isRef || isTopLevel)
      .map(t => generateTypeDefinition(t, isTopLevel = false, context))
      .mkString("")

    s"""case class ${typeDef.name}(
       |  $classFields${if (isTopLevel)
         s""",
            |  id: Option[String] = None
            |) extends Record {
            |
            |  override def uniqueKey: Option[String] = ${context.uniqueKey
              .map(key => s"${key._1}.map(${typeDef.name}.uniqueKey)")
              .getOrElse("None")}
            |  override def lookupKeys: Seq[String] = Seq(${context.keys
              .map(key => s"${key._1}.map(${typeDef.name}.${key._2}Key)")
              .mkString(", ")})${if (context.keys.nonEmpty) ".collect{case Some(x) => x}" else ""}
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
            |  ${if (context.uniqueKey.isDefined)
              s"\n  def uniqueKey(key: String): String = s${quoted(s"${context.uniqueKey.get._2}:$${key.toUpperCase}")}"
            else ""}${if (context.keys.nonEmpty)
              "\n  " + context.keys
                .map(key => s"""def ${key._2}Key(key: String): String = s${quoted(s"${key._2}:$${key.toUpperCase}")}""")
                .mkString("\n  ")
            else ""}
            |  
            |  import Validator._
         """.stripMargin
       else ""}
       |  override val gen: Gen[${typeDef.name}] = ${if (fieldGenerators.isEmpty) "Gen const"
       else
         s"""for {
            |    $fieldGenerators
            |  } yield""".stripMargin} ${typeDef.name}($fieldsInitialization)
       |
       |  $subtypesDefinitions
       |
       |  override val validate: Validator[${typeDef.name}] = Validator($fieldValidators)
       |
       |  $sanitizers
       |  override val sanitizers: Seq[Update] = Seq($sanitizerList)
       |
       |  implicit val formats: Format[${typeDef.name}] = Json.format[${typeDef.name}]
       |    ${if (isTopLevel) generateExternalizedVals(context) else ""}
       |}
       |
     """.stripMargin
  }

  private def generateClassFields(typeDef: TypeDefinition): String =
    typeDef.definition.properties
      .take(22)
      .map(prop => s"""${prop.name}: ${typeOf(prop, typeDef.prefix)}""".stripMargin)
      .mkString(",\n  ")

  private def typeOf(definition: Definition, prefix: String, wrapOption: Boolean = true): String = {
    val typeName = definition match {
      case _: StringDefinition  => "String"
      case _: NumberDefinition  => "BigDecimal"
      case _: BooleanDefinition => "Boolean"
      case a: ArrayDefinition   => s"Seq[${a.item.typeName}]"
      case o: ObjectDefinition  => s"${if (o.isRef) "" else prefix}${o.typeName}"
      case o: OneOfDefinition   => typeOf(o.variants.head, prefix, wrapOption = false)
    }
    if (!definition.isMandatory && wrapOption) s"Option[$typeName] = None" else typeName
  }

  private def generateFieldGenerators(definition: ObjectDefinition, context: Context): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.variableName} <- ${valueGenerator(prop, context)}""".stripMargin)
      .mkString("\n    ")

  private def generateGenFieldsInitialization(definition: ObjectDefinition): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.name} = ${prop.variableName}""".stripMargin)
      .mkString("\n    ", ",\n    ", "\n  ")

  private def valueGenerator(property: Definition, context: Context, wrapOption: Boolean = true): String = {
    val gen = property match {
      case s: StringDefinition =>
        s.customGenerator.getOrElse(
          if (s.enum.isDefined) {
            if (s.enum.get.size == 1) s"""Gen.const("${s.enum.get.head}")"""
            else s"""Gen.oneOf(${context.reference(s"Seq(${s.enum.get.mkString("\"", "\",\"", "\"")})")})"""
          } else if (s.pattern.isDefined)
            s"""Generator.regex(${context.reference(quoted(s.pattern.get))})"""
          else if (s.minLength.isDefined || s.maxLength.isDefined)
            s"""Generator.stringMinMaxN(${s.minLength.getOrElse(1)},${s.maxLength.getOrElse(256)})"""
          else "Generator.stringMaxN(256)")

      case n: NumberDefinition =>
        n.customGenerator.getOrElse((n.minimum, n.maximum, n.multipleOf) match {
          case (Some(min), Some(max), mlt) => s"Generator.chooseBigDecimal($min,$max,$mlt)"
          case (Some(min), None, mlt)      => s"Generator.chooseBigDecimal($min,100000000,$mlt)"
          case (None, Some(max), mlt)      => s"Generator.chooseBigDecimal(0,$max,$mlt)"
          case _                           => "Gen.const(BigDecimal(0))"
        })

      case b: BooleanDefinition => "Generator.biasedBooleanGen"
      case a: ArrayDefinition   => s"Generator.nonEmptyListOfMaxN(3,${valueGenerator(a.item, context)})"
      case o: ObjectDefinition  => s"${o.typeName}.gen"

      case o: OneOfDefinition =>
        o.variants.head match {
          case i: ObjectDefinition => s"${i.typeName}.gen"
          case x                   => valueGenerator(x, context)
        }
    }
    if (!property.isMandatory && wrapOption) s"""Generator.biasedOptionGen($gen)""" else gen
  }

  private def generateFieldValidators(definition: ObjectDefinition, context: Context): String =
    definition.properties
      .take(22)
      .map(prop => valueValidator(prop, context))
      .collect { case Some(validator) => s"""$validator""".stripMargin }
      .mkString(",\n  ")

  private def valueValidator(property: Definition, context: Context, isMandatory: Boolean = false): Option[String] =
    property match {
      case s: StringDefinition =>
        if (s.enum.isDefined) Some(s"""  check(_.${property.name}.isOneOf(${context.reference(s"Seq(${s.enum.get
          .mkString("\"", "\",\"", "\"")})")}), "Invalid ${property.name}, does not match allowed values")""")
        else if (s.pattern.isDefined)
          Some(s"""  check(_.${property.name}.matches(${context
            .reference(context.reference(quoted(s.pattern.get)))}), s${quoted(
            s"Invalid ${property.name}, does not matches regex $${${context.reference(quoted(s.pattern.get))}}")})""")
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

      case n: NumberDefinition =>
        (n.minimum, n.maximum, n.multipleOf) match {
          case (Some(min), Some(max), mlt) =>
            Some(s"""  check(_.${property.name}.inRange(BigDecimal($min),BigDecimal($max),${mlt.map(a =>
              s"BigDecimal($a)")}),"Invalid number ${property.name}, must be in range <$min,$max>")""")
          case (None, Some(max), mlt) =>
            Some(s"""  check(_.${property.name}.lteq(BigDecimal($max),${mlt
              .map(a => s"BigDecimal($a)")}),"Invalid number ${property.name}, must be lower than or equal to $max")""")
          case (Some(min), None, mlt) =>
            Some(s"""  check(_.${property.name}.gteq(BigDecimal($min),${mlt
              .map(a => s"BigDecimal($a)")}),"Invalid number ${property.name}, must be greater than or equal to $min")""")
          case _ => None
        }

      case a: ArrayDefinition =>
        (a.item match {
          case o: ObjectDefinition => Some(s"""${o.typeName}.validate""")
          case x                   => valueValidator(x, context)
        }).map(vv =>
          if (property.isMandatory || isMandatory) s""" checkEach(_.${property.name}, $vv)"""
          else s"""  checkEachIfSome(_.${property.name}, $vv)""")

      case _: ObjectDefinition =>
        if (property.isMandatory || isMandatory)
          Some(s""" checkObject(_.${property.name}, ${property.typeName}.validate)""")
        else Some(s"""  checkObjectIfSome(_.${property.name}, ${property.typeName}.validate)""")

      case o: OneOfDefinition =>
        if (o.variants.isEmpty) None
        else valueValidator(o.variants.head, context, o.isMandatory)

      case _: BooleanDefinition => None
    }

  private def generateSanitizerList(definition: ObjectDefinition): String =
    definition.properties
      .filter(!_.isMandatory)
      .take(22)
      .map(prop => s"${prop.name}Sanitizer")
      .mkString("\n    ", ",\n    ", "\n  ")

  private def generateSanitizers(definition: ObjectDefinition, context: Context): String =
    definition.properties
      .filter(!_.isMandatory)
      .take(22)
      .map(prop =>
        s"""  val ${prop.name}Sanitizer: Update = seed => entity =>
           |    entity.copy(${prop.name} = entity.${prop.name}.orElse(Generator.get(${valueGenerator(
             prop,
             context,
             wrapOption = false)})(seed)))
         """.stripMargin)
      .mkString("\n  ")

  private def generateExternalizedVals(context: Context): String =
    context.commonVals
      .map {
        case (value, name) => s"  val $name = $value"
      }
      .mkString("object Common {\n  ", "\n  ", "\n}")

}
