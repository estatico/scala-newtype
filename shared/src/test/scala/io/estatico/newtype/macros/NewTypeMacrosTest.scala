package io.estatico.newtype.macros

import org.scalatest.{FlatSpec, Matchers}
import io.estatico.newtype.ops._
import org.scalacheck.Arbitrary

class NewTypeMacrosTest extends FlatSpec with Matchers {

  import NewTypeMacrosTest._

  behavior of "@newtype case class"

  it should "generate a type alias, companion object, and constructor" in {

    // Ensure that we can access the type and the constructor.
    val res = Foo(1)
    assertCompiles("res: Foo")

    // Should have the same runtime representation as Int.
    res shouldBe 1
    res shouldBe Foo(1)
  }

  it should "generate an accessor extension method" in {
    Foo(1).value shouldBe 1
  }

  it should "not generate an accessor method if private" in {
    // This is also useful so we can define local newtypes.
    @newtype case class Foo0[A](private val value: A)
    Foo0('a') shouldBe 'a'
    assertDoesNotCompile("Foo0('a').value")
  }

  it should "convert instance methods into extension methods" in {
    val res: Bar = Bar(2).twice
    res shouldBe 4
  }

  it should "work in arrays" in {
    val foo = Foo(313)
    // See https://github.com/estatico/scala-newtype/issues/25
    // Array(foo).apply(0) shouldBe foo
    Array[Int](313).asInstanceOf[Array[Foo]].apply(0) shouldBe foo
  }

  behavior of "@newtype class"

  it should "not expose a default constructor" in {
    assertTypeError("""Baz("foo")""")
    Baz.create("foo") shouldBe "FOO"
  }

  it should "not expose its constructor argument by default" in {
    assertDoesNotCompile("""Baz.create("foo").value""")
  }

  it should "expose its constructor argument if defined as a val" in {
    Baz2.create("foo").value shouldBe "FOO"
  }

  behavior of "@newtype with type arguments"

  it should "generate a proper constructor" in {
    val repr = List(Option(1))
    val ot = OptionT(repr)
    assertCompiles("ot: OptionT[List, Int]")
    ot shouldBe repr
  }

  it should "be Coercible" in {
    val repr = List(Option(1))
    val ot = OptionT(repr)

    val x = ot.coerce[List[Option[Int]]]
    x shouldBe repr

    val y = Vector(repr).coerce[Vector[OptionT[List, Int]]]
    y shouldBe Vector(repr)
  }

  it should "not coerce array types" in {
    val repr = List(Option(1))
    val ot = OptionT(repr)
    assertTypeError("Array(ot).coerce[Array[List[Option[Int]]]]")
  }

  it should "support covariance" in {
    val x = Cov(List(Some(1)))
    assertCompiles("x: Cov[Some[Int]]")

    val y = Cov(List(None))
    assertCompiles("y: Cov[None.type]")

    def someOrZero[A](c: Cov[Option[Int]]): Cov[Int] = Cov(c.value.map(_.getOrElse(0)))

    someOrZero(x) shouldBe List(1)
    someOrZero(y) shouldBe List(0)
  }

  it should "work in arrays" in {
    import scala.collection.immutable.Set
    val repr = Set(Option("newtypes"))
    val ot = OptionT(repr)
    // See https://github.com/estatico/scala-newtype/issues/25
    // Array(ot).apply(0) shouldBe ot
    Array(repr).asInstanceOf[Array[OptionT[Set, String]]].apply(0) shouldBe ot
  }

  behavior of "@newtype with type bounds"

  it should "enforce type bounds" in {
    val x = Sub(new java.util.HashMap[String, Int]): Sub[java.util.HashMap[String, Int]]
    val y = Sub(new java.util.concurrent.ConcurrentHashMap[String, Int])

    assertCompiles("x: Sub[java.util.HashMap[String, Int]]")
    assertCompiles("y: Sub[java.util.concurrent.ConcurrentHashMap[String, Int]]")

    assertDoesNotCompile("x: Sub[java.util.concurrent.ConcurrentHashMap[String, Int]]")
    assertDoesNotCompile("y: Sub[java.util.HashMap[String, Int]]")
  }

  behavior of "deriving"

  it should "support deriving type class instances for simple newtypes" in {
    @newtype case class Text(private val s: String)
    object Text {
      implicit val arb: Arbitrary[Text] = deriving
    }
    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")
  }

  it should "support deriving type class instances for simple newtypes using deriving in annotation" in {
    @newtype(deriving = deriving[Arbitrary]) case class Text(private val s: String)
    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")
  }

  it should "support deriving several type class instances for simple newtypes via deriving" in {
    trait OtherTC[X] {
      def make: X
    }
    implicit val stringOtherTC: OtherTC[String] = new OtherTC[String] { def make: String = "ABC" }
    @newtype(deriving = deriving[Arbitrary, OtherTC]) case class Text(private val s: String)

    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")

    val x2 = scala.Predef.implicitly[OtherTC[Text]].make
    assertCompiles("x2: Text")
    val y2 = x2.coerce[String]
    assertCompiles("y2: String")
  }

  it should "support deriving type class instances for simple newtypes via coerce" in {
    @newtype case class Text(private val s: String)
    object Text {
      implicit val arb: Arbitrary[Text] = scala.Predef.implicitly[Arbitrary[String]].coerce
    }
    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")
  }

  it should "support deriving type class instances for higher-kinded newtypes" in {
    @newtype class Nel[A](private val list: List[A])
    object Nel {
      def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce[Nel[A]]
      implicit val functor: Functor[Nel] = derivingK
    }

    val x = scala.Predef.implicitly[Functor[Nel]].map(Nel(1, List(2, 3)))(_ * 2)
    assertCompiles("x: Nel[Int]")
    x shouldBe List(2, 4, 6)
  }

  it should "support deriving type class instances for higher-kinded newtypes via coerce" in {
    @newtype class Nel[A](private val list: List[A])
    object Nel {
      def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce[Nel[A]]
      implicit val functor: Functor[Nel] = scala.Predef.implicitly[Functor[List]].coerce
    }

    val x = scala.Predef.implicitly[Functor[Nel]].map(Nel(1, List(2, 3)))(_ * 2)
    assertCompiles("x: Nel[Int]")
    x shouldBe List(2, 4, 6)
  }

  it should "support deriving type class instances for higher-kinded newtypes via derivingK" in {
    @newtype(derivingK = derivingK[Functor]) class Nel[A](private val list: List[A])
    object Nel {
      def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce[Nel[A]]
    }

    val x = scala.Predef.implicitly[Functor[Nel]].map(Nel(1, List(2, 3)))(_ * 2)
    assertCompiles("x: Nel[Int]")
    x shouldBe List(2, 4, 6)
  }

  it should "support auto-deriving type class instances for simple newtypes" in {
    @newtype case class Text(private val s: String)
    object Text {
      implicit def typeclass[T[_]](implicit ev: T[String]): T[Text] = deriving
    }
    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")
  }

  it should "support auto-deriving type class instances for simple newtypes via coerce" in {
    @newtype case class Text(private val s: String)
    object Text {
      implicit def typeclass[T[_]](implicit ev: T[String]): T[Text] = ev.coerce
    }
    val x = scala.Predef.implicitly[Arbitrary[Text]].arbitrary.sample.get
    assertCompiles("x: Text")
    val y = x.coerce[String]
    assertCompiles("y: String")
  }

  it should "support deriving type class instances for newtypes with type params" in {
    @newtype case class EitherT[F[_], L, R](private val x: F[Either[L, R]])
    object EitherT {
      // Derive the Arbitrary instance explicitly
      implicit def arb[F[_], L, R](
        implicit a: Arbitrary[F[Either[L, R]]]
      ): Arbitrary[EitherT[F, L, R]] = deriving
    }
    val x = {
      import scala.Predef._
      scala.Predef.implicitly[Arbitrary[EitherT[List, String, Int]]].arbitrary.sample.get
    }
    assertCompiles("x: EitherT[List, String, Int]")
    val y = x.coerce[List[Either[String, Int]]]
    assertCompiles("y: List[Either[String, Int]]")
  }

  it should "support deriving type class instances for newtypes with type params via coerce" in {
    @newtype case class EitherT[F[_], L, R](private val x: F[Either[L, R]])
    object EitherT {
      // Derive the Arbitrary instance explicitly
      implicit def arb[F[_], L, R](
        implicit a: Arbitrary[F[Either[L, R]]]
      ): Arbitrary[EitherT[F, L, R]] = a.coerce
    }
    val x = {
      import scala.Predef._
      scala.Predef.implicitly[Arbitrary[EitherT[List, String, Int]]].arbitrary.sample.get
    }
    assertCompiles("x: EitherT[List, String, Int]")
    val y = x.coerce[List[Either[String, Int]]]
    assertCompiles("y: List[Either[String, Int]]")
  }

  it should "support deriving type class instances for newtypes with type params via deriving" in {
    @newtype(deriving = deriving[Arbitrary]) case class EitherT[F[_], L, R](private val x: F[Either[L, R]])
    val x = {
      import scala.Predef._
      scala.Predef.implicitly[Arbitrary[EitherT[List, String, Int]]].arbitrary.sample.get
    }
    assertCompiles("x: EitherT[List, String, Int]")
    val y = x.coerce[List[Either[String, Int]]]
    assertCompiles("y: List[Either[String, Int]]")
  }

  it should "support auto-deriving type class instances for newtypes with type params" in {
    @newtype case class EitherT[F[_], L, R](private val x: F[Either[L, R]])
    object EitherT {
      // Auto-derive all type classes of kind * -> *
      implicit def typeclass[T[_], F[_], L, R](
        implicit t: T[F[Either[L, R]]]
      ): T[EitherT[F, L, R]] = deriving
    }
    val x = {
      import scala.Predef._
      scala.Predef.implicitly[Arbitrary[EitherT[List, String, Int]]].arbitrary.sample.get
    }
    assertCompiles("x: EitherT[List, String, Int]")
    val y = x.coerce[List[Either[String, Int]]]
    assertCompiles("y: List[Either[String, Int]]")
  }

  it should "support auto-deriving type class instances for newtypes with type params via coerce" in {
    @newtype case class EitherT[F[_], L, R](private val x: F[Either[L, R]])
    object EitherT {
      // Auto-derive all type classes of kind * -> *
      implicit def typeclass[T[_], F[_], L, R](
        implicit t: T[F[Either[L, R]]]
      ): T[EitherT[F, L, R]] = t.coerce
    }
    val x = {
      import scala.Predef._
      scala.Predef.implicitly[Arbitrary[EitherT[List, String, Int]]].arbitrary.sample.get
    }
    assertCompiles("x: EitherT[List, String, Int]")
    val y = x.coerce[List[Either[String, Int]]]
    assertCompiles("y: List[Either[String, Int]]")
  }

  behavior of "this"

  it should "work in extension methods" in {
    val x0 = Maybe(null: String)
    val x1 = x0.filter(_.contains("a"))
    x1.isEmpty shouldBe true
    x1 shouldBe Maybe.empty
    x1 shouldBe (null: Any)

    val y0 = Maybe("apple")
    val y1 = y0.filter(_.contains("a"))
    y1.isDefined shouldBe true
    y1 shouldBe "apple"

    val z0 = Maybe("apple")
    val z1 = z0.filter(_.contains("z"))
    z1.isEmpty shouldBe true
    z1 shouldBe Maybe.empty
    z1 shouldBe (null: Any)

    val n0 = Maybe(0)
    val n1 = n0.filter(_ > 0)
    n1.isEmpty shouldBe true
    n1 shouldBe Maybe.empty
    n1 shouldBe (null: Any)
  }

  behavior of "nested @newtypes"

  it should "work" in {
    val x = Nested(Foo(1))
    assertCompiles("x: Nested")
    val y = x.coerce[Foo]
    assertCompiles("y: Foo")
    val z = y.coerce[Int]
    assertCompiles("z: Int")
  }

  behavior of "Id[A]"

  it should "work" in {
    val x = Id(1)
    x shouldBe 1
    assertCompiles("x: Id[Int]")
    val y = Id(1).map(_ + 2)
    y shouldBe 3
    assertCompiles("y: Id[Int]")
    val z = Id(2).flatMap(x => Id(x * 2))
    z shouldBe 4
    assertCompiles("z: Id[Int]")
  }

  behavior of "optimizeOps = false"

  it should "work with @newtype" in {
    @newtype(optimizeOps = false) case class Foo(value: Int)
    Foo(1: Int).value shouldBe 1
    @newtype(optimizeOps = false) case class Bar[A](value: A)
    Bar("foo").value shouldBe "foo"
  }

  it should "work with @newsubtype" in {
    @newsubtype(optimizeOps = false) case class Foo(value: Int)
    Foo(1: Int).value shouldBe 1
    @newsubtype(optimizeOps = false) case class Bar[A](value: A)
    Bar("foo").value shouldBe "foo"
  }

  behavior of "Coercible"

  it should "work for nested type constructors" in {
    val x = List(Option(1))
    val y = x.coerce[List[Option[Foo]]]
  }

  "unapply = true" should "generate an unapply method" in {
    @newtype   (unapply = true) case class X0(private val x: String)
    @newtype   (unapply = true) case class X1[A](private val x: A)
    @newsubtype(unapply = true) case class Y0(private val x: String)
    @newsubtype(unapply = true) case class Y1[A](private val x: A)

    // Note that we're using (x0: String) to assert the type of x0 at compile time.
    // Also checking that unapply doesn't compile for ill-typed expressions.

    val x0 = X0("x") match { case X0(x) => x }
    (x0: String) shouldBe "x"
    assertTypeError(""" "x" match { case X0(x) => x }""")
    assertTypeError("""  1  match { case X0(x) => x }""")

    val x1 = X1("x") match { case X1(x) => x }
    (x1: String) shouldBe "x"
    assertTypeError(""" "x" match { case X1(x) => x }""")
    assertTypeError("""  1  match { case X1(x) => x }""")

    val y0 = Y0("y") match { case Y0(x) => x }
    (y0: String) shouldBe "y"
    assertTypeError(""" "x" match { case Y0(x) => x }""")
    assertTypeError("""  1  match { case Y0(x) => x }""")

    val y1 = Y1("y") match { case Y1(x) => x }
    (y1: String) shouldBe "y"
    assertTypeError(""" "x" match { case Y1(x) => x }""")
    assertTypeError("""  1  match { case Y1(x) => x }""")
  }

  // Unfortunately, we don't have a way to assert on compiler warnings, which is
  // what happens with the code below. If we run with -Xfatal-warnings, the test
  // won't compile at all, so leaving here to do manual checking until scalatest
  // can provide support for this.
  // See https://github.com/scalatest/scalatest/issues/1352
  //"type-based pattern matching" should "emit compiler warnings" in {
  //  assertDoesNotCompile("Foo(1) match { case x: Foo => x }")
  //  assertDoesNotCompile("1 match { case x: Foo => x }")
  //  assertDoesNotCompile(""" "foo" match { case x: Foo => x }""")
  //  assertDoesNotCompile("(1: Any) match { case x: Foo => x }")
  //}
}

object NewTypeMacrosTest {

  @newtype case class Foo(value: Int)

  @newtype case class Nested(value: Foo)

  @newtype case class Bar(value: Int) {
    def twice: Bar = Bar(value * 2)
  }

  @newtype class Baz(value: String)
  object Baz {
    def create(value: String): Baz = value.toUpperCase.coerce[Baz]
  }

  @newtype class Baz2(val value: String)
  object Baz2 {
    def create(value: String): Baz2 = value.toUpperCase.coerce[Baz2]
  }

  @newtype case class OptionT[F[_], A](value: F[Option[A]])

  @newtype case class Sub[A <: java.util.Map[String, Int]](value: A)

  @newtype case class Cov[+A](value: List[A])

  @newtype case class Maybe[A](unsafeGet: A) {
    def isEmpty: Boolean = unsafeGet == null
    def isDefined: Boolean = unsafeGet != null
    def map[B](f: A => B): Maybe[B] = if (isEmpty) Maybe.empty else Maybe(f(unsafeGet))
    def flatMap[B](f: A => Maybe[B]): Maybe[B] = if (isEmpty) Maybe.empty else f(unsafeGet)
    def filter(p: A => Boolean): Maybe[A] = if (isEmpty || !p(unsafeGet)) Maybe.empty else this
    def filterNot(p: A => Boolean): Maybe[A] = if (isEmpty || p(unsafeGet)) Maybe.empty else this
    def orElse(ma: => Maybe[A]): Maybe[A] = if (isDefined) this else ma
    def getOrElse(a: => A): A = if (isDefined) unsafeGet else a
    def getOrThrow: A = if (isDefined) unsafeGet else throw new NoSuchElementException("Maybe.empty.get")
    def cata[B](ifEmpty: => B, ifDefined: A => B): B = if (isEmpty) ifEmpty else ifDefined(unsafeGet)
    def fold[B](ifEmpty: => B)(ifDefined: A => B): B = cata(ifEmpty, ifDefined)
    def contains(a: A): Boolean = isDefined && unsafeGet == a
    def exists(p: A => Boolean): Boolean = isDefined && p(unsafeGet)
  }
  object Maybe {
    def empty[A]: Maybe[A] = null.asInstanceOf[Maybe[A]]
    def fromOption[A](x: Option[A]): Maybe[A] = (if (x.isDefined) x.get else null).asInstanceOf[Maybe[A]]
  }

  @newtype case class Id[A](value: A) {
    def map[B](f: A => B): Id[B] = Id(f(value))
    def flatMap[B](f: A => Id[B]): Id[B] = f(value)
  }

  trait Functor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }

  object Functor {
    implicit val list: Functor[List] = new Functor[List] {
      override def map[A, B](fa: List[A])(f: A => B): List[B] = fa.map(f)
    }
  }
}
