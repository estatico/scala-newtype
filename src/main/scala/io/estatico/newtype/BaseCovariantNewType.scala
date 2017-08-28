package io.estatico.newtype

trait BaseCovariantNewType {
  type SuperType
  type Base[A]
  trait Tag[A]
  final type Type[A] = BaseNewType.Aux[Base[A], Tag[A], A]

  // Define Coercible instances for which we can safely cast to/from.
  @inline implicit def unsafeWrap[A <: SuperType]: Coercible[A, Type[A]] = Coercible.instance
  @inline implicit def unsafeUnwrap[A <: SuperType]: Coercible[Type[A], A] = Coercible.instance
  @inline implicit def unsafeWrapM[M[_], A <: SuperType]: Coercible[M[A], M[Type[A]]] = Coercible.instance
  @inline implicit def unsafeUnwrapM[M[_], A <: SuperType]: Coercible[M[Type[A]], M[A]] = Coercible.instance
  @inline implicit def unsafeConvert[N <: BaseNewType.Aux[_, _, A], A <: SuperType]: Coercible[Type[A], N] = Coercible.instance
}

object BaseCovariantNewType {

  trait Of[S] extends BaseCovariantNewType {
    final type SuperType = S
  }
}

