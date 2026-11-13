#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$ROOT_DIR/gestion"
DB_NAME="${DB_NAME:-gestionrh}"
DB_USER="${DB_USERNAME:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-3958}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

fail() {
  printf 'Erreur : %s\n' "$*" >&2
  exit 1
}

# Utilise en premier le JDK 21 local au projet, sans modifier le Java systeme.
if [[ -x "$ROOT_DIR/.tools/jdk-21/bin/java" ]]; then
  JAVA_HOME="$ROOT_DIR/.tools/jdk-21"
elif [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  CURRENT_JAVA="$("$JAVA_HOME/bin/java" -version 2>&1 | awk -F '\"' '/version/ {print $2; exit}')"
  [[ "${CURRENT_JAVA%%.*}" == 21 ]] || JAVA_HOME=""
else
  JAVA_HOME=""
fi
if [[ -z "${JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
fi
# Un JDK Homebrew peut etre installe sans etre enregistre aupres de macOS.
if [[ -z "${JAVA_HOME:-}" ]]; then
  for candidate in \
    /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
    /opt/homebrew/opt/openjdk@21 \
    /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk@21; do
    if [[ -x "$candidate/bin/java" ]]; then
      JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

command -v java >/dev/null 2>&1 || fail "Java 21 est requis. Installe un JDK 21 puis relance ./deploy.sh."
JAVA_VERSION="$(java -version 2>&1 | awk -F '\"' '/version/ {print $2; exit}')"
JAVA_MAJOR="${JAVA_VERSION%%.*}"
[[ "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || fail "Version Java illisible : ${JAVA_VERSION:-inconnue}."
[[ "$JAVA_MAJOR" == 21 ]] || fail "Java 21 est requis pour compiler ce projet avec Lombok (version detectee : ${JAVA_VERSION:-inconnue}). Installe/configure un JDK 21."
printf 'JDK utilise : %s (%s)\n' "$JAVA_HOME" "$JAVA_VERSION"

[[ -x "$APP_DIR/mvnw" ]] || fail "Wrapper Maven introuvable : $APP_DIR/mvnw"

# Tente de lancer PostgreSQL sans toucher aux fichiers de donnees.
if ! command -v pg_isready >/dev/null 2>&1 || ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -q; then
  if command -v brew >/dev/null 2>&1; then
    POSTGRES_FORMULA=""
    for formula in postgresql@18 postgresql@17 postgresql@16 postgresql@15 postgresql@14 postgresql@13 postgresql; do
      if [[ -x "/opt/homebrew/opt/$formula/bin/pg_isready" || -x "/usr/local/opt/$formula/bin/pg_isready" ]]; then
        POSTGRES_FORMULA="$formula"
        break
      fi
    done
    if [[ -n "$POSTGRES_FORMULA" ]]; then
      brew services start "$POSTGRES_FORMULA" >/dev/null 2>&1 || true
    fi
  fi
  for _ in {1..15}; do
    command -v pg_isready >/dev/null 2>&1 && pg_isready -h "$DB_HOST" -p "$DB_PORT" -q && break
    sleep 2
  done
fi
command -v pg_isready >/dev/null 2>&1 || fail "Client PostgreSQL absent (pg_isready introuvable). Installe PostgreSQL et ajoute ses outils au PATH."
pg_isready -h "$DB_HOST" -p "$DB_PORT" -q || fail "PostgreSQL ne repond pas sur $DB_HOST:$DB_PORT. Demarre le service PostgreSQL puis relance."

# Cree la base seulement si elle n'existe pas. Aucun schema ou donnees existants ne sont effaces.
export PGPASSWORD="$DB_PASSWORD"
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -tAc \
  "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" 2>/dev/null | grep -q 1; then
  createdb -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$DB_NAME" \
    || fail "Impossible de creer/verifier la base '$DB_NAME'. Verifie DB_USERNAME, DB_PASSWORD et les droits PostgreSQL."
fi

export DB_URL="${DB_URL:-jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME}"
export DB_USERNAME="$DB_USER"
export DB_PASSWORD

printf 'Demarrage de GestionRH sur http://localhost:8080\n'
cd "$APP_DIR"
exec ./mvnw spring-boot:run
