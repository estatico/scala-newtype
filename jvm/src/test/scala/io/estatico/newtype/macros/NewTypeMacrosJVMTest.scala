package io.estatico.newtype.macros

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NewTypeMacrosJVMTest extends AnyFlatSpec with Matchers {

  behavior of "@newsubtype"

  it should "not box primitives" in {
    // Introspect the runtime type returned by the `apply` method
    def ctorReturnType(o: Any) = scala.Predef.genericArrayOps(o.getClass.getMethods).find(_.getName == "apply").get.getReturnType

    // newtypes will box primitive values.
    @newtype case class BoxedInt(private val x: Int)
    ctorReturnType(BoxedInt) shouldBe scala.Predef.classOf[Object]

    // newsubtypes will NOT box primitive values.
    @newsubtype case class UnboxedInt(private val x: Int)
    ctorReturnType(UnboxedInt) shouldBe scala.Predef.classOf[Int]
  }
}
