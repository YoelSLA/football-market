#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# SINCRONIZACIÓN DE IMÁGENES DE JUGADORES
# =============================================================================
# Ejecuta la sincronización de imágenes contra TheSportsDB vía API REST con JWT.
#
# Modos de uso:
#   1. Por plantel de equipo (rápido y masivo, ~4 min total):
#      ./scripts/sync-images.sh --roster
#
#   2. Por plantel de un equipo específico:
#      ./scripts/sync-images.sh --roster --team 1
#
#   3. Individual solo para jugadores que faltan:
#      ./scripts/sync-images.sh --missing-only
#
#   4. Individual con límite de jugadores:
#      ./scripts/sync-images.sh --missing-only --limit 20
# =============================================================================

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
OPERATOR_EMAIL="${QUOTE_OPERATOR_EMAIL:-market.owner@example.com}"
OPERATOR_PASSWORD="${QUOTE_OPERATOR_PASSWORD:-passsword123}"

ROSTER_MODE=false
FORCE=false
MISSING_ONLY=false
LIMIT=""
TEAM_ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --roster)
      ROSTER_MODE=true
      shift
      ;;
    --force)
      FORCE=true
      shift
      ;;
    --missing-only)
      MISSING_ONLY=true
      shift
      ;;
    --limit)
      LIMIT="$2"
      shift 2
      ;;
    --team)
      TEAM_ID="$2"
      shift 2
      ;;
    *)
      echo "Parámetro desconocido: $1" >&2
      exit 1
      ;;
  esac
done

for dependency in curl jq; do
  if ! command -v "$dependency" >/dev/null 2>&1; then
    echo "Error: $dependency es obligatorio para ejecutar este script." >&2
    exit 1
  fi
done

RESPONSE_BODY="$(mktemp)"
trap 'rm -f "$RESPONSE_BODY"' EXIT

request() {
  local method="$1"
  local url="$2"
  local token="${3:-}"
  local body="${4:-}"
  local args=(-sS -o "$RESPONSE_BODY" -w '%{http_code}' -X "$method" "$url")

  [[ -z "$token" ]] || args+=(-H "Authorization: Bearer $token")
  [[ -z "$body" ]] || args+=(-H 'Content-Type: application/json' -d "$body")
  HTTP_STATUS="$(curl "${args[@]}" 2>/dev/null || echo "000")"
}

echo "=================================================="
echo " Sincronización de Imágenes de Jugadores"
echo "=================================================="

# 1. Comprobar backend
request GET "$API_BASE_URL/actuator/health"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Error: Backend no disponible en $API_BASE_URL (HTTP $HTTP_STATUS)." >&2
  exit 1
fi

# 2. Autenticación
echo "-> Autenticando con $OPERATOR_EMAIL..."
login_body="$(jq -n --arg email "$OPERATOR_EMAIL" --arg password "$OPERATOR_PASSWORD" \
  '{email: $email, password: $password}')"

request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Error al iniciar sesión (HTTP $HTTP_STATUS):" >&2
  cat "$RESPONSE_BODY" >&2
  exit 1
fi

TOKEN="$(jq -er '.token' "$RESPONSE_BODY")"

# 3. Construir URL de sincronización
if [[ "$ROSTER_MODE" == "true" ]]; then
  SYNC_URL="$API_BASE_URL/api/players/images/sync/roster?force=$FORCE"
  if [[ -n "$TEAM_ID" ]]; then
    SYNC_URL="${SYNC_URL}&teamId=$TEAM_ID"
    echo "-> Modo: Plantel completo de equipo (teamId=$TEAM_ID)"
  else
    echo "-> Modo: Plantel completo de todos los equipos vigentes"
  fi
else
  SYNC_URL="$API_BASE_URL/api/players/images/sync?force=$FORCE"
  [[ "$MISSING_ONLY" == "true" ]] && SYNC_URL="${SYNC_URL}&missingOnly=true"
  [[ -n "$LIMIT" ]] && SYNC_URL="${SYNC_URL}&limit=$LIMIT"
  [[ -n "$TEAM_ID" ]] && SYNC_URL="${SYNC_URL}&teamId=$TEAM_ID"
  echo "-> Modo: Individual (missingOnly=$MISSING_ONLY, limit=${LIMIT:-sin límite}, teamId=${TEAM_ID:-todos})"
fi

echo "-> Iniciando sincronización contra TheSportsDB..."
START_TIME=$(date +%s)
request POST "$SYNC_URL" "$TOKEN"

if [[ "$HTTP_STATUS" == "200" ]]; then
  END_TIME=$(date +%s)
  DURATION=$(( END_TIME - START_TIME ))
  echo "=================================================="
  echo "✅ Sincronización completada exitosamente en ${DURATION}s"
  echo "=================================================="
  echo "Estado: $(jq -r '.status' "$RESPONSE_BODY")"
  echo "Run ID: $(jq -r '.id' "$RESPONSE_BODY")"
  echo ""
  echo "Resultados:"
  jq -r '.counters | " - Evaluados: \(.evaluated)\n - Procesados: \(.processed)\n - Encontrados: \(.found)\n - No encontrados: \(.notFound)\n - Errores reintentables: \(.retryableErrors)\n - Fallidos: \(.failed)\n - Conflictos: \(.conflicts)\n - Omitidos: \(.skippedFound + .skippedRetryWindow + .skippedFailed)"' "$RESPONSE_BODY"
elif [[ "$HTTP_STATUS" == "404" ]]; then
  echo "Error 404: El endpoint no fue encontrado en el backend." >&2
  echo "Asegurate de haber reiniciado el backend tras compilar los cambios." >&2
  exit 1
elif [[ "$HTTP_STATUS" == "409" ]]; then
  echo "Error 409: Ya hay una sincronización activa en progreso." >&2
  exit 1
else
  echo "Error durante la sincronización (HTTP $HTTP_STATUS):" >&2
  cat "$RESPONSE_BODY" >&2
  exit 1
fi
