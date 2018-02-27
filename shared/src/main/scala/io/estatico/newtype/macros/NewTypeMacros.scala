package io.estatico.newtype.macros

import io.estatico.newtype.Coercible
import scala.reflect.macros.blackbox

//noinspection TypeAnnotation
@macrocompat.bundle
private[macros] class NewTypeMacros(val c: blackbox.Context) {

  import c.universe._

  def newtypeAnnotation(annottees: Tree*): Tree = annottees match {
    case List(clsDef: ClassDef) => runClass(clsDef)
    case List(clsDef: ClassDef, modDef: ModuleDef) => runClassWithObj(clsDef, modDef)
    case _ => fail("Unsupported newtype definition")
  }

  // Support Flag values which are not available in Scala 2.10
  implicit final class FlagSupportOps(val repr: Flag.type) {
    def CASEACCESSOR = scala.reflect.internal.Flags.CASEACCESSOR.toLong.asInstanceOf[FlagSet]
    def PARAMACCESSOR = scala.reflect.internal.Flags.PARAMACCESSOR.toLong.asInstanceOf[FlagSet]
  }

  val CoercibleCls = typeOf[Coercible[Nothing, Nothing]].typeSymbol
  val CoercibleObj = CoercibleCls.companion

  // We need to know if the newtype is defined in an object so we can report
  // an error message if methods are defined on it (otherwise, the user will
  // get a cryptic error of 'value class may not be a member of another class'
  // due to our generated extension methods.
  val isDefinedInObject = c.internal.enclosingOwner.isModuleClass

  def fail(msg: String) = c.abort(c.enclosingPosition, msg)

  def runClass(clsDef: ClassDef) = {
    runClassWithObj(clsDef, q"object ${clsDef.name.toTermName}".asInstanceOf[ModuleDef])
  }

  def runClassWithObj(clsDef: ClassDef, modDef: ModuleDef) = {
    val valDef = extractConstructorValDef(getConstructor(clsDef.impl.body))
    // Converts [F[_], A] to [F, A]; needed for applying the defined type params.
    val tparamNames: List[TypeName] = clsDef.tparams.map(_.name)
    // Type params with variance removed for building methods.
    val tparamsNoVar: List[TypeDef] = clsDef.tparams.map(td =>
      TypeDef(Modifiers(Flag.PARAM), td.name, td.tparams, td.rhs)
    )
    val tparamsWild = tparamsNoVar.map {
      case TypeDef(mods, _, args, tree) => TypeDef(mods, typeNames.WILDCARD, args, tree)
    }
    // Ensure we're not trying to inherit from anything.
    validateParents(clsDef.impl.parents)
    // Build the type and object definitions.
    generateNewType(clsDef, modDef, valDef, tparamsNoVar, tparamNames, tparamsWild)
  }

  def generateNewType(
    clsDef: ClassDef, modDef: ModuleDef, valDef: ValDef,
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef]
  ): Tree = {
    val q"object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf => ..$objDefs }" = modDef
    val typeName = clsDef.name
    val tparams = clsDef.tparams
    val clsName = clsDef.name.decodedName
    val baseRefinementName = TypeName(clsName + "$newtype")
    val classTagName = TermName(clsName + "$classTag")
    val companionExtraDefs =
      generateClassTag(classTagName, tparamsNoVar, tparamNames) ::
        maybeGenerateApplyMethod(clsDef, valDef, tparamsNoVar, tparamNames) :::
        maybeGenerateOpsDef(clsDef, valDef, tparamsNoVar, tparamNames) :::
        generateCoercibleInstances(tparamsNoVar, tparamNames, tparamsWild) :::
        generateDerivingMethods(tparamsNoVar, tparamNames, tparamsWild)

    if (tparams.isEmpty) {
      q"""
          type $typeName = $objName.Type
          object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
            ..$objDefs
            type Repr = ${valDef.tpt}
            type Base = { type $baseRefinementName }
            trait Tag
            type Type <: Base with Tag
            ..$companionExtraDefs
          }
        """
    } else {
      q"""
          type $typeName[..$tparams] = ${typeName.toTermName}.Type[..$tparamNames]
          object $objName extends { ..$objEarlyDefs } with ..$objParents { $objSelf =>
            ..$objDefs
            type Repr[..$tparams] = ${valDef.tpt}
            type Base = { type $baseRefinementName }
            trait Tag[..$tparams]
            type Type[..$tparams] <: Base with Tag[..$tparamNames]
            ..$companionExtraDefs
          }
        """
    }
  }

  def maybeGenerateApplyMethod(
    clsDef: ClassDef, valDef: ValDef, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): List[Tree] = {
    if (!clsDef.mods.hasFlag(Flag.CASE)) Nil else List(
      if (tparamsNoVar.isEmpty) {
        q"def apply(${valDef.name}: ${valDef.tpt}): Type = ${valDef.name}.asInstanceOf[Type]"
      } else {
        q"""
            def apply[..$tparamsNoVar](${valDef.name}: ${valDef.tpt}): Type[..$tparamNames] =
              ${valDef.name}.asInstanceOf[Type[..$tparamNames]]
          """
      }
    )
  }

  // We should expose the constructor argument as an extension method only if
  // it was defined as a public param.
  def shouldGenerateValMethod(clsDef: ClassDef, valDef: ValDef): Boolean = {
    clsDef.impl.body.collectFirst {
      case vd: ValDef
        if (vd.mods.hasFlag(Flag.CASEACCESSOR) || vd.mods.hasFlag(Flag.PARAMACCESSOR))
          && !vd.mods.hasFlag(Flag.PRIVATE)
          && vd.name == valDef.name => ()
    }.isDefined
  }

  def maybeGenerateValMethod(
    clsDef: ClassDef, valDef: ValDef
  ): Option[Tree] = {
    if (!shouldGenerateValMethod(clsDef, valDef)) {
      None
    } else if (!isDefinedInObject) {
      c.abort(valDef.pos, s"""
        |Fields can only be defined for newtypes defined in an object
        |Consider defining as: private val ${valDef.name.decodedName}
      """.trim.stripMargin)
    } else {
      Some(q"def ${valDef.name}: ${valDef.tpt} = $$this$$.asInstanceOf[${valDef.tpt}]")
    }
  }

  def maybeGenerateOpsDef(
    clsDef: ClassDef, valDef: ValDef, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): List[Tree] = {
    val extensionMethods =
      maybeGenerateValMethod(clsDef, valDef) ++ getInstanceMethods(clsDef)

    if (extensionMethods.isEmpty) {
      Nil
    } else {
      // Note that we generate the implicit class for extension methods and the
      // implicit def to convert `this` used in the Ops to our newtype value.
      if (clsDef.tparams.isEmpty) {
        List(
          q"""
              implicit final class Ops$$newtype(val $$this$$: Type) extends AnyVal {
                ..$extensionMethods
              }
            """,
          q"implicit def opsThis(x: Ops$$newtype): Type = x.$$this$$"
        )
      } else {
        List(
          q"""
              implicit final class Ops$$newtype[..${clsDef.tparams}](
                val $$this$$: Type[..$tparamNames]
              ) extends AnyVal {
                ..$extensionMethods
              }
            """,
          q"""
              implicit def opsThis[..$tparamsNoVar](
                x: Ops$$newtype[..$tparamNames]
              ): Type[..$tparamNames] = x.$$this$$
            """
        )
      }
    }
  }

  def generateDerivingMethods(
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef]
  ): List[Tree] = {
    if (tparamsNoVar.isEmpty) {
      List(q"def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]")
    } else {
      List(
        q"""
          def deriving[T[_], ..$tparamsNoVar](
            implicit ev: T[Repr[..$tparamNames]]
          ): T[Type[..$tparamNames]] = ev.asInstanceOf[T[Type[..$tparamNames]]]
        """,
        q"""
          def derivingK[T[_[..$tparamsWild]]](implicit ev: T[Repr]): T[Type] =
            ev.asInstanceOf[T[Type]]
        """
      )

    }
  }

  def generateCoercibleInstances(
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef]
  ): List[Tree] = {
    if (tparamsNoVar.isEmpty) List(
      q"@inline implicit def unsafeWrap: $CoercibleCls[Repr, Type] = $CoercibleObj.instance",
      q"@inline implicit def unsafeUnwrap: $CoercibleCls[Type, Repr] = $CoercibleObj.instance",
      q"@inline implicit def unsafeWrapM[M[_]]: $CoercibleCls[M[Repr], M[Type]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeUnwrapM[M[_]]: $CoercibleCls[M[Type], M[Repr]] = $CoercibleObj.instance",
      // Avoid ClassCastException with Array types by prohibiting Array coercing.
      q"@inline implicit def cannotWrapArrayAmbiguous1: $CoercibleCls[Array[Repr], Array[Type]] = $CoercibleObj.instance",
      q"@inline implicit def cannotWrapArrayAmbiguous2: $CoercibleCls[Array[Repr], Array[Type]] = $CoercibleObj.instance",
      q"@inline implicit def cannotUnwrapArrayAmbiguous1: $CoercibleCls[Array[Type], Array[Repr]] = $CoercibleObj.instance",
      q"@inline implicit def cannotUnwrapArrayAmbiguous2: $CoercibleCls[Array[Type], Array[Repr]] = $CoercibleObj.instance"
    ) else List(
      q"@inline implicit def unsafeWrap[..$tparamsNoVar]: $CoercibleCls[Repr[..$tparamNames], Type[..$tparamNames]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeUnwrap[..$tparamsNoVar]: $CoercibleCls[Type[..$tparamNames], Repr[..$tparamNames]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeWrapM[M[_], ..$tparamsNoVar]: $CoercibleCls[M[Repr[..$tparamNames]], M[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeUnwrapM[M[_], ..$tparamsNoVar]: $CoercibleCls[M[Type[..$tparamNames]], M[Repr[..$tparamNames]]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeWrapK[T[_[..$tparamsNoVar]]]: $CoercibleCls[T[Repr], T[Type]] = $CoercibleObj.instance",
      q"@inline implicit def unsafeUnwrapK[T[_[..$tparamsNoVar]]]: $CoercibleCls[T[Type], T[Repr]] = $CoercibleObj.instance",
      // Avoid ClassCastException with Array types by prohibiting Array coercing.
      q"@inline implicit def cannotWrapArrayAmbiguous1[..$tparamsNoVar]: $CoercibleCls[Array[Repr[..$tparamNames]], Array[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@inline implicit def cannotWrapArrayAmbiguous2[..$tparamsNoVar]: $CoercibleCls[Array[Repr[..$tparamNames]], Array[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@inline implicit def cannotUnwrapArrayAmbiguous1[..$tparamsNoVar]: $CoercibleCls[Array[Type[..$tparamNames]], Array[Repr[..$tparamNames]]] = $CoercibleObj.instance",
      q"@inline implicit def cannotUnwrapArrayAmbiguous2[..$tparamsNoVar]: $CoercibleCls[Array[Type[..$tparamNames]], Array[Repr[..$tparamNames]]] = $CoercibleObj.instance"
    )
  }

  def getConstructor(body: List[Tree]): DefDef = body.collectFirst {
    case dd: DefDef if dd.name == termNames.CONSTRUCTOR => dd
  }.getOrElse(fail("Failed to locate constructor"))

  def extractConstructorValDef(ctor: DefDef): ValDef = ctor.vparamss match {
    case List(List(vd)) => vd
    case _ => fail("Unsupported constructor, must have exactly one argument")
  }

  def getInstanceMethods(clsDef: ClassDef): List[DefDef] = {
    val res = clsDef.impl.body.flatMap {
      case vd: ValDef =>
        if (vd.mods.hasFlag(Flag.CASEACCESSOR) || vd.mods.hasFlag(Flag.PARAMACCESSOR)) Nil
        else c.abort(vd.pos, "val definitions not supported, use def instead")
      case dd: DefDef =>
        if (dd.name == termNames.CONSTRUCTOR) Nil else List(dd)
      case x =>
        c.abort(x.pos, s"illegal definition in newtype: $x")
    }
    if (res.nonEmpty && !isDefinedInObject) {
      c.abort(res.head.pos, "Methods can only be defined for newtypes defined in an object")
    }
    res
  }

  def validateParents(parents: List[Tree]): Unit = {
    val ignoredExtends = List(tq"scala.Product", tq"scala.Serializable", tq"scala.AnyRef")
    val unsupported = parents.filterNot(t => ignoredExtends.exists(t.equalsStructure))
    if (unsupported.nonEmpty) {
      fail(s"newtypes do not support inheritance; illegal supertypes: ${unsupported.mkString(", ")}")
    }
  }

  // The erasure of opaque newtypes is always Object.
  def generateClassTag(
    name: TermName, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): Tree = {
    val ClassTag = tq"_root_.scala.reflect.ClassTag"
    val objectClassTag = q"_root_.scala.reflect.ClassTag(classOf[_root_.java.lang.Object])"
    if (tparamsNoVar.isEmpty) q"implicit val $name: $ClassTag[Type] = $objectClassTag"
    else q"implicit def $name[..$tparamsNoVar]: $ClassTag[Type[..$tparamNames]] = $objectClassTag"
  }
}
