package io.estatico.newtype

import cats._
import cats.implicits._
import io.estatico.newtype.ops._
import io.estatico.newtype.macros.{newsubtype, newtype}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class NewTypeCatsTest extends FlatSpec with Matchers with ScalaCheckPropertyChecks {

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

  "Monoid[Nel[A]]" should "work" in {
    Nel.of(1, 2, 3).combine(Nel.of(4, 5, 6)) shouldBe Nel.of(1, 2, 3, 4, 5, 6)
  }

  "Show[Nel]" should "work" in {
    Nel.of(1, 2, 3).show shouldBe "Nel(1,2,3)"
  }

  "Monoid[Sum]" should "work" in {
    Monoid[Sum].empty shouldBe 0
    List(2, 3, 4).coerce[List[Sum]].combineAll shouldBe 9
  }

  "Monoid[Prod]" should "work" in {
    Monoid[Prod].empty shouldBe 1
    List(2, 3, 4).coerce[List[Prod]].combineAll shouldBe 24
  }

  "Monoid[SumN[A]]" should "work" in {
    Monoid[SumN[Double]].empty shouldBe 0d
    List(2d, 3d, 4d).coerce[List[SumN[Double]]].combineAll shouldBe 9d
  }

  "Monoid[ProdN[A]]" should "work" in {
    Monoid[ProdN[Double]].empty shouldBe 1d
    List(2d, 3d, 4d).coerce[List[ProdN[Double]]].combineAll shouldBe 24d
  }

  behavior of "SubNel[A]"

  it should "be a subtype of List[A]" in {
    def unsafeHead[A](xs: List[A]) = xs.head
    unsafeHead(SubNel.of(1, 2, 3)) shouldBe 1

    def sum(xs: List[Int]) = xs.sum
    sum(SubNel.of(1, 2, 3)) shouldBe 6
  }

  behavior of "Functor[SubNel]"

  it should "be the same as Functor[List]" in {
    Functor[SubNel] shouldBe Functor[List]
  }

  it should "get extension methods" in {
    SubNel.of(1, 2, 3).map(_ * 2) shouldBe SubNel.of(2, 4, 6)
  }

  behavior of "Monad[SubNel]"

  it should "be the same as Monad[List]" in {
    Monad[SubNel] shouldBe Monad[List]
  }

  it should "get extension methods" in {
    1.pure[SubNel] shouldBe SubNel.of(1)
    SubNel.of(1, 2, 3).flatMap(x => SubNel.of(x, x * 2)) shouldBe
      SubNel.of(1, 2, 2, 4, 3, 6)
  }

  it should "work in for comprehensions" in {
    val res = for {
      x <- SubNel.of(1, 2, 3)
      y <- SubNel.of(x, x * 2)
    } yield x + y

    res shouldBe SubNel.of(2, 3, 4, 6, 6, 9)
  }

  it should "work in the same scope in which it is defined" in {
    testSubNelTypeAliasExpansion shouldBe testSubNelTypeAliasExpansionExpectedResult
  }

  "Monoid[SubNel[A]]" should "work" in {
    SubNel.of(1, 2, 3).combine(SubNel.of(4, 5, 6)) shouldBe SubNel.of(1, 2, 3, 4, 5, 6)
  }

  "Coercible" should "support automatic type class derivation" in {
    implicit def coercibleShow[R, N](implicit ev: Coercible[Show[R], Show[N]], R: Show[R]): Show[N] = ev(R)
    @newtype case class Foo(private val x: Int)
    forAll { (n: Int) => Foo(n).show shouldBe n.show }
    Show[Foo] shouldBe Show[Int]
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

  @newsubtype case class Sum(value: Int)
  object Sum {
    implicit val monoid: Monoid[Sum] = new Monoid[Sum] {
      override def empty: Sum = Sum(0)
      override def combine(x: Sum, y: Sum): Sum = Sum(x.value + y.value)
    }
  }

  @newsubtype case class Prod(value: Int)
  object Prod {
    implicit val monoid: Monoid[Prod] = new Monoid[Prod] {
      override def empty: Prod = Prod(1)
      override def combine(x: Prod, y: Prod): Prod = Prod(x.value * y.value)
    }
  }

  @newsubtype case class SumN[A](value: A)
  object SumN {
    implicit def monoid[A](implicit A: Numeric[A]): Monoid[SumN[A]] = new Monoid[SumN[A]] {
      override def empty: SumN[A] = SumN[A](A.fromInt(0))
      override def combine(x: SumN[A], y: SumN[A]): SumN[A] = SumN[A](A.plus(x, y))
    }
  }

  @newsubtype case class ProdN[A](value: A)
  object ProdN {
    implicit def monoid[A](implicit A: Numeric[A]): Monoid[ProdN[A]] = new Monoid[ProdN[A]] {
      override def empty: ProdN[A] = ProdN[A](A.fromInt(1))
      override def combine(x: ProdN[A], y: ProdN[A]): ProdN[A] = ProdN[A](A.times(x, y))
    }
  }

  /** Same as [[Nel]] except also a subtype of List[A] */
  @newsubtype case class SubNel[A](toList: List[A]) {
    def head: A = toList.head
    def tail: List[A] = toList.tail
    def iterator: Iterator[A] = toList.iterator
  }
  object SubNel {
    def apply[A](head: A, tail: List[A]): SubNel[A] = (head +: tail).coerce
    def of[A](head: A, tail: A*): SubNel[A] = (head +: tail.toList).coerce
    implicit def show[A](implicit A: Show[A]): Show[SubNel[A]] = new Show[SubNel[A]] {
      def show(nel: SubNel[A]): String = "SubNel(" + nel.iterator.map(A.show).mkString(",") + ")"
    }
    implicit def monoid[A]: Monoid[SubNel[A]] = deriving
    implicit val monad: Monad[SubNel] = derivingK
  }

  // See https://github.com/scala/bug/issues/10750
  private val testSubNelTypeAliasExpansion = for {
    x <- SubNel.of(1, 2, 3)
    y <- SubNel.of(x, x * 2)
  } yield x + y

  private val testSubNelTypeAliasExpansionExpectedResult = SubNel.of(2, 3, 4, 6, 6, 9)
}
