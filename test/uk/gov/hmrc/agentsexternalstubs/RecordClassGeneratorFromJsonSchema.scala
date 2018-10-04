package uk.gov.hmrc.agentsexternalstubs
import better.files.File
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.agentsexternalstubs.JsonSchema._

import scala.io.Source
import scala.util.matching.Regex

/**
  * This app generates a ready-to-use implementation of a Record
  * based on the provided json schema.
  *
  * Usage:
  * <pre>sbt "test:runMain uk.gov.hmrc.agentsexternalstubs.RecordClassGeneratorFromJsonSchema {sourceSchemaPath} {targetClassPath} {recordName} output:[record|payload]"</pre>
  *
  * output options:
  *  - record  : full option
  *  - payload : validators only, no generators nor sanitizers
  */
object RecordClassGeneratorFromJsonSchema extends App {

  require(args.length >= 3, "Expected args: source sink className")
  val source = args(0)
  val sink = args(1)
  val className = args(2)
  require(source != null && !source.isEmpty)
  require(sink != null && !sink.isEmpty)
  require(className != null && !className.isEmpty)

  val options = args.drop(3)

  val schema = Json.parse(Source.fromFile(source, "utf-8").mkString).as[JsObject]
  val definition = JsonSchema.read(schema)
  val code = RecordCodeRenderer.render(
    className,
    definition,
    options,
    s"""sbt "test:runMain ${this.getClass.getName.dropRight(1)} $source $sink $className ${options
      .mkString(" ")}""""
  )
  File(sink).write(code)
}

trait JsonSchemaRenderer {
  def render(className: String, definition: Definition, options: Seq[String], description: String): String

  protected def quoted(s: String): String = "\"\"\"" + s + "\"\"\""
}

trait JsonSchemaCodeRenderer extends JsonSchemaRenderer {

  def render(className: String, typeDef: TypeDefinition, options: Seq[String], description: String): String

  final def render(className: String, definition: Definition, options: Seq[String], description: String): String =
    findObjectDefinition(definition).fold(
      throw new IllegalArgumentException("Provided json schema does not represent valid object definition")
    ) { definition =>
      val typeDef = moveRefTypesToTheTop(typeDefinition(className, definition))
      render(className, typeDef, options, description)
    }

  private def findObjectDefinition(definition: Definition): Option[ObjectDefinition] = definition match {
    case o: ObjectDefinition => Some(o)
    case o: OneOfDefinition  => o.variants.map(findObjectDefinition).collectFirst { case Some(x) => x }
    case _                   => None
  }

  private def moveRefTypesToTheTop(typeDef: TypeDefinition): TypeDefinition = {
    val refTypesMap: Map[String, TypeDefinition] = collectRefTypes(typeDef)
      .map(t => t.copy(prefix = ""))
      .groupBy(_.definition.path)
      .mapValues(_.reduce((a, b) => a.copy(interfaces = a.interfaces ++ b.interfaces)))

    val commonRefTypes = refTypesMap.values.toSeq.filterNot(_.definition == typeDef.definition)

    val typeDef1: TypeDefinition = removeNestedRefTypes(typeDef)
    typeDef.copy(nestedTypes = (commonRefTypes ++ typeDef1.nestedTypes).sortBy(_.definition.typeName))
  }

  private def collectRefTypes(typeDef: TypeDefinition): Seq[TypeDefinition] =
    (if (typeDef.definition.isRef) Seq(typeDef) else Seq.empty) ++ typeDef.nestedTypes.flatMap(collectRefTypes)

  private def removeNestedRefTypes(typeDef: TypeDefinition): TypeDefinition =
    if (typeDef.isInterface) typeDef
    else
      typeDef.copy(
        nestedTypes = typeDef.nestedTypes.filter(!_.definition.isRef).map(removeNestedRefTypes)
      )

  case class TypeDefinition(
    name: String,
    definition: ObjectDefinition,
    nestedTypes: Seq[TypeDefinition] = Seq.empty,
    prefix: String,
    isInterface: Boolean = false,
    interfaces: Seq[TypeDefinition] = Seq.empty,
    subtypes: Seq[TypeDefinition] = Seq.empty,
    interfaceMethods: Set[(String, String)] = Set.empty) {

    def hasInterfaces: Boolean = interfaces.nonEmpty
  }

  private def typeDefinition(typeName: String, definition: ObjectDefinition, prefix: String = ""): TypeDefinition =
    TypeDefinition(
      typeName,
      definition,
      definition.properties.collect {
        case od: ObjectDefinition => Seq(typeDefinition(od.typeName, od, s"${od.typeName}."))
        case oneOf: OneOfDefinition if oneOf.variants.collect { case _: ObjectDefinition => }.nonEmpty =>
          val subtypes = oneOf.variants
            .collect { case o: ObjectDefinition => o }
            .map(od2 => typeDefinition(od2.typeName, od2, s"${od2.typeName}."))
          if (subtypes.size <= 1) subtypes
          else {
            // New artificial interface type to span over multiple oneOf variants
            //val isRef = oneOf.isRef || subtypes.exists(_.definition.isRef)
            val superType = TypeDefinition(
              oneOf.typeName,
              ObjectDefinition(
                oneOf.name,
                oneOf.path,
                Seq.empty,
                Seq.empty,
                oneOf.isRef,
                oneOf.description,
                oneOf.isMandatory),
              prefix = if (oneOf.isRef) "" else prefix,
              isInterface = true,
              interfaceMethods = findInterfaceMethods(subtypes)
            )
            val subtypes2 = subtypes.map(s => s.copy(interfaces = s.interfaces :+ superType))
            Seq(superType.copy(subtypes = subtypes2, nestedTypes = subtypes2))
          }
        case a: ArrayDefinition if a.item.isInstanceOf[ObjectDefinition] =>
          Seq(typeDefinition(a.item.typeName, a.item.asInstanceOf[ObjectDefinition], s"${a.item.typeName}."))
      }.flatten,
      prefix
    )

  private def findInterfaceMethods(subtypes: Seq[TypeDefinition]): Set[(String, String)] =
    subtypes
      .map(_.definition)
      .map {
        case o: ObjectDefinition => o.properties.map(d => (d.name, typeOf(d, ""))).toSet
        case _                   => Set.empty[(String, String)]
      }
      .reduce[Set[(String, String)]]((a, b) => a.intersect(b))

  protected def typeOf(
    definition: Definition,
    prefix: String,
    wrapOption: Boolean = true,
    defaultValue: Boolean = true): String = {
    val typeName = definition match {
      case _: StringDefinition  => "String"
      case _: NumberDefinition  => "BigDecimal"
      case _: BooleanDefinition => "Boolean"
      case a: ArrayDefinition   => s"Seq[${a.item.typeName}]"
      case o: ObjectDefinition  => s"${if (o.isRef) "" else prefix}${o.typeName}"
      case o: OneOfDefinition =>
        if (o.variants.isEmpty) "Nothing"
        else if (o.variants.size == 1) typeOf(o.variants.head, prefix, wrapOption = false)
        else
          s"${if (o.isRef) "" else prefix}${o.typeName}"
    }
    if (!definition.isMandatory && wrapOption) s"Option[$typeName]${if (defaultValue) " = None" else ""}"
    else { s"""$typeName ${if (defaultValue && definition.isBoolean) " = false" else ""}""" }
  }
}

object RecordCodeRenderer extends JsonSchemaCodeRenderer with KnownFieldGenerators {

  case class Context(
    outputType: String,
    uniqueKey: Option[(String, String)],
    keys: Seq[(String, String)],
    commonVals: Map[String, String]) {

    val isRecordOutputType: Boolean = outputType == "record"
    val isPayloadOutputType: Boolean = outputType == "payload"

    def commonReference(s: String): String = commonVals.get(s).map(n => s"Common.$n").getOrElse(s)
  }

  object Context {

    def apply(definition: Definition, outputType: String): Context = {

      val uniqueKey = findUniqueKey(definition)
      val keys = findKeys(definition)

      val externalizedStrings = mapCommonVals(definition, Map.empty.withDefaultValue(Nil))
        .mapValues(list => {
          list.map(_.replaceAll("\\d", "")).distinct.minBy(_.length)
        })
        .groupBy { case (_, v) => v }
        .mapValues(m => if (m.size <= 1) m else m.toSeq.zipWithIndex.map { case ((k, v), i) => (k, v + i) }.toMap)
        .foldLeft[Map[String, String]](Map())((a, v) => a ++ v._2)

      Context(outputType, uniqueKey, keys, externalizedStrings)
    }

    private def findUniqueKey(definition: Definition, path: List[Definition] = Nil): Option[(String, String)] =
      definition match {
        case s: StringDefinition => if (s.isUniqueKey) Some((accessorFor(s :: path), s.name)) else None
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
      case (o: ObjectDefinition) :: xs =>
        val prefix =
          if (o.name.isEmpty) ""
          else if (o.isMandatory) s"${o.name}."
          else s"${o.name}.${if (option) "flatMap" else "map"}(_."
        val suffix = if (o.name.isEmpty) "" else if (!o.isMandatory) ")" else ""
        accessorFor(xs, prefix + nested + suffix, !o.isMandatory || option)
      case (s: Definition) :: xs =>
        accessorFor(xs, s.name, !s.isMandatory)
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

  def render(className: String, typeDef: TypeDefinition, options: Seq[String], description: String): String = {
    val outputType = options.find(_.startsWith("output:")).map(_.drop(7)).getOrElse("record")
    require(Set("record", "payload").contains(outputType), s"output:[record|payload] but was output:$outputType")
    val context = Context(typeDef.definition, outputType)

    // -----------------------------------------
    //    FILE HEADER
    // -----------------------------------------
    s"""package uk.gov.hmrc.agentsexternalstubs.models
       |
       |${if (context.isRecordOutputType) "import org.scalacheck.{Arbitrary, Gen}" else "\r"}
       |import play.api.libs.json._
       |import uk.gov.hmrc.agentsexternalstubs.models.$className._
       |
       |/**
       |  * ----------------------------------------------------------------------------
       |  * THIS FILE HAS BEEN GENERATED - DO NOT MODIFY IT, CHANGE THE SCHEMA IF NEEDED
       |  * How to regenerate? Run this command in the project root directory:
       |  * $description
       |  * ----------------------------------------------------------------------------
       |  *
       |  *  ${generateTypesMap(typeDef)}
       |  */
       |
       |${generateTypeDefinition(typeDef, isTopLevel = true, context)}
       |
     """.stripMargin
  }

  private def generateTypesMap(typeDef: TypeDefinition, level: Int = 1): String =
    s"${typeDef.name}${typeDef.nestedTypes
      .filter(!_.definition.isRef || level == 1)
      .map(t => "\n  *  " + ("-  " * level) + generateTypesMap(t, level + 1))
      .mkString("")}"

  private def generateTypeDefinition(typeDef: TypeDefinition, isTopLevel: Boolean, context: Context): String = {

    lazy val classFields = generateClassFields(typeDef)
    lazy val propertyValidators = generatePropertyValidators(typeDef.definition, context)
    lazy val objectValidator = generateObjectValidator(typeDef.definition, context)
    lazy val fieldGenerators = generateFieldGenerators(typeDef.definition, context)
    lazy val fieldsInitialization = generateGenFieldsInitialization(typeDef.definition)
    lazy val sanitizers = generateSanitizers(typeDef.definition, context)
    lazy val sanitizerList = generateSanitizerList(typeDef.definition)
    lazy val nestedTypesDefinitions: String = typeDef.nestedTypes
      .filter(!_.definition.isRef || isTopLevel)
      .map(t => generateTypeDefinition(t, isTopLevel = false, context))
      .mkString("")

    lazy val recordObjectMembersCode: String = if (isTopLevel) {
      if (context.isRecordOutputType)
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
           |  import Generator.GenOps._
         """.stripMargin
      else
        """
          |   import Validator._
        """.stripMargin
    } else ""

    lazy val generatorsCode: String =
      if (context.isRecordOutputType)
        s"""
           |  override val gen: Gen[${typeDef.name}] = ${if (typeDef.isInterface)
             s"Gen.oneOf[${typeDef.name}](${typeDef.subtypes
               .map(st => s"${st.name}.gen.map(_.asInstanceOf[${typeDef.name}])")
               .mkString(",\n  ")})"
           else if (fieldGenerators.isEmpty)
             s"Gen const ${typeDef.name}($fieldsInitialization)"
           else
             s"""for {
                |    $fieldGenerators
                |  } yield ${typeDef.name}($fieldsInitialization)""".stripMargin}
           |  """.stripMargin
      else ""

    lazy val validatorCode: String =
      s"""  ${if (context.isRecordOutputType) "override " else ""}val validate: Validator[${typeDef.name}] = ${if (typeDef.isInterface && typeDef.subtypes.nonEmpty)
        s"{${typeDef.subtypes
          .map(subTypeDef => s"""case x: ${subTypeDef.name}   => ${subTypeDef.name}.validate(x)""")
          .mkString("\n    ")}}"
      else s"Validator($objectValidator)"}"""

    lazy val sanitizersCode: String = if (context.isRecordOutputType) {
      if (typeDef.isInterface && typeDef.subtypes.nonEmpty)
        s"""val sanitizer: Update = seed => {${typeDef.subtypes
             .map(subTypeDef => s"""  case x: ${subTypeDef.name}   => ${subTypeDef.name}.sanitize(seed)(x)""")
             .mkString("\n    ")}}
           |  override val sanitizers: Seq[Update] = Seq(sanitizer)""".stripMargin
      else s"""$sanitizers
      |  override val sanitizers: Seq[Update] = Seq($sanitizerList)"""
    } else ""

    lazy val formatsCode: String =
      if (typeDef.isInterface)
        s"""
           |  implicit val reads: Reads[${typeDef.name}] = new Reads[${typeDef.name}] {
           |      override def reads(json: JsValue): JsResult[${typeDef.name}] = {
           |      ${typeDef.subtypes.zipWithIndex
             .map {
               case (subTypeDef, i) =>
                 s"""  val r$i = ${if (i > 0) s"r${i - 1}.orElse(" else ""}${subTypeDef.name}.formats.reads(json).flatMap(e => ${subTypeDef.name}.validate(e).fold(_ => JsError(), _ => JsSuccess(e)))${if (i > 0)
                   ")"
                 else ""}"""
             }
             .mkString("\n  ")}
           |        r${typeDef.subtypes.size - 1}.orElse(aggregateErrors(JsError("Could not match json object to any variant of ${typeDef.name}, i.e. ${typeDef.subtypes
             .map(_.name)
             .mkString(", ")}"),${(for (i <- typeDef.subtypes.indices)
             yield s"r$i").mkString(",")}))
           |      }
           |
           |      private def aggregateErrors[T](errors: JsResult[T]*): JsError =
           |        errors.foldLeft(JsError())((a, r) =>
           |          r match {
           |            case e: JsError => JsError(a.errors ++ e.errors)
           |            case _          => a
           |        })
           |  }
           |
           |  implicit val writes: Writes[${typeDef.name}] = new Writes[${typeDef.name}] {
           |    override def writes(o: ${typeDef.name}): JsValue = o match {
           |      ${typeDef.subtypes
             .map(subTypeDef => s"""case x: ${subTypeDef.name}   => ${subTypeDef.name}.formats.writes(x)""")
             .mkString("\n    ")}
           |    }
           |  }
          """.stripMargin
      else
        s"""
           |implicit val formats: Format[${typeDef.name}] = Json.format[${typeDef.name}]
           |""".stripMargin

    // -----------------------------------------
    //    CASE CLASS AND OBJECT TEMPLATE
    // -----------------------------------------
    s"""${if (typeDef.isInterface)
         s"""sealed trait ${typeDef.name} {${generateInterfaceMethods(typeDef)}}""".stripMargin
       else
         s"""case class ${typeDef.name}(
         |  $classFields${if (isTopLevel && context.isRecordOutputType)
           s""",
              |  id: Option[String] = None
              |) extends Record${if (typeDef.hasInterfaces) " with " else ""}${generateClassInterfaces(typeDef)} {
              |
              |  override def uniqueKey: Option[String] = ${context.uniqueKey
                .map(key => s"${key._1}.map(${typeDef.name}.uniqueKey)")
                .getOrElse("None")}
              |  override def lookupKeys: Seq[String] = Seq(${context.keys
                .map(key => s"${key._1}.map(${typeDef.name}.${key._2}Key)")
                .mkString(", ")})${if (context.keys.nonEmpty) ".collect{case Some(x) => x}" else ""}
              |  override def withId(id: Option[String]): ${typeDef.name} = copy(id = id)
              |""".stripMargin
         else
           s")${if (typeDef.hasInterfaces) " extends " else ""}${generateClassInterfaces(typeDef)} {"}
         |
         |  ${generateBuilderMethods(typeDef)}
         |}"""}
       |
       |object ${typeDef.name}${if (context.isRecordOutputType) s" extends RecordUtils[${typeDef.name}]" else ""} {
       |  $recordObjectMembersCode
       |  $propertyValidators
       |  $validatorCode
       |  $generatorsCode
       |  $sanitizersCode
       |  $formatsCode
       |  $nestedTypesDefinitions
       |  ${if (isTopLevel) generateCustomObjectDeclaration(context) else ""}
       |}
       |
     """.stripMargin
  }

  private def generateClassInterfaces(typeDef: TypeDefinition): String =
    if (typeDef.interfaces.isEmpty) ""
    else typeDef.interfaces.map(it => s"${it.prefix}${it.name}").distinct.mkString(" with ")

  private def generateClassFields(typeDef: TypeDefinition): String =
    typeDef.definition.properties
      .take(22)
      .map(prop =>
        s"""${if (typeDef.interfaces.exists(_.interfaceMethods.exists(_._1 == prop.name))) "override val " else ""}${prop.name}: ${typeOf(
             prop,
             typeDef.prefix)}""".stripMargin)
      .mkString(",\n  ")

  def generateInterfaceMethods(typeDef: TypeDefinition): String =
    if (!typeDef.isInterface) ""
    else
      typeDef.interfaceMethods.map { case (name, typeOf) => s"def $name: $typeOf" }.mkString("\n ")

  private def generateFieldGenerators(definition: ObjectDefinition, context: Context): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.variableName} <- ${generateValueGenerator(prop, context)}""".stripMargin)
      .mkString("\n    ")

  private def generateGenFieldsInitialization(definition: ObjectDefinition): String =
    definition.properties
      .filter(_.isMandatory)
      .take(22)
      .map(prop => s"""${prop.name} = ${prop.variableName}""".stripMargin)
      .mkString("\n    ", ",\n    ", "\n  ")

  private def generateBuilderMethods(typeDef: TypeDefinition): String =
    s"""${typeDef.definition.properties
         .take(22)
         .map(prop => {
           val propType = typeOf(prop, typeDef.prefix, defaultValue = false)
           s"""  def with${prop.nameWithFirstCharUpper}(${prop.name}: $propType): ${typeDef.name} = copy(${prop.name} = ${prop.name})
           |  def modify${prop.nameWithFirstCharUpper}(pf: PartialFunction[$propType, $propType]): ${typeDef.name} =
           |    if (pf.isDefinedAt(${prop.name})) copy(${prop.name} = pf(${prop.name})) else this"""
         })
         .mkString("\n  ")}""".stripMargin

  private def generateValueGenerator(property: Definition, context: Context, wrapOption: Boolean = true): String = {
    val gen = knownFieldGenerators(property.name)
      .orElse(knownFieldGenerators(property.pathLastPart))
      .getOrElse(property match {
        case s: StringDefinition =>
          s.customGenerator.getOrElse(if (s.enum.isDefined) {
            if (s.enum.get.size == 1) s"""Gen.const("${s.enum.get.head}")"""
            else s"""Gen.oneOf(${context.commonReference(s"Seq(${s.enum.get.mkString("\"", "\",\"", "\"")})")})"""
          } else if (s.pattern.isDefined)
            s"""Generator.regex(${context.commonReference(quoted(s.pattern.get))})"""
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

        case b: BooleanDefinition => "Generator.booleanGen"
        case a: ArrayDefinition   => s"Generator.nonEmptyListOfMaxN(1,${generateValueGenerator(a.item, context)})"
        case o: ObjectDefinition  => s"${o.typeName}.gen"

        case o: OneOfDefinition =>
          if (o.variants.isEmpty) "???"
          else if (o.variants.size == 1) generateValueGenerator(o.variants.head, context)
          else
            o.variants.head match {
              case _: ObjectDefinition => s"${o.typeName}.gen"
              case _ =>
                s"Gen.oneOf[${o.typeName}](${o.variants
                  .map(v => s"${generateValueGenerator(v, context)}.map(_.asInstanceOf[${typeOf(v, "")}])")
                  .mkString(",\n  ")})"
            }
      })

    val genWithConstraints = property match {
      case s: StringDefinition =>
        val withMinLength = s.minLength.map(minLength => s"""$gen.suchThat(_.length>=$minLength)""").getOrElse(gen)
        val withMaxLength =
          s.maxLength.map(maxLength => s"""$withMinLength.suchThat(_.length<=$maxLength)""").getOrElse(withMinLength)
        withMaxLength
      case _ => gen
    }

    if (!property.isMandatory && wrapOption) s"""Generator.optionGen($genWithConstraints)""" else genWithConstraints
  }

  private def generatePropertyValidators(definition: ObjectDefinition, context: Context): String =
    definition.properties
      .take(22)
      .map(prop => generateValueValidator(prop, context, extractProperty = false).map((prop, _)))
      .collect {
        case Some((prop, validator)) =>
          s"""val ${prop.name}Validator: Validator[${typeOf(prop, "", defaultValue = false)}] = $validator""".stripMargin
      }
      .mkString("\n", "\n  ", "\n")

  private def generateObjectValidator(definition: ObjectDefinition, context: Context): String = {
    val propertyValidatorsCalls = definition.properties
      .take(22)
      .map(prop => generateValueValidatorCall(prop, context))
      .collect { case Some(validator) => s"""$validator""".stripMargin }
    val validators =
      if (definition.alternatives.isEmpty) propertyValidatorsCalls
      else
        propertyValidatorsCalls :+ s"""  checkIfOnlyOneSetIsDefined(${definition.alternatives
          .map(_.map(a => {
            definition.properties.find(_.name == a).map(prop => s"_.$a${if (prop.isBoolean) ".asOption" else ""}").get
          }).mkString("Set(", ",", ")"))
          .mkString("Seq(", ",", ")")},"${definition.alternatives
          .map(_.mkString("{", ",", "}"))
          .mkString("[", ",", "]")}")"""
    validators.mkString(",\n  ")
  }

  private def generateValueValidator(
    property: Definition,
    context: Context,
    isMandatory: Boolean = false,
    extractProperty: Boolean = true): Option[String] = {
    val propertyReference = if (extractProperty) s"_.${property.name}" else "_"
    val propertyExtractor = if (extractProperty) s"_.${property.name}" else "identity"
    property match {
      case s: StringDefinition =>
        if (s.enum.isDefined) Some(s"""  check($propertyReference.isOneOf(${context.commonReference(s"Seq(${s.enum.get
          .mkString("\"", "\",\"", "\"")})")}), "Invalid ${property.name}, does not match allowed values")""")
        else if (s.pattern.isDefined)
          Some(s"""  check($propertyReference.matches(${context
            .commonReference(context.commonReference(quoted(s.pattern.get)))}), s${quoted(
            s"Invalid ${property.name}, does not matches regex $${${context.commonReference(quoted(s.pattern.get))}}")})""")
        else if (s.minLength.isDefined && s.maxLength.isDefined)
          Some(
            s"""  check($propertyReference.lengthMinMaxInclusive(${s.minLength.get},${s.maxLength.get}), "Invalid length of ${property.name}, should be between ${s.minLength.get} and ${s.maxLength.get} inclusive")""")
        else if (s.minLength.isDefined)
          Some(
            s"""  check($propertyReference.lengthMin(${s.minLength.get}),"Invalid length of ${property.name}, minimum length should be ${s.minLength.get}")""")
        else if (s.maxLength.isDefined)
          Some(
            s"""  check($propertyReference.lengthMax(${s.maxLength.get}),"Invalid length of ${property.name}, maximum length should be ${s.maxLength.get}")""")
        else None

      case n: NumberDefinition =>
        (n.minimum, n.maximum, n.multipleOf) match {
          case (Some(min), Some(max), mlt) =>
            Some(s"""  check($propertyReference.inRange(BigDecimal($min),BigDecimal($max),${mlt.map(a =>
              s"BigDecimal($a)")}),"Invalid number ${property.name}, must be in range <$min,$max>")""")
          case (None, Some(max), mlt) =>
            Some(s"""  check($propertyReference.lteq(BigDecimal($max),${mlt
              .map(a => s"BigDecimal($a)")}),"Invalid number ${property.name}, must be lower than or equal to $max")""")
          case (Some(min), None, mlt) =>
            Some(s"""  check($propertyReference.gteq(BigDecimal($min),${mlt
              .map(a => s"BigDecimal($a)")}),"Invalid number ${property.name}, must be greater than or equal to $min")""")
          case _ => None
        }

      case a: ArrayDefinition =>
        (a.item match {
          case o: ObjectDefinition => Some(s"""${o.typeName}.validate""")
          case x                   => generateValueValidator(x, context)
        }).map(vv =>
          if (property.isMandatory || isMandatory) s""" checkEach($propertyExtractor, $vv)"""
          else s"""  checkEachIfSome($propertyExtractor, $vv)""")

      case _: ObjectDefinition =>
        if (property.isMandatory || isMandatory)
          Some(s""" checkProperty($propertyExtractor, ${property.typeName}.validate)""")
        else Some(s"""  checkIfSome($propertyExtractor, ${property.typeName}.validate)""")

      case o: OneOfDefinition =>
        if (o.variants.isEmpty) None
        else if (o.variants.size == 1) generateValueValidator(o.variants.head, context, o.isMandatory)
        else
          o.variants.head match {
            case _: ObjectDefinition =>
              if (property.isMandatory || isMandatory)
                Some(s""" checkProperty($propertyExtractor, ${property.typeName}.validate)""")
              else Some(s"""  checkIfSome($propertyExtractor, ${property.typeName}.validate)""")
            case _ =>
              generateValueValidator(o.variants.head, context, o.isMandatory)
          }

      case _: BooleanDefinition => None
    }
  }

  private def generateValueValidatorCall(
    property: Definition,
    context: Context,
    isMandatory: Boolean = false): Option[String] =
    property match {
      case _: BooleanDefinition => None
      case _                    => Some(s"""  checkProperty(_.${property.name}, ${property.name}Validator)""")
    }

  private def generateSanitizerList(definition: ObjectDefinition): String = {
    val simpleSanitizerList = definition.properties
      .filter(p => !(p.isMandatory && p.isPrimitive) && !definition.alternatives.exists(_.contains(p.name)))
      .take(22)
      .map(prop => s"${prop.name}Sanitizer")
    val sanitizerList =
      if (definition.alternatives.isEmpty) simpleSanitizerList
      else
        simpleSanitizerList :+ s"${generateComposedFieldName(definition.alternatives.map(_.head), "Or")}AlternativeSanitizer"
    sanitizerList.mkString(",\n  ")
  }

  private def generateComposedFieldName(parts: Seq[String], sep: String): String =
    (parts.head +: parts.tail.map(p => p.take(1).toUpperCase + p.drop(1))).mkString(sep)

  private def generateSanitizers(definition: ObjectDefinition, context: Context): String = {
    val simpleSanitizers = definition.properties
      .take(22)
      .map(prop =>
        if (prop.isMandatory) {
          if (prop.isPrimitive) ""
          else
            s"""  val ${prop.name}Sanitizer: Update = seed => entity =>
               |    entity.copy(${prop.name} = ${prop match {
                 case o: ObjectDefinition => s"${o.typeName}.sanitize(seed)(entity.${prop.name})"
                 case a: ArrayDefinition if !a.item.isPrimitive =>
                   s"entity.${prop.name}.map(item => ${a.item.typeName}.sanitize(seed)(item))"
                 case o: OneOfDefinition if o.variants.nonEmpty && !o.variants.head.isPrimitive =>
                   if (o.variants.size == 1) s"${o.variants.head.typeName}.sanitize(seed)(entity.${prop.name})"
                   else
                     o.variants
                       .map(v => s"case x:${v.typeName} => ${v.typeName}.sanitize(seed)(x)")
                       .mkString(s"entity.${prop.name} match {\n  ", "\n  ", "\n}")
                 case _ => s"entity.${prop.name}"
               }})
         """.stripMargin
        } else {
          if (prop.isPrimitive)
            s"""  val ${prop.name}Sanitizer: Update = seed => entity =>
               |    entity.copy(${prop.name} = ${prop.name}Validator(entity.${prop.name}).fold(_ => None, _ => entity.${prop.name})
               |      .orElse(Generator.get(${generateValueGenerator(prop, context, wrapOption = false)})(seed)))
           """.stripMargin
          else
            s"""  val ${prop.name}Sanitizer: Update = seed => entity =>
               |    entity.copy(${prop.name} = entity.${prop.name}
               |      .orElse(Generator.get(${generateValueGenerator(prop, context, wrapOption = false)})(seed))${generateSanitizerSuffix(
                 prop)})
           """.stripMargin
      })

    val sanitizers =
      if (definition.alternatives.isEmpty) simpleSanitizers
      else simpleSanitizers :+ generateAlternativeSanitizers(definition, context)
    sanitizers.mkString("\n")
  }

  private def generateAlternativeSanitizers(definition: ObjectDefinition, context: Context): String = {

    val compoundSanitizers = definition.alternatives
      .map(set =>
        generateCompoundSanitizer(definition, set, definition.alternatives.filterNot(_ == set).reduce(_ ++ _), context))
      .mkString("\n", "\n  ", "\n")

    compoundSanitizers + s"""|
                             |  val ${generateComposedFieldName(definition.alternatives.map(_.head), "Or")}AlternativeSanitizer: Update = seed => entity =>
                             |          ${definition.alternatives
                              .map(set =>
                                s"if(entity.${set.head}.isDefined) ${generateComposedFieldName(set.toSeq, "And")}CompoundSanitizer(seed)(entity)")
                              .mkString("\nelse      ")}
                             |          else Generator.get(Gen.chooseNum(0,${definition.alternatives.size - 1}))(seed) match {
                             |      ${definition.alternatives.zipWithIndex
                              .map {
                                case (set, i) =>
                                  s"case ${if (i == definition.alternatives.size - 1) "_" else s"Some($i)"} => ${generateComposedFieldName(set.toSeq, "And")}CompoundSanitizer(seed)(entity)"
                              }
                              .mkString("\n      ")}
                             |    }
                             |""".stripMargin
  }

  private def generateCompoundSanitizer(
    definition: ObjectDefinition,
    included: Set[String],
    excluded: Set[String],
    context: Context): String =
    s"""
       |val ${generateComposedFieldName(included.toSeq, "And")}CompoundSanitizer: Update = seed =>
       |    entity =>
       |      entity.copy(
       |        ${included
         .map(name => {
           definition.properties
             .find(_.name == name)
             .map(
               prop =>
                 if (prop.isBoolean) s"""$name = true"""
                 else
                   s"""$name = entity.$name.orElse(Generator.get(${generateValueGenerator(
                     prop,
                     context,
                     wrapOption = false)})(seed))${generateSanitizerSuffix(prop)}""")
             .getOrElse("")
         })
         .mkString(",\n        ")}
       |       ${excluded
         .map(name => {
           definition.properties
             .find(_.name == name)
             .map(prop =>
               if (prop.isBoolean) s"""$name = false"""
               else s"""$name = None""")
             .getOrElse("")
         })
         .mkString(",\n        ", ",\n        ", "")}  
       |   )""".stripMargin

  private def generateSanitizerSuffix(definition: Definition): String = definition match {
    case a: ArrayDefinition  => s".map(_.map(${a.item.typeName}.sanitize(seed)))"
    case o: ObjectDefinition => s".map(${o.typeName}.sanitize(seed))"
    case o: OneOfDefinition =>
      if (o.variants.isEmpty) ""
      else if (o.variants.size == 1) generateSanitizerSuffix(o.variants.head)
      else
        s""".map(${o.typeName}.sanitize(seed))"""
    case _ => ""
  }

  private def generateCustomObjectDeclaration(context: Context): String =
    context.commonVals
      .map {
        case (value, name) => s"  val $name = $value"
      }
      .mkString("object Common {\n  ", "\n  ", "\n}")

}

trait KnownFieldGenerators {

  val phoneNumber: Regex = "^(.*?)(?:tele)?phonenumber(.*)$".r
  val mobileNumber: Regex = "^(.*?)mobilenumber(.*)$".r
  val faxNumber: Regex = "^(.*?)faxnumber(.*)$".r
  val emailAddress: Regex = "^(.*?)email(?:address)?(.*)$".r
  val addressLine: Regex = "^(.*?)(?:address)?line(1|2|3|4)(.*)$".r
  val postalCode: Regex = "^(.*?)post(?:al)?code(.*)$".r
  val organisationName: Regex = "^(.*?)(?:organisation|company)name(.*)$".r
  val lastName: Regex = "^(.*?)(?:lastname|surname)(.*)$".r
  val firstName: Regex = "^(.*?)firstname(.*)$".r
  val agencyName: Regex = "^(.*?)agen(?:t|cy)name(.*)$".r
  val date: Regex = "^(.*?)date(?:string)?(.*)$".r

  val knownFieldGenerators: String => Option[String] = s =>
    Option(s.toLowerCase match {
      case "safeid"               => "Generator.safeIdGen"
      case "agentreferencenumber" => "Generator.arnGen"
      case "nino"                 => "Generator.ninoNoSpacesGen"
      case "mtdbsa"               => "Generator.mtdbsaGen"
      case "vrn"                  => "Generator.vrnGen"
      case "utr"                  => "Generator.utrGen"
      case "eori"                 => "Generator.eoriGen"
      case date(a, b)             => s"Generator.dateYYYYMMDDGen${withPerturb(a, b)}"
      case phoneNumber(a, b) =>
        s"Generator.ukPhoneNumber${withPerturb(a, b)}"
      case mobileNumber(a, b) =>
        s"Generator.ukPhoneNumber${withPerturb(a, b)}"
      case faxNumber(a, b) =>
        s"Generator.ukPhoneNumber${withPerturb(a, b)}"
      case emailAddress(a, b) =>
        s"Generator.emailGen${withPerturb(a, b)}"
      case addressLine(a, n, b)   => s"Generator.address4Lines35Gen.map(_.line$n)${withPerturb(a, b)}"
      case postalCode(a, b)       => s"Generator.postcode${withPerturb(a, b)}"
      case "tradingname"          => "Generator.tradingNameGen"
      case organisationName(a, b) => s"Generator.company${withPerturb(a, b)}"
      case lastName(a, b)         => s"Generator.surname${withPerturb(a, b)}"
      case firstName(a, b)        => s"Generator.forename()${withPerturb(a, b)}"
      case "middlename"           => s"Generator.forename()${withPerturb("middle")}"
      case agencyName(a, b)       => s"UserGenerator.agencyNameGen${withPerturb(a, b)}.map(_.take(40))"
      case _                      => null
    })

  def withPerturb(p: String*): String =
    if (p.forall(_.isEmpty)) "" else s""".variant("${p.filterNot(_.isEmpty).mkString("-")}")"""
}
