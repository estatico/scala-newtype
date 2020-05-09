package io.estatico.newtype.macros

import scala.reflect.macros.blackbox

private[macros] trait ExtraDefs {
  val c: blackbox.Context

  import c.universe._

  val enableHKTs: List[Tree] = Nil

}
