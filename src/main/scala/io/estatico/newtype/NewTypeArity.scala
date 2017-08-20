package io.estatico.newtype

object NewTypeArity {

  trait _1 extends BaseNewTypeArity._1 { self =>
    type Base[A] = { type Repr = self.Repr[A] }
  }

  object _1 {
    trait Of[R[_]] extends BaseNewTypeArity._1.Of[R] with _1
    trait Default[R[_]] extends Of[R] with Extras

    trait Extras extends ApplyM with Deriving { self: _1 => }

    trait Apply { self: _1 =>
      @inline final def apply[A](x: Repr[A]): Type[A] = x.asInstanceOf[Type[A]]
    }

    trait ApplyM extends Apply { self: _1 =>
      @inline final def applyM[M[_], A](mx: M[Repr[A]]): M[Type[A]] = mx.asInstanceOf[M[Type[A]]]
    }

    trait Deriving { self: _1 =>

      /** Derive an instance of type class `T` if one exists for `Repr`.  */
      def deriving[T[_], A](implicit ev: T[Repr[A]]): T[Type[A]] = ev.asInstanceOf[T[Type[A]]]

      /** Derive an instance of higher kinded type class `T` if one exists for `Repr`.  */
      def derivingM[T[_[_]]](implicit ev: T[Repr]): T[Type] = ev.asInstanceOf[T[Type]]
    }
  }
}

object BaseNewTypeArity {

  trait _1 {
    type Base[A]
    type Repr[A]
    trait Tag[A]
    final type Type[A] = BaseNewType.Aux[Base[A], Tag[A], Repr[A]]

    // Define Coercible instances for which we can safely cast to/from.
    @inline implicit def wrap[A]: Coercible[Repr[A], Type[A]] = Coercible.instance
    @inline implicit def unwrap[A]: Coercible[Type[A], Repr[A]] = Coercible.instance
    @inline implicit def wrapM[M[_], A]: Coercible[M[Repr[A]], M[Type[A]]] = Coercible.instance
    @inline implicit def unwrapM[M[_], A]: Coercible[M[Type[A]], M[Repr[A]]] = Coercible.instance
    @inline implicit def convert[N <: BaseNewType.Aux[_, _, Repr[A]], A]: Coercible[Type[A], N] = Coercible.instance
  }

  object _1 {
    trait Of[R[_]] extends _1 {
      final type Repr[A] = R[A]
    }
  }
}
