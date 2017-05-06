package io.estatico.newtype

import io.estatico.newtype.ops.NewTypeOps

trait NewType {
  final type Type = NewType.Aux[Tag, Repr]
  type Repr
  trait Tag
}

object NewType {

  /**
   * The `with Tagged[T]` is a trick to force the compiler to look in the user's
   * object which extends NewType[R] for implicits.
   */
  type Aux[T, R] = ({ type Repr = R }) with Tagged[T]
  trait Tagged[T]

  trait For[R] extends NewType {
    final type Repr = R
  }

  trait Default[R] extends For[R] with NewTypeExtras
}

trait NewTypeAutoOps extends NewType {
  implicit def toNewTypeOps(x: Type): NewTypeOps[Tag, Repr] = new NewTypeOps[Tag, Repr](x)
}

trait NewTypeCasts extends NewType {
  @inline protected def cast(x: Repr): Type = x.asInstanceOf[Type]
  @inline protected def castM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeApply extends NewType {
  /** Convert a `Repr` to a `Type`. */
  @inline def apply(x: Repr): Type = x.asInstanceOf[Type]
}

trait NewTypeApplyM extends NewTypeApply {
  /** Convert an `M[Repr]` to a `M[Type]`. */
  @inline def applyM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeDeriving extends NewType {
  /** Derive an instance of type class `T` if one exists for `Repr`.  */
  def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
}

trait NewTypeExtras
  extends NewTypeApplyM
  with NewTypeDeriving
  with NewTypeAutoOps
