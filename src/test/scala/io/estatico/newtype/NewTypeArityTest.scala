package io.estatico.newtype

import java.util
import org.scalacheck.Arbitrary
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.PropertyChecks

class NewTypeArityTest extends FlatSpec with PropertyChecks with Matchers {

  import NewTypeArityTest._

  "NewTypeArity" should "create wrappers around types with arity" in {
    val raw = util.Arrays.asList(1, 2)
    val xs = JavaList(raw)
    xs shouldEqual raw
    xs.head shouldEqual Some(1)
    JavaList[Int](util.Collections.emptyList()).head shouldEqual None
  }

  it should "be Coercible" in {
    type Foo[A] = Foo.Type[A]
    object Foo extends NewTypeArity._1.Default[util.Set]

    // Scala doesn't infer subtype relationships well, so type it as the newtype Repr.
    val raw: util.Set[String] = new util.HashSet[String]

    // Using type annotations to prove that coerce methods return the right type.

    (Foo.wrap(raw): Foo[String]) shouldEqual raw
    (Foo.unwrap(Foo(raw)): util.Set[String]) shouldEqual raw
    (Foo.wrapM(List(raw)): List[Foo[String]]) shouldEqual List(raw)
    (Foo.unwrapM(List(Foo(raw))): List[util.Set[String]]) shouldEqual List(raw)

    import io.estatico.newtype.ops._

    (raw.coerce[Foo[String]]: Foo[String]) shouldEqual raw
    (Foo(raw).coerce[util.Set[String]]: util.Set[String]) shouldEqual raw
    (List(raw).coerce[List[Foo[String]]]: List[Foo[String]]) shouldEqual List(raw)
    (List(Foo(raw)).coerce[List[util.Set[String]]]: List[util.Set[String]]) shouldEqual List(raw)
  }

  it should "find implicit instances" in {
    // Does not contain a derived instance
    type Vec0[A] = Vec0.Type[A]
    object Vec0 extends NewTypeArity._1.Default[Vector]

    assertDoesNotCompile("implicitly[Arbitrary[Vec0[Int]]]")

    // Contains a derived instance
    type Vec1[A] = Vec1.Type[A]
    object Vec1 extends NewTypeArity._1.Default[Vector] {
      implicit def arb[A : Arbitrary]: Arbitrary[Type[A]] = deriving
    }

    // Assert compiles
    implicitly[Arbitrary[Vec1[Int]]]
  }
}

object NewTypeArityTest {

  type JavaList[A] = JavaList.Type[A]
  object JavaList extends NewTypeArity._1.Default[util.List] {
    implicit final class Ops[A](val me: Type[A]) extends AnyVal {
      private def unwrapped = unwrap(me)
      def head: Option[A] = if (unwrapped.size() == 0) None else Some(unwrapped.get(0))
    }
  }
}
