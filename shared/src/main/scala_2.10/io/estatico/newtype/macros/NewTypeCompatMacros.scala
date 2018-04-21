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
}

