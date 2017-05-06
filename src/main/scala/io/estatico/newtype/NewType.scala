package io.estatico.newtype

import io.estatico.newtype.ops.NewTypeOps

trait NewType extends BaseNewType { self =>
  type Base = { type Repr = self.Repr }
}

object NewType {

  trait For[R] extends NewType {
    final type Repr = R
  }

  trait Default[R] extends For[R] with NewTypeExtras
}

trait NewTypeAutoOps extends BaseNewType {
  implicit def toNewTypeOps(
    x: Type
  ): NewTypeOps[Type, Tag, Repr] = new NewTypeOps[Type, Tag, Repr](x)
}

trait NewTypeCasts extends BaseNewType {
  @inline protected def cast(x: Repr): Type = x.asInstanceOf[Type]
  @inline protected def castM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeApply extends BaseNewType {
  /** Convert a `Repr` to a `Type`. */
  @inline def apply(x: Repr): Type = x.asInstanceOf[Type]
}

trait NewTypeApplyM extends NewTypeApply {
  /** Convert an `M[Repr]` to a `M[Type]`. */
  @inline def applyM[M[_]](mx: M[Repr]): M[Type] = mx.asInstanceOf[M[Type]]
}

trait NewTypeDeriving extends BaseNewType {
  /** Derive an instance of type class `T` if one exists for `Repr`.  */
  def deriving[T[_]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
}

trait NewTypeExtras
  extends NewTypeApplyM
  with NewTypeDeriving
  with NewTypeAutoOps
