package io.estatico.newtype.macros

import scala.annotation.compileTimeOnly

sealed trait deriving

/**
 * Syntax support for defining newtypes with derived type class instances generated in their companion object.
 * Heavily inspired by https://github.com/oleg-py/enumeratum-macro
 */
object deriving extends deriving {
  def evicted: Nothing =
    sys.error("Runtime application of @newtype DSL is not possible")

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_]](implicit sig: T1): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_]](implicit sig: T2): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_]](implicit sig: T3): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_]](implicit sig: T4): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_], M5[_]](implicit sig: T5): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_], M5[_], M6[_]](implicit sig: T6): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_], M5[_], M6[_], M7[_]](implicit sig: T7): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_], M5[_], M6[_], M7[_], M8[_]](implicit sig: T8): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_], M2[_], M3[_], M4[_], M5[_], M6[_], M7[_], M8[_], M9[_]](implicit sig: T9): deriving = evicted

  // Dummies used to give deriving[...] different erased signatures
  // Which is required for scalac to compile above definitions
  trait T1; trait T2; trait T3; trait T4; trait T5
  trait T6; trait T7; trait T8; trait T9

  // Defining them here will make IDEs like IntelliJ not complain about missing implicit values
  implicit val t1: T1 = null
  implicit val t2: T2 = null
  implicit val t3: T3 = null
  implicit val t4: T4 = null
  implicit val t5: T5 = null
  implicit val t6: T6 = null
  implicit val t7: T7 = null
  implicit val t8: T8 = null
  implicit val t9: T9 = null
}
