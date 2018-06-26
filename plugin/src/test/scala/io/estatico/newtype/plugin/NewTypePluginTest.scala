package io.estatico.newtype.plugin

import org.scalatest.{FlatSpec, Matchers}
import scala.Int
import scala.Predef.classOf

class NewTypePluginTest extends FlatSpec with Matchers {

  import NewTypePluginTest.Foo

  "plugin" should "work" in {
    val foo: Foo = Foo(1)
    Foo.getClass.getMethod("apply", classOf[Int]).getReturnType shouldBe classOf[Int]
  }
}

object NewTypePluginTest {

  // Temporary stub
  class newtype

  @newtype case class Foo(value: Int)
}
