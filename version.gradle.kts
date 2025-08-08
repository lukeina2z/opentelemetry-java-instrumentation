val stableVersion = "2.11.0-adot2"
val alphaVersion = "2.11.0-adot2-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
