# NewType

NewTypes for Scala with no runtime overhead.

[![Build Status](https://travis-ci.org/estatico/scala-newtype.svg?branch=master)](https://travis-ci.org/estatico/scala-newtype)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/estatico/scala-newtype)
[![Maven Central](https://img.shields.io/maven-central/v/io.estatico/newtype_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/io.estatico/newtype_2.12)

## Getting NewType

If you are using SBT, add the following line to your build file -

```scala
libraryDependencies += "io.estatico" %% "newtype" % "0.1.0"
```

For Maven or other build tools, see the Maven Central badge at the top of this README.

## Usage

The easiest way to get going is to create an object that extends from
`NewType.Of` -

```scala
import io.estatico.newtype.NewType

object WidgetId extends NewType.Of[Int]
```

This will function as your companion object. Use the `.Type` type member to get access
to the type for signatures. A common pattern is to include this in a package object
so it can be easily imported.

```scala
package object types {
  type WidgetId = WidgetId.Type
  object WidgetId extends NewType.Of[Int]
}
```

Now you can import `types.WidgetId` and use it in type signatures as well as the companion
object.

### `NewType.Of` vs. `NewType.Default`

Extending `NewType.Of` simply creates the newtype wrapper; however, you will often
want to extend `NewType.Default` to provide some helper methods on the companion
object -

```scala
// Safely casts an Int to a WidgetId
scala> WidgetId(1)
res0: WidgetId.Type = 1

// Safely casts M[Int] to M[WidgetId]
scala> WidgetId.applyM(List(1, 2))
res1: List[WidgetId.Type] = List(1, 2)
```

See `NewTypeExtras` for the available mixins for creating newtype wrappers.

If you wish to do something different, you can supply your own smart-constructor
instead -

```scala
object Nat extends NewType.Of[Int] {
  def apply(n: Int): Option[Type] = if (n < 0) None else Some(wrap(n))
}
```

The `wrap` method you see here is actually just explicit usage of the
implicit instance of `Coercible[Int, Nat.Type]`.
See the section on [Coercible](#coercible) for more info.

### Extension methods

You probably want to be able to add methods to your newtypes. You can do this using
Scala's extension methods via implicit classes -

```scala
type Point = Point.Type
object Point extends NewType.Of[(Int, Int)] {

  def apply(x: Int, y: Int): Type = wrap((x, y))

  implicit final class Ops(val self: Type) extends AnyVal {
    def toTuple: (Int, Int) = unwrap(self)
    def x: Int = toTuple._1
    def y: Int = toTuple._2
  }
}
```
```scala
scala> val p = Point(1, 2)
p: Point.Type = (1,2)

scala> p.toTuple
res7: (Int, Int) = (1,2)

scala> p.x
res8: Int = 1

scala> p.y
res9: Int = 2
```

### Type class instances and implicits

As mentioned, the object you create via extending one of the `NewType` helpers
functions as the companion object for your newtype. As such, you can leverage this
for type class instances to avoid orphan instances -

```scala
object Nat extends NewType.Of[Int] {
  implicit val show: Show[Type] = Show.instance(_.toString)
}
```

If you use `NewType.Default`, you can use the `deriving` method to derive
type class instances for those that exist for your newtype's base type.

```scala
object Nat extends NewType.Default[Int] {
  implicit def show: Show[Type] = deriving
}
```

As long as an implicit instance of `Show[Int]` exists in scope, `deriving` will
cast the instance to one suitable for your newtype. This is similar to GHC Haskell's
`GeneralizedNewtypeDeriving` extension.

### NewSubType

With `NewType`, you get a brand new type that can't be used as the type you
are wrapping.

```scala
type Nat = Nat.Type
object Nat extends NewType.Default[Int]

def plus(x: Int, y: Int): Int = x + y
```
```scala
scala> plus(Nat(1), Nat(2))
<console>:19: error: type mismatch;
 found   : Nat.Type
 required: Int
```

If you wish for your newtype to be a subtype of the type you are wrapping,
you can use `NewSubType` -

```scala
type Nat = Nat.Type
object Nat extends NewSubType.Default[Int]

def plus(x: Int, y: Int): Int = x + y
```
```scala
scala> plus(Nat(1), Nat(2))
res0: Int = 3
```

### Coercible

This library introduces the `Coercible` type class for types that can safely
be cast to/from newtypes. This is mostly useful when you want to write code
that can work generically with newtypes or to simply leverage the compiler
to tell you when you can do `.asInstanceOf`.

**NOTE: You generally shouldn't be creating instances of Coercible yourself.**
This library is designed to create the instances needed for you which are safe.
If you manually create instances, you may be permitting unsafe operations which will
lead to runtime casting errors.

With that out of the way, here's how we can do safe casting with Coercible -

```scala
type Point = Point.Type
object Point extends NewType.Of[(Int, Int)]
```
```scala
scala> Coercible[Point, (Int, Int)]
res10: io.estatico.newtype.Coercible[Point,(Int, Int)] = io.estatico.newtype.Coercible$$anon$1@56c24c2a

scala> Coercible[(Int, Int), Point]
res11: io.estatico.newtype.Coercible[(Int, Int),Point] = io.estatico.newtype.Coercible$$anon$1@56c24c2a

scala> Coercible[String, Point]
<console>:21: error: could not find implicit value for parameter ev: io.estatico.newtype.Coercible[String,Point]
       Coercible[String, Point]
```

This library provides extension methods for safe casting as well -

```scala
scala> import io.estatico.newtype.ops._
import io.estatico.newtype.ops._

scala> val p = Point(1, 2)
p: Point.Type = (1,2)

scala> p.coerce[(Int, Int)]
res14: (Int, Int) = (1,2)

scala> (3, 4).coerce[Point]
res15: Point = (3,4)

scala> (3.2, 4.3).coerce[Point]
<console>:24: error: could not find implicit value for parameter ev: io.estatico.newtype.Coercible[(Double, Double),Point]
       (3.2, 4.3).coerce[Point]
                        ^
```

## Motivation

The Haskell language provides a `newtype` keyword for creating new types from existing
ones without runtime overhead.

```haskell
newtype WidgetId = WidgetId Int

lookupWidget :: WidgetId -> Maybe Widget
lookupWidget (WidgetId wId) = lookup wId widgetDB
```

In the example above, the `WidgetId` type is simply an `Int` at runtime; however, the
compiler will treat it as its own type at compile time, helping you to avoid errors.
In this case, we can be sure that the ID we are providing to our `lookupWidget` function
refers to a `WidgetId` and not some other entity nor an arbitrary `Int` value.

This library attempts to bring newtypes to Scala.

### Tagged Types

Both Scalaz and Shapeless provide a feature known as _Tagged Types_. This library
operates on roughly the same principle except provides the proper infrastructure
needed to -

* Control whether newtypes are or are not subtypes of their wrapped type instead of picking
    a side (Shapeless' are subtypes, Scalaz's are not)
* Easily provide methods for newtypes
* Resolve implicits and type class instances defined in the companion object
* Optimize constructing newtypes via casting with automatic smart constructors
* Provide facilities to operate generically on newtypes
* Support safe casting generically via the `Coercible` type class
