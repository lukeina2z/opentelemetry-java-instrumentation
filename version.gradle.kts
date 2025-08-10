val stableVersion = "2.11.0-adot1"
val alphaVersion = "2.11.0-adot1-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
