package io.estatico.newtype

import org.scalacheck.Arbitrary
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}

class NewTypeTest extends FlatSpec with PropertyChecks with Matchers {

  "NewType" should "create a type with no runtime overhead" in {
    object NatInt extends NewType.For[Int] with NewTypeCasts {
      def apply(i: Int): Option[Type] = if (i < 0) None else castM(Some(i))
    }
    NatInt(1) shouldEqual Some(1)
    NatInt(-1) shouldEqual None
  }

  it should "find implicit instances" in {
    type Box = Box.Type
    object Box extends NewType.For[String] with NewTypeDeriving {
      implicit val arb: Arbitrary[Type] = deriving[Arbitrary]
    }
    implicitly[Arbitrary[Box]].arbitrary.sample shouldBe defined
  }

  "NewTypeApply" should "automatically create an apply method" in {
    object PersonId extends NewType.For[Int] with NewTypeApply
    PersonId(1) shouldEqual 1
  }

  "DefaultNewType" should "get NewTypeOps" in {
    object Gold extends NewType.Default[Double]
    val gold = Gold(34.56)
    gold.repr shouldEqual 34.56
    gold.withRepr(_ / 2) shouldEqual Gold(17.28)
  }

  "NewTypeOps" should "not be available without extending NewTypeAutoOps or importing ops._" in {
    object Simple extends NewType.For[Int] with NewTypeApply
    assertCompiles("Simple(1)")
    assertDoesNotCompile("Simple(1).repr")
    assertCompiles("""
      import io.estatico.newtype.ops._
      Simple(1).repr
    """)
    object HasOps extends NewType.For[Int] with NewTypeApply with NewTypeAutoOps
    assertCompiles("HasOps(1).repr")
    assertCompiles("""
      import io.estatico.newtype.ops._
      HasOps(1).repr
    """)
  }
}
