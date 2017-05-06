package io.estatico.newtype

/** Base skeleton for building newtypes. */
trait BaseNewType {
  type Base
  type Repr
  trait Tag
  final type Type = BaseNewType.Aux[Base, Tag, Repr]
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
