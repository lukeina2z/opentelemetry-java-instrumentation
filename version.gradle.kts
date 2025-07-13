val stableVersion = "2.20.99-SNAPSHOT"
val alphaVersion = "2.20.99-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
