#!/usr/bin/env sh
set -eu

if [ "$#" -ne 1 ]; then
  echo "Usage: scripts/mysql-restore.sh <backup.sql>" >&2
  exit 2
fi

INPUT="$1"
CONTAINER="${MYSQL_CONTAINER_NAME:-smartcloset-mysql}"

if [ ! -f "$INPUT" ]; then
  echo "Backup file not found: $INPUT" >&2
  exit 2
fi

if [ "${SMARTCLOSET_RESTORE_CONFIRM:-}" != "restore" ]; then
  echo "Set SMARTCLOSET_RESTORE_CONFIRM=restore to restore into $CONTAINER." >&2
  exit 2
fi

docker exec -i "$CONTAINER" sh -c \
  'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' \
  < "$INPUT"
