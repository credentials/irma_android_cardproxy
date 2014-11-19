# irma_android_cardproxy


The Card Proxy app allows an NFC-enabled android phone to act as a proxy to the IRMA card for services running in the browser of a 'normal' PC.

## Prerequisites

This application has the following dependencies.  All these dependencies will be automatically downloaded by gradle when building or installing the library.

External depenencies:

 * [Android Asynchronous HTTP Client](http://loopj.com/android-async-http/)
 * Android support v4
 * [Google GSON](https://code.google.com/p/google-gson/)

Internal dependencies:

 * [irma_android_library](https://github.com/credentials/irma_android_library/), The IRMA android library
 * [Scuba](https://github.com/credentials/scuba), The smartcard abstraction layer, uses `scuba_sc_android` and `scuba_smartcard`

Gradle will take care of the transitive dependencies. However, you must make sure that you [build and install the idemix_library](https://github.com/credentials/idemix_library/) yourself.

The build system depends on gradle version at least 2.1, which is why we've included the gradle wrapper, so you always have the right version.

## Building

Run

    ./gradlew assemble

this will create the required `.apk`s and place them in `build/outputs/apk`.

## Installing on your own device

You can install the application to you own device by running

    ./gradlew installDebug
