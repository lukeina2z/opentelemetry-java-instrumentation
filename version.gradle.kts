val stableVersion = "2.18.99"
val alphaVersion = "2.18.99-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
