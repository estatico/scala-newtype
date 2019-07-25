package io.estatico.newtype
package ops

final class NewTypeOps[B, T, R](
  private val self: BaseNewType.Aux[B, T, R]
) extends AnyVal {
  type Type = BaseNewType.Aux[B, T, R]
  def repr: R = self.asInstanceOf[R]
  def withRepr(f: R => R): Type = f(self.asInstanceOf[R]).asInstanceOf[Type]
}

trait ToNewTypeOps {
  implicit def toNewTypeOps[B, T, R](
    x: BaseNewType.Aux[B, T, R]
  ): NewTypeOps[B, T, R] = new NewTypeOps[B, T, R](x)
}
