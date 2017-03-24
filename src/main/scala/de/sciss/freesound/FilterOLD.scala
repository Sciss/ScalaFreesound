package de.sciss.freesound

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.freesound.FilterOLD.{DateSpec, FileTypeUnion, StringTokens, StringUnion, UDoubleSpec, UIntSpec}
import de.sciss.optional.Optional

import scala.language.implicitConversions

object FilterOLD {
  private final implicit class DefinesPropertyOps(private val p: DefinesProperty) extends AnyVal {
    def mkParam(key: String): Option[String] = p.toPropertyOption.map { value =>
      if (p.isNegated) s"-$key:$value" else s"$key:$value"
    }
  }

  private final implicit class OptionalBooleanOps(private val opt: Optional[Boolean]) /* extends AnyVal */ {
    def mkParam(key: String): Option[String] = opt.map(value => s"$key=$value")
  }

  trait DefinesProperty {
    def toPropertyOption: Option[String]

    def isNegated: Boolean
  }

  object UIntSpec {
    implicit def fromInt(i: Int): UIntSpec = {
      require(i >= 0)
      UIntSpec(start = i, end = i)
    }

    implicit def fromRange(r: Range): UIntSpec = {
      require(r.nonEmpty  , "Range must be non-empty")
      require(r.start >= 0, s"Unsigned range must have start value >=0: $r")
      require(r.step == 1 , s"Range must be contiguous: $r")
      UIntSpec(start = r.start, end = r.last)
    }

    // avoid overloading here:
    //      implicit def fromIntOption  (opt: Option[Int  ]): UInt = opt.fold(empty)(fromInt  )

    implicit def fromRangeOption(opt: Option[Range]): UIntSpec = opt.fold(empty)(fromRange)

    def from(start: Int): UIntSpec = {
      require(start >= 0, s"Unsigned integer must be >= 0: $start")
      UIntSpec(start = start, end = -1)
    }

    def until(stop: Int): UIntSpec = {
      require(stop > 0, s"Exclusive range end must be > 0: $stop")
      UIntSpec(start = -1, end = stop - 1)
    }

    def to(end: Int): UIntSpec = {
      require(end >= 0, s"Inclusive range end must be >= 0: $end")
      UIntSpec(start = -1, end = end)
    }

    val empty: UIntSpec = UIntSpec(start = -1, end = -1)
  }
  final case class UIntSpec private(private val start: Int, private val end: Int, isNegated: Boolean = false)
    extends DefinesProperty {

    def unary_! : UIntSpec = copy(isNegated = !isNegated)

    override def toString: String =
      if      (isEmpty    ) "None"
      else if (isSingle   ) s"Some($start)"
      else if (end   == -1) s"UIntSpec.from($start)"
      else if (start == -1) s"UIntSpec.to($end)"
      else                  s"Some($start to $end)"

    def isSingle: Boolean = isDefined && start == end

    def toRangeOption: Option[Range] = if (isEmpty) None else Some(start to end)

    def isEmpty  : Boolean = start == -1 && end == -1
    def isDefined: Boolean = !isEmpty

    def toPropertyOption: Option[String] =
      if      (isEmpty    ) None
      else if (isSingle   ) Some(start.toString)
      else {
        val startS = if (start == -1) "*" else start.toString
        val endS   = if (end   == -1) "*" else end  .toString
        Some(s"[$startS TO $endS]")
      }
  }

  object UDoubleSpec {
    implicit def fromDouble(d: Double): UDoubleSpec = {
      require(d >= 0)
      UDoubleSpec(start = d, end = d)
    }

    implicit def fromRange(r: Range): UDoubleSpec = {
      require(r.nonEmpty  , "Range must be non-empty")
      require(r.start >= 0, s"Unsigned range must have start value >=0: $r")
      require(r.step == 1 , s"Range must be contiguous: $r")
      UDoubleSpec(start = r.start, end = r.last)
    }

    // avoid overloading here:
    //      implicit def fromDoubleOption  (opt: Option[Double  ]): UDouble = opt.fold(empty)(fromDouble  )

    implicit def fromRangeOption(opt: Option[Range]): UDoubleSpec = opt.fold(empty)(fromRange)

    def from(start: Double): UDoubleSpec = {
      require(start >= 0, s"Unsigned integer must be >= 0: $start")
      UDoubleSpec(start = start, end = -1)
    }

    def until(stop: Double): UDoubleSpec = {
      require(stop > 0, s"Exclusive range end must be > 0: $stop")
      UDoubleSpec(start = -1, end = stop - 1)
    }

    def to(end: Double): UDoubleSpec = {
      require(end >= 0, s"Inclusive range end must be >= 0: $end")
      UDoubleSpec(start = -1, end = end)
    }

    val empty: UDoubleSpec = UDoubleSpec(start = -1, end = -1)
  }
  final case class UDoubleSpec private(private val start: Double, private val end: Double, isNegated: Boolean = false)
    extends DefinesProperty {

    override def toString: String =
      if      (isEmpty    ) "None"
      else if (isSingle   ) s"Some($start)"
      else if (end   == -1) s"UDoubleSpec.from($start)"
      else if (start == -1) s"UDoubleSpec.to($end)"
      else                  s"Some($start to $end)"

    def unary_! : UDoubleSpec = copy(isNegated = !isNegated)

    def isSingle: Boolean = isDefined && start == end

    def startOption: Option[Double] = if (start == -1) None else Some(start)
    def endOption  : Option[Double] = if (end   == -1) None else Some(end  )

    def isEmpty  : Boolean = start == -1 && end == -1
    def isDefined: Boolean = !isEmpty

    def toPropertyOption: Option[String] =
      if      (isEmpty    ) None
      else if (isSingle   ) Some(start.toString)
      else {
        val startS = if (start == -1) "*" else start.toString
        val endS   = if (end   == -1) "*" else end  .toString
        Some(s"[$startS TO $endS]")
      }
  }

  object StringUnion {
    implicit def fromString (s  :        String ): StringUnion = StringUnion(s :: Nil)
    implicit def fromSeq    (xs : Seq   [String]): StringUnion = StringUnion(xs .toList)
    implicit def fromOption (opt: Option[String]): StringUnion = StringUnion(opt.toList)

    val empty: StringUnion = StringUnion(Nil)
  }
  final case class StringUnion(elems: List[String], isNegated: Boolean = false) extends DefinesProperty {
    def unary_! : StringUnion = copy(isNegated = !isNegated)

    def | (that: StringUnion): StringUnion = StringUnion(elems = this.elems union that.elems)

    def toPropertyOption: Option[String] = elems match {
      // XXX TODO --- escape strings, put in double quotes if necessary
      case Nil            => None
      case single :: Nil  => Some(single)
      case more           => Some(more.mkString("(", " OR ", ")"))
    }
  }

  object StringTokens {
    implicit def fromString(s  :        String ): StringTokens = StringTokens(s)
    implicit def fromSeq   (xs : Seq   [String]): StringTokens = StringTokens(xs: _*)
    implicit def fromOption(opt: Option[String]): StringTokens = StringTokens(opt.toList: _*)

    val empty: StringTokens = StringTokens()
  }
  final case class StringTokens(elems: String*) extends DefinesProperty {
    // def | (that: StringTokens): StringTokens = StringTokens(elems = this.elems union that.elems: _*)

    def isNegated = false // XXX TODO --- should we allow to change this?

    def toPropertyOption: Option[String] = if (elems.isEmpty) None else {
      // XXX TODO --- escape strings, put in double quotes if necessary
      Some(elems.mkString(" "))
    }
  }

  object FileTypeUnion {
    implicit def fromString(s  : String)          : FileTypeUnion = FileTypeUnion(s :: Nil)
    implicit def fromSeq   (xs : Seq   [FileType]): FileTypeUnion = FileTypeUnion(xs .toList)
    implicit def fromOption(opt: Option[FileType]): FileTypeUnion = FileTypeUnion(opt.toList)

    val empty: FileTypeUnion = FileTypeUnion(Nil)
  }
  final case class FileTypeUnion(elems: List[FileType], isNegated: Boolean = false) extends DefinesProperty {
    def unary_! : FileTypeUnion = copy(isNegated = !isNegated)

    def | (that: FileTypeUnion): FileTypeUnion = FileTypeUnion(elems = this.elems union that.elems)

    def toPropertyOption: Option[String] = {
      // XXX TODO --- escape strings, put in double quotes if necessary
      val xs = elems.map(_.toProperty)
      xs match {
        case Nil            => None
        case single :: Nil  => Some(single)
        case more           => Some(more.mkString("(", " OR ", ")"))
      }
    }
  }

  object DateSpec {
    implicit def fromDate(d: Date): DateSpec = DateSpec(startOption = Some(d), endOption = Some(d))

    implicit def fromDateOption(opt: Option[Date]): DateSpec = opt.fold(empty)(fromDate)

    def from(start: Date): DateSpec = DateSpec(startOption = Some(start), endOption = None     )
    def to  (end  : Date): DateSpec = DateSpec(startOption = None       , endOption = Some(end))

    val empty: DateSpec = DateSpec(startOption = None, endOption = None)

    // XXX TODO --- is this the correct one for Freesound?
    private val formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
  }
  final case class DateSpec private(startOption: Option[Date], endOption: Option[Date], isNegated: Boolean = false)
    extends DefinesProperty {

    import DateSpec.formatter

    def unary_! : DateSpec = copy(isNegated = !isNegated)

    override def toString: String = toPropertyOption.toString

    def isSingle : Boolean = isDefined && startOption == endOption

    def isEmpty  : Boolean = startOption.isEmpty && endOption.isEmpty
    def isDefined: Boolean = !isEmpty

    def toPropertyOption: Option[String] =
      if      (isEmpty    ) None
      else if (isSingle   ) startOption.map(formatter.format)
      else {
        val startS = startOption.fold("*")(formatter.format)
        val endS   = endOption  .fold("*")(formatter.format)
        Some(s"[$startS TO $endS]")
      }
  }
}

/** The definition of a search filter. By default, all fields
  * are in their undefined state, so one can use named arguments
  * to add filter criteria.
  *
  * @param id                 sound id on freesound
  * @param userName           string, not tokenized
  * @param created            date
  * @param fileName           string, tokenized
  * @param description        string, tokenized
  * @param tag                string
  * @param license            string (“Attribution”, “Attribution Noncommercial” or “Creative Commons 0”)
  * @param isRemix            boolean
  * @param wasRemixed         boolean
  * @param pack               string
  * @param packTokens         string, tokenized
  * @param geoTagged          boolean
  * @param fileType           string, original file type (“wav”, “aif”, “aiff”, “ogg”, “mp3” or “flac”)
  * @param duration           numerical, duration of sound in seconds
  * @param bitDepth           integer, WARNING is not to be trusted right now
  * @param bitRate            numerical, WARNING is not to be trusted right now
  * @param sampleRate         integer
  * @param fileSize           integer, file size in bytes
  * @param numChannels        integer, number of channels in sound (mostly 1 or 2)
  * @param md5                string, 32-byte md5 hash of file
  * @param numDownloads       integer
  * @param avgRating          numerical, average rating, from 0 to 5
  * @param numRatings         integer, number of ratings
  * @param comment            string, tokenized (filter is satisfied if sound contains the specified value in at least one of its comments)
  * @param numComments        numerical, number of comments
  */
final case class FilterOLD(
                         id          : UIntSpec          = None,
                         userName    : StringUnion       = None,
                         created     : DateSpec          = None,
                         fileName    : StringTokens      = None,
                         description : StringTokens      = None,
                         tag         : StringUnion       = None,
                         license     : StringUnion       = None,
                         isRemix     : Optional[Boolean] = None,
                         wasRemixed  : Optional[Boolean] = None,
                         pack        : StringUnion       = None,
                         packTokens  : StringTokens      = None,
                         geoTagged   : Optional[Boolean] = None,
                         fileType    : FileTypeUnion     = None,
                         duration    : UDoubleSpec       = None,
                         bitDepth    : UIntSpec          = None,
                         bitRate     : UDoubleSpec       = None,
                         sampleRate  : UIntSpec          = None,
                         fileSize    : UIntSpec          = None,
                         numChannels : UIntSpec          = None,
                         md5         : StringUnion       = None,
                         numDownloads: UIntSpec          = None,
                         avgRating   : UDoubleSpec       = None,
                         numRatings  : UIntSpec          = None,
                         comment     : StringTokens      = None,
                         numComments : UIntSpec          = None
                       ) {

  require(avgRating.startOption.forall(_ <= 5) &&
    avgRating.endOption.forall(_ <= 5),
    s"avgRating out of range: $avgRating")

  def toPropertyOption: Option[String] = {
    import FilterOLD.OptionalBooleanOps
    val options = Seq(
      id            .mkParam("id"),
      userName      .mkParam("username"),
      created       .mkParam("created"),
      fileName      .mkParam("original_filename"),
      description   .mkParam("description"),
      tag           .mkParam("tag"),
      license       .mkParam("license"),
      isRemix       .mkParam("is_remix"),
      wasRemixed    .mkParam("was_remixed"),
      pack          .mkParam("pack"),
      packTokens    .mkParam("pack_tokenized"),
      geoTagged     .mkParam("is_geotagged"),
      fileType      .mkParam("type"),
      duration      .mkParam("duration"),
      bitDepth      .mkParam("bitdepth"),
      bitRate       .mkParam("bitrate"),
      sampleRate    .mkParam("samplerate"),
      fileSize      .mkParam("filesize"),
      numChannels   .mkParam("channels"),
      md5           .mkParam("md5"),
      numDownloads  .mkParam("num_downloads"),
      avgRating     .mkParam("avg_rating"),
      numRatings    .mkParam("num_ratings"),
      comment       .mkParam("comment"),
      numComments   .mkParam("comments")
    )
    val params = options.flatten
    if (params.isEmpty) None else Some(params.mkString(" "))
  }
}
