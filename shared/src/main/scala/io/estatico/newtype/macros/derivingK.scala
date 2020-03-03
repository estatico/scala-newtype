package io.estatico.newtype.macros

import scala.annotation.compileTimeOnly

sealed trait derivingK

/**
 * Higher-kinded version of [[deriving]].
 */
object derivingK extends derivingK {
  def evicted: Nothing =
    sys.error("Runtime application of @newtype DSL is not possible")

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]]](implicit sig: T1): derivingK = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]]](implicit sig: T2): derivingK = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]]](implicit sig: T3): derivingK = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]]](implicit sig: deriving.T4): derivingK = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]], M5[_[_]]](implicit sig: T5): derivingK = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]], M5[_[_]], M6[_[_]]](implicit sig: T6): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]], M5[_[_]], M6[_[_]], M7[_[_]]](implicit sig: T7): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]], M5[_[_]], M6[_[_]], M7[_[_]], M8[_[_]]](implicit sig: T8): deriving = evicted

  @compileTimeOnly("Part of @newtype DSL that should not be used directly")
  def apply[M1[_[_]], M2[_[_]], M3[_[_]], M4[_[_]], M5[_[_]], M6[_[_]], M7[_[_]], M8[_[_]], M9[_[_]]](implicit sig: T9): deriving = evicted

  // Dummies used to give derivingK[...] different erased signatures
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


