package io.estatico.newtype.arrays

import scala.reflect.ClassTag

trait AsArray[N] {
  type Repr
  def clsTag: ClassTag[Repr]

  final def empty: Array[N] = Array.empty(clsTag).asInstanceOf[Array[N]]

  final def apply(xs: N*): Array[N] =
    Array(xs.asInstanceOf[Seq[Repr]]: _*)(clsTag).asInstanceOf[Array[N]]

  final def upcast[R](array: Array[R]): Array[N] = array.asInstanceOf[Array[N]]

  final def downcast(array: Array[N]): Array[Repr] = array.asInstanceOf[Array[Repr]]
}

object AsArray {

  type Aux[N, R] = AsArray[N] { type Repr = R }

  /** Summon the AsArray instance for the given newtype N. */
  def apply[N](implicit ev: AsArray[N]): Aux[N, ev.Repr] = ev

  /** Construct a new array of newtype N from varargs. */
  def apply[N](xs: N*)(implicit ev: AsArray[N]): Array[N] = ev(xs: _*)

  /** Construct an empty array of newtype N. */
  def empty[N](implicit ev: AsArray[N]): Array[N] = ev.empty

  def unsafeDerive[N, R](implicit ct: ClassTag[R]): Aux[N, R] =
    new AsArray[N] {
      type Repr = R
      override def clsTag: ClassTag[Repr] = ct
    }
}

