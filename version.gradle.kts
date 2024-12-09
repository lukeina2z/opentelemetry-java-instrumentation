val stableVersion = "2.10.0-adot1"
val alphaVersion = "2.10.0-adot1-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
