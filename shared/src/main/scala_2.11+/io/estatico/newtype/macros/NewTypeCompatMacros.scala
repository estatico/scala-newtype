package io.estatico.newtype.macros

import scala.reflect.macros.blackbox

trait NewTypeCompatMacros {

  val c: blackbox.Context

  import c.universe._

  def opsClsParent: Symbol = typeOf[AnyVal].typeSymbol

  val emitTrait: Boolean = false
}
