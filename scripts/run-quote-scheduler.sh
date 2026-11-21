#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# SCHEDULER DE COTIZACIONES DE MERCADO (4 RECÁLCULOS OBLIGATORIOS POR DÍA)
# =============================================================================
# Garantiza 4 recálculos obligatorios al día (cada 6 horas: 00:00, 06:00, 12:00, 18:00).
# Si la computadora estuvo apagada, en suspensión o el backend no estaba levantado,
# el script detecta las ventanas perdidas y ejecuta los recálculos pendientes
# en cuanto el backend esté disponible ("catch-up" garantizado).
#
# Uso como demonio en segundo plano (vigilancia continua):
#   ./scripts/run-quote-scheduler.sh &
#
# Uso para cron o verificación puntual (ejecuta solo si hay pendientes y sale):
#   ./scripts/run-quote-scheduler.sh --once
#
# Modo rápido / demo (para pruebas o demostración en vivo):
#   QUOTE_INTERVAL_SECONDS=30 ./scripts/run-quote-scheduler.sh
# =============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
LOG_FILE="${QUOTE_LOG_FILE:-$SCRIPT_DIR/quote-scheduler.log}"
RECALCULATE_SCRIPT="$SCRIPT_DIR/recalculate-quotes.sh"
ONCE_MODE=false

if [[ "${1:-}" == "--once" || "${QUOTE_ONCE:-}" == "true" ]]; then
  ONCE_MODE=true
fi

# Redirigir stdout y stderr al archivo de log y a la consola
exec > >(tee -a "$LOG_FILE") 2>&1

if [[ ! -x "$RECALCULATE_SCRIPT" ]]; then
  echo "Error: No se encontró o no tiene permisos de ejecución: $RECALCULATE_SCRIPT" >&2
  exit 1
fi

check_backend_ready() {
  local status
  status="$(curl -sS -o /dev/null -w '%{http_code}' "$API_BASE_URL/actuator/health" 2>/dev/null || echo "000")"
  [[ "$status" == "200" ]]
}

get_login_token() {
  local email="${QUOTE_OPERATOR_EMAIL:-quote.automation@example.test}"
  local password="${QUOTE_OPERATOR_PASSWORD:-QuoteScheduler2026!}"
  local username="${QUOTE_OPERATOR_USERNAME:-quote_scheduler}"

  # Asegurar registro
  curl -s -X POST "$API_BASE_URL/api/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$username\",\"email\":\"$email\",\"password\":\"$password\"}" >/dev/null 2>&1 || true

  # Obtener token
  curl -s -X POST "$API_BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" | jq -r '.token // empty'
}

count_executions_today() {
  local today="$1"
  local token="$2"
  local response

  response="$(curl -s -X GET "$API_BASE_URL/api/quotes/executions?date=$today" \
    -H "Authorization: Bearer $token" \
    -H "Accept: application/json" 2>/dev/null || echo '{"executions":[]}')"

  echo "$response" | jq -r '.executions | length // 0'
}

calculate_expected_runs_for_hour() {
  local hour="$1" # 00..23 (formato decimal)
  # 4 franjas al día:
  # Franja 1 (00:00 - 05:59): al menos 1 corrida
  # Franja 2 (06:00 - 11:59): al menos 2 corridas
  # Franja 3 (12:00 - 17:59): al menos 3 corridas
  # Franja 4 (18:00 - 23:59): al menos 4 corridas
  if (( 10#$hour < 6 )); then
    echo 1
  elif (( 10#$hour < 12 )); then
    echo 2
  elif (( 10#$hour < 18 )); then
    echo 3
  else
    echo 4
  fi
}

echo "=============================================================================="
echo "[$(date --iso-8601=seconds)] Iniciando Scheduler de Cotizaciones (4 al día)"
echo "  - Backend URL: $API_BASE_URL"
echo "  - Log: $LOG_FILE"
echo "  - Estrategia: 4 franjas diarias (00:00, 06:00, 12:00, 18:00) con recuperación"
echo "=============================================================================="

# Modo por intervalo directo si se especifica QUOTE_INTERVAL_SECONDS
if [[ -n "${QUOTE_INTERVAL_SECONDS:-}" ]]; then
  echo "[$(date --iso-8601=seconds)] Modo demostración por intervalo activado: cada $QUOTE_INTERVAL_SECONDS segundos."
  while true; do
    if check_backend_ready; then
      echo "[$(date --iso-8601=seconds)] Ejecutando recálculo por intervalo..."
      "$RECALCULATE_SCRIPT" || echo "[$(date --iso-8601=seconds)] Falló el recálculo; se reintentará en el próximo ciclo." >&2
    else
      echo "[$(date --iso-8601=seconds)] Backend no disponible. Esperando al próximo ciclo..."
    fi
    sleep "$QUOTE_INTERVAL_SECONDS"
  done
fi

# Modo normal: 4 recálculos obligatorios al día con recuperación automática
LAST_CHECKED_MINUTE=""

while true; do
  CURRENT_MINUTE="$(date +%Y-%m-%d-%H-%M)"

  if [[ "$CURRENT_MINUTE" != "$LAST_CHECKED_MINUTE" ]]; then
    LAST_CHECKED_MINUTE="$CURRENT_MINUTE"

    # 1. Comprobar disponibilidad del backend
    if ! check_backend_ready; then
      if [[ "$ONCE_MODE" == "true" ]]; then
        echo "[$(date --iso-8601=seconds)] Backend no disponible en $API_BASE_URL. Omitiendo verificación puntual."
        exit 0
      fi
      # No inundar el log: solo un aviso cada 10 minutos si está caído
      if [[ "$(date +%M)" =~ ^[0-5]?0$ ]]; then
        echo "[$(date --iso-8601=seconds)] [ESPERA] Backend no disponible en $API_BASE_URL. El scheduler reintentará cuando el backend esté levantado."
      fi
      sleep 30
      continue
    fi

    # 2. Determinar fecha y hora actual
    TODAY_DATE="$(date +%Y-%m-%d)"
    CURRENT_HOUR="$(date +%H)"
    EXPECTED_RUNS="$(calculate_expected_runs_for_hour "$CURRENT_HOUR")"

    # 3. Consultar ejecuciones reales realizadas hoy
    TOKEN="$(get_login_token)"
    if [[ -z "$TOKEN" ]]; then
      echo "[$(date --iso-8601=seconds)] [ALERTA] No se pudo autenticar para consultar el historial de ejecuciones." >&2
      sleep 30
      continue
    fi

    ACTUAL_RUNS="$(count_executions_today "$TODAY_DATE" "$TOKEN")"

    # 4. ¿Faltan recálculos obligatorios para el momento del día actual?
    if (( ACTUAL_RUNS < EXPECTED_RUNS )); then
      MISSING_RUNS=$(( EXPECTED_RUNS - ACTUAL_RUNS ))
      echo "[$(date --iso-8601=seconds)] [ATENCIÓN] Recálculos obligatorios pendientes para hoy ($TODAY_DATE):"
      echo "  - Hora actual: $(date +%H:%M)"
      echo "  - Objetivo acumulado a esta hora: $EXPECTED_RUNS corridas"
      echo "  - Ejecutadas hasta ahora: $ACTUAL_RUNS corridas"
      echo "  - Corridas a recuperar: $MISSING_RUNS"

      for (( i = 1; i <= MISSING_RUNS; i++ )); do
        CURRENT_STEP=$(( ACTUAL_RUNS + i ))
        echo "[$(date --iso-8601=seconds)] [RECUPERACIÓN] Ejecutando recálculo obligatorio $CURRENT_STEP de $EXPECTED_RUNS..."
        if "$RECALCULATE_SCRIPT"; then
          echo "[$(date --iso-8601=seconds)] [ÉXITO] Recálculo $CURRENT_STEP registrado correctamente."
        else
          echo "[$(date --iso-8601=seconds)] [ERROR] Falló la ejecución del recálculo $CURRENT_STEP." >&2
          break
        fi

        # Si hay más de una pendiente, esperar 5 segundos entre ejecuciones para que los precios escalen ordenadamente
        if (( i < MISSING_RUNS )); then
          sleep 5
        fi
      done
    else
      if [[ "$ONCE_MODE" == "true" ]]; then
        echo "[$(date --iso-8601=seconds)] [AL DÍA] Hay $ACTUAL_RUNS de $EXPECTED_RUNS recálculos previstos para esta franja. Nada pendiente."
      fi
    fi

    if [[ "$ONCE_MODE" == "true" ]]; then
      exit 0
    fi
  fi

  # Dormir en intervalos de 30 segundos para detectar despertares de suspensión inmediatamente
  sleep 30
done
