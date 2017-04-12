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
import de.sciss.swingplus.GroupPanel

import scala.swing.event.{ButtonClicked, EditDone}
import scala.swing.{Button, CheckBox, Component, FlowPanel, Label, ScrollPane, SequentialContainer, TextField}

object FilterViewImpl {
  def apply(init: Filter): FilterView = new Impl(init)

  private final class EditButton extends Button("\u2335") {

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

  private final class StringPartConst(s0: String) extends StringPart {
    private[this] lazy val ggText = new TextField(6)

    lazy val component: Component = {
      ggText.listenTo(ggText)
      ggText.reactions += {
        case EditDone(_) => parent.fireChange()
      }
      val ggEdit = new EditButton
      new FlowPanel(FlowPanel.Alignment.Leading)(ggText, ggEdit)
    }

    def toExpr: StringExpr = ggText.text
  }

  private sealed trait StringParentContainerBase extends StringPartParent {
    protected var _children: List[StringPart]

    protected def childComponentOffset: Int

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
          component.contents.remove(idx + childComponentOffset)
          setChildrenAndRevalidate(newChildren)
      }
    }

    final def replace(before: StringPart, now: StringPart): Unit = {
      val idx = _children.indexOf(before)
      require(idx >= 0)
      val newChildren = _children.patch(idx, now :: Nil, 1)
      now.parent = this
      component.contents.update(idx + childComponentOffset, now.component)
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
  }

  private final case class StringPartOr(children: List[StringPart])
    extends StringParentContainer(children) {

    protected def childComponentOffset: Int = 0

    lazy val component: Component with SequentialContainer.Wrapper = {
      val childViews  = children.map(_.component)
      val ggEdit      = new EditButton
      new FlowPanel(childViews :+ ggEdit: _*)
    }

    def toExpr: StringExpr = children.map(_.toExpr).reduceLeft(StringExpr.Or)
  }

  private final case class StringPartAnd(children: List[StringPart])
    extends StringParentContainer(children) {

    protected def childComponentOffset: Int = 0

    lazy val component: Component with SequentialContainer.Wrapper = {
      val childViews  = children.map(_.component)
      val ggEdit      = new EditButton
      new FlowPanel(childViews :+ ggEdit: _*)
    }

    def toExpr: StringExpr = children.map(_.toExpr).reduceLeft(StringExpr.Or)
  }

  private final case class StringPartNot(child: StringPart)
    extends StringParentContainer(child :: Nil) {

    protected def childComponentOffset: Int = 1

    lazy val component: Component with SequentialContainer.Wrapper =
      new FlowPanel(new Label("¬("), child.component, new Label(")"))

    def toExpr: StringExpr = StringExpr.Not(child.toExpr)
  }

  private final class StringPartTop(private[this] var value: StringExpr.Option, set: StringExpr.Option => Unit)
    extends StringParentContainerBase {

    protected def childComponentOffset = 1

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
      new FlowPanel(FlowPanel.Alignment.Leading)(ggEnabled :: _children.map(_.component): _*)
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
      component.contents.insert(childComponentOffset, now.component)
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
