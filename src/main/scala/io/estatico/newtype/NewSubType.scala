package io.estatico.newtype

trait NewSubType extends BaseNewType {
  type Base = Repr
}

object NewSubType {

  trait For[R] extends NewSubType {
    final type Repr = R
  }

  trait Default[R] extends For[R] with NewTypeExtras
}
