package io.estatico.newtype.ops

import io.estatico.newtype.NewType

final class NewTypeOps[Tag, R](private val newtype: NewType.Aux[Tag, R]) extends AnyVal {
  type Type = NewType.Aux[Tag, R]
  def repr: R = newtype.asInstanceOf[R]
  def withRepr(f: R => R): Type = f(newtype.asInstanceOf[R]).asInstanceOf[Type]
}

trait ToNewTypeOps {
  implicit def toNewTypeOps[T, R](x: NewType.Aux[T, R]): NewTypeOps[T, R] = new NewTypeOps[T, R](x)
}
