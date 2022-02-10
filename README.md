# NewType

NewTypes for Scala with no runtime overhead.

[![Build Status](https://travis-ci.org/estatico/scala-newtype.svg?branch=master)](https://travis-ci.org/estatico/scala-newtype)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/estatico/scala-newtype)
[![Maven Central](https://img.shields.io/maven-central/v/io.estatico/newtype_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/io.estatico/newtype_2.13)

## Getting NewType

If you are using SBT, add the following line to your build file -

```scala
libraryDependencies += "io.estatico" %% "newtype" % "0.4.4"
```

Make sure you have [macro-paradise](https://docs.scala-lang.org/overviews/macros/paradise.html) enabled
 - for Scala 2.13.0-M3 and lower add the following line to your build file
```scala
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
```
- for Scala 2.13.0-M4 and above via compiler flag [`-Ymacro-annotations`](https://github.com/scala/scala/pull/6606)

For Maven or other build tools, see the Maven Central badge at the top of this README.

## Motivation

This is an alternative to scala's native value classes. 

Why this is better than simply `case class Thing(value: String) extends AnyVal`? 

That is because value classes [will sometime allocate](https://docs.scala-lang.org/overviews/core/value-classes.html#allocation-summary).
In contratry `@newtype` is allocation free.

For more detailed comparison see https://failex.blogspot.com/2017/04/the-high-cost-of-anyval-subclasses.html

## Usage

For generating newtypes via the `@newtype` macro, see [@newtype macro](#newtype-macro)
For non-macro usage, see the section on [Legacy encoding](#legacy-encoding).

### @newtype macro

As of newtype 0.2, you can now encode newtypes using the `@newtype` macro. Its implementation
and usage aligns closely with idiomatic Scala syntax, so IDE support _just works_ out of the box.

```scala
import io.estatico.newtype.macros.newtype

package object types {

  @newtype case class WidgetId(toInt: Int)
}
```

This expands into a `type` and companion `object` definition, so newtypes _must_ be defined
in an `object` or `package object`.

The example above will generate code similar to the following -

```scala
package object types {
  type WidgetId = WidgetId.Type
  object WidgetId {
    type Repr = Int
    type Base = Any { type WidgetId$newtype }
    trait Tag extends Any
    type Type <: Base with Tag

    def apply(x: Int): WidgetId = x.asInstanceOf[WidgetId]

    implicit final class Ops$newtype(val $this$: Type) extends AnyVal {
      def toInt: Int = $this$.asInstanceOf[Int]
    }
  }
}
```

You can also create newtypes which have type parameters -

```scala
@newtype case class EitherT[F[_], L, R](x: F[Either[L, R]])
```

Note that it is impossible to have your newtype _extend_ any types, which
makes sense since it has its own distinct type at compile time and at runtime is just
the underlying value.

Also, since the `@newtype` annotation gives your type a distinct type at compile-time,
primitives will naturally box as they do when they are applied in any generic context.
See the following section on `@newsubtype` for unboxed primitive newtypes.

#### @newsubtype macro

As of newtype 0.4 you now have access to the `@newsubtype` macro. Its usage is identical
to `@newtype`. The difference is that it functions as a _subtype_ of the underlying
type as opposed to having a completely different type at compile time. This may or may
not be desirable, and it's recommended to use `@newtype` if you're not entirely sure you
actually need `@newsubtype`.

The difference in the generated code is that `@newsubtype` defines its `Base` type defined as -

```scala
type Base = Repr
```

The main benefit of `@newsubtype` is that primitives are unboxed. For example, the
following `@newtype` definition will box the `Int`, making it a `java.lang.Integer`
at runtime -

```scala
@newtype case class Foo(x: Int)
```

However, the following `@newsubtype` definition will be a primitive `int` at runtime -

```scala
@newsubtype case class Bar(x: Int)
```

Note however that calling `getClass` on a newsubtype will fool you -

```scala
scala> Bar(1).getClass
res2: Class[_ <: Bar] = class java.lang.Integer
```

Reason is that scalac boxes unnecessarily when calling `getClass`, see https://github.com/scala/bug/issues/10770

We can confirm that we do in fact have a primitive `int` at runtime back by inspecting the byte code -

```scala
scala> class Test { def test = Bar(1) }
scala> :javap Test
```
```java
...
  public int test();
...
```

Another "feature" of `@newsubtype` is that its values can be passed to functions
which accept its `Repr` type without needing to convert them first -

```scala
scala> def half(b: Int): Int = b / 2
scala> half(Bar(12))
res6: Int = 6
```

Note that this feature can be undesirable since the newsubtype will be automatically
unwrapped, even when you might not mean to. Again, unless you have a good reason to use
`@newsubtype`, it's recommend to use `@newtype` by default.

#### Smart Constructors and Accessor Methods

This library gives you a few choices when it comes to defining smart constructors
and accessor methods for your newtypes. Efforts have been made to keep things idiomatic.
Note that extractors (`unapply` methods) are **not** generated by newtypes.

Using `case class` gives us a smart constructor (an `apply` method on the companion object)
that will accept a value of type `A` and return the newtype `N`.

```scala
@newtype case class N(a: A)
```

You also get an accessor extension method to get the underlying `A`. Note that you can
prevent this by defining the field as private.

```scala
@newtype case class N(private val a: A)
```

Using `class` will not generate a smart constructor (no `apply` method). This allows
you to specify your own. Note that `new` never works for newtypes and will fail to compile.

If you wish to generate an accessor method for your underlying value, you can define it as `val`
just as if you were dealing with a normal class.

```scala
@newtype class N(val a: A)
```

If you need to define your own smart constructor, use the
`.coerce` extension method to cast to your newtype.

```scala
import io.estatico.newtype.ops._

@newtype class Id(val strValue: String)

object Id {
  def fromString(str: String): Either[String, Id] = {
    if (str.isEmpty) Left("Id cannot be empty")
    else Right(str.coerce)
  }
}
```

#### Extension Methods

Defining extension methods are as simple as defining normal methods in any class -

```scala
@newtype case class OptionT[F[_], A](value: F[Option[A]]) {

  def fold[B](default: => B)(f: A => B)(implicit F: Functor[F]): F[B] =
    F.map(value)(_.fold(default)(f))

  def cata[B](default: => B, f: A => B)(implicit F: Functor[F]): F[B] =
    fold(default)(f)

  def map[B](f: A => B)(implicit F: Functor[F]): OptionT[F, B] =
    OptionT(F.map(value)(_.map(f)))
}
```

#### Companion Objects

The companion object works just as you'd expect. You can place your type class instances
there and implicit resolution just works.

Companion objects also contain special `deriving` and `derivingK`
methods to auto-derive instances for you if one exists for your underlying type.
This is similar to GHC Haskell's `GeneralizedNewtypeDeriving` extension.

`deriving` is used for type classes whose type parameter is _not_ higher kinded.

```scala
@newtype case class Text(s: String)
object Text {
  implicit val arb: Arbitrary[Text] = deriving
}
```

`derivingK` is used for type classes whose type parameter _is_ higher kinded.

```scala
@newtype class Nel[A](val toList: List[A])
object Nel {
  def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce[Nel[A]]
  implicit val functor: Functor[Nel] = derivingK
}
```

Note that since these methods are created by the `@newtype` macro, IDEs will generally
not be able to resolve them. If the red highlighting bothers you, you can use
`.coerce` to safely cast the base type class to support your newtype -

```scala
import io.estatico.newtype.ops._

@newtype case class Text(s: String)
object Text {
  implicit val arb: Arbitrary[Text] = implicitly[Arbitrary[String]].coerce
}

@newtype class Nel[A](val toList: List[A])
object Nel {
  def apply[A](head: A, tail: List[A]): Nel[A] = (head +: tail).coerce[Nel[A]]
  implicit val functor: Functor[Nel] = implicitly[Functor[List]].coerce
}
```

### Coercible Instance Trick

**Note that this is NOT recommended!**

In some cases, you may want to automatically derive a type class instance
for all newtypes by leveraging `Coercible`. While seemingly convenient, this is
**NOT** recommended as it in some ways goes against the spirit of using
a newtype in the first place. Specializing a specific instance for a
newtype will be tricky and will require clever implicit scoping. Also,
it can [greatly increase your compile times](https://github.com/estatico/scala-newtype/issues/64).
Instead, it's generally better to explicitly define instances for your
newtypes.

**You have been warned!**

The following example generates an `Eq` instance for all newtypes in which
their underlying `Repr` type has an `Eq` instance.

```scala
scala> :paste

import cats._, cats.implicits._

/** If we have an Eq instance for Repr type R, derive an Eq instance for newtype N. */
implicit def coercibleEq[R, N](implicit ev: Coercible[Eq[R], Eq[N]], R: Eq[R]): Eq[N] =
  ev(R)

@newtype case class Foo(x: Int)

// Exiting paste mode, now interpreting.

scala> Foo(1) === Foo(2)
res0: Boolean = false
```

However, as mentioned, it's generally better to explicitly define your
instances.

```scala
@newtype case class Foo(x: Int)
object Foo {
  implicit val eq: Eq[Foo] = deriving
}
```

You may not always be able to put your instance in the companion object, likely
because the type class is not available where you are defining your newtype.
In this case, simply define an orphan instance and import it where you need.

```scala
object EqOrphans {
  implicit val eqFoo: Eq[Foo] = implicitly[Eq[Int]].coerce
}
```

### Legacy encoding

If you don't wish to use the macro API, you can still use the legacy API for building
newtypes manually via companion objects. Note that this method does not support newtypes
with type parameters. If you need type parameters, use the macro API.

The easiest way to get going with the legacy encoding is to create an object that extends from
`NewType.Default` -

```scala
import io.estatico.newtype.NewType

object WidgetId extends NewType.Default[Int]
```

This will be the companion object for your newtype. Use the `.Type` type member to get access
to the type for signatures. A common pattern is to include this in a package object
so it can be easily imported.

```scala
package object types {
  type WidgetId = WidgetId.Type
  object WidgetId extends NewType.Default[Int]
}
```

Now you can import `types.WidgetId` and use it in type signatures as well as the companion
object.

#### `NewType.Of` vs. `NewType.Default`

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

#### Legacy extension methods

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

#### Legacy type class instances and implicits

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

#### Legacy NewSubType

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

## Coercible

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
