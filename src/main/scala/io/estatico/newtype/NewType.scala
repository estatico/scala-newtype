package io.estatico.newtype

trait NewType extends BaseNewType { self =>
  type Base = { type Repr = self.Repr }
}

object NewType {
  trait Of[R] extends BaseNewType.Of[R] with NewType
  trait Default[R] extends Of[R] with NewTypeExtras.All
}
