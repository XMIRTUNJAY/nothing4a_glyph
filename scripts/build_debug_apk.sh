#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AAR="$ROOT_DIR/app/libs/glyph-matrix-sdk-2.0.aar"

if [[ ! -f "$AAR" ]]; then
  cat >&2 <<MSG
Missing Nothing Glyph SDK AAR:
  $AAR

Download glyph-matrix-sdk-2.0.aar from Nothing's GlyphMatrix-Developer-Kit and place it there,
or run ./scripts/download_nothing_sdk.sh, then re-run this script.
MSG
  exit 2
fi

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set. Use JDK 17 or 21 for Android builds." >&2
  exit 2
fi

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "JAVA_HOME does not point to a valid JDK: $JAVA_HOME" >&2
  exit 2
fi

"$JAVA_HOME/bin/java" -version
cd "$ROOT_DIR"
gradle :app:assembleDebug

APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [[ -f "$APK" ]]; then
  echo "Built installable APK: $APK"
else
  echo "Gradle finished but APK not found at: $APK" >&2
  exit 1
fi
