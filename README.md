# ScalaFreesound

[![Build Status](https://github.com/Sciss/ScalaFreesound/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/ScalaFreesound/actions?query=workflow%3A%22Scala+CI%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/scalafreesound_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/scalafreesound-core_2.13)
<a href="https://liberapay.com/sciss"><img alt="Donate using Liberapay" src="https://liberapay.com/assets/widgets/donate.svg" height="24"></a>

## statement

ScalaFreesound is a library to query the [Freesound audio database](https://freesound.org). It is
(C)opyright 2010&ndash;2020 by Hanns Holger Rutz. All rights reserved. It is released under the
[GNU Affero General Public License](https://git.iem.at/sciss/ScalaFreesound/blob/main/LICENSE) and comes with 
absolutely no warranties. To contact the author, send an e-mail to `contact@sciss.de`.

Please consider supporting this project through Liberapay (see badge above) – thank you!

## requirements / installation

This project builds with sbt against Scala 2.12, 2.13, Dotty. The last version to support Scala 2.11 was 1.19.0.

To link to it:

    libraryDependencies += "de.sciss" %% "scalafreesound"  % v
    
The current version `v` is `"2.4.0"`

Or to link to an individual module

    libraryDependencies += "de.sciss" %% "scalafreesound-core"        % v
    libraryDependencies += "de.sciss" %% "scalafreesound-swing"       % v
    libraryDependencies += "de.sciss" %% "scalafreesound-lucre"       % v
    libraryDependencies += "de.sciss" %% "scalafreesound-views"       % v
    libraryDependencies += "de.sciss" %% "scalafreesound-compression" % v

- the `core` module provides functions for searching the database and downloading previews and files.
- the `swing` module provides user interface elements for searching and viewing results.
- the `lucre` module provides a bridge to SoundProcesses/.
- the `views` module provides a bridge to [Mellite](https://www.sciss.de/mellite/).
- the `compression` module provides decoders from FLAC, Ogg, and mp3 to PCM.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## overview

The easiest to play around is to run `sbt test:console` which will import useful symbols
and also load the files `client.json` and `access_token` if found. An implicit `Client` is required
for searches, and an implicit `AccessToken` is required for downloading sounds.

```scala
import de.sciss.freesound._
import Implicits._
```

If you have never used Freesound, you must first create an account. Then you need to generate an
API key. Go to [www.freesound.org/docs/api/authentication.html#token-authentication](https://www.freesound.org/docs/api/authentication.html#token-authentication) and 
follow the link to [www.freesound.org/apiv2/apply](http://www.freesound.org/apiv2/apply).
Once you have created a key, create a file `client.json` with the following content:

```
{
  "id"    : "<client-id>",
  "secret": "<client-secret>"
}
```

With the bits copied from the __Client id__  and __Client secret/Api key__ sections into
these two entries. The client keys identify the application that is running queries against
the Freesound database, for operations that can be performed without identifying as a particular
user. If you don't want to restart the sbt console to automatically load those keys,
simply define them as follows:

```scala
implicit val client = Client(id = "foo", secret = "bar") // your correct code here
```

Now you should be able to run a query

```scala
val fut = Freesound.textSearch("water", Filter(numChannels = 2, sampleRate = 44100, duration = 1.0 to *), sort = Sort.DurationShortest)
```

The search filter specifies that the sound must be stereophonic at 44.1 kHz sampling rate, and with a duration of at least one second.
This returns a `Future[Seq[Sound]]`, sorted by duration. So once that is finished, you can access the values:

```scala
fut.foreach { res => println(res.head) }
```

This will print something like

```
Sound(234600,
  fileName    = Knife Hit Pan Filled With Water 4.wav,
  tags        = List(impact, hit, metal, water, knife, struck, pong, pan),
  description = Recorded using a Zoom H4N.,
  userName    = riddzy,
  created     = 2014-04-23T17:38:19.079851,
  license     = http://creativecommons.org/licenses/by/3.0/,
  pack 	      = None,
  geoTag      = None,
  fileType    = Wave,
  duration    = 1.000,
  numChannels = 2,
  sampleRate  = 44100.0,
  bitDepth    = 16,
  bitRate     = 1387.0,
  fileSize    = 177492,
  numDownloads= 20,
  avgRating   = 0.0,
  numRatings  = 0,
  numComments = 0
)
```

Let's assume the future is completed, and we assign the first result for simplicity:

```scala
val sound = fut.value.get.get.head
```

The download URL would be this:

```scala
Freesound.urlSoundBrowse.format(sound.userName, sound.id)
```

Giving [www.freesound.org/people/riddzy/sounds/234600/](https://www.freesound.org/people/riddzy/sounds/234600/).

Next, we want to download that sound. For this we need an OAuth2 authentication based on our user account.
You must open the following website:
https://www.freesound.org/apiv2/oauth2/authorize/?client_id=########&response_type=code
Where `########` is replaced by the __Client id__ section of the api-key registration page.
This will then give you a message like

    Permission granted to application ScalaFreesound!.
    Your authorization code:
    ########

We can then generate access keys using 

```scala
val futAuth = Freesound.getAuth(<code>)
```

And write those keys to disk if we wish:

```scala
futAuth.foreach { implicit auth => Freesound.writeAuth() }
```

Now we're ready to download the sound file.

```scala
Freesound.download(sound.id, new java.io.File(sound.fileName))
```

We should then find the file `Knife Hit Pan Filled With Water 4.wav` on the disk.
