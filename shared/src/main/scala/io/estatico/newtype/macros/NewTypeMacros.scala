package io.estatico.newtype.macros

import io.estatico.newtype.Coercible
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox

private[macros] class NewTypeMacros(val c: blackbox.Context) {

  import c.universe._

  def newtypeAnnotation(annottees: Tree*): Tree =
    runAnnotation(subtype = false, annottees)

  def newsubtypeAnnotation(annottees: Tree*): Tree =
    runAnnotation(subtype = true, annottees)

  def runAnnotation(subtype: Boolean, annottees: Seq[Tree]): Tree = {
    val (name, result) = annottees match {
      case List(clsDef: ClassDef) =>
        (clsDef.name, runClass(clsDef, subtype))
      case List(clsDef: ClassDef, modDef: ModuleDef) =>
        (clsDef.name, runClassWithObj(clsDef, modDef, subtype))
      case _ =>
        fail(s"Unsupported @$macroName definition")
    }
    if (debug) scala.Predef.println(s"Expanded @$macroName $name:\n" + show(result))
    if (debugRaw) scala.Predef.println(s"Expanded @$macroName $name (raw):\n" + showRaw(result))
    result
  }

  val CoercibleCls = typeOf[Coercible[Nothing, Nothing]].typeSymbol
  val CoercibleObj = CoercibleCls.companion
  val ClassTagCls = typeOf[ClassTag[Nothing]].typeSymbol
  val ClassTagObj = ClassTagCls.companion
  val ObjectCls = typeOf[Object].typeSymbol

  // We need to know if the newtype is defined in an object so we can report
  // an error message if methods are defined on it (otherwise, the user will
  // get a cryptic error of 'value class may not be a member of another class'
  // due to our generated extension methods.
  val isDefinedInObject = c.internal.enclosingOwner.isModuleClass

  val macroName: Tree = {
    c.prefix.tree match {
      case Apply(Select(New(name), _), _) => name
      case _ => c.abort(c.enclosingPosition, "Unexpected macro application")
    }
  }

  val (optimizeOps, unapply, debug, debugRaw) = c.prefix.tree match {
    case q"new ${`macroName`}(..$args)" =>
      (
        args.collectFirst { case q"optimizeOps = false" => }.isEmpty,
        args.collectFirst { case q"unapply = true" => }.isDefined,
        args.collectFirst { case q"debug = true" => }.isDefined,
        args.collectFirst { case q"debugRaw = true" => }.isDefined
      )
    case _ => (true, false, false, false)
  }

  def fail(msg: String) = c.abort(c.enclosingPosition, msg)

  def runClass(clsDef: ClassDef, subtype: Boolean) = {
    runClassWithObj(clsDef, q"object ${clsDef.name.toTermName}".asInstanceOf[ModuleDef], subtype)
  }

  def runClassWithObj(clsDef: ClassDef, modDef: ModuleDef, subtype: Boolean) = {
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
    generateNewType(clsDef, modDef, valDef, tparamsNoVar, tparamNames, tparamsWild, subtype)
  }

  def mkBaseTypeDef(clsDef: ClassDef, reprType: Tree, subtype: Boolean) = {
    val refinementName = TypeName(s"__${clsDef.name.decodedName.toString}__newtype")
    (clsDef.tparams, subtype) match {
      case (_, false)      =>  q"type Base             = _root_.scala.Any { type $refinementName } "
      case (Nil, true)     =>  q"type Base             = $reprType"
      case (tparams, true) =>  q"type Base[..$tparams] = $reprType"
    }
  }

  def mkTypeTypeDef(clsDef: ClassDef, tparamsNames: List[TypeName], subtype: Boolean) =
    (clsDef.tparams, subtype) match {
      case (Nil, false) =>     q"type Type             <: Base with Tag"
      case (tparams, false) => q"type Type[..$tparams] <: Base with Tag[..$tparamsNames]"
      case (Nil, true)  =>     q"type Type             <: Base with Tag"
      case (tparams, true) =>  q"type Type[..$tparams] <: Base[..$tparamsNames] with Tag[..$tparamsNames]"
    }

  def generateNewType(
    clsDef: ClassDef, modDef: ModuleDef, valDef: ValDef,
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef],
    subtype: Boolean
  ): Tree = {
    val ModuleDef(objMods, objName, Template(objParents, objSelf, objDefs)) = modDef
    val typeName = clsDef.name
    val clsName = clsDef.name.decodedName
    val reprType = valDef.tpt
    val typesTraitName = TypeName(s"${clsName.decodedName}__Types")
    val tparams = clsDef.tparams
    val companionExtraDefs =
      maybeGenerateApplyMethod(clsDef, valDef, tparamsNoVar, tparamNames) :::
      maybeGenerateUnapplyMethod(clsDef, valDef, tparamsNoVar, tparamNames) :::
      maybeGenerateOpsDef(clsDef, valDef, tparamsNoVar, tparamNames) :::
      generateCoercibleInstances(tparamsNoVar, tparamNames, tparamsWild) :::
      generateDerivingMethods(tparamsNoVar, tparamNames, tparamsWild)

    // Note that we use an abstract type alias
    // `type Type <: Base with Tag` and not `type Type = ...` to prevent
    // scalac automatically expanding the type alias.

    val baseTypeDef = mkBaseTypeDef(clsDef, reprType, subtype)
    val typeTypeDef = mkTypeTypeDef(clsDef, tparamNames, subtype)
    val enableImplicits = List( q"import _root_.scala.language.implicitConversions" )

    val newtypeObjParents = objParents
    val newtypeObjDef = ModuleDef(
      objMods, objName, Template(newtypeObjParents, objSelf, objDefs ++ enableImplicits ++ companionExtraDefs ++ Seq(
        q"type Repr[..$tparams] = $reprType",
        baseTypeDef,
        q"trait Tag[..$tparams] extends _root_.scala.Any",
        typeTypeDef
      ))
    )

    q"""
      type $typeName[..$tparams] = $objName.Type[..$tparamNames]
      $newtypeObjDef
    """
  }

  def maybeGenerateApplyMethod(
    clsDef: ClassDef, valDef: ValDef, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): List[Tree] = {
    if (!clsDef.mods.hasFlag(Flag.CASE)) Nil else List(
      if (tparamsNoVar.isEmpty) {
        q"def apply(${valDef.name}: ${valDef.tpt}): ${clsDef.name} = ${valDef.name}.asInstanceOf[${clsDef.name}]"
      } else {
        q"""
          def apply[..$tparamsNoVar](${valDef.name}: ${valDef.tpt}): ${clsDef.name}[..$tparamNames] =
            ${valDef.name}.asInstanceOf[${clsDef.name}[..$tparamNames]]
        """
      }
    )
  }

  def maybeGenerateUnapplyMethod(
    clsDef: ClassDef, valDef: ValDef, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): List[Tree] = {
    if (!unapply) Nil else {
      // Note that our unapply method should Some since its isEmpty/get is constant.
      List(
        if (tparamsNoVar.isEmpty) {
          q"""def unapply(x: ${clsDef.name}): Some[${valDef.tpt}] =
              Some(x.asInstanceOf[${valDef.tpt}])"""
        } else {
          q"""def unapply[..$tparamsNoVar](x: ${clsDef.name}[..$tparamNames]): Some[${valDef.tpt}] =
              Some(x.asInstanceOf[${valDef.tpt}])"""
        }
      )
    }
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
    } else if (!isDefinedInObject && optimizeOps) {
      c.abort(valDef.pos, List(
        "Fields can only be defined for newtypes defined in an object",
        s"Consider defining as: private val ${valDef.name.decodedName}"
      ).mkString(" "))
    } else {
      Some(q"def ${valDef.name}: ${valDef.tpt} = $$this$$.asInstanceOf[${valDef.tpt}]")
    }
  }

  def maybeGenerateOpsDef(
    clsDef: ClassDef, valDef: ValDef, tparamsNoVar: List[TypeDef], tparamNames: List[TypeName]
  ): List[Tree] = {
    val extensionMethods =
      maybeGenerateValMethod(clsDef, valDef).toList ++ getInstanceMethods(clsDef)

    if (extensionMethods.isEmpty) {
      Nil
    } else {
      val parent = if (optimizeOps) typeOf[AnyVal].typeSymbol else typeOf[AnyRef].typeSymbol
      // Note that we generate the implicit class for extension methods and the
      // implicit def to convert `this` used in the Ops to our newtype value.
      if (clsDef.tparams.isEmpty) {
        List(
          q"""
              implicit final class Ops$$newtype(val $$this$$: Type) extends $parent {
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
              ) extends $parent {
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
      List(q"def deriving[TC[_]](implicit ev: TC[Repr]): TC[Type] = ev.asInstanceOf[TC[Type]]")
    } else {
      // Creating a fresh type name so it doesn't collide with the tparams passed in.
      val TC = TypeName(c.freshName("TC"))
      List(
        q"""
          def deriving[$TC[_], ..$tparamsNoVar](
            implicit ev: $TC[Repr[..$tparamNames]]
          ): $TC[Type[..$tparamNames]] = ev.asInstanceOf[$TC[Type[..$tparamNames]]]
        """,
        q"""
          def derivingK[$TC[_[..$tparamsWild]]](implicit ev: $TC[Repr]): $TC[Type] =
            ev.asInstanceOf[$TC[Type]]
        """
      )

    }
  }

  def generateCoercibleInstances(
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef]
  ): List[Tree] = {
    if (tparamsNoVar.isEmpty) List(
      q"@_root_.scala.inline implicit def unsafeWrap: $CoercibleCls[Repr, Type] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeUnwrap: $CoercibleCls[Type, Repr] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeWrapM[M[_]]: $CoercibleCls[M[Repr], M[Type]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeUnwrapM[M[_]]: $CoercibleCls[M[Type], M[Repr]] = $CoercibleObj.instance",
      // Avoid ClassCastException with Array types by prohibiting Array coercing.
      q"@_root_.scala.inline implicit def cannotWrapArrayAmbiguous1:   $CoercibleCls[_root_.scala.Array[Repr], _root_.scala.Array[Type]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotWrapArrayAmbiguous2:   $CoercibleCls[_root_.scala.Array[Repr], _root_.scala.Array[Type]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotUnwrapArrayAmbiguous1: $CoercibleCls[_root_.scala.Array[Type], _root_.scala.Array[Repr]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotUnwrapArrayAmbiguous2: $CoercibleCls[_root_.scala.Array[Type], _root_.scala.Array[Repr]] = $CoercibleObj.instance"
    ) else List(
      q"@_root_.scala.inline implicit def unsafeWrap[..$tparamsNoVar]: $CoercibleCls[Repr[..$tparamNames], Type[..$tparamNames]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeUnwrap[..$tparamsNoVar]: $CoercibleCls[Type[..$tparamNames], Repr[..$tparamNames]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeWrapM[M[_], ..$tparamsNoVar]: $CoercibleCls[M[Repr[..$tparamNames]], M[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeUnwrapM[M[_], ..$tparamsNoVar]: $CoercibleCls[M[Type[..$tparamNames]], M[Repr[..$tparamNames]]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeWrapK[T[_[..$tparamsNoVar]]]: $CoercibleCls[T[Repr], T[Type]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def unsafeUnwrapK[T[_[..$tparamsNoVar]]]: $CoercibleCls[T[Type], T[Repr]] = $CoercibleObj.instance",
      // Avoid ClassCastException with Array types by prohibiting Array coercing.
      q"@_root_.scala.inline implicit def cannotWrapArrayAmbiguous1[..$tparamsNoVar]:   $CoercibleCls[_root_.scala.Array[Repr[..$tparamNames]], _root_.scala.Array[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotWrapArrayAmbiguous2[..$tparamsNoVar]:   $CoercibleCls[_root_.scala.Array[Repr[..$tparamNames]], _root_.scala.Array[Type[..$tparamNames]]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotUnwrapArrayAmbiguous1[..$tparamsNoVar]: $CoercibleCls[_root_.scala.Array[Type[..$tparamNames]], _root_.scala.Array[Repr[..$tparamNames]]] = $CoercibleObj.instance",
      q"@_root_.scala.inline implicit def cannotUnwrapArrayAmbiguous2[..$tparamsNoVar]: $CoercibleCls[_root_.scala.Array[Type[..$tparamNames]], _root_.scala.Array[Repr[..$tparamNames]]] = $CoercibleObj.instance"
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
    if (res.nonEmpty && !isDefinedInObject && optimizeOps) {
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
}
