package io.estatico.newtype

package object macros {

  // This will trigger "it is not recommended to define
  // classes/objects inside of package objects" if the macro
  // generates an indirection trait on scala 2.11+
  @newtype case class TestForUnwantedIndirection(x: Int)
  @newsubtype case class TestForUnwantedIndirectionSubtype(x: Int)

}
