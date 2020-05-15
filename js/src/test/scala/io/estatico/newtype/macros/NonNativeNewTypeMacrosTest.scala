package io.estatico.newtype.macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import io.estatico.newtype.ops._
import org.scalacheck.Arbitrary

class NonNativeNewTypeMacrosTest extends AnyFlatSpec with Matchers {

  import NewTypeMacrosTest._

  behavior of "@newtype with type bounds"

  it should "enforce type bounds" in {
    val x = Sub(new java.util.HashMap[String, Int]): Sub[java.util.HashMap[String, Int]]
    val y = Sub(new java.util.concurrent.ConcurrentHashMap[String, Int])

    assertCompiles("x: Sub[java.util.HashMap[String, Int]]")
    assertCompiles("y: Sub[java.util.concurrent.ConcurrentHashMap[String, Int]]")

    assertDoesNotCompile("x: Sub[java.util.concurrent.ConcurrentHashMap[String, Int]]")
    assertDoesNotCompile("y: Sub[java.util.HashMap[String, Int]]")
  }

}
