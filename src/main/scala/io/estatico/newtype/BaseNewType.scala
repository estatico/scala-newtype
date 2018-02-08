package io.estatico.newtype

/** Base skeleton for building newtypes. */
trait BaseNewType {
  type Base
  type Repr
  trait Tag
  final type Type = BaseNewType.Aux[Base, Tag, Repr]

  // Define Coercible instances for which we can safely cast to/from.
  @inline implicit def wrap: Coercible[Repr, Type] = Coercible.instance
  @inline implicit def unwrap: Coercible[Type, Repr] = Coercible.instance
  @inline implicit def wrapM[M[_]]: Coercible[M[Repr], M[Type]] = Coercible.instance
  @inline implicit def unwrapM[M[_]]: Coercible[M[Type], M[Repr]] = Coercible.instance
  @inline implicit def convert[N <: BaseNewType.Aux[_, _, Repr]]: Coercible[Type, N] = Coercible.instance
  // Avoid ClassCastException with Array types by prohibiting Array coercing.
  @inline implicit def cannotWrapArrayAmbiguous1: Coercible[Array[Repr], Array[Type]] = Coercible.instance
  @inline implicit def cannotWrapArrayAmbiguous2: Coercible[Array[Repr], Array[Type]] = Coercible.instance
  @inline implicit def cannotUnwrapArrayAmbiguous1: Coercible[Array[Type], Array[Repr]] = Coercible.instance
  @inline implicit def cannotUnwrapArrayAmbiguous2: Coercible[Array[Type], Array[Repr]] = Coercible.instance
}

object BaseNewType {
  /** `Type` implementation for all newtypes; see `BaseNewType`. */
  type Aux[B, T, R] = B with Meta[T, R]
  trait Meta[T, R]

  /** Helper trait to refine Repr via a type parameter. */
  trait Of[R] extends BaseNewType {
    final type Repr = R
  }
}
