/*
 *  QueryExpr.scala
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

import de.sciss.freesound.QueryExpr.{Base, Factory}

object QueryExpr {
  sealed trait Base[Repr]
  trait Const[Repr] extends Base[Repr] {
    private[freesound] def constString: String

    override def toString: String = constString
  }
  trait And[Repr] extends Base[Repr] {
    private[freesound] def a: Repr
    private[freesound] def b: Repr
  }
  trait Or[Repr] extends Base[Repr] {
    private[freesound] def a: Repr
    private[freesound] def b: Repr
  }

  trait Not[Repr] extends Base[Repr] {
    private[freesound] def a: Repr
  }

//  sealed trait OrEmpty
//  case object Empty extends OrEmpty

  trait Factory[Repr] {
    def not(a: Repr         ): Repr
    def and(a: Repr, b: Repr): Repr
    def or (a: Repr, b: Repr): Repr
  }
}
/** Query expression that can make use of the Solr-style logical operators. */
trait QueryExpr /* extends OrEmpty */ {
  // ---- abstract ----

  type Repr <: QueryExpr

  protected def factory: Factory[Repr]
  protected def self   : Repr with Base[Repr]

  // ---- impl ----

  final def | (that: Repr): Repr = factory.or (self, that)
  final def & (that: Repr): Repr = factory.and(self, that)
  final def unary_!       : Repr = factory.not(self)

  final def toQueryString(fieldName: String): String = (self: Base[Repr]) match {
    case op: QueryExpr.Not[Repr]  => s"-$fieldName=${op.a.toQueryStringFragment}"
    case _                        => s"fieldName=$toQueryStringFragment"
  }

  // XXX TODO --- we could pretty print `((A OR B) OR C)` and `(A OR (B OR C))` as `(A OR B OR C)`
  final private[freesound] def toQueryStringFragment: String = (self: Base[Repr]) match {
    case c : QueryExpr.Const[Repr]  => c.constString  // XXX TODO --- I guess we must escape spaces and reserved words such as OR and AND
    case op: QueryExpr.And  [Repr]  => s"(${op.a.toQueryStringFragment} AND ${op.b.toQueryStringFragment})"
    case op: QueryExpr.Or   [Repr]  => s"(${op.a.toQueryStringFragment} OR ${op.b.toQueryStringFragment})"
    case op: QueryExpr.Not  [Repr]  => s"(*:* NOT ${op.a.toQueryStringFragment})"
  }
}
