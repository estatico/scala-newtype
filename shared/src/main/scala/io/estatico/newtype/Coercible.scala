package io.estatico.newtype

/** Safe type casting from A to B. */
trait Coercible[A, B] {
  @inline final def apply(a: A): B = a.asInstanceOf[B]
}

object Coercible {

  def apply[A, B](implicit ev: Coercible[A, B]): Coercible[A, B] = ev

  def instance[A, B]: Coercible[A, B] = _instance.asInstanceOf[Coercible[A, B]]

  private val _instance = new Coercible[Any, Any] {}

  // Support nested type constructors
  implicit def unsafeWrapMM[M1[_], M2[_], A, B](
    implicit ev: Coercible[M2[A], M2[B]]
  ): Coercible[M1[M2[A]], M1[M2[B]]] = Coercible.instance
}

final class CoercibleIdOps[A](private val repr: A) extends AnyVal {
  @inline def coerce[B](implicit ev: Coercible[A, B]): B = repr.asInstanceOf[B]
}

trait ToCoercibleIdOps {
  @inline implicit def toCoercibleIdOps[A](a: A): CoercibleIdOps[A] = new CoercibleIdOps(a)
}
