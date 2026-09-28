#!/bin/sh
set -eu

BUILD_DIR="${TMPDIR:-/tmp}/nonprofit-account-delete-test-classes"
mkdir -p "$BUILD_DIR"
find src/main/java src/test/java -name '*.java' -print | xargs javac -d "$BUILD_DIR"
java -cp "$BUILD_DIR" org.community.privacy.application.AccountDeletionServiceTest
java -cp "$BUILD_DIR" org.community.privacy.infrai.InfraiAccountClientTest
