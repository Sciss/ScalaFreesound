/*
 *  FilterViewImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound
package swing
package impl

import de.sciss.model.impl.ModelImpl
import de.sciss.swingplus.{GroupPanel, PopupMenu}

import scala.swing.event.{ButtonClicked, EditDone}
import scala.swing.{Action, BoxPanel, Button, CheckBox, Component, FlowPanel, Label, MenuItem, Orientation, ScrollPane, SequentialContainer, Swing, TextField}
import Swing._

object FilterViewImpl {
  def apply(init: Filter): FilterView = new Impl(init)

  private final class EditButton(pop: => PopupMenu) extends Button("\u2335") {
    private[this] lazy val _pop = pop

    listenTo(this)
    reactions += {
      case ButtonClicked(_) =>
        _pop.show(this, 0, peer.getHeight)
    }
  }

  private trait StringPartParent {
    def fireChange(): Unit
    def remove(child: StringPart): Unit
    def replace(before: StringPart, now: StringPart): Unit
  }

  private sealed trait StringPart {
    private[this] var _parent: StringPartParent = _

    final def parent: StringPartParent = {
      require(_parent != null)
      _parent
    }

    final def parent_=(value: StringPartParent): Unit =
      _parent = value

    def component: Component

    def toExpr: StringExpr
  }

  private def mkPopupStringConst(p: StringPartConst): PopupMenu = new PopupMenu {
    contents += new MenuItem(Action("\u00d7 remove") {
      p.parent.remove(p)
    })
    contents += new MenuItem(Action("\u22c1 or") {
      val app = new StringPartConst("")
      val par = p.parent  // catch it early!
      val or  = StringPartOr (p :: app :: Nil)
      par.replace(p, or)
    })
    contents += new MenuItem(Action("\u22c0 and") {
      val app = new StringPartConst("")
      val par = p.parent  // catch it early!
      val and = StringPartAnd(p :: app :: Nil)
      par.replace(p, and)
    })
    contents += new MenuItem(Action("\u00ac not") {
      val par = p.parent  // catch it early!
      val not = StringPartNot(p)
      par.replace(p, not)
    })
  }

  private final class StringPartConst(s0: String) extends StringPart {
    private[this] lazy val ggText = {
      val res         = new TextField(4)
      res.minimumSize = res.preferredSize
      res.columns     = 6
      res
    }

    lazy val component: Component = {
      ggText.listenTo(ggText)
      ggText.reactions += {
        case EditDone(_) => parent.fireChange()
      }
      val ggEdit = new EditButton(mkPopupStringConst(this))
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggText
      box.contents += ggEdit
      box
    }

    def toExpr: StringExpr = ggText.text
  }

  private final case class ChildPosition(child: Int, start: Int, count: Int)

  private sealed trait StringParentContainerBase extends StringPartParent {
    protected var _children: List[StringPart]

    protected def childPosition(idx: Int): ChildPosition

    def fireChange(): Unit

    def component: Component with SequentialContainer.Wrapper

    protected def becameEmpty(): Unit
    protected def becameSingle(child: StringPart): Unit

    final def remove(child: StringPart): Unit = {
      val idx = _children.indexOf(child)
      require(idx >= 0)
      val newChildren = _children.patch(idx, Nil, 1)
      newChildren match {
        case Nil            => becameEmpty()
        case single :: Nil  => becameSingle(single)
        case _ =>
          val pos = childPosition(idx)
          component.contents.remove(pos.start, pos.count)
          setChildrenAndRevalidate(newChildren)
      }
    }

    final def replace(before: StringPart, now: StringPart): Unit = {
      val idx = _children.indexOf(before)
      require(idx >= 0)
      val newChildren = _children.patch(idx, now :: Nil, 1)
      now.parent  = this
      val pos     = childPosition(idx)
      // because `now.component` is lazy and
      // might add the before component, make
      // sure we remove first, and not call `update`
      val compPar = component
      compPar.contents.remove(pos.child)
      val compNow = now.component
      compPar.contents.insert(pos.child, compNow)
      setChildrenAndRevalidate(newChildren)
    }

    private def setChildrenAndRevalidate(newChildren: List[StringPart]): Unit = {
      _children = newChildren
      component.revalidate()
      component.repaint()
      fireChange()
    }
  }

  private sealed abstract class StringParentContainer(protected var _children: List[StringPart])
    extends StringPart with StringParentContainerBase {

    _children.foreach(_.parent = this)

    final def fireChange(): Unit = parent.fireChange()

    protected def becameEmpty()                   : Unit = parent.remove (this)
    protected def becameSingle(child: StringPart) : Unit = parent.replace(this, child)

    protected def childPosition(idx: Int): ChildPosition = {
      val start = idx * 2 + 1
      val child = start + 1
      val count = 2
      ChildPosition(child = child, start = start, count = count)
    }
  }

  private trait AndOrLike {
    _: StringParentContainer =>

    protected def opString: String

    protected def mkOp(a: StringExpr, b: StringExpr): StringExpr

    lazy val component: Component with SequentialContainer.Wrapper = {
      val childViews  = _children.map(_.component)
      val ggEdit      = new EditButton(new PopupMenu)
      val ggAll       = childViews.zipWithIndex.flatMap { case (view, i) =>
        new Label(if (i == 0) "(" else s" $opString ") :: view :: Nil
      }
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += HStrut(4)
      box.contents ++= ggAll
      box.contents += new Label(")")
      box.contents += ggEdit
      box.contents += HStrut(4)
      box
    }

    def toExpr: StringExpr = _children.map(_.toExpr).reduceLeft(mkOp)
  }

  private final case class StringPartOr(children: List[StringPart])
    extends StringParentContainer(children) with AndOrLike {

    protected def opString = "\u22c1"

    protected def mkOp(a: StringExpr, b: StringExpr): StringExpr = StringExpr.Or(a, b)
  }

  private final case class StringPartAnd(children: List[StringPart])
    extends StringParentContainer(children) with AndOrLike {

    protected def opString = "\u22c0"

    protected def mkOp(a: StringExpr, b: StringExpr): StringExpr = StringExpr.And(a, b)
  }

  private final case class StringPartNot(child: StringPart)
    extends StringParentContainer(child :: Nil) {

//    override protected def childPosition(idx: Int): ChildPosition =
//      ChildPosition(child = 1, start = 0, count = 2)

    lazy val component: Component with SequentialContainer.Wrapper = {
      val ggEdit = new EditButton(new PopupMenu)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents ++= Seq(
        HStrut(4), new Label("¬("), child.component, new Label(")"), ggEdit
      )
      box
    }

    def toExpr: StringExpr = StringExpr.Not(child.toExpr)
  }

  private final class StringPartTop(private[this] var value: StringExpr.Option, set: StringExpr.Option => Unit)
    extends StringParentContainerBase {

    protected def childPosition(idx: Int): ChildPosition = ChildPosition(child = 2, start = 2, count = 1)

    protected var _children: List[StringPart] = value match {
      case StringExpr.None  => Nil
      case ex: StringExpr   =>
        val c = mkStringPart(ex)
        c.parent = this
        c :: Nil
    }

    private[this] lazy val ggEnabled = {
      val res     = new CheckBox
      res.listenTo(res)
      res.reactions += {
        case ButtonClicked(_) =>
          val state   = res.selected
          val child   = _children.headOption
          (child, state) match {
            case (Some(c), false) =>
              remove(c)
            case (None, true) =>
              val c = new StringPartConst("")
              insert(c)
            case (_, true) =>
              fireChange()
            case _ =>
          }
      }
      res.selected = _children.nonEmpty
      res
    }

    lazy val component: Component with SequentialContainer.Wrapper = {
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggEnabled
      box.contents += HStrut(4)
      box.contents ++ _children.map(_.component)
      box.contents += HGlue
      box
    }

    def toExprOption: StringExpr.Option =
      _children match {
        case c :: Nil if ggEnabled.selected => c.toExpr
        case _                              => StringExpr.None
      }

    def fireChange(): Unit = {
      val newValue = toExprOption
      if (value != newValue) {
        value = newValue
        set(newValue)
      }
    }

    protected def becameEmpty(): Unit = {
      ggEnabled.selected = false
      fireChange()
    }

    protected def becameSingle(child: StringPart): Unit = throw new IllegalStateException()

    private def insert(now: StringPart): Unit = {
      require(_children.isEmpty)
      _children = now :: Nil
      now.parent = this
      val pos = childPosition(0)
      component.contents.insert(pos.child, now.component)
      component.revalidate()
      component.repaint()
      ggEnabled.selected = true
      fireChange()
    }
  }

  private def mkStringPart(ex: StringExpr): StringPart = ex match {
    case StringExpr.Const(s0) =>
      new StringPartConst(s0)

    case StringExpr.Or(a, b) =>
      val pa = mkStringPart(a)
      val pb = mkStringPart(b)
      (pa, pb) match {
        case (StringPartOr (ca), StringPartOr (cb)) => StringPartOr (ca ++ cb)
        case (StringPartOr (ca), _                ) => StringPartOr (ca :+ pb)
        case (_                , StringPartOr (cb)) => StringPartOr (pa +: cb)
        case _                                      => StringPartOr (pa :: pb :: Nil)
      }

    case StringExpr.And(a, b) =>
      val pa = mkStringPart(a)
      val pb = mkStringPart(b)
      (pa, pb) match {
        case (StringPartAnd(ca), StringPartAnd(cb)) => StringPartAnd(ca ++ cb)
        case (StringPartAnd(ca), _                ) => StringPartAnd(ca :+ pb)
        case (_                , StringPartAnd(cb)) => StringPartAnd(pa +: cb)
        case _                                      => StringPartAnd(pa :: pb :: Nil)
      }
    case StringExpr.Not(a) =>
      val pa = mkStringPart(a)
      StringPartNot(pa)
  }

  private final class Impl(private var _filter: Filter) extends FilterView with ModelImpl[Filter] {
    private def mkStr(name: String, init: StringExpr.Option)
                            (copy: StringExpr.Option => Filter): (Label, Component) = {
      val top = new StringPartTop(init, { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      new Label(s"${name.capitalize}:") -> top.component
    }

//    private[this] val ggQuery: Component = new TextField(20)

    private[this] val fields = Seq(
//      new Label("Query:") -> ggQuery,
      mkStr("tags"       , _filter.tags       )(v => _filter.copy(tags        = v)),
      mkStr("description", _filter.description)(v => _filter.copy(description = v)),
      mkStr("file name"  , _filter.fileName   )(v => _filter.copy(fileName    = v)),
      mkStr("license"    , _filter.license    )(v => _filter.copy(license     = v)),
      mkStr("user name"  , _filter.userName   )(v => _filter.copy(userName    = v)),
      mkStr("comment"    , _filter.comment    )(v => _filter.copy(comment     = v))
    )

    lazy val component: Component = {
      val gp = new GroupPanel {
        val lbTags = new Label("Tags:")
        import GroupPanel.Element
        horizontal  = Seq(Par(fields.map(_._1: Element): _*), Par(fields.map(_._2: Element): _*))
        vertical    = Seq(
          fields.map(tup => Par(GroupPanel.Alignment.Baseline)(tup._1, tup._2)): _*
        )
        preferredSize = {
          val p = preferredSize
          p.width = math.max(p.width, 400)  // XXX TODO --- quite arbitrary
          p
        }
      }
      val scroll = new ScrollPane(gp)
      scroll.peer.putClientProperty("styleId", "undecorated")
      scroll
    }

    /*

    id          : UIntExpr    .Option = None,
      fileName    : StringTokens        = None,
      tags        : StringExpr  .Option = None,
      description : StringTokens        = None,
      userName    : StringExpr  .Option = None,
    created     : DateExpr    .Option = None,
      license     : StringExpr  .Option = None,
    geoTag      : Optional[Boolean]   = None,
    fileType    : FileTypeExpr.Option = None,
    duration    : UDoubleExpr .Option = None,
    numChannels : UIntExpr    .Option = None,
    sampleRate  : UIntExpr    .Option = None,
    bitDepth    : UIntExpr    .Option = None,
    bitRate     : UDoubleExpr .Option = None,
    fileSize    : UIntExpr    .Option = None,
    numDownloads: UIntExpr    .Option = None,
    avgRating   : UDoubleExpr .Option = None,
    numRatings  : UIntExpr    .Option = None,
      comment     : StringTokens        = None,
    numComments : UIntExpr    .Option = None,
    isRemix     : Optional[Boolean]   = None,
    wasRemixed  : Optional[Boolean]   = None,

    pack        : StringExpr  .Option = None,
    packTokens  : StringTokens        = None,
    md5         : StringExpr  .Option = None

     */

    def filter: Filter = _filter
    def filter_=(value: Filter): Unit = {
      ???
      _filter = value
    }
  }
}
