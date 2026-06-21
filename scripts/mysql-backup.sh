#!/usr/bin/env sh
set -eu

CONTAINER="${MYSQL_CONTAINER_NAME:-smartcloset-mysql}"
OUTPUT_DIR="${MYSQL_BACKUP_DIR:-backups/mysql}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUTPUT="${1:-$OUTPUT_DIR/smartcloset-$TIMESTAMP.sql}"

umask 077
mkdir -p "$(dirname "$OUTPUT")"
TMP_OUTPUT="$(mktemp "$(dirname "$OUTPUT")/.smartcloset-backup.XXXXXX")"
trap 'rm -f "$TMP_OUTPUT"' EXIT

docker exec "$CONTAINER" sh -c \
  'mysqldump --single-transaction --quick --routines --triggers --add-drop-table --no-tablespaces -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  > "$TMP_OUTPUT"

mv "$TMP_OUTPUT" "$OUTPUT"
trap - EXIT

printf '%s\n' "$OUTPUT"
