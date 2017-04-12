package de.sciss.freesound.swing

import scala.swing.{Frame, MainFrame, SimpleSwingApplication}

object TestFilterView extends SimpleSwingApplication {
  lazy val top: Frame = new MainFrame {
    title = "Filter Test"
    contents = {
      val view = FilterView()
      view.addListener {
        case update => println(s"New filter: $update")
      }
      view.component
    }
  }
}
