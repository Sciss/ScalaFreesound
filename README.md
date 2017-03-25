# ScalaFreesound

[![Flattr this](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=sciss&url=https%3A%2F%2Fgithub.com%2FSciss%2FScalaFreesound&title=ScalaFreesound&language=Scala&tags=github&category=software)
[![Build Status](https://travis-ci.org/Sciss/ScalaFreesound.svg?branch=master)](https://travis-ci.org/Sciss/ScalaFreesound)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/scalafreesound_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/scalafreesound_2.12)

## statement

ScalaFreesound is a library to query the Freesound audio database ("freesound.org":http://freesound.org). It is (C)opyright 2010&ndash;2017 by Hanns Holger Rutz. All rights reserved. It is released under the [GNU Lesser General Public License](http://github.com/Sciss/ScalaFreesound/blob/master/licenses/ScalaFreesound-License.txt) and comes with absolutely no warranties. To contact the author, send an email to `contact@sciss.de`

## requirements / installation

ScalaOSC currently builds against Scala 2.12, 2.11, 2.10 using sbt 0.13.

To link to it:

    libraryDependencies += "de.sciss" %% "scalafreesound" % v

The current version `v` is `"1.0.0"`

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## overview

The API is currently being reworked. What currently already works is constructing text search strings, like so:

```scala
TextSearch("water", Filter(numChannels = 2, sampleRate=96000, avgRating = 3.0 to *)).toQueryString
```

You can pop this into a manual `curl` call:

    curl "http://www.freesound.org/apiv2/search/text/?token=<your-secret-token>&<query-string>

(to-do)
