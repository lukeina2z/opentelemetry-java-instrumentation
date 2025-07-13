#!/bin/sh

set -e
set -x


./gradlew assemble

./gradlew publishToMavenLocal


