package io.estatico.newtype

object CovariantNewTypeExtras {

  trait All extends ApplyM

  trait Apply extends BaseCovariantNewType {
    @inline final def apply[A <: SuperType](a: A): Type[A] = a.asInstanceOf[Type[A]]
  }

  trait ApplyM extends Apply {
    @inline final def applyM[M[_], A <: SuperType](a: M[A]): M[Type[A]] = a.asInstanceOf[M[Type[A]]]
  }
}
