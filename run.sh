#!/usr/bin/env sh
# Build and run without Maven: only a JDK (17+) is needed.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/main/java/puffpatrol/*.java
java -cp out puffpatrol.Main "$@"
