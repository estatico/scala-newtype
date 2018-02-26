package io.estatico.newtype.macros

import scala.annotation.StaticAnnotation

class newtype extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro NewTypeMacros.newtypeAnnotation
}
