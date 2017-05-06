package io.estatico.newtype

trait BaseNewType {
  type Base
  type Type = Base with NewTypeMeta[Tag, Repr]
  type Repr
  trait Tag
}

object BaseNewType {
  type Aux[B, T, R] = B with NewTypeMeta[T, R]
}

trait NewTypeMeta[T, R]
