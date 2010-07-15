package de.sciss.freesound

import java.util.Date

trait Comment {
   def user: User
   def date: Date
   def text: String
}