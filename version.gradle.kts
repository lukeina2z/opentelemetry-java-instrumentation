val stableVersion = "2.20.1-adot-lambda1"
val alphaVersion = "2.20.1-adot-lambda1-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
