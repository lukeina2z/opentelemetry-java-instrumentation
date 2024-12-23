<<<<<<< HEAD
val stableVersion = "2.18.1"
val alphaVersion = "2.18.1-alpha"
=======
val stableVersion = "2.11.0-adot1"
val alphaVersion = "2.11.0-adot1-alpha"
>>>>>>> 392b954d0e ([R:] applied patch from adot java repo.)

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
