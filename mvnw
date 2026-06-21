#!/bin/sh
# ============================================================
#  Maven Wrapper for Unix/Mac – SmartScrabble
#  Downloads Apache Maven 3.9.6 on first run if not cached.
#  Usage:  ./mvnw clean javafx:run
# ============================================================
WRAPPER_DIR="$(dirname "$0")/.mvn/wrapper"
MAVEN_VERSION="3.9.6"
MAVEN_HOME="$WRAPPER_DIR/apache-maven-$MAVEN_VERSION"
MVN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -f "$MVN_CMD" ]; then
  echo "[mvnw] Apache Maven $MAVEN_VERSION not found – downloading ..."
  mkdir -p "$WRAPPER_DIR"
  ZIP_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$ZIP_URL" -o "$WRAPPER_DIR/maven.zip"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$ZIP_URL" -O "$WRAPPER_DIR/maven.zip"
  else
    echo "[mvnw] Neither curl nor wget found. Install Maven manually."
    exit 1
  fi
  unzip -q "$WRAPPER_DIR/maven.zip" -d "$WRAPPER_DIR"
  rm -f "$WRAPPER_DIR/maven.zip"
  echo "[mvnw] Maven $MAVEN_VERSION ready."
fi

exec "$MVN_CMD" "$@"
