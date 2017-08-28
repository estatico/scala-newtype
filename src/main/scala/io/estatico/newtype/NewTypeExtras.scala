package io.estatico.newtype

import io.estatico.newtype.ops.NewTypeOps

object NewTypeExtras {

  trait All
    extends ApplyM
      with Deriving
      with AutoOps

  trait AutoOps extends BaseNewType {
    implicit def toNewTypeOps(
      x: Type
    ): NewTypeOps[Type, Tag, Repr] = new NewTypeOps[Type, Tag, Repr](x)
  }

  trait Apply extends BaseNewType {
    /** Convert a `Repr` to a `Type`. */
    @inline final def apply(x: Repr): Type = x.asInstanceOf[Type]
  }

  trait ApplyM extends Apply {
    /** Convert an `M[Repr]` to a `M[Type]`. */
    @inline final def applyM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
  }

  trait Deriving extends BaseNewType {
    /** Derive an instance of type class `T` if one exists for `Repr`.  */
    def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
  }
}
