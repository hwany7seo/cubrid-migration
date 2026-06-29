#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIST_DIR="$SCRIPT_DIR/dist"

mkdir -p "$DIST_DIR"

for LANG in en ko; do
    echo "=== Building $LANG HTML ==="
    (cd "$SCRIPT_DIR/$LANG" && make clean && make html)

    echo "=== Building $LANG PDF ==="
    (cd "$SCRIPT_DIR/$LANG" && make pdf)

    echo "=== Packaging $LANG ==="

    # HTML -> zip
    HTML_DIR="$SCRIPT_DIR/$LANG/_build/html"
    ZIP_FILE="$DIST_DIR/coradb-migration-manual-$LANG.zip"
    rm -f "$ZIP_FILE"
    (cd "$HTML_DIR" && zip -qr "$ZIP_FILE" .)
    echo "Created: $ZIP_FILE"

    # PDF -> dist (sphinx-simplepdf outputs to _build/pdf/)
    PDF_SRC="$SCRIPT_DIR/$LANG/_build/pdf/MiT_Manual_$LANG.pdf"
    PDF_DST="$DIST_DIR/coradb-migration-manual-$LANG.pdf"
    cp "$PDF_SRC" "$PDF_DST"
    echo "Created: $PDF_DST"
done

echo ""
echo "=== Build complete ==="
ls -lh "$DIST_DIR"
