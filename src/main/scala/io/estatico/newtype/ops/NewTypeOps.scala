package io.estatico.newtype
package ops

final class NewTypeOps[B, T, R](
  private val newtype: BaseNewType.Aux[B, T, R]
) extends AnyVal {
  type Type = BaseNewType.Aux[B, T, R]
  def repr: R = newtype.asInstanceOf[R]
  def withRepr(f: R => R): Type = f(newtype.asInstanceOf[R]).asInstanceOf[Type]
}

trait ToNewTypeOps {
  implicit def toNewTypeOps[B, T, R](
    x: BaseNewType.Aux[B, T, R]
  ): NewTypeOps[B, T, R] = new NewTypeOps[B, T, R](x)
}
