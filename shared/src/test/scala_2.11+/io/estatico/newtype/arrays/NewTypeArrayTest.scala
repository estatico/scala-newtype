package io.estatico.newtype.arrays

import io.estatico.newtype.macros._
import io.estatico.newtype.macros.NewTypeMacrosTest.Functor
import io.estatico.newtype.ops._
import org.scalatest.{FlatSpec, Matchers}

class NewTypeArrayTest extends FlatSpec with Matchers {

  behavior of "NewTypeArray"

  it should "work with @newtype X(Int)" in {
    @newtype case class X(private val value: Int)

    val cls = scala.Predef.classOf[Array[Int]]
    val v: Int = 1

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X].downcast(a)
    assertCompiles("a2: Array[Int]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X].upcast(a2)
    assertCompiles("a3: Array[X]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[Int]]
    assertCompiles("a4: Array[Int]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X]]
    assertCompiles("a5: Array[X]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }

  it should "work with @newsubtype X(Int)" in {
    @newsubtype case class X(private val value: Int)

    val cls = scala.Predef.classOf[Array[Int]]
    val v: Int = 1

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X].downcast(a)
    assertCompiles("a2: Array[Int]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X].upcast(a2)
    assertCompiles("a3: Array[X]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[Int]]
    assertCompiles("a4: Array[Int]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X]]
    assertCompiles("a5: Array[X]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }

  it should "work with @newtype X[A](List[A])" in {
    @newtype case class X[A](private val value: List[A])

    val cls = scala.Predef.classOf[Array[List[Int]]]
    val v: List[Int] = List(1)

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X[Int]]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X[Int]].downcast(a)
    assertCompiles("a2: Array[List[Int]]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X[Int]].upcast(a2)
    assertCompiles("a3: Array[X[Int]]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[List[Int]]]
    assertCompiles("a4: Array[List[Int]]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X[Int]]]
    assertCompiles("a5: Array[X[Int]]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }

  it should "work with @newsubtype X[A](List[A])" in {
    @newsubtype case class X[A](private val value: List[A])

    val cls = scala.Predef.classOf[Array[List[Int]]]
    val v: List[Int] = List(1)

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X[Int]]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X[Int]].downcast(a)
    assertCompiles("a2: Array[List[Int]]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X[Int]].upcast(a2)
    assertCompiles("a3: Array[X[Int]]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[List[Int]]]
    assertCompiles("a4: Array[List[Int]]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X[Int]]]
    assertCompiles("a5: Array[X[Int]]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }

  it should "work with @newtype X[F[_]](Functor[F])" in {
    @newtype case class X[F[_]](private val value: Functor[F])

    val cls = scala.Predef.classOf[Array[Functor[List]]]
    val v: Functor[List] = Functor.list

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X[List]]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X[List]].downcast(a)
    assertCompiles("a2: Array[Functor[List]]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X[List]].upcast(a2)
    assertCompiles("a3: Array[X[List]]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[Functor[List]]]
    assertCompiles("a4: Array[Functor[List]]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X[List]]]
    assertCompiles("a5: Array[X[List]]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }

  it should "work with @newsubtype X[F[_]](Functor[F])" in {
    @newsubtype case class X[F[_]](private val value: Functor[F])

    val cls = scala.Predef.classOf[Array[Functor[List]]]
    val v: Functor[List] = Functor.list

    val a = NewTypeArray(X(v))
    assertCompiles("a: Array[X[List]]")
    a.apply(0) shouldBe v
    a.getClass shouldBe cls

    val a2 = NewTypeArray[X[List]].downcast(a)
    assertCompiles("a2: Array[Functor[List]]")
    a2.apply(0) shouldBe v
    a2.getClass shouldBe cls

    val a3 = NewTypeArray[X[List]].upcast(a2)
    assertCompiles("a3: Array[X[List]]")
    a3.apply(0) shouldBe v
    a3.getClass shouldBe cls

    val a4 = a.coerce[Array[Functor[List]]]
    assertCompiles("a4: Array[Functor[List]]")
    a4.apply(0) shouldBe v
    a4.getClass shouldBe cls

    val a5 = a4.coerce[Array[X[List]]]
    assertCompiles("a5: Array[X[List]]")
    a5.apply(0) shouldBe v
    a5.getClass shouldBe cls
  }
}
