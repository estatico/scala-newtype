package io.estatico.newtype

/** A newtype which is a subtype of its Repr. */
trait NewSubType extends BaseNewType {
  type Base = Repr
}

object NewSubType {
  trait Of[R] extends BaseNewType.Of[R] with NewSubType
  trait Default[R] extends Of[R] with NewTypeExtras.All
}
