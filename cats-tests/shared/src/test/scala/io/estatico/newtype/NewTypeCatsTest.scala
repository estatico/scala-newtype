package io.estatico.newtype

import cats._
import cats.implicits._
import io.estatico.newtype.ops._
import io.estatico.newtype.macros.newtype
import org.scalatest.{FlatSpec, Matchers}

class NewTypeCatsTest extends FlatSpec with Matchers {

  import NewTypeCatsTest._

  behavior of "Functor[Nel]"

  it should "be the same as Functor[List]" in {
    Functor[Nel] shouldBe Functor[List]
  }

  it should "get extension methods" in {
    Nel.of(1, 2, 3).map(_ * 2) shouldBe Nel.of(2, 4, 6)
  }

  behavior of "Monad[Nel]"

  it should "be the same as Monad[List]" in {
    Monad[Nel] shouldBe Monad[List]
  }

  it should "get extension methods" in {
    1.pure[Nel] shouldBe Nel.of(1)
    Nel.of(1, 2, 3).flatMap(x => Nel.of(x, x * 2)) shouldBe
      Nel.of(1, 2, 2, 4, 3, 6)
  }

  it should "work in for comprehensions" in {
    val res = for {
      x <- Nel.of(1, 2, 3)
      y <- Nel.of(x, x * 2)
    } yield x + y

    res shouldBe Nel.of(2, 3, 4, 6, 6, 9)
  }

  it should "work in the same scope in which it is defined" in {
    testNelTypeAliasExpansion shouldBe testNelTypeAliasExpansionExpectedResult
  }
}

object NewTypeCatsTest {

  @newtype class Nel[A](val toList: List[A]) {
    def head: A = toList.head
    def tail: List[A] = toList.tail
    def iterator: Iterator[A] = toList.iterator
  }
  object Nel {
    def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce
    def of[A](head: A, tail: A*): Nel[A] = (head +: tail.toList).coerce
    implicit def show[A](implicit A: Show[A]): Show[Nel[A]] = new Show[Nel[A]] {
      def show(nel: Nel[A]): String = "Nel(" + nel.iterator.map(A.show).mkString(",") + ")"
    }
    implicit def monoid[A]: Monoid[Nel[A]] = deriving
    implicit val monad: Monad[Nel] = derivingK
  }

  // See https://github.com/scala/bug/issues/10750
  private val testNelTypeAliasExpansion = for {
    x <- Nel.of(1, 2, 3)
    y <- Nel.of(x, x * 2)
  } yield x + y

  private val testNelTypeAliasExpansionExpectedResult = Nel.of(2, 3, 4, 6, 6, 9)
}
