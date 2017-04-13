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

import javax.swing.SpinnerNumberModel

import de.sciss.freesound
import de.sciss.model.impl.ModelImpl
import de.sciss.swingplus.{GroupPanel, PopupMenu, Spinner}

import scala.swing.Swing._
import scala.swing.event.{ButtonClicked, EditDone, ValueChanged}
import scala.swing.{Action, BoxPanel, Button, CheckBox, Component, Label, MenuItem, Orientation, ScrollPane, SequentialContainer, TextField}

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

    def mkPart(ex: R): Part[R]

    def zero: R

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
    def from(ex: R with QueryExpr.Const[R]): R
    def to  (ex: R with QueryExpr.Const[R]): R
  }

  private implicit object StringPartFactory extends PartFactory[StringExpr] {
    def zero: StringExpr = ""

    override def mkPart(ex: StringExpr): Part[StringExpr] = ex match {
      case StringExpr.Const(s0) => new StringPartConst(s0)
      case _ => mkLogicPart(ex)
    }
  }

  private final class UIntPartFactory(min: Int, max: Int, step: Int) extends NumberPartFactory[UIntExpr] {
    import freesound.{UIntExpr => R}

    def zero: R = min

    def from(ex: R with QueryExpr.Const[R]): R = {
      val R.ConstSingle(v) = ex
      R.from(v)
    }

    def to  (ex: R with QueryExpr.Const[R]): R = {
      val R.ConstSingle(v) = ex
      R.to(v)
    }

    private def mkConstPart(i0: Int): PartConst[R] =
      new UIntPartConstSingle(i0, min = min, max = max, step = step)

    override def mkPart(ex: R): Part[R] = ex match {
      case R.ConstSingle(i0) => mkConstPart(i0)
      case R.ConstRange (start, end)  =>
        if (start == -1) {
          val endP = mkConstPart(end)
          PartLtEq[R](endP)
        } else if (end == -1) {
          val startP = mkConstPart(start)
          PartGtEq[R](startP)
        } else {
          ??? // new UIntPartConst(s0)
        }
      case _ => mkLogicPart(ex)
    }
  }

  private final class UDoublePartFactory(min: Double, max: Double, step: Double)
    extends NumberPartFactory[UDoubleExpr] {

    import freesound.{UDoubleExpr => R}

    def zero: R = min

    def from(ex: R with QueryExpr.Const[R]): R = {
      val R.ConstSingle(v) = ex
      R.from(v)
    }

    def to  (ex: R with QueryExpr.Const[R]): R = {
      val R.ConstSingle(v) = ex
      R.to(v)
    }

    private def mkConstPart(i0: Double): PartConst[R] =
      new UDoublePartConstSingle(i0, min = min, max = max, step = step)

    override def mkPart(ex: R): Part[R] = ex match {
      case R.ConstSingle(i0) => mkConstPart(i0)
      case R.ConstRange (start, end)  =>
        if (start == -1) {
          val endP = mkConstPart(end)
          PartLtEq[R](endP)
        } else if (end == -1) {
          val startP = mkConstPart(start)
          PartGtEq[R](startP)
        } else {
          ??? // new UDoublePartConst(s0)
        }
      case _ => mkLogicPart(ex)
    }
  }

  private def mkMenuItemRemove[R <: Expr[R]](p: PartConst[R]): MenuItem =
    new MenuItem(Action("\u00d7 remove") {
      p.parent.remove(p)
    })

  private def mkMenuItemOr[R <: Expr[R]](p: PartConst[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u22c1 or") {
      val app = factory.mkPart(factory.zero) // new StringPartConst("")
      val par = p.parent  // catch it early!
      val or  = PartOr (p :: app :: Nil)
      par.replace(p, or)
    })

  private def mkMenuItemAnd[R <: Expr[R]](p: PartConst[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u22c0 and") {
      val app = factory.mkPart(factory.zero)
      val par = p.parent  // catch it early!
      val or  = PartAnd(p :: app :: Nil)
      par.replace(p, or)
    })

  private def mkMenuItemNot[R <: Expr[R]](p: PartConst[R])(implicit factory: PartFactory[R]): MenuItem =
    new MenuItem(Action("\u00ac not") {
      val par = p.parent  // catch it early!
      val not = PartNot(p)
      par.replace(p, not)
    })

  private def mkMenuItemLtEq[R <: Expr[R]](p: PartConst[R])(implicit factory: NumberPartFactory[R]): MenuItem =
    new MenuItem(Action("\u2264") {
      val par = p.parent
      val or  = PartLtEq(p)
      par.replace(p, or)
    })

  private def mkMenuItemGtEq[R <: Expr[R]](p: PartConst[R])(implicit factory: NumberPartFactory[R]): MenuItem =
    new MenuItem(Action("\u2265") {
      val par = p.parent
      val or  = PartGtEq(p)
      par.replace(p, or)
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
      val ggEdit = new EditButton(pop)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggText
      box.contents += ggEdit
      box
    }

    def toExpr: StringExpr.Const = ggText.text
  }

  private trait NumberPartConstSingle[R <: Expr[R]] extends PartConst[R] { part =>
    
    protected def model: SpinnerNumberModel
    implicit protected def factory: NumberPartFactory[R]

    private[this] lazy val ggSpinner = new Spinner(model)

    lazy val component: Component = {
      ggSpinner.listenTo(ggSpinner)
      ggSpinner.reactions += {
        case ValueChanged(_) => parent.fireChange()
      }
      val pop = new PopupMenu {
        contents += mkMenuItemRemove(part)
        contents += mkMenuItemLtEq  (part)
        contents += mkMenuItemGtEq  (part)
        contents += mkMenuItemOr    (part)
        contents += mkMenuItemAnd   (part)
        contents += mkMenuItemNot   (part)
      }
      val ggEdit = new EditButton(pop)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggSpinner
      box.contents += ggEdit
      box
    }
  }

  private final class UIntPartConstSingle(init: Int, min: Int, max: Int, step: Int)
                                         (implicit protected val factory: NumberPartFactory[UIntExpr])
    extends NumberPartConstSingle[UIntExpr] {
    part =>

    protected val model = new SpinnerNumberModel(init, min, max, step)

    def toExpr: UIntExpr.ConstSingle = model.getNumber.intValue()
  }

  private final class UDoublePartConstSingle(init: Double, min: Double, max: Double, step: Double)
                                         (implicit protected val factory: NumberPartFactory[UDoubleExpr])
    extends NumberPartConstSingle[UDoubleExpr] {
    part =>

    protected val model = new SpinnerNumberModel(init, min, max, step)

    def toExpr: UDoubleExpr.ConstSingle = model.getNumber.doubleValue()
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
          setChildrenAndRevalidate(newChildren)
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
      setChildrenAndRevalidate(newChildren)
    }

    private def setChildrenAndRevalidate(newChildren: List[Part[Repr]]): Unit = {
      _children = newChildren
      component.revalidate()
      component.repaint()
      fireChange()
    }
  }

  private sealed abstract class ParentContainer[Repr](protected var _children: List[Part[Repr]])
    extends Part[Repr] with ParentContainerBase[Repr] {

    _children.foreach(_.parent = this)

    final def fireChange(): Unit = parent.fireChange()

    protected def becameEmpty()                   : Unit = parent.remove (this)
    protected def becameSingle(child: Part[Repr]) : Unit = parent.replace(this, child)

    protected def childPosition(idx: Int): ChildPosition = {
      val start = idx * 2 + 1
      val child = start + 1
      val count = 2
      ChildPosition(child = child, start = start, count = count)
    }
  }

  private trait AndOrLike[Repr] {
    _: ParentContainer[Repr] =>

    protected def opString: String

    protected def mkOp(a: Repr, b: Repr): Repr

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

    def toExpr: Repr = _children.map(_.toExpr).reduceLeft(mkOp)
  }

  type Expr[R] = QueryExpr { type Repr = R }

  private final case class PartOr[R <: Expr[R]](children: List[Part[R]])
    extends ParentContainer(children) with AndOrLike[R] {

    protected def opString = "\u22c1"

    protected def mkOp(a: R, b: R): R = a | b
  }

  private final case class PartAnd[R <: Expr[R]](children: List[Part[R]])
    extends ParentContainer(children) with AndOrLike[R] {

    protected def opString = "\u22c0"

    protected def mkOp(a: R, b: R): R = a & b
  }

  private final case class PartNot[R <: Expr[R]](child: Part[R])
    extends ParentContainer(child :: Nil) {

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

    def toExpr: R = !child.toExpr
  }

  private final case class PartGtEq[R <: Expr[R]](child: PartConst[R])(implicit factory: NumberPartFactory[R])
    extends ParentContainer(child :: Nil) {

    lazy val component: Component with SequentialContainer.Wrapper = {
      val ggEdit = new EditButton(new PopupMenu)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents ++= Seq(
        HStrut(4), new Label("\u2265"), child.component, ggEdit
      )
      box
    }

    def toExpr: R = factory.from(child.toExpr)
  }

  private final case class PartLtEq[R <: Expr[R]](child: PartConst[R])(implicit factory: NumberPartFactory[R])
    extends ParentContainer(child :: Nil) {

    lazy val component: Component with SequentialContainer.Wrapper = {
      val ggEdit = new EditButton(new PopupMenu)
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents ++= Seq(
        HStrut(4), new Label("\u2264"), child.component, ggEdit
      )
      box
    }

    def toExpr: R = factory.to(child.toExpr)
  }

  private trait PartTop[R <: Expr[R]] extends ParentContainerBase[R] {

    // ---- abstract ----

    protected def factory: PartFactory[R]
    protected var valueOption: Option[R]
    protected def mkConst(): Part[R]

    // ---- impl ----

    protected def childPosition(idx: Int): ChildPosition = ChildPosition(child = 2, start = 2, count = 1)

    protected var _children: List[Part[R]] = valueOption match {
      case None  => Nil
      case Some(ex) =>
        val c = factory.mkPart(ex)
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

    lazy val component: Component with SequentialContainer.Wrapper = {
      val box = new BoxPanel(Orientation.Horizontal)
      box.contents += ggEnabled
      box.contents += HStrut(4)
      box.contents ++ _children.map(_.component)
      box.contents += HGlue
      box
    }

    def toExprOption: Option[R] =
      _children match {
        case c :: Nil if ggEnabled.selected => Some(c.toExpr)
        case _                              => None
      }

    def fireChange(): Unit = {
      val newValue = toExprOption
      if (valueOption != newValue) {
        valueOption = newValue
//        value = newValue
//        set(newValue)
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
      component.contents.insert(pos.child, now.component)
      component.revalidate()
      component.repaint()
      ggEnabled.selected = true
      fireChange()
    }
  }

  private final class StringPartTop(private[this] var _value: StringExpr.Option, set: StringExpr.Option => Unit)
    extends PartTop[StringExpr] {

    type R = StringExpr
    val  R = StringExpr

    protected def factory: PartFactory[R] = StringPartFactory

    protected def valueOption: Option[R] = _value match {
      case StringExpr.None => None
      case ex: StringExpr  => Some(ex)
    }

    protected def valueOption_=(opt: Option[R]): Unit = {
      val newValue = opt.fold[R.Option](R.None)(identity)
      set(newValue)
    }

    protected def mkConst(): Part[R] = new StringPartConst("")
  }

  private final class UIntPartTop(private[this] var _value: UIntExpr.Option,
                                  set: UIntExpr.Option => Unit, default: Int = 0, min: Int = 0,
                                  max: Int = Int.MaxValue, step: Int = 1)
    extends PartTop[UIntExpr] {

    type R = UIntExpr
    val  R = UIntExpr

    protected implicit val factory: NumberPartFactory[R] = new UIntPartFactory(min = min, max = max, step = step)

    protected def valueOption: Option[R] = _value match {
      case UIntExpr.None => None
      case ex: UIntExpr  => Some(ex)
    }

    protected def valueOption_=(opt: Option[R]): Unit = {
      val newValue = opt.fold[R.Option](R.None)(identity)
      set(newValue)
    }

    protected def mkConst(): Part[R] = new UIntPartConstSingle(init = default, min = min, max = max, step = step)
  }

  private final class UDoublePartTop(private[this] var _value: UDoubleExpr.Option,
                                  set: UDoubleExpr.Option => Unit, default: Double = 0.0, min: Double = 0.0,
                                  max: Double = Double.MaxValue, step: Double = 0.1)
    extends PartTop[UDoubleExpr] {

    type R = UDoubleExpr
    val  R = UDoubleExpr

    protected implicit val factory: NumberPartFactory[R] = new UDoublePartFactory(min = min, max = max, step = step)

    protected def valueOption: Option[R] = _value match {
      case UDoubleExpr.None => None
      case ex: UDoubleExpr  => Some(ex)
    }

    protected def valueOption_=(opt: Option[R]): Unit = {
      val newValue = opt.fold[R.Option](R.None)(identity)
      set(newValue)
    }

    protected def mkConst(): Part[R] = new UDoublePartConstSingle(init = default, min = min, max = max, step = step)
  }

  private final class Impl(private var _filter: Filter) extends FilterView with ModelImpl[Filter] {
    private def mkLabel(name: String): Label = new Label(s"${name.capitalize}:")

    private def mkStr(name: String, init: StringExpr.Option)
                            (copy: StringExpr.Option => Filter): (Label, Component) = {
      val top = new StringPartTop(init, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      mkLabel(name) -> top.component
    }

    private def mkUInt(name: String, init: UIntExpr.Option, default: Int = 0, min: Int = 0, max: Int = Int.MaxValue,
                       step: Int = 1)
                      (copy: UIntExpr.Option => Filter): (Label, Component) = {
      val top = new UIntPartTop(init, default = default, min = min, max = max, step = step, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      mkLabel(name) -> top.component
    }

    private def mkUDouble(name: String, init: UDoubleExpr.Option, default: Double = 0.0,
                          min: Double = 0.0, max: Double = Double.MaxValue, step: Double = 0.1)
                         (copy: UDoubleExpr.Option => Filter): (Label, Component) = {
      val top = new UDoublePartTop(init, default = default, min = min, max = max, step = step, set = { v =>
        _filter = copy(v)
        dispatch(_filter)
      })
      mkLabel(name) -> top.component
    }
    
    private[this] val fields = Seq(
//      new Label("Query:") -> ggQuery,
      mkStr("tags"       , _filter.tags       )(v => _filter.copy(tags        = v)),
      mkStr("description", _filter.description)(v => _filter.copy(description = v)),
      mkStr("file name"  , _filter.fileName   )(v => _filter.copy(fileName    = v)),
      mkStr("license"    , _filter.license    )(v => _filter.copy(license     = v)),
//      mkStr("user name"  , _filter.userName   )(v => _filter.copy(userName    = v)),
//      mkStr("comment"    , _filter.comment    )(v => _filter.copy(comment     = v))
      mkUDouble("duration [s]", _filter.duration, default = 2, min = 1, max = 8192)(v => _filter.copy(duration = v)),
      mkUInt("num-channels", _filter.numChannels, default = 2, min = 1, max = 8192)(v => _filter.copy(numChannels = v)),
      mkUInt("sample-rate [Hz]" , _filter.sampleRate, default = 44100, min = 0, max = 96000 * 4)(v => _filter.copy(sampleRate = v)),
      mkUInt("bit-depth", _filter.bitDepth, default = 16, min = 8, max = 64, step = 8)(v => _filter.copy(bitDepth = v)),
      // XXX TODO --- bit-rate filter seems broken, no matter what ranges we put, result is empty
      mkUInt("bit-rate [kbps]", _filter.bitRate, default = 320, min = 8, max = 24000, step = 8)(v => _filter.copy(bitRate = v)),
//      mkUInt("file-size", _filter.fileSize)(v => _filter.copy(fileSize = v)),
      mkUInt("num-downloads", _filter.numDownloads, default = 1, max = 1000000)(v => _filter.copy(numDownloads = v)),
      mkUDouble("average rating", _filter.avgRating, default = 5, min = 0, max = 5)(v => _filter.copy(avgRating = v)),
      mkUInt("num-ratings", _filter.numRatings, default = 1, max = 100000)(v => _filter.copy(numRatings = v))
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

    def filter: Filter = _filter
    def filter_=(value: Filter): Unit = {
      ???
      _filter = value
    }
  }
}