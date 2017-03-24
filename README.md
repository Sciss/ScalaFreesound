# ScalaFreesound

## statement

ScalaFreesound is a library to query the Freesound audio database ("freesound.org":http://freesound.org). It is (C)opyright 2010&ndash;2017 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU Lesser General Public License](http://github.com/Sciss/ScalaFreesound/blob/master/licenses/ScalaFreesound-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact@sciss.de`

ScalaFreesound currently relies on the `curl` unix command to execute queries.

## requirements / installation

ScalaOSC currently builds against Scala 2.12, 2.11, 2.10 using sbt 0.13.

To link to it:

    libraryDependencies += "de.sciss" %% "scalafreesound" % v

The current version `v` is `"1.0.0"`

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## overview

The following example session is obsolete, as the API is currently being reworked. What currently already works is constructing text search strings, like so:

```scala
TextSearch("water", Filter(numChannels = 2, sampleRate=96000, avgRating = 3.0 to *)).toQueryString
```

You can pop this into a manual `curl` call:

    curl "http://www.freesound.org/apiv2/search/text/?token=<your-secret-token>&<query-string>

Old session:

```
Welcome to Scala version 2.8.0.final (Java HotSpot(TM) Client VM, Java 1.6.0_20).
Type in expressions to have them evaluated.
Type :help for more information.
```

```
scala> import de.sciss.freesound._
import de.sciss.freesound._

scala> val lp = Freesound.login("<user>", "<pass>")
lp: de.sciss.freesound.LoginProcess = LoginProcess(<user>)

scala> lp.perform()
Trying to log in...
Login was successful.

pre. scala> implicit val l = lp.login.get
l: de.sciss.freesound.Login = Login(<user>)

scala> val smp = Sample(25)
smp: de.sciss.freesound.Sample = Sample(25)

scala> smp.performInfo
Getting info for sample #25...
Info for sample #25 retrieved.

scala> smp.addListener { case Sample.DownloadProgress(p) => println("P = " + p + "%") }
res2: de.sciss.freesound.Model.Listener = <function1>

scala> smp.performDownload
Downloading sample #25...
P = 0%
P = 1%
P = 2%
...
P = 98%
P = 100%
Sample #25 downloaded (/private/var/folders/Dt/DtvfRgm6FGaibyjzjqE6OE+++TI/-Tmp-/25__Anton__Glass_C_mf.wav).

scala> val s = l.search( SearchOptions( "Helicopter" ))
s: de.sciss.freesound.Search = Search(SearchOptions(keyword = "Helicopter", descriptions = true, tags = true, fileNames = false, userNames = false, minDuration = 1, maxDuration = 20, order = 1, maxItems = 100))

scala> s.perform()
Performing search...
Search was successful (45 samples found).

scala> val smps = s.samples.get
smps: scala.collection.immutable.IndexedSeq[de.sciss.freesound.Sample] = Vector(Sample(23289), Sample(12659), Sample(23288), Sample(39865), Sample(8135), Sample(25951), Sample(23287), Sample(7033), Sample(7034), Sample(7536), Sample(7533), Sample(7538), Sample(7535), Sample(41729), Sample(48561), Sample(41004), Sample(49482), Sample(7534), Sample(5668), Sample(19443), Sample(5469), Sample(69607), Sample(38123), Sample(94867), Sample(69609), Sample(78889), Sample(65457), Sample(69608), Sample(48559), Sample(34941), Sample(50608), Sample(48564), Sample(46803), Sample(93076), Sample(48563), Sample(48562), Sample(76553), Sample(88464), Sample(48560), Sample(71045), Sample(43027), Sample(47167), Sample(81510), Sample(98116), Sample(100150))

scala> val x = smps(0)
x: de.sciss.freesound.Sample = Sample(23289)

scala> x.performInfo()
Getting info for sample #23289...
Info for sample #23289 retrieved.

scala> val i = x.info.get
i: de.sciss.freesound.SampleInfo = SampleInfo(23289, Resocopter Fast.aif)

scala> i.duration
res6: Double = 18.0

scala> i.sampleRate
res7: Double = 44100.0

scala> i.descriptions.head.text
res8: String = a digital representation of the blades of a helicopter rotating at a fast speed
```

## known issues

There is currently a problem with Scala 2.8 actors that makes them "starve" very easily when using blocking methods such as the downloads of ScalaFreesound. The workaround is to set a specific system property prior using downloading:

    System.setProperty( "actors.enableForkJoin", "false" )