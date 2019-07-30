package io.estatico.newtype.macros

import scala.reflect.macros.blackbox

trait NewTypeCompatMacros {

  val c: blackbox.Context

  import c.universe._

  /**
    * In scala 2.10 we can't reliably use AnyVal due to the
    * way it combines == methods. For instance -
    * {{{
    *   trait Tag
    *   final class Ops(val x: Int with Tag) extends AnyVal
    * }}}
    * doesn't work in Scala 2.10 -
    * {{{
    *   error: ambiguous reference to overloaded definition,
    *   both method == in class Object of type (x: AnyRef)Boolean
    *   and  method == in class Int of type (x: Double)Boolean
    *   match argument types (Int with Tag) and expected result type Boolean
    * }}}
    * @return
    */
  def opsClsParent: Symbol = typeOf[AnyRef].typeSymbol

  /**
    * Scala 2.10 doesn't support objects having abstract type
    * members, so we have to use some indirection by defining the
    * abstract type in a trait then having the companion object extend
    * the trait. See https://github.com/scala/bug/issues/10750
    */
  val emitTrait: Boolean = true

  /** scala 2.10 has problems with NewTypeArray, so we'll only generate it for 2.11+ */
  def generateExtra(
    clsDef: ClassDef, modDef: ModuleDef, valDef: ValDef,
    tparamsNoVar: List[TypeDef], tparamNames: List[TypeName], tparamsWild: List[TypeDef],
    subtype: Boolean
  ): List[Tree] = Nil
}
