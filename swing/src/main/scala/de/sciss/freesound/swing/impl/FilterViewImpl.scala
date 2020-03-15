/*
 *  FilterViewImpl.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.swing.impl

import de.sciss.freesound
import de.sciss.freesound.swing.FilterView
import de.sciss.freesound.{Filter, QueryExpr, StringExpr, UDoubleExpr, UIntExpr}
import de.sciss.model.impl.ModelImpl
import de.sciss.swingplus.{GroupPanel, PopupMenu, Spinner}
import javax.swing.SpinnerNumberModel

import scala.swing.Swing._
import scala.swing.event.{ButtonClicked, EditDone, ValueChanged}
import scala.swing.{Action, Alignment, BorderPanel, BoxPanel, Button, CheckBox, Component, Label, MenuItem, Orientation, ScrollPane, SequentialContainer, TextField}

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

  private trait PartParent[Repr] {
    def fireChange(): Unit
    def remove(child: Part[Repr]): Unit
    def replace(before: Part[Repr], now: Part[Repr]): Unit
  }

  private sealed trait Part[Repr] {
    private[this] var _parent: PartParent[Repr] = _

    final def parent: PartParent[Repr] = {
      require(_parent != null)
      _parent
    }


    final def parent_=(value: PartParent[Repr]): Unit =
      _parent = value

    def component: Component

    def toExpr: Repr
  }


  private trait PartFactory[R <: Expr[R]] {
    implicit def self: this.type = this

    type ROpt <: QueryExpr.Option { type Repr = R }

    def mkPart(ex: R): Part[R]

    def zero: R

    def fromOption(opt: Option[R]): ROpt

    protected final def mkLogicPart(ex: R): Part[R] = (ex.self: QueryExpr.Base[R]) match {
      case or: QueryExpr.Or[R] /* StringExpr.Or(a, b) */ =>
        val a  = or.a
        val b  = or.b
        val pa = mkPart(a)
        val pb = mkPart(b)
        (pa, pb) match {
          case (PartOr (ca), PartOr (cb)) => PartOr (ca ++ cb)
          case (PartOr (ca), _          ) => PartOr (ca :+ pb)
          case (_          , PartOr (cb)) => PartOr (pa +: cb)
          case _                          => PartOr (pa :: pb :: Nil)
        }

      case and: QueryExpr.And[R] /* StringExpr.And(a, b) */ =>
        val a  = and.a
        val b  = and.b
        val pa = mkPart(a)
        val pb = mkPart(b)
        (pa, pb) match {
          case (PartAnd(ca), PartAnd(cb)) => PartAnd(ca ++ cb)
          case (PartAnd(ca), _          ) => PartAnd(ca :+ pb)
          case (_          , PartAnd(cb)) => PartAnd(pa +: cb)
          case _                          => PartAnd(pa :: pb :: Nil)
        }

      case not: QueryExpr.Not[R] /* StringExpr.Not(a) */ =>
        val a  = not.a
        val pa = mkPart(a)
        PartNot(pa)

      case _: QueryExpr.Const[R] => throw new IllegalArgumentException("Must match constants before")
    }
  }

  private trait NumberPartFactory[R <: Expr[R]] extends PartFactory[R] {
    def from(ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R]
    def to  (ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R]
  }

  private implicit object StringPartFactory extends PartFactory[StringExpr] {
    import freesound.{StringExpr => R}

    type ROpt = R.Option

    def zero: R = ""

    def fromOption(opt: Option[R]): R.Option = opt.fold[R.Option](R.None)(identity)

    def mkConstPart(s0: String): PartConst[R] = new StringPartConst(s0)

    override def mkPart(ex: R): Part[R] = ex match {
      case StringExpr.Const(s0) => mkConstPart(s0)
      case _ => mkLogicPart(ex)
    }
  }

  private final class UIntPartFactory(min: Int, max: Int, step: Int) extends NumberPartFactory[UIntExpr] {
    import freesound.{UIntExpr => R}

    type ROpt = R.Option

    def zero: R = min

    def fromOption(opt: Option[R]): R.Option = opt.fold[R.Option](R.None)(identity)

    def from(ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R] = {
      val R.ConstSingle(v) = ex
      R.from(v)
    }

    def to  (ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R] = {
      val R.ConstSingle(v) = ex
      R.to(v)
    }

    def mkConstPart(relation: NumberPartRel, i0: Int): PartConst[R] =
      new UIntPartConst(relation, i0, min = min, max = max, step = step)

    override def mkPart(ex: R): Part[R] = ex match {
      case R.ConstSingle(i0) => mkConstPart(NumberPartEq, i0)
      case R.ConstRange (start, end)  =>
        if (start == -1) {
          mkConstPart(NumberPartLtEq, end)
        } else if (end == -1) {
          mkConstPart(NumberPartGtEq, start)
        } else {
          ??? // new UIntPartConst(s0)
        }
      case _ => mkLogicPart(ex)
    }
  }

  private final class UDoublePartFactory(min: Double, max: Double, step: Double)
    extends NumberPartFactory[UDoubleExpr] {

    import freesound.{UDoubleExpr => R}

    type ROpt = R.Option

    def zero: R = min

    def fromOption(opt: Option[R]): R.Option = opt.fold[R.Option](R.None)(identity)

    def from(ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R] = {
      val R.ConstSingle(v) = ex
      R.from(v)
    }

    def to  (ex: R with QueryExpr.Const[R]): R with QueryExpr.Const[R] = {
      val R.ConstSingle(v) = ex
      R.to(v)
    }

    def mkConstPart(relation: NumberPartRel, i0: Double): PartConst[R] =
      new UDoublePartConst(relation, i0, min = min, max = max, step = step)

    override def mkPart(ex: R): Part[R] = ex match {
      case R.ConstSingle(i0) => mkConstPart(NumberPartEq, i0)
      case R.ConstRange (start, end)  =>
        if (start == -1) {
          mkConstPart(NumberPartLtEq, end)
        } else if (end == -1) {
          mkConstPart(NumberPartGtEq, start)
        } else {
          ??? // new UDoublePartConst(s0)
        }
      case _ => mkLogicPart(ex)
    }
  }

  private def mkMenuItemRemove[R <: Expr[R]](p: Part[R]): MenuItem =
    new MenuItem(Action("\u00d7 remove") {
      p.parent.remove(p)
    })

  private def mkMenuItemPop[R <: Expr[R]](p: ParentContainer[R]): MenuItem =
    new MenuItem(Action("\u00d7 remove") {
      val c   = p.pop
      val rep = p.factory.mkPart(c)
      val par = p.parent
      par.replace(p, rep)
    })

  private def mkMenuItemOr[R <: Expr[R]](p: Part[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u22c1 or") {
      val app = factory.mkPart(factory.zero) // new StringPartConst("")
      val par = p.parent  // catch it early!
      val or  = PartOr (p :: app :: Nil)
      par.replace(p, or)
    })

  private def mkMenuItemAnd[R <: Expr[R]](p: Part[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u22c0 and") {
      val app = factory.mkPart(factory.zero)
      val par = p.parent  // catch it early!
      val or  = PartAnd(p :: app :: Nil)
      par.replace(p, or)
    })

  private def mkMenuItemNot[R <: Expr[R]](p: Part[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u00ac not") {
      val par = p.parent  // catch it early!
      val not = PartNot(p)
      par.replace(p, not)
    })

  private def mkMenuItemEq[R <: Expr[R]](p: NumberPartConst[R]): MenuItem =
    new MenuItem(Action("=") {
      val c   = p.toSingleExpr
      val rep = p.factory.mkPart(c)
      val par = p.parent
      par.replace(p, rep)
    })

  private def mkMenuItemLtEq[R <: Expr[R]](p: NumberPartConst[R]): MenuItem =
    new MenuItem(Action("\u2264") {
      val c   = p.toSingleExpr
      val gt  = p.factory.to(c)
      val rep = p.factory.mkPart(gt)
      val par = p.parent
      par.replace(p, rep)
    })

  private def mkMenuItemGtEq[R <: Expr[R]](p: NumberPartConst[R]): MenuItem =
    new MenuItem(Action("\u2265") {
      val c   = p.toSingleExpr
      val gt  = p.factory.from(c)
      val rep = p.factory.mkPart(gt)
      val par = p.parent
      par.replace(p, rep)
    })

  private trait PartConst[Repr] extends Part[Repr] {
    override def toExpr: Repr with QueryExpr.Const[Repr]
  }

  private final class StringPartConst(s0: String) extends PartConst[StringExpr] { part =>
    type R = StringExpr

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
      val pop = new PopupMenu {
        contents += mkMenuItemRemove(part)
        contents += mkMenuItemOr    (part)
        contents += mkMenuItemAnd   (part)
        contents += mkMenuItemNot   (part)
      }
      val ggEdit    = new EditButton(pop)
      val box       = new BoxPanel(Orientation.Horizontal)
      box.contents += ggText
      box.contents += ggEdit
      box
    }

    def toExpr: StringExpr.Const = ggText.text
  }

  private sealed trait NumberPartRel
  private case object  NumberPartEq   extends NumberPartRel
  private case object  NumberPartLtEq extends NumberPartRel
  private case object  NumberPartGtEq extends NumberPartRel

  private trait NumberPartConst[R <: Expr[R]] extends PartConst[R] { part =>

    // ---- abstract ----

    protected def model: SpinnerNumberModel

    implicit def factory: NumberPartFactory[R]

    protected def relation: NumberPartRel

    def toSingleExpr: R with QueryExpr.Const[R]

    // ---- impl ----

    private[this] lazy val ggSpinner = new Spinner(model)

    lazy val component: Component = {
      ggSpinner.listenTo(ggSpinner)
      ggSpinner.reactions += {
        case ValueChanged(_) => parent.fireChange()
      }
      val pop = new PopupMenu {
        contents += mkMenuItemRemove(part)
        if (relation != NumberPartEq  ) contents += mkMenuItemEq    (part)
        if (relation != NumberPartLtEq) contents += mkMenuItemLtEq  (part)
        if (relation != NumberPartGtEq) contents += mkMenuItemGtEq  (part)
        contents += mkMenuItemOr    (part)
        contents += mkMenuItemAnd   (part)
        contents += mkMenuItemNot   (part)
      }
      val ggEdit = new EditButton(pop)
      val box = new BoxPanel(Orientation.Horizontal)
      if (relation != NumberPartEq) {
        val lbRel     = if (relation == NumberPartGtEq) "\u2265" else "\u2264"
        box.contents += HStrut(4)
        box.contents += new Label(lbRel)
      }
      box.contents += ggSpinner
      box.contents += ggEdit
      box
    }

    final def toExpr: R with QueryExpr.Const[R] = {
      val s = toSingleExpr
      relation match {
        case NumberPartEq   => s
        case NumberPartGtEq => factory.from(s)
        case NumberPartLtEq => factory.to  (s)
      }
    }
  }

  private final class UIntPartConst(protected val relation: NumberPartRel,
                                    init: Int, min: Int, max: Int, step: Int)
                                   (implicit val factory: NumberPartFactory[UIntExpr])
    extends NumberPartConst[UIntExpr] {
    part =>

    protected val model = new SpinnerNumberModel(init, min, max, step)

    def toSingleExpr: UIntExpr.ConstSingle = model.getNumber.intValue()
  }

  private final class UDoublePartConst(protected val relation: NumberPartRel,
                                       init: Double, min: Double, max: Double, step: Double)
                                      (implicit val factory: NumberPartFactory[UDoubleExpr])
    extends NumberPartConst[UDoubleExpr] {
    part =>

    protected val model = new SpinnerNumberModel(init, min, max, step)

    def toSingleExpr: UDoubleExpr.ConstSingle = model.getNumber.doubleValue()
  }

  private final case class ChildPosition(child: Int, start: Int, count: Int)

  private sealed trait ParentContainerBase[Repr] extends PartParent[Repr] {
    protected var _children: List[Part[Repr]]

    protected def childPosition(idx: Int): ChildPosition

    def fireChange(): Unit

    def component: Component with SequentialContainer.Wrapper

    protected def becameEmpty(): Unit
    protected def becameSingle(child: Part[Repr]): Unit

    final def remove(child: Part[Repr]): Unit = {
      val idx = _children.indexOf(child)
      require(idx >= 0)
      val newChildren = _children.patch(idx, Nil, 1)
      newChildren match {
        case Nil            => becameEmpty()
        case single :: Nil  => becameSingle(single)
        case _ =>
          val pos = childPosition(idx)
          component.contents.remove(pos.start, pos.count)
          setChildrenAndRevalidate(newChildren, fire = true)
      }
    }

    final def replace(before: Part[Repr], now: Part[Repr]): Unit = {
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
      setChildrenAndRevalidate(newChildren, fire = true)
    }

    final protected def setChildrenAndRevalidate(newChildren: List[Part[Repr]], fire: Boolean): Unit = {
      _children = newChildren
      val box = component
      box.revalidate()
      box.repaint()
      if (fire) fireChange()
    }
  }

  private sealed abstract class ParentContainer[R <: Expr[R]](protected var _children: List[Part[R]])
    extends Part[R] with ParentContainerBase[R] {

    // ---- abstract ----

    def pop: R

    implicit def factory: PartFactory[R]

    // ---- impl ----

    _children.foreach(_.parent = this)

    final def fireChange(): Unit = parent.fireChange()

    protected def becameEmpty()                : Unit = parent.remove (this)
    protected def becameSingle(child: Part[R]) : Unit = parent.replace(this, child)

    protected def childPosition(idx: Int): ChildPosition = {
      val start = idx * 2 + 1
      val child = start + 1
      val count = 2
      ChildPosition(child = child, start = start, count = count)
    }
  }

  private trait AndOrLike[R <: Expr[R]] extends ParentContainer[R] {
    part =>

    // ---- abstract ----

    protected def opString: String

    protected def mkOp(a: R, b: R): R

    // ---- impl ----

    lazy val component: Component with SequentialContainer.Wrapper = {
      val childViews  = _children.map(_.component)
      val pop = new PopupMenu {
        contents += mkMenuItemPop (part)
        contents += mkMenuItemOr  (part)
        contents += mkMenuItemAnd (part)
        contents += mkMenuItemNot (part)
      }
      val ggEdit      = new EditButton(pop)
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

    def toExpr: R = _children.map(_.toExpr).reduceLeft(mkOp)
  }

  type Expr[R] = QueryExpr { type Repr = R }

  private final case class PartOr[R <: Expr[R]](children: List[Part[R]])(implicit val factory: PartFactory[R])
    extends ParentContainer(children) with AndOrLike[R] {

    protected def opString = "\u22c1"

    protected def mkOp(a: R, b: R): R = a | b

    def pop: R = children.head.toExpr
  }

  private final case class PartAnd[R <: Expr[R]](children: List[Part[R]])(implicit val factory: PartFactory[R])
    extends ParentContainer(children) with AndOrLike[R] {

    protected def opString = "\u22c0"

    protected def mkOp(a: R, b: R): R = a & b

    def pop: R = children.head.toExpr
  }

  private final case class PartNot[R <: Expr[R]](child: Part[R])(implicit val factory: PartFactory[R])
    extends ParentContainer(child :: Nil) { part =>

    lazy val component: Component with SequentialContainer.Wrapper = {
      val pop = new PopupMenu {
        contents += mkMenuItemPop (part)
        contents += mkMenuItemOr  (part)
        contents += mkMenuItemAnd (part)
      }
      val ggEdit = new EditButton(pop)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents ++= Seq(
        HStrut(4), new Label("¬("), child.component, new Label(")"), ggEdit
      )
      box
    }

    def toExpr: R = !child.toExpr

    def pop: R = child.toExpr
  }

  private trait PartTop[R <: Expr[R]] extends ParentContainerBase[R] { self =>

    // ---- abstract ----

    type ROpt <: QueryExpr.Option { type Repr = R }

    import self.{ROpt => ROpt1}

    protected def factory: PartFactory[R] { type ROpt = ROpt1 }

    protected def mkConst(): Part[R]

    protected def get: () => ROpt
    protected def set:       ROpt => Unit

    // ---- impl ----

    private[this] var _value: ROpt = get()

    final def update(): Unit = {
      val newValue: ROpt = get()
      if (_value != newValue) {
        _value = newValue
        updateChildren()
      }
    }

    protected def childPosition(idx: Int): ChildPosition = ChildPosition(child = 2, start = 2, count = 1)

    private def mkChildren(): List[Part[R]] = _value.toQueryOption match {
      case None  => Nil
      case Some(ex) =>
        val c = factory.mkPart(ex)
        c.parent = this
        c :: Nil
    }

    private def updateChildren(): Unit = {
      val newChildren = mkChildren()
      val box = component
      box.contents.remove(2, box.contents.size - 2)
      val childC = newChildren.map { child =>
        val x = child.component
//        println(x.preferredSize)
//        x.peer.setSize(x.preferredSize)
//        x.border = MatteBorder(2, 2, 2, 2, Color.blue)
        x
      }
      box.contents ++= childC
//      box.contents += HGlue
//      box.border = MatteBorder(2, 2, 2, 2, Color.red)
//      val TEST = box.preferredSize
      setChildrenAndRevalidate(newChildren, fire = false)
      ggEnabled.selected = _children.nonEmpty
    }

    protected final var _children: List[Part[R]] = mkChildren()

    private[this] lazy val ggEnabled = {
      val res     = new CheckBox
//      res.preferredSize = {
//        val d = res.preferredSize
//        d.height = math.max(24, d.height)
//        d
//      }
      res.listenTo(res)
      res.reactions += {
        case ButtonClicked(_) =>
          val state   = res.selected
          val child   = _children.headOption
          (child, state) match {
            case (Some(c), false) =>
              remove(c)
            case (None, true) =>
              val c = mkConst()
              insert(c)
            case (_, true) =>
              fireChange()
            case _ =>
          }
      }
      res.selected = _children.nonEmpty
      res
    }

    final lazy val component: Component with SequentialContainer.Wrapper = {
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggEnabled
      box.contents += HStrut(4)
      box.contents ++ _children.map(_.component)
      box.contents += HGlue
      box
    }

    private def toExprOption: ROpt = {
      val opt = _children match {
        case c :: Nil if ggEnabled.selected => Some(c.toExpr)
        case _ => None
      }
      factory.fromOption(opt)
    }

    def fireChange(): Unit = {
      val newValue = toExprOption
      if (_value != newValue) {
        _value = newValue
        set(newValue)
      }
    }

    protected def becameEmpty(): Unit = {
      ggEnabled.selected = false
      fireChange()
    }

    protected def becameSingle(child: Part[R]): Unit = throw new IllegalStateException()

    private def insert(now: Part[R]): Unit = {
      require(_children.isEmpty)
      _children = now :: Nil
      now.parent = this
      val pos = childPosition(0)
      val box = component
      box.contents.insert(pos.child, now.component)
      box.revalidate()
      box.repaint()
      ggEnabled.selected = true
      fireChange()
    }
  }

  private final case class EntryView[R <: Expr[R]](label: Component, top: PartTop[R]) {
    def editor: Component = top.component
  }

  private final class StringPartTop(val get: () => StringExpr.Option, val set: StringExpr.Option => Unit,
                                    default: String = "")
    extends PartTop[StringExpr] {

    import freesound.{StringExpr => R}

    type ROpt = R.Option

    protected def factory: StringPartFactory.type = StringPartFactory

    protected def mkConst(): Part[R] = factory.mkConstPart(default)
  }

  private final class UIntPartTop(val get: () => UIntExpr.Option,
                                  val set: UIntExpr.Option => Unit, default: Int = 0, min: Int = 0,
                                  max: Int = Int.MaxValue, step: Int = 1)
    extends PartTop[UIntExpr] {

    import freesound.{UIntExpr => R}

    type ROpt = R.Option

    protected implicit val factory: UIntPartFactory = new UIntPartFactory(min = min, max = max, step = step)

    protected def mkConst(): Part[R] = factory.mkConstPart(NumberPartEq, default)
  }

  private final class UDoublePartTop(val get: () => UDoubleExpr.Option,
                                     val set: UDoubleExpr.Option => Unit, default: Double = 0.0, min: Double = 0.0,
                                     max: Double = Double.MaxValue, step: Double = 0.1)
    extends PartTop[UDoubleExpr] {

    import freesound.{UDoubleExpr => R}

    type ROpt = R.Option

    protected implicit val factory: UDoublePartFactory = new UDoublePartFactory(min = min, max = max, step = step)

    protected def mkConst(): Part[R] = factory.mkConstPart(NumberPartEq, default)
  }

  private final class Impl(private var _filter: Filter) extends FilterView with ModelImpl[Filter] {
    private def mkLabel(name: String): Component = {
      val lb = new Label(s"${name.capitalize}:", null, Alignment.Leading)
//      new BoxPanel(Orientation.Vertical) {
//        contents += lb
//        preferredSize = {
//          val d = preferredSize
//          d.height = math.max(d.height, 48)
//          d
//        }
//      }
      new BorderPanel {
//        add(VGlue, BorderPanel.Position.North)
//        add(VGlue, BorderPanel.Position.South)
        border = EmptyBorder(6, 0, 6, 6)
//        border = MatteBorder(6, 0, 6, 0, java.awt.Color.red)
        add(lb, BorderPanel.Position.Center)
        maximumSize = {
          val d = lb.preferredSize
          d
        }
      }
    }

    private def mkS(name: String)(init: => StringExpr.Option)
                   (copy: StringExpr.Option => Filter): EntryView[StringExpr] = {
      val top = new StringPartTop(() => init, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      EntryView(mkLabel(name), top)
    }

    private def mkI(name: String, df: Int, min: Int = 0, max: Int,
                    step: Int = 1)(init: => UIntExpr.Option)
                   (copy: UIntExpr.Option => Filter): EntryView[UIntExpr] = {
      val top = new UIntPartTop(() => init, default = df, min = min, max = max, step = step, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      EntryView(mkLabel(name), top)
    }

    private def mkD(name: String, df: Double, min: Double, max: Double, step: Double = 0.1)
                   (init: => UDoubleExpr.Option)
                   (copy: UDoubleExpr.Option => Filter): EntryView[UDoubleExpr] = {
      val top = new UDoublePartTop(() => init, default = df, min = min, max = max, step = step, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      EntryView(mkLabel(name), top)
    }

    private[this] lazy val entryTags  = mkS("tags"            )(_filter.tags       )(v => _filter.copy(tags        = v))
    private[this] lazy val entryDescr = mkS("description"     )(_filter.description)(v => _filter.copy(description = v))
    private[this] lazy val entryFile  = mkS("file name"       )(_filter.fileName   )(v => _filter.copy(fileName    = v))
//    private[this] lazy val entryLic   = mkS("license"         )(_filter.license    )(v => _filter.copy(license     = v))
    private[this] lazy val entryDur   = mkD("duration [s]", df = 2, min = 1, max = 8192)(_filter.duration)(v => _filter.copy(duration = v))
    private[this] lazy val entryChan  = mkI("num-channels", df = 2, min = 1, max = 8192)(_filter.numChannels)(v => _filter.copy(numChannels = v))
    private[this] lazy val entrySr    = mkI("sample-rate [Hz]", df = 44100, min = 0, max = 96000 * 4)(_filter.sampleRate)(v => _filter.copy(sampleRate = v))
    private[this] lazy val entryDepth = mkI("bit-depth", df = 16, min = 8, max = 64, step = 8)(_filter.bitDepth)(v => _filter.copy(bitDepth = v))
    private[this] lazy val entryKpbs  = mkI("bit-rate [kbps]", df = 320, min = 8, max = 24000, step = 8)(_filter.bitRate)(v => _filter.copy(bitRate = v))
    private[this] lazy val entryDown  = mkI("num-downloads", df = 1, max = 1000000)(_filter.numDownloads)(v => _filter.copy(numDownloads = v))
    private[this] lazy val entryAvgRat= mkD("average rating", df = 5, min = 0, max = 5)(_filter.avgRating)(v => _filter.copy(avgRating = v))
    private[this] lazy val entryNumRat= mkI("num-ratings", df = 1, max = 100000)(_filter.numRatings)(v => _filter.copy(numRatings = v))

    private[this] val fields = Seq(
      entryTags,
      entryDescr,
      entryFile,
//      entryLic,
      entryDur,
      entryChan,
      entrySr,
      entryDepth,
      entryKpbs,
      entryDown,
      entryAvgRat,
      entryNumRat
    )

    def filter: Filter = _filter
    def filter_=(value: Filter): Unit = {
      _filter = value
      fields.foreach(_.top.update())
    }

    lazy val component: Component = {
      val gp: GroupPanel = new GroupPanel {
//        autoContainerGaps = false
        autoGaps          = false
//        autoContainerGaps = true
//        autoGaps          = true
//        val lbTags = new Label("Tags:")
        import GroupPanel.Element
        horizontal  = Seq(Par(fields.map(_.label: Element): _*), Par(fields.map(_.editor: Element): _*))
        vertical    = Seq(
          fields.map(entry => Par(GroupPanel.Alignment.Baseline)(entry.label, entry.editor)): _*
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
    created     : DateExpr    .Option = None,
      license     : StringExpr  .Option = None,
    geoTag      : Optional[Boolean]   = None,
    fileType    : FileTypeExpr.Option = None,
      duration    : UDoubleExpr .Option = None,
      numChannels : UIntExpr    .Option = None,
      sampleRate  : UIntExpr    .Option = None,
      bitDepth    : UIntExpr    .Option = None,
      bitRate     : UDoubleExpr .Option = None,
      numDownloads: UIntExpr    .Option = None,
      avgRating   : UDoubleExpr .Option = None,
      numRatings  : UIntExpr    .Option = None,

    userName    : StringExpr  .Option = None,
    fileSize    : UIntExpr    .Option = None,
    comment     : StringTokens        = None,
    numComments : UIntExpr    .Option = None,
    isRemix     : Optional[Boolean]   = None,
    wasRemixed  : Optional[Boolean]   = None,

    pack        : StringExpr  .Option = None,
    packTokens  : StringTokens        = None,
    md5         : StringExpr  .Option = None

     */
  }
}