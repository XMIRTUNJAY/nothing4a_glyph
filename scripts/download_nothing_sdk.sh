#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT_DIR/app/libs/glyph-matrix-sdk-2.0.aar"
URL="https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/raw/refs/heads/main/glyph-matrix-sdk-2.0.aar"

mkdir -p "$(dirname "$DEST")"

echo "Downloading Nothing Glyph Matrix SDK AAR..."
echo "Source: $URL"
echo "Dest:   $DEST"

if command -v curl >/dev/null 2>&1; then
  curl -L --fail --retry 3 -A "Mozilla/5.0" -o "$DEST" "$URL"
elif command -v wget >/dev/null 2>&1; then
  wget -O "$DEST" "$URL"
else
  echo "Neither curl nor wget is installed." >&2
  exit 2
fi

if ! unzip -t "$DEST" >/dev/null 2>&1; then
  echo "Downloaded file is not a valid AAR/ZIP: $DEST" >&2
  rm -f "$DEST"
  exit 1
fi

printf 'Downloaded SDK AAR: '
stat -c '%n (%s bytes)' "$DEST" 2>/dev/null || ls -lh "$DEST"
