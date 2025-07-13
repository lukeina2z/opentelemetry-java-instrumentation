val stableVersion = "2.19.99-SNAPSHOT"
val alphaVersion = "2.19.99-alpha-SNAPSHOT"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
