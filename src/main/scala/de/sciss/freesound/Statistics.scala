package de.sciss.freesound

//case class Statistics( numDownloads: Int, numRatings: Int, rating: Int ) {
//   override def toString = "Statistics(numDownloads = " + numDownloads + ", numRatings = " + numRatings +
//      ", rating = " + rating + ")"
//}

trait Statistics { 
   def numDownloads : Int
   def numRatings : Int
   def rating : Int
}