package io.estatico.newtype

/** NewType to carry subtype information in Type[A].  */
trait CovariantNewType extends BaseCovariantNewType {
  type Top = Type[SuperType]
  type Base[A] = { type Repr = A }
}

object CovariantNewType {
  trait Of[S] extends BaseCovariantNewType.Of[S] with CovariantNewType
  trait Default[S] extends Of[S] with CovariantNewTypeExtras.All
}
