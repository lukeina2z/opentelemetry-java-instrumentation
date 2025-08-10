val stableVersion = "2.18.1-adot1"
val alphaVersion = "2.18.1-adot1-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
