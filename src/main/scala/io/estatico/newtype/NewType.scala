package io.estatico.newtype

import io.estatico.newtype.ops.NewTypeOps

trait NewType[R] { self =>
  final type Type = NewType.Aux[Tag, Repr]
  final type Repr = R
  final abstract class Tag
}

object NewType {
  // The `with Tagged[T]` is a trick to force the compiler to look in the user's
  // object which extends NewType[R] for implicits.
  type Aux[T, R] = ({ type Tag = T ; type Repr = R }) with Tagged[T]
  trait Tagged[T]
}

trait DefaultNewType[R]
  extends NewTypeApplyM[R]
  with NewTypeDeriving[R]
  with NewTypeAutoOps[R]

trait NewTypeAutoOps[R] extends NewType[R] {
  implicit def toNewTypeOps(x: Type): NewTypeOps[Tag, Repr] = new NewTypeOps[Tag, Repr](x)
}

trait NewTypeCasts[R] extends NewType[R] {
  @inline protected def cast(x: Repr): Type = x.asInstanceOf[Type]
  @inline protected def castM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeApply[R] extends NewType[R] {
  /** Convert a `Repr` to a `Type`. */
  @inline def apply(x: Repr): Type = x.asInstanceOf[Type]
}

trait NewTypeApplyM[R] extends NewTypeApply[R] {
  /** Convert an `M[Repr]` to a `M[Type]`. */
  @inline def applyM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeDeriving[R] extends NewType[R] {
  /** Derive an instance of type class `T` if one exists for `Repr`.  */
  def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
}
