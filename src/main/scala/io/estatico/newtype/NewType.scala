package io.estatico.newtype

import io.estatico.newtype.ops.NewTypeOps

trait NewType extends BaseNewType { self =>
  type Base = { type Repr = self.Repr }
}

object NewType {
  trait Of[R] extends BaseNewType.Of[R] with NewType
  trait Default[R] extends Of[R] with NewTypeExtras
}

trait NewTypeAutoOps extends BaseNewType {
  implicit def toNewTypeOps(
    x: Type
  ): NewTypeOps[Type, Tag, Repr] = new NewTypeOps[Type, Tag, Repr](x)
}

trait NewTypeApply extends BaseNewType {
  /** Convert a `Repr` to a `Type`. */
  @inline final def apply(x: Repr): Type = x.asInstanceOf[Type]
}

trait NewTypeApplyM extends NewTypeApply {
  /** Convert an `M[Repr]` to a `M[Type]`. */
  @inline final def applyM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeDeriving extends BaseNewType {
  /** Derive an instance of type class `T` if one exists for `Repr`.  */
  def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
}

trait NewTypeExtras
  extends NewTypeApplyM
  with NewTypeDeriving
  with NewTypeAutoOps
