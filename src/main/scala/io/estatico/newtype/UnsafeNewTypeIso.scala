package io.estatico.newtype

/** Isomorphism for casting between a NewType and its Repr. */
trait UnsafeNewTypeIso[Type] {
  type Repr
}

object UnsafeNewTypeIso {

  type Aux[T, R] = UnsafeNewTypeIso[T] { type Repr = R }

  def apply[R, T](implicit ev: Aux[R, T]): UnsafeNewTypeIso.Aux[R, T] = ev

  def instance[T, R]: UnsafeNewTypeIso.Aux[T, R] = new UnsafeNewTypeIso[T] { type Repr = R }

  @inline def wrap[T, R](x: R)(implicit ev: Aux[T, R]): T = x.asInstanceOf[T]

  @inline def wrapM[M[_], T, R](x: M[R])(implicit ev: Aux[T, R]): M[T] = x.asInstanceOf[M[T]]

  @inline def unwrap[M[_], T, R](x: T)(implicit ev: Aux[T, R]): R = x.asInstanceOf[R]

  @inline def unwrapM[M[_], T, R](x: M[T])(implicit ev: Aux[T, R]): M[R] = x.asInstanceOf[M[R]]

  trait Methods[T, R] {

    @inline final def wrap(x: R): T = x.asInstanceOf[T]

    @inline final def wrapM[M[_]](x: M[R]): M[T] = x.asInstanceOf[M[T]]

    @inline final def unwrap(x: T): R = x.asInstanceOf[R]

    @inline final def unwrapM[M[_]](x: M[T]): M[R] = x.asInstanceOf[M[R]]
  }

  object Methods {

    def apply[T, R](implicit ev: UnsafeNewTypeIso.Aux[T, R]): Methods[T, R]
      = impl.asInstanceOf[Methods[T, R]]

    def apply(t: BaseNewType)(
      implicit ev: UnsafeNewTypeIso.Aux[t.Type, t.Repr]
    ): Methods[t.Type, t.Repr] = apply[t.Type, t.Repr]

    private val impl = new Methods[Nothing, Nothing] {}
  }
}
