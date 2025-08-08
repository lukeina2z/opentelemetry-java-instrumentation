<<<<<<< HEAD
val stableVersion = "2.18.1"
val alphaVersion = "2.18.1-alpha"
=======
val stableVersion = "2.11.0-adot2"
val alphaVersion = "2.11.0-adot2-alpha"
>>>>>>> 75cccac7ad (applying first patch in the v2.11 branch.)

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
