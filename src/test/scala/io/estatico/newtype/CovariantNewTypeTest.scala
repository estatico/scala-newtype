package io.estatico.newtype

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class CovariantNewTypeTest extends FlatSpec with PropertyChecks with Matchers {

  import CovariantNewTypeTest._

  "CovariantNewType" should "create a type with no runtime overhead" in {
    EggLayer(platypus) shouldEqual Some(platypus)
    EggLayer(yoshi) shouldEqual Some(yoshi)
    EggLayer(guile) shouldEqual None

    assertCompiles("EggLayer(platypus): Option[EggLayer[Platypus]]")
    assertCompiles("EggLayer(platypus): Option[EggLayer[Mammal]]")
    assertCompiles("EggLayer(platypus): Option[EggLayer[Animal]]")
    assertDoesNotCompile("EggLayer(platypus): Option[EggLayer[Reptile]]")
  }

  it should "get ops" in {
    EggLayer(platypus).map(_.isHairyEggLayer) shouldEqual Some(true)
    EggLayer(yoshi).map(_.isHairyEggLayer) shouldEqual Some(false)
    EggLayer(guile).map(_.isHairyEggLayer) shouldEqual None
  }

  it should "not be a subtype of its Repr" in {
    type Foo[A] = Foo.Type[A]
    object Foo extends CovariantNewType.Default[Int]
    assertCompiles("Foo(1): Foo[Int]")
    assertDoesNotCompile("Foo(1): Int")
  }

  it should "find implicit instances" in {
    type Box[A] = Box.Type[A]
    object Box extends CovariantNewType.Of[CharSequence] {

      implicit def show[A <: SuperType]: Loud[Type[A]] = _show.asInstanceOf[Loud[Type[A]]]

      implicit val _show: Loud[Type[CharSequence]] = new Loud[Type[CharSequence]] {
        override def show(a: Type[CharSequence]): String = a.toString.toUpperCase
      }
    }

    trait Loud[A] {
      def show(a: A): String
    }

    implicitly[Loud[Box[String]]].show(Box.unsafeWrap("foo")) shouldEqual "FOO"
  }

  it should "be Coercible" in {
    type Creature[A] = Creature.Type[A]
    object Creature extends CovariantNewType.Default[Animal]

    // Using type annotations to prove that coerce methods return the right type.

    (Creature.unsafeWrap(yoshi): Creature[Yoshi]) shouldEqual yoshi
    (Creature.unsafeUnwrap(Creature(yoshi)): Yoshi) shouldEqual yoshi
    (Creature.unsafeWrapM(List(yoshi)): List[Creature[Yoshi]]) shouldEqual List(yoshi)
    (Creature.unsafeUnwrapM(List(Creature(yoshi))): List[Yoshi]) shouldEqual List(yoshi)

    import io.estatico.newtype.ops._

    (yoshi.coerce[Creature[Yoshi]]: Creature[Yoshi]) shouldEqual yoshi
    (Creature(yoshi).coerce[Yoshi]: Yoshi) shouldEqual yoshi
    (List(yoshi).coerce[List[Creature[Yoshi]]]: List[Creature[Yoshi]]) shouldEqual List(yoshi)
    (List(Creature(yoshi)).coerce[List[Yoshi]]: List[Yoshi]) shouldEqual List(yoshi)
  }
}

object CovariantNewTypeTest {

  type EggLayer[A] = EggLayer.Type[A]
  object EggLayer extends CovariantNewType.Of[Animal] {

    def apply[A <: Animal](a: A): Option[Type[A]] = {
      if (a.laysEggs) Some(unsafeWrap(a)) else None
    }

    implicit final class Ops[A <: SuperType](val self: Type[A]) extends AnyVal {
      def toAnimal: A = unsafeUnwrap(self)
      def isHairyEggLayer: Boolean = toAnimal.hairy && toAnimal.laysEggs
    }
  }

  trait Animal {
    def hairy: Boolean
    def laysEggs: Boolean
  }

  trait Reptile extends Animal {
    def hairy: Boolean = false
    def laysEggs: Boolean = true
  }

  trait Mammal extends Animal {
    def hairy: Boolean = true
    def laysEggs: Boolean = false
  }

  class Platypus extends Mammal {
    override def laysEggs: Boolean = true
  }

  class Yoshi extends Reptile

  class Guile extends Mammal

  val platypus = new Platypus
  val yoshi = new Yoshi
  val guile = new Guile
}
