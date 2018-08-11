package io.estatico.newtype.macros

import io.estatico.newtype.arrays.NewTypeArray
import scala.reflect.ClassTag
import scala.reflect.macros.blackbox

trait NewTypeCompatMacros {

  val c: blackbox.Context

  import c.universe._

  def opsClsParent: Symbol = typeOf[AnyVal].typeSymbol

  val emitTrait: Boolean = false

  def generateExtra(
    clsDef: ClassDef, modDef: ModuleDef, valDef: ValDef,
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef],
    subtype: Boolean
  ): List[Tree] = {
    val Repr = tq"${valDef.tpt}"
    val Type = if (tparamNames.isEmpty) tq"${clsDef.name}" else tq"${clsDef.name}[..$tparamNames]"
    val genNewTypeArray = summonImplicit(tq"$ClassTagCls[$Repr]") match {
      case Some(Typed(ct, _)) =>
        if (tparamNames.isEmpty) {
          q"""implicit val newtypeArray: $NewTypeArrayObj.Aux[$Type, $Repr] =
                $NewTypeArrayObj.unsafeDerive[$Type, $Repr]($ct)"""
        } else {
          q"""implicit def newtypeArray[..$tparamsNoVar]: $NewTypeArrayObj.Aux[$Type, $Repr] =
                __newtypeArray.asInstanceOf[$NewTypeArrayObj.Aux[$Type, $Repr]]
              private val __newtypeArray = $NewTypeArrayObj.unsafeDerive[Any, $Repr]($ct)"""
        }
      case _ =>
        q"""implicit def newtypeArray[..$tparamsNoVar](
            implicit ct: $ClassTagCls[$Repr]
          ): $NewTypeArrayObj.Aux[$Type, $Repr] =
            $NewTypeArrayObj.unsafeDerive[$Type, $Repr](ct)"""
    }
    List(genNewTypeArray)
  }

  private val ClassTagCls = typeOf[ClassTag[Nothing]].typeSymbol
  private val NewTypeArrayCls = typeOf[NewTypeArray[Nothing]].typeSymbol
  private val NewTypeArrayObj = NewTypeArrayCls.companion

  /** Return the implicit value, if exists, for the given type `tpt`. */
  private def summonImplicit(tpt: Tree): Option[Tree] = {
    val typeResult = c.typecheck(tpt, c.TYPEmode, silent = true)
    if (typeResult.isEmpty) None else {
      val implicitResult = c.inferImplicitValue(typeResult.tpe)
      if (implicitResult.isEmpty) None else Some(implicitResult)
    }
  }
}
