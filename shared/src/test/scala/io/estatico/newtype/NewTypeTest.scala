package io.estatico.newtype

import org.scalacheck.Arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NewTypeTest extends AnyFlatSpec with ScalaCheckPropertyChecks with Matchers {

  import NewTypeTest._

  "NewType" should "create a type with no runtime overhead" in {
    object NatInt extends NewType.Of[Int] {
      def apply(i: Int): Option[Type] = if (i < 0) None else wrapM(Some(i))
    }
    NatInt(1) shouldEqual Some(1)
    NatInt(-1) shouldEqual None
  }

  it should "not be a subtype of its Repr" in {
    type Foo = Foo.Type
    object Foo extends NewType.Default[Int]
    assertCompiles("Foo(1): Foo")
    assertDoesNotCompile("Foo(1): Int")
  }

  it should "find implicit instances" in {
    type Box = Box.Type
    object Box extends NewType.Of[String] with NewTypeDeriving {
      implicit val arb: Arbitrary[Type] = deriving[Arbitrary]
    }
    scala.Predef.implicitly[Arbitrary[Box]].arbitrary.sample shouldBe defined
  }

  it should "support user ops" in {
    GoodInt(3).cube shouldEqual 27
  }

  it should "be Coercible" in {
    type Foo = Foo.Type
    object Foo extends NewType.Default[Int]

    // Using type annotations to prove that coerce methods return the right type.

    (Foo.wrap(1): Foo) shouldEqual 1
    (Foo.unwrap(Foo(1)): Int) shouldEqual 1
    (Foo.wrapM(List(1)): List[Foo]) shouldEqual List(1)
    (Foo.unwrapM(List(Foo(1))): List[Int]) shouldEqual List(1)

    import io.estatico.newtype.ops._

    (1.coerce[Foo]: Foo) shouldEqual 1
    (Foo(1).coerce[Int]: Int) shouldEqual 1
    (List(1).coerce[List[Foo]]: List[Foo]) shouldEqual List(1)
    (List(Foo(1)).coerce[List[Int]]: List[Int]) shouldEqual List(1)
  }

  it should "work in Arrays" in {
    type Foo = Foo.Type
    object Foo extends NewType.Default[Int]

    val foo = Foo(42)
    Array(foo).apply(0) shouldEqual foo
  }

  "NewTypeApply" should "automatically create an apply method" in {
    object PersonId extends NewType.Of[Int] with NewTypeApply
    PersonId(1) shouldEqual 1
  }

  "DefaultNewType" should "get NewTypeOps" in {
    object Gold extends NewType.Default[Double]
    val gold = Gold(34.56)
    gold.repr shouldEqual 34.56
    gold.withRepr(_ / 2) shouldEqual Gold(17.28)
  }

  "NewTypeOps" should "not be available without extending NewTypeAutoOps or importing ops._" in {
    object Simple extends NewType.Of[Int] with NewTypeApply
    assertCompiles("Simple(1)")
    assertDoesNotCompile("Simple(1).repr")
    assertCompiles("""
      import io.estatico.newtype.ops._
      Simple(1).repr
    """)
    object HasOps extends NewType.Of[Int] with NewTypeApply with NewTypeAutoOps
    assertCompiles("HasOps(1).repr")
    assertCompiles("""
      import io.estatico.newtype.ops._
      HasOps(1).repr
    """)
  }

  "NewSubType" should "be a subtype of its Repr" in {
    type Foo = Foo.Type
    object Foo extends NewSubType.Of[String] with NewTypeApply
    assertCompiles("""Foo("bar"): Foo""")
    assertCompiles("""Foo("bar"): String""")
    Foo("bar").toUpperCase shouldEqual "BAR"
    Foo("bar").toUpperCase shouldEqual Foo("BAR")
  }

  it should "be Coercible" in {
    type Foo = Foo.Type
    object Foo extends NewSubType.Default[Int]

    // Using type annotations to prove that coerce methods return the right type.

    (Foo.wrap(1): Foo) shouldEqual 1
    (Foo.unwrap(Foo(1)): Int) shouldEqual 1
    (Foo.wrapM(List(1)): List[Foo]) shouldEqual List(1)
    (Foo.unwrapM(List(Foo(1))): List[Int]) shouldEqual List(1)

    import io.estatico.newtype.ops._

    (1.coerce[Foo]: Foo) shouldEqual 1
    (Foo(1).coerce[Int]: Int) shouldEqual 1
    (List(1).coerce[List[Foo]]: List[Foo]) shouldEqual List(1)
    (List(Foo(1)).coerce[List[Int]]: List[Int]) shouldEqual List(1)
  }

  it should "work in Arrays" in {
    type Foo = Foo.Type
    object Foo extends NewSubType.Default[Int]

    val foo = Foo(-273)
    Array(foo).apply(0) shouldEqual foo
  }

  "Coercible" should "work across newtypes" in {
    type Foo = Foo.Type
    object Foo extends NewType.Default[Int]

    type Bar = Bar.Type
    object Bar extends NewType.Default[Int]

    import io.estatico.newtype.ops._

    Foo(1).coerce[Bar] shouldEqual 1
    Bar(2).coerce[Foo] shouldEqual 2
  }

  "Coercible" should "not allow coercion of Array types" in {
    type Foo = Foo.Type
    object Foo extends NewType.Default[Int]

    // JVM will throw ClassCastException, JS will throw UndefinedBehaviorError
    a [Throwable] should be thrownBy Array(Foo(1)).asInstanceOf[Array[Int]]

    assertDoesNotCompile("Coercible[Array[Int], Array[Foo]]")
    assertDoesNotCompile("Coercible[Array[Foo], Array[Int]]")
  }
}

object NewTypeTest {

  type GoodInt = GoodInt.Type
  object GoodInt extends NewType.Default[Int] {
    implicit final class Ops(private val me: GoodInt) extends AnyVal {
      def cube: GoodInt = {
        val i = unwrap(me)
        wrap(i * i * i)
      }
    }
  }
}
