#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_SQL="$SCRIPT_DIR/DB/script-demo-global.sql"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-gestionrh}"
DB_USERNAME="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-3958}"

export PGPASSWORD="$DB_PASSWORD"

if [[ ! -f "$DEMO_SQL" ]]; then
    echo "Fichier introuvable : $DEMO_SQL" >&2
    exit 1
fi

schema_existe=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" \
    -Atc "select to_regclass('public.utilisateur') is not null;")

if [[ "$schema_existe" != "t" ]]; then
    echo "Le schéma n'existe pas dans la base '$DB_NAME'." >&2
    echo "Exécutez d'abord : psql -U $DB_USERNAME -d $DB_NAME -f DB/script-global.sql" >&2
    exit 1
fi

echo "Chargement des données de démonstration dans '$DB_NAME'..."
echo "Les données opérationnelles existantes seront remplacées."

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -f "$DEMO_SQL"

echo "Données de démonstration chargées."
