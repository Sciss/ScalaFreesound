package de.sciss.freesound

case class FreesoundOptions(
   keyword: String,
   descriptions : Boolean = true,
   tags : Boolean = true,
   fileNames : Boolean = false,
   userNames : Boolean = false,
   minDuration : Int = 1,
   maxDuration : Int = 20,
   order : Int = 1,
   offset : Int = 0,
   maxItems : Int = 100
)
