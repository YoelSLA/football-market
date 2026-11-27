#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# RECÁLCULO DE COTIZACIONES DE MERCADO
# =============================================================================
# Ejecuta una corrida de recálculo sobre el backend vía API REST con JWT.
#
# Modos de uso:
#   ./scripts/recalculate-quotes.sh                  # Recálculo habitual híbrido (Deportivo + Demanda)
#   ./scripts/recalculate-quotes.sh --market-demand  # Recálculo exclusivo por demanda de mercado (SPEC-028)
#
# Con parámetros personalizados:
#   QUOTE_ADJUSTMENT_FACTOR=0.25 QUOTE_VARIATION_CAP=0.15 ./scripts/recalculate-quotes.sh
# =============================================================================

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
RECALC_MODE="hybrid" # hybrid | market_demand

while [[ $# -gt 0 ]]; do
  case "$1" in
    --market-demand|--demand)
      RECALC_MODE="market_demand"
      shift
      ;;
    --hybrid)
      RECALC_MODE="hybrid"
      shift
      ;;
    -h|--help)
      echo "Uso: $0 [--hybrid | --market-demand]"
      exit 0
      ;;
    *)
      shift
      ;;
  esac
done

if [[ "$RECALC_MODE" == "market_demand" ]]; then
  # Para el endpoint exclusivo de demanda (SPEC-028) se requiere rol SUPERUSER
  OPERATOR_EMAIL="${SUPERUSER_EMAIL:-market.owner@example.com}"
  OPERATOR_PASSWORD="${SUPERUSER_PASSWORD:-passsword123}"
  OPERATOR_USERNAME="${SUPERUSER_USERNAME:-market.owner}"
else
  OPERATOR_EMAIL="${QUOTE_OPERATOR_EMAIL:-quote.automation@example.test}"
  OPERATOR_PASSWORD="${QUOTE_OPERATOR_PASSWORD:-QuoteScheduler2026!}"
  OPERATOR_USERNAME="${QUOTE_OPERATOR_USERNAME:-quote_scheduler}"
fi
RANDOMIZE_PARAMS="${QUOTE_RANDOMIZE_PARAMS:-true}"
MIN_PRICE_FLOOR="${QUOTE_MIN_PRICE_FLOOR:-1}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-0}"

# Selección de parámetros aleatorios de mercado (o manuales si se especifican):
if [[ "$RANDOMIZE_PARAMS" == "true" && -z "${QUOTE_ADJUSTMENT_FACTOR:-}" && -z "${QUOTE_VARIATION_CAP:-}" ]]; then
  # 3 Climas bursátiles simulados:
  # - Calmo (30%): baja volatilidad, variaciones suaves (factor ~0.12, cap ~0.08)
  # - Moderado (45%): volatilidad estándar, mercado activo (factor ~0.22, cap ~0.15)
  # - Volátil (25%): alta volatilidad, genera grandes movimientos para tickers/dashboard (factor ~0.38, cap ~0.24)
  MOOD_ROLL=$(( RANDOM % 100 ))
  if (( MOOD_ROLL < 30 )); then
    MARKET_MOOD="CALMO (Baja volatilidad)"
    BASE_FACTOR=12
    BASE_CAP=8
  elif (( MOOD_ROLL < 75 )); then
    MARKET_MOOD="MODERADO (Volatilidad estándar)"
    BASE_FACTOR=22
    BASE_CAP=15
  else
    MARKET_MOOD="VOLÁTIL (Rally / Rueda activa)"
    BASE_FACTOR=38
    BASE_CAP=24
  fi

  # Jitter aleatorio de +/- 2 puntos porcentuales para variación natural
  JITTER=$(( (RANDOM % 5) - 2 ))
  CALC_FACTOR=$(( BASE_FACTOR + JITTER ))
  if (( CALC_FACTOR < 10 )); then CALC_FACTOR=10; fi
  if (( CALC_FACTOR > 50 )); then CALC_FACTOR=50; fi

  CALC_CAP=$(( BASE_CAP + JITTER ))
  if (( CALC_CAP < 5 )); then CALC_CAP=5; fi
  if (( CALC_CAP > 30 )); then CALC_CAP=30; fi

  ADJUSTMENT_FACTOR="$(printf "0.%02d" "$CALC_FACTOR")"
  VARIATION_CAP="$(printf "0.%02d" "$CALC_CAP")"

  # Estrategia: 85% POSITIONAL_PERFORMANCE, 15% GENERAL_ATTRIBUTES (si no está fijada)
  if [[ -z "${QUOTE_STRATEGY:-}" ]]; then
    if (( (RANDOM % 100) < 15 )); then
      STRATEGY="GENERAL_ATTRIBUTES"
    else
      STRATEGY="POSITIONAL_PERFORMANCE"
    fi
  else
    STRATEGY="$QUOTE_STRATEGY"
  fi
else
  STRATEGY="${QUOTE_STRATEGY:-POSITIONAL_PERFORMANCE}"
  ADJUSTMENT_FACTOR="${QUOTE_ADJUSTMENT_FACTOR:-0.20}"
  VARIATION_CAP="${QUOTE_VARIATION_CAP:-0.15}"
  MARKET_MOOD="MANUAL (Parámetros fijados por entorno)"
fi

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

check_backend() {
  request GET "$API_BASE_URL/actuator/health"
  [[ "$HTTP_STATUS" == "200" ]]
}

if ! check_backend; then
  if (( WAIT_TIMEOUT_SECONDS > 0 )); then
    echo "[$(date --iso-8601=seconds)] Backend no disponible en $API_BASE_URL. Esperando hasta $WAIT_TIMEOUT_SECONDS segundos..."
    ELAPSED=0
    while (( ELAPSED < WAIT_TIMEOUT_SECONDS )); do
      sleep 5
      ELAPSED=$((ELAPSED + 5))
      if check_backend; then
        echo "[$(date --iso-8601=seconds)] Backend disponible tras ${ELAPSED}s."
        break
      fi
    done
    if ! check_backend; then
      echo "Error: El backend no estuvo listo tras $WAIT_TIMEOUT_SECONDS segundos." >&2
      exit 1
    fi
  else
    echo "Error: No se pudo conectar al backend en $API_BASE_URL (HTTP $HTTP_STATUS)." >&2
    exit 1
  fi
fi

# Autenticación del operador de automatización
register_body="$(jq -n \
  --arg username "$OPERATOR_USERNAME" \
  --arg email "$OPERATOR_EMAIL" \
  --arg password "$OPERATOR_PASSWORD" \
  '{username: $username, email: $email, password: $password}')"

request POST "$API_BASE_URL/api/auth/register" '' "$register_body"
if [[ "$HTTP_STATUS" != "201" && "$HTTP_STATUS" != "409" ]]; then
  echo "Error durante el registro del operador (HTTP $HTTP_STATUS):" >&2
  cat "$RESPONSE_BODY" >&2
  exit 1
fi

login_body="$(jq -n \
  --arg email "$OPERATOR_EMAIL" \
  --arg password "$OPERATOR_PASSWORD" \
  '{email: $email, password: $password}')"

request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Error durante el inicio de sesión del operador (HTTP $HTTP_STATUS):" >&2
  cat "$RESPONSE_BODY" >&2
  exit 1
fi

TOKEN="$(jq -er '.token' "$RESPONSE_BODY")"

# Ejecutar recálculo
if [[ "$RECALC_MODE" == "market_demand" ]]; then
  echo "[$(date --iso-8601=seconds)] Modo: DEMANDA PURA DE MERCADO (SPEC-028)"
  echo "[$(date --iso-8601=seconds)] Disparando recálculo de cotizaciones sobre todos los jugadores según demanda neta de 24h..."
  request POST "$API_BASE_URL/api/quotes/recalculate/market-demand" "$TOKEN"
else
  RECALCULATE_BODY="$(jq -n \
    --arg strategy "$STRATEGY" \
    --argjson adjustmentFactor "$ADJUSTMENT_FACTOR" \
    --argjson variationCap "$VARIATION_CAP" \
    --argjson minPriceFloor "$MIN_PRICE_FLOOR" \
    '{strategy: $strategy, adjustmentFactor: $adjustmentFactor, variationCap: $variationCap, minPriceFloor: $minPriceFloor}')"

  echo "[$(date --iso-8601=seconds)] Clima bursátil: $MARKET_MOOD"
  echo "[$(date --iso-8601=seconds)] Disparando recálculo híbrido (deporte + demanda) (estrategia=$STRATEGY, factor=$ADJUSTMENT_FACTOR, cap=$VARIATION_CAP)..."
  request POST "$API_BASE_URL/api/quotes/recalculate" "$TOKEN" "$RECALCULATE_BODY"
fi

if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Error: Falló el recálculo de cotizaciones (HTTP $HTTP_STATUS):" >&2
  cat "$RESPONSE_BODY" >&2
  exit 1
fi

EXECUTION_ID="$(jq -r '.executionId // empty' "$RESPONSE_BODY")"
QUOTE_DATE="$(jq -r '.quoteDate // empty' "$RESPONSE_BODY")"
RECALC_TYPE="$(jq -r '.recalculationType // "SPORT_AND_MARKET"' "$RESPONSE_BODY")"
PRICED_PLAYERS="$(jq -r '.pricedPlayers // 0' "$RESPONSE_BODY")"
RECOMPUTED_ROWS="$(jq -r '.recomputedRows // 0' "$RESPONSE_BODY")"
CARRIED_FORWARD="$(jq -r '.carriedForwardRows // 0' "$RESPONSE_BODY")"
FAILED_PLAYERS="$(jq -r '.failedPlayers // 0' "$RESPONSE_BODY")"
STARTED_AT="$(jq -r '.startedAt // empty' "$RESPONSE_BODY")"
FINISHED_AT="$(jq -r '.finishedAt // empty' "$RESPONSE_BODY")"

echo "[$(date --iso-8601=seconds)] Recálculo finalizado exitosamente:"
echo "  - ID Ejecución: $EXECUTION_ID ($RECALC_TYPE)"
echo "  - Fecha de cotización: $QUOTE_DATE"
echo "  - Jugadores cotizados: $PRICED_PLAYERS (recalculados: $RECOMPUTED_ROWS, arrastrados: $CARRIED_FORWARD, fallidos: $FAILED_PLAYERS)"
echo "  - Inicio: $STARTED_AT | Fin: $FINISHED_AT"
