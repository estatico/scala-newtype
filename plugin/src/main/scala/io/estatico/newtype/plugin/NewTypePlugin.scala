package io.estatico.newtype.plugin

import scala.tools.nsc.{Global, Phase}
import scala.tools.nsc.plugins.{Plugin, PluginComponent}
import scala.tools.nsc.transform.TypingTransformers

class NewTypePlugin(override val global: Global) extends Plugin {

  import global._

  private val debug = false

  override val name: String = "newtype"

  override val components: List[PluginComponent] = List(
    RewritePhase
  )

  override val description: String = "generates newtypes"

  private val triggers: List[TypeName] = List(newTypeName("newtype"))

  private def info(pos: Position, msg: String): Unit = global.reporter.info(pos, msg, force = true)

  private def fail(msg: String): Nothing = global.abort(msg)

  private object RewritePhase extends PluginComponent with TypingTransformers {
    override val global: NewTypePlugin.this.global.type = NewTypePlugin.this.global
    override val phaseName: String = NewTypePlugin.this.name
    override val runsAfter: List[String] = List("parser")
    override val runsBefore: List[String] = List("namer")

    private def extractNewtypeClassDef(t: Tree): Option[ClassDef] = t match {
      case c: ClassDef if hasNewtypeAnn(c.mods) => Some(c)
      case _                                    => None
    }

    private def hasNewtypeAnn(mods: Modifiers): Boolean =
      triggers.exists(mods.hasAnnotationNamed)

    private def newTransformer(unit: CompilationUnit): TypingTransformer =
      new TypingTransformer(unit) {
        override def transform(tree: Tree): Tree = doTransform(super.transform(tree))
      }

    private def doTransform(tree: Tree): Tree = {
      if (!tree.exists(t => extractNewtypeClassDef(t).isDefined)) tree else {
        tree match {
          case Template(parents, self, body) =>
            val newBody = body.flatMap { t =>
              extractNewtypeClassDef(t) match {
                case None => List(t)
                case Some(ClassDef(mods, clsName, tparams, impl)) =>
                  val objName = clsName.toTermName
                  val valDef = getCtorValDef(impl)
                  List(
                    q"type $clsName <: (scala.Any { type __newtypeTag }) with $objName.Tag",
                    q"""
                      object $objName {
                        trait Tag extends scala.Any
                        def apply(x: ${valDef.tpt}): $clsName = x.asInstanceOf[$clsName]
                      }
                    """
                  )
              }
            }
            Template(parents, self, newBody)
          case _ => tree
        }
      }
    }

    private def getCtorValDef(impl: Template): ValDef = impl.body.collectFirst {
      case dd: DefDef if dd.name == termNames.CONSTRUCTOR =>
        dd.vparamss match {
          case List(List(vd)) => vd
          case _ => fail("Unsupported constructor, must have exactly one argument")
        }
    }.getOrElse(fail("Failed to locate constructor"))

    override def newPhase(prev: Phase): Phase = new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit =
        newTransformer(unit).transformUnit(unit)
    }
  }
}

