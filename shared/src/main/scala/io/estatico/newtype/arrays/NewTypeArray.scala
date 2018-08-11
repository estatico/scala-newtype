package io.estatico.newtype.arrays

import scala.reflect.ClassTag

/** Type class for building arrays for newtypes. */
trait NewTypeArray[N] {
  type Repr
  def clsTag: ClassTag[Repr]

  final def empty: Array[N] = Array.empty(clsTag).asInstanceOf[Array[N]]

  final def apply(xs: N*): Array[N] =
    Array(xs.asInstanceOf[Seq[Repr]]: _*)(clsTag).asInstanceOf[Array[N]]

  final def upcast(a: Array[Repr]): Array[N] = a.asInstanceOf[Array[N]]

  final def downcast(a: Array[N]): Array[Repr] = a.asInstanceOf[Array[Repr]]
}

object NewTypeArray {

  type Aux[N, R] = NewTypeArray[N] { type Repr = R }

  def apply[N](implicit ev: NewTypeArray[N]): Aux[N, ev.Repr] = ev

  def apply[N](xs: N*)(implicit ev: NewTypeArray[N]): Array[N] = ev(xs: _*)

  def empty[N](implicit ev: NewTypeArray[N]): Array[N] = ev.empty

  def unsafeDerive[N, R](implicit ct: ClassTag[R]): Aux[N, R] = new NewTypeArray[N] {
    type Repr = R
    override def clsTag: ClassTag[Repr] = ct
  }
}
