package io.estatico.newtype.macros

import scala.annotation.StaticAnnotation

class newsubtype(
  debug: Boolean = false,
  debugRaw: Boolean = false
) extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro NewTypeMacros.newsubtypeAnnotation
}

