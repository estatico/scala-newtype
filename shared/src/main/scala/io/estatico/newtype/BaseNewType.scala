package io.estatico.newtype

import scala.reflect.ClassTag

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

// Scala 2.10 doesn't support abstract type aliases in object definitions, so
// we have to create the abstract type alias Aux in a trait and have the
// BaseNewType object extend from it.
trait BaseNewType$Types {
  /** `Type` implementation for all newtypes; see `BaseNewType`. */
  type Aux[B, T, R] <: B with Meta[T, R]
  trait Meta[T, R]
}

object BaseNewType extends BaseNewType$Types {

  /** Helper trait to refine Repr via a type parameter. */
  trait Of[R] extends BaseNewType {
    final type Repr = R
  }

  // Since Aux is abstract, this is necessary to make Arrays work.
  @inline implicit def classTag[B, T, R](implicit base: ClassTag[B]): ClassTag[Aux[B, T, R]] =
    ClassTag(base.runtimeClass)
}
