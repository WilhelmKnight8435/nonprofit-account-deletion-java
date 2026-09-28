#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "usage: ./run-example.sh <user-id> <credential-id>" >&2
  exit 2
fi

BUILD_DIR="${TMPDIR:-/tmp}/nonprofit-account-delete-classes"
mkdir -p "$BUILD_DIR"
find src/main/java -name '*.java' -print | xargs javac -d "$BUILD_DIR"
java -cp "$BUILD_DIR" org.community.privacy.NonprofitDeletionMain "$1" "$2"
