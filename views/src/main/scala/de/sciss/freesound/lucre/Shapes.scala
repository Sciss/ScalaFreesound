/*
 *  Shapes.scala
 *  (ScalaFreesound)
 *
 *  Copyright (c) 2010-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.freesound.lucre

import java.awt.geom.Path2D

object Shapes {
  /////////////////////////////////////////////////////////////
  // the following shapes are taken from the Noun project
  // https://thenounproject.com/term/justice/643699/
  //
  // Creative Commons CC BY 3.0 US
  //
  // Copyright (c) Andrejs Kirma

  def Justice(p: Path2D): Unit = {
    p.moveTo(16.0f, 0.0f)
    p.curveTo(13.938175f, 0.0f, 12.267557f, 1.6709048f, 12.267578f, 3.7460938f)
    p.lineTo(5.1328125f, 5.3515625f)
    p.curveTo(4.8150315f, 5.4103193f, 4.522585f, 5.611568f, 4.345703f, 6.0f)
    p.lineTo(0.08398437f, 16.11914f)
    p.curveTo(0.02339437f, 16.263098f, -0.0037f, 16.413918f, 0.0f, 16.560547f)
    p.curveTo(0.014224f, 19.493914f, 2.3973255f, 21.867188f, 5.3339844f, 21.867188f)
    p.curveTo(8.270642f, 21.867188f, 10.651796f, 19.493912f, 10.666016f, 16.560547f)
    p.curveTo(10.669716f, 16.413918f, 10.644596f, 16.263098f, 10.583985f, 16.11914f)
    p.lineTo(6.810547f, 7.1601562f)
    p.lineTo(12.886719f, 5.7929688f)
    p.curveTo(13.36463f, 6.513684f, 14.085993f, 7.060239f, 14.933594f, 7.3125f)
    p.lineTo(14.933594f, 27.199219f)
    p.lineTo(9.0625f, 27.199219f)
    p.curveTo(8.479161f, 27.199219f, 8.0f, 27.674967f, 8.0f, 28.261719f)
    p.lineTo(8.0f, 30.9375f)
    p.curveTo(8.0f, 31.519815f, 8.475875f, 32.0f, 9.0625f, 32.0f)
    p.lineTo(22.9375f, 32.0f)
    p.curveTo(23.52084f, 32.0f, 24.00001f, 31.524252f, 24.0f, 30.9375f)
    p.lineTo(24.0f, 28.261719f)
    p.curveTo(24.0f, 27.679405f, 23.524124f, 27.199219f, 22.9375f, 27.199219f)
    p.lineTo(17.066406f, 27.199219f)
    p.lineTo(17.066406f, 7.3125f)
    p.curveTo(18.44497f, 6.902212f, 19.489208f, 5.716674f, 19.695312f, 4.2617188f)
    p.lineTo(24.878906f, 3.0957031f)
    p.lineTo(21.416016f, 11.3203125f)
    p.curveTo(21.355425f, 11.46427f, 21.330286f, 11.613137f, 21.333986f, 11.759767f)
    p.curveTo(21.348206f, 14.693132f, 23.729359f, 17.066406f, 26.66602f, 17.066406f)
    p.curveTo(29.602676f, 17.066406f, 31.985783f, 14.693366f, 32.000004f, 11.759766f)
    p.curveTo(32.003704f, 11.613135f, 31.976633f, 11.4623165f, 31.916023f, 11.318358f)
    p.lineTo(27.65625f, 1.203125f)
    p.curveTo(27.64361f, 1.171455f, 27.63064f, 1.141248f, 27.61523f, 1.111328f)
    p.curveTo(27.443607f, 0.77406406f, 27.133795f, 0.5827255f, 26.806637f, 0.5410155f)
    p.curveTo(26.731247f, 0.5314155f, 26.652214f, 0.5303092f, 26.574215f, 0.5371095f)
    p.curveTo(26.510145f, 0.5424395f, 26.447083f, 0.5537226f, 26.384762f, 0.5703126f)
    p.lineTo(19.38086f, 2.1464844f)
    p.curveTo(18.784325f, 0.8782814f, 17.49429f, 0.0f, 16.0f, 0.0f)
    p.lineTo(16.0f, 0.0f)
    p.moveTo(26.666016f, 4.3847656f)
    p.lineTo(29.77539f, 11.732422f)
    p.lineTo(23.558594f, 11.732422f)
    p.lineTo(26.666016f, 4.3847656f)
    p.moveTo(5.3339844f, 9.167969f)
    p.lineTo(8.441406f, 16.533203f)
    p.lineTo(2.2246094f, 16.533203f)
    p.lineTo(5.3339844f, 9.167969f)
  }
}