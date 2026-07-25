#!/bin/sh
set -e

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
DIST_URL=$(sed -n 's/^distributionUrl=//p' "$PROPS")
MAVEN_VERSION=$(printf '%s' "$DIST_URL" | sed -n 's#.*apache-maven/\([^/]*\)/.*#\1#p')
CACHE_DIR="${MAVEN_USER_HOME:-$HOME/.m2}/wrapper/dists/apache-maven-$MAVEN_VERSION"
MAVEN_HOME="$CACHE_DIR/apache-maven-$MAVEN_VERSION"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$CACHE_DIR"
  ZIP_FILE="$CACHE_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ZIP_FILE"
  else
    echo "curl or wget is required to download Maven." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ZIP_FILE" -d "$CACHE_DIR"
  else
    echo "unzip is required to install Maven." >&2
    exit 1
  fi
  rm -f "$ZIP_FILE"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
