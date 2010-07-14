package de.sciss.freesound

case class FreesoundInfo(
   numDownloads: Int,
   extension: String,
   sampleRate: Double,
   bitRate: Int,
   bitDepth: Int,
   numChannels: Int,
   duration: Double,
   fileSize: Long,
   id: Long,
   userID: Long,
   userName: String,
   index: Int
)
