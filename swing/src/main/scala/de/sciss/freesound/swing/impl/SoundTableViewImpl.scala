/*
 *  SoundTableViewImpl.scala
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

import java.awt.Color
import java.awt.geom.Path2D
import java.util.Comparator
import javax.swing.table.{AbstractTableModel, DefaultTableCellRenderer, TableCellRenderer, TableRowSorter}
import javax.swing.{Icon, JTable, SwingConstants}

import de.sciss.icons.raphael

import scala.collection.immutable.{Seq => ISeq}
import scala.swing.Table.AutoResizeMode
import scala.swing.{Component, ScrollPane, Table}

object SoundTableViewImpl {
  def apply(init: ISeq[Sound]): SoundTableView = new Impl(init)
  
//  private def toRawData(in: ISeq[Sound]): Array[Array[AnyRef]] = in.map(sound =>
//    sound.productIterator.map(_.asInstanceOf[AnyRef]).toArray
//  )(breakOut)

  private case class Column(idx: Int, name: String, minWidth: Int, prefWidth: Int, maxWidth: Int,
                            extract: Sound => Any, cellRenderer: Option[TableCellRenderer] = None,
                            sorter: Option[Comparator[_]] = None, headerRenderer: Option[TableCellRenderer] = None)

  private object RatingRenderer extends DefaultTableCellRenderer with Icon {
    setIcon(this)

    private[this] var rating = 0.0

    override def setValue(value: AnyRef): Unit = {
      rating = if (value == null) 0.0 else value match {
        case i: java.lang.Double  => i.doubleValue()
        case _                    => 0.0
      }
    }

    def getIconWidth  = 21
    def getIconHeight = 16

    def paintIcon(c: java.awt.Component, g: java.awt.Graphics, x: Int, y: Int): Unit = {
      g.setColor(getForeground)
      var xi = x + 1
      val y0 = y + 2
      val y1 = y + 14
      val xn = (x + rating * 58/5).toInt // 2
      while (xi < xn) {
        g.drawLine(xi, y0, xi, y1)
        xi += 2
      }
    }
  }

  private val RightAlignedRenderer = {
    val res = new DefaultTableCellRenderer
    res.setHorizontalAlignment(SwingConstants.TRAILING)
    res
  }

  private object DurationRenderer extends DefaultTableCellRenderer {
    setHorizontalAlignment(SwingConstants.TRAILING)

    override def setValue(value: AnyRef): Unit = value match {
      case d: java.lang.Double  => setText(f"${d.doubleValue()}%1.1f")
      case _                    => super.setValue(value)
    }
  }

  private def mkIconHeader(tt: String, shape: Path2D => Unit): TableCellRenderer = {
    val res = new DefaultTableCellRenderer
    res.setIcon(raphael.Icon(extent = 14, fill = Color.black)(shape))
    res.setHorizontalAlignment(SwingConstants.CENTER)
    res.setToolTipText(tt)
    res
  }

  private val DownloadHeaderRenderer = mkIconHeader("No. of Downloads", Shapes.Download)
  private val DurationHeaderRenderer = mkIconHeader("Duration [s]"    , raphael.Shapes.FutureTime)
  private val CommentsHeaderRenderer = mkIconHeader("No. of Comments" , Shapes.Comment)
  private val ChannelsHeaderRenderer = mkIconHeader("No. of Channels" , raphael.Shapes.Flickr /* "stereo" */)

  private object TagsRenderer extends DefaultTableCellRenderer {
    override def setValue(value: AnyRef): Unit = value match {
      case xs: List[_] => setText(xs.mkString(" "))
      case _           => super.setValue(value)
    }
  }

  private object PackRenderer extends DefaultTableCellRenderer {
    setHorizontalAlignment(SwingConstants.TRAILING)

    override def setValue(value: AnyRef): Unit = value match {
      case opt: Option[_] => setText(opt.fold("")(_.toString))
      case _ => super.setValue(value)
    }
  }

  private object GeoTagRenderer extends DefaultTableCellRenderer {
    override def setValue(value: AnyRef): Unit = value match {
      case opt: Option[_] => setText(opt.fold("")(_.toString))
      case _ => super.setValue(value)
    }
  }

  private object FileSizeRenderer extends DefaultTableCellRenderer {
    setHorizontalAlignment(SwingConstants.TRAILING)

    override def setValue(value: AnyRef): Unit = value match {
      case d: java.lang.Long =>
        val si    = true
        val unit  = if (si) 1000 else 1024
        val bytes = d.longValue()
        val s =
          if (bytes < unit) bytes + " B"
          else {
            val exp = (math.log(bytes) / math.log(unit)).toInt
            val pre = (if (si) "kMGTPE" else "KMGTPE").charAt(exp - 1) + (if (si) "" else "i")
            String.format("%.1f %sB", (bytes / math.pow(unit, exp)).asInstanceOf[java.lang.Double], pre)
          }
        setText(s)

      case _ => super.setValue(value)
    }
  }

  private val columns: Array[Column] = Array(
    Column( 0, "Id"             , 64,  64,  64, _.id              , Some(RightAlignedRenderer), Some(Ordering.Int)),
    Column( 1, "Name"           , 64, 144, 256, _.fileName        , None, None),
    Column( 2, "Tags"           , 64, 144, 256, _.tags            , Some(TagsRenderer), None),
    Column( 3, "Description"    , 64, 160, 384, _.description     , None, None),
    Column( 4, "User"           , 56,  72, 128, _.userName        , None, None),
    Column( 5, "Created"        , 64,  96, 152, _.created         , None, None),
    Column( 6, "License"        , 64,  96, 360, _.license         , None, None),
    Column( 7, "Pack"           , 48,  52,  64, _.packId          , Some(PackRenderer), Some(Ordering.Option[Int /* URI */])),
    Column( 8, "Geo"            , 48,  60, 160, _.geoTag          , Some(GeoTagRenderer), Some(Ordering.Option[GeoTag])),
    Column( 9, "Type"           , 48,  52,  56, _.fileType        , None, None),
    Column(10, null             , 48,  52,  64, _.duration        , Some(DurationRenderer    ), Some(Ordering.Double),
      headerRenderer = Some(DurationHeaderRenderer)),
    Column(11, null             , 32,  32,  48, _.numChannels     , Some(RightAlignedRenderer), Some(Ordering.Int   ),
      headerRenderer = Some(ChannelsHeaderRenderer)),
    Column(12, "sr [Hz]"        , 48,  60,  64, _.sampleRate.toInt, Some(RightAlignedRenderer), Some(Ordering.Int   )),
    Column(13, "Bits"           , 48,  52,  64, _.bitDepth.getOrElse(0), Some(RightAlignedRenderer), Some(Ordering.Int   )),
    Column(14, "kbps"           , 48,  52,  64, _.bitRate/*.toInt*/,Some(RightAlignedRenderer), Some(Ordering.Int   )),
    Column(15, "Size"           , 48,  64,  72, _.fileSize        , Some(FileSizeRenderer)    , Some(Ordering.Long  )),
    Column(16, null             , 48,  52,  64, _.numDownloads    , Some(RightAlignedRenderer), Some(Ordering.Int   ),
      headerRenderer = Some(DownloadHeaderRenderer)),
    Column(17, "Avg.\u2605"     , 60,  60,  60, _.avgRating       , Some(RatingRenderer)      , Some(Ordering.Double)),
    Column(18, "No.\u2605"      , 48,  52,  56, _.numRatings      , Some(RightAlignedRenderer), Some(Ordering.Int   )),
    Column(19, null             , 48,  52,  64, _.numComments     , Some(RightAlignedRenderer), Some(Ordering.Int   ),
      headerRenderer = Some(CommentsHeaderRenderer))
  )

  private final class Impl(private[this] var _sounds: ISeq[Sound]) extends SoundTableView {
    private object model extends AbstractTableModel {
      def getRowCount   : Int = _sounds.size
      def getColumnCount: Int = columns.length

      override def getColumnName(colIdx: Int): String = columns(colIdx).name

      def getValueAt(rowIdx: Int, colIdx: Int): AnyRef = {
        val sound = _sounds(rowIdx)
        val col   = columns(colIdx)
        col.extract(sound).asInstanceOf[AnyRef]
      }
    }

//    private[this] val model = new DefaultTableModel {
//      override def isCellEditable(row: Int, column: Int): Boolean = false
//    }

    sounds = _sounds  // initializes model

    val table: Table = {
      val res = new Table {
        // https://github.com/scala/scala-swing/issues/47
        override lazy val peer: JTable = new JTable with SuperMixin
      }
//      import de.sciss.swingplus.Implicits._
      res.model   = model
      val resJ    = res.peer
      val cm      = resJ.getColumnModel
      val sorter  = new TableRowSorter(model)
      columns.foreach { col =>
        val tc = cm.getColumn(col.idx)
        col.sorter.foreach(sorter.setComparator(col.idx, _))
        tc.setMinWidth      (col.minWidth )
        tc.setMaxWidth      (col.maxWidth )
        tc.setPreferredWidth(col.prefWidth)
        col.cellRenderer  .foreach(tc.setCellRenderer  )
        col.headerRenderer.foreach(tc.setHeaderRenderer)
      }
      // cm.setColumnMargin(6)
      resJ.setRowSorter(sorter)
      // cf. http://stackoverflow.com/questions/5968355/horizontal-bar-on-jscrollpane/5970400
      res.autoResizeMode = AutoResizeMode.Off
      // resJ.setPreferredScrollableViewportSize(resJ.getPreferredSize)
      res
    }

    val component: Component = {
      val res = new ScrollPane(table)
//      res.horizontalScrollBarPolicy = BarPolicy.Always
//      res.verticalScrollBarPolicy   = BarPolicy.Always
      res.peer.putClientProperty("styleId", "undecorated")
      res.preferredSize = {
        val d = res.preferredSize
        d.width = math.min(1024, table.preferredSize.width)
        d
      }
      res
    }

    def sounds: ISeq[Sound] = _sounds
    def sounds_=(xs: ISeq[Sound]): Unit = {
      _sounds = xs
      model.fireTableDataChanged() // setDataVector(toRawData(_sounds), colNames)
    }
  }
}
