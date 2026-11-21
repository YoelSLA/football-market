#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# ESTADO GENERAL Y DIAGNÓSTICO DEL MERCADO DE FÚTBOL
# =============================================================================
# Inspecciona el estado de la infraestructura, cotizaciones, mercado P2P,
# transacciones, schedulers y generadores automáticos en un vistazo.
#
# Uso:
#   ./scripts/market-status.sh
# =============================================================================

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-football-market-postgres}"
DB_NAME="${DB_NAME:-football_market_dev}"
DB_USER="${DB_USER:-postgres}"

psql_cmd() {
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1" 2>/dev/null || echo "N/A"
}

format_currency() {
  local cents="$1"
  if [[ "$cents" =~ ^[0-9]+$ ]]; then
    awk -v c="$cents" 'BEGIN { printf "₵%.2f", c/100 }'
  else
    echo "₵0.00"
  fi
}

echo "=============================================================================="
echo "          📊 ESTADO GENERAL DEL MERCADO (FOOTBALL MARKET DASHBOARD)           "
echo "                      $(date '+%Y-%m-%d %H:%M:%S %Z')                         "
echo "=============================================================================="

# 1. INFRAESTRUCTURA Y SERVICIOS
echo
echo "🖥️  SERVICIOS E INFRAESTRUCTURA:"
if docker ps --filter "name=$DB_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$DB_CONTAINER"; then
  echo "  ✔ PostgreSQL ($DB_CONTAINER): ACTIVO"
else
  echo "  ✖ PostgreSQL ($DB_CONTAINER): DETENIDO O NO ENCONTRADO"
fi

BACKEND_HEALTH="$(curl -s -o /dev/null -w '%{http_code}' "$API_BASE_URL/actuator/health" 2>/dev/null || echo "000")"
if [[ "$BACKEND_HEALTH" == "200" ]]; then
  echo "  ✔ Backend API ($API_BASE_URL): ONLINE"
else
  echo "  ✖ Backend API ($API_BASE_URL): OFFLINE (HTTP $BACKEND_HEALTH)"
fi

FRONTEND_STATUS="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:5173" 2>/dev/null || echo "000")"
if [[ "$FRONTEND_STATUS" == "200" ]]; then
  echo "  ✔ Frontend Vite (http://localhost:5173): ONLINE"
else
  echo "  ○ Frontend Vite (http://localhost:5173): NO DETECTADO O NO LEVANTADO"
fi

# 2. COTIZACIONES Y RECÁLCULO
echo
echo "📈 COTIZACIONES DE JUGADORES:"
LAST_EXECUTION_INFO="$(psql_cmd "
SELECT id || '|' || quote_date || '|' || strategy_name || '|' || adjustment_factor || '|' || variation_cap || '|' || priced_players || '|' || started_at
FROM quote_executions
ORDER BY started_at DESC
LIMIT 1;
")"

if [[ "$LAST_EXECUTION_INFO" != "N/A" && -n "$LAST_EXECUTION_INFO" ]]; then
  IFS='|' read -r exec_id exec_date exec_strat exec_fact exec_cap exec_players exec_started <<<"$LAST_EXECUTION_INFO"
  echo "  - Último recálculo: Ejecución #$exec_id ($exec_date a las $(echo "$exec_started" | cut -d' ' -f2 | cut -d'.' -f1) UTC)"
  echo "  - Estrategia: $exec_strat | Factor: $exec_fact | Cap: $exec_cap"
  echo "  - Jugadores cotizados: $exec_players"
else
  echo "  - No hay ejecuciones de recálculo registradas."
fi

TODAY_RUNS="$(psql_cmd "SELECT COUNT(*) FROM quote_executions WHERE quote_date = CURRENT_DATE;")"
echo "  - Recálculos ejecutados hoy: $TODAY_RUNS de 4 obligatorios"

# 3. MERCADO P2P Y PUBLICACIONES
echo
echo "⚽ MERCADO P2P:"
ACTIVE_LISTINGS="$(psql_cmd "SELECT COUNT(*) FROM token_listings WHERE status = 'ACTIVE' AND expires_at > NOW();")"
SOLD_LISTINGS="$(psql_cmd "SELECT COUNT(*) FROM token_listings WHERE status = 'SOLD';")"
EXPIRED_LISTINGS="$(psql_cmd "SELECT COUNT(*) FROM token_listings WHERE status = 'CANCELLED' OR (status = 'ACTIVE' AND expires_at <= NOW());")"
echo "  - Publicaciones Activas vigentes: $ACTIVE_LISTINGS"
echo "  - Publicaciones Vendidas (Trades): $SOLD_LISTINGS"
echo "  - Publicaciones Canceladas / Expiradas: $EXPIRED_LISTINGS"

# 4. VOLUMEN Y ACTIVIDAD (ÚLTIMAS 24 HORAS)
echo
echo "💰 ACTIVIDAD Y VOLUMEN (24 HORAS):"
TOTAL_TX_24H="$(psql_cmd "SELECT COUNT(*) FROM market_transactions WHERE executed_at >= NOW() - INTERVAL '24 hours';")"
TOTAL_VOL_CENTS="$(psql_cmd "SELECT COALESCE(SUM(total_amount), 0) FROM market_transactions WHERE executed_at >= NOW() - INTERVAL '24 hours';")"
ALL_TIME_TX="$(psql_cmd "SELECT COUNT(*) FROM market_transactions;")"
echo "  - Transacciones últimas 24h: $TOTAL_TX_24H (Histórico total: $ALL_TIME_TX)"
echo "  - Volumen negociado 24h: $(format_currency "$TOTAL_VOL_CENTS")"

# 5. MARKET MOVERS (TOP GANADORES Y PERDEDORES)
echo
echo "🏆 MARKET MOVERS (TOP GANADORES DE HOY):"
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "
WITH latest_quotes AS (
  SELECT DISTINCT ON (q.player_id) q.*
  FROM player_quotes q
  ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
)
SELECT '    ' || p.name || ': ₵' || ROUND(q.price / 100.0, 2) || ' (+' || ROUND(((q.price - q.previous_price)::numeric / q.previous_price::numeric) * 100, 2) || '%)'
FROM latest_quotes q
JOIN players p ON p.id = q.player_id
WHERE q.previous_price IS NOT NULL AND q.price > q.previous_price
ORDER BY ((q.price - q.previous_price)::numeric / q.previous_price::numeric) DESC
LIMIT 3;
" 2>/dev/null || echo "    Sin datos suficientes"

echo "📉 MARKET MOVERS (TOP PERDEDORES DE HOY):"
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "
WITH latest_quotes AS (
  SELECT DISTINCT ON (q.player_id) q.*
  FROM player_quotes q
  ORDER BY q.player_id, q.effective_date DESC, q.calculated_at DESC, q.id DESC
)
SELECT '    ' || p.name || ': ₵' || ROUND(q.price / 100.0, 2) || ' (' || ROUND(((q.price - q.previous_price)::numeric / q.previous_price::numeric) * 100, 2) || '%)'
FROM latest_quotes q
JOIN players p ON p.id = q.player_id
WHERE q.previous_price IS NOT NULL AND q.price < q.previous_price
ORDER BY ((q.price - q.previous_price)::numeric / q.previous_price::numeric) ASC
LIMIT 3;
" 2>/dev/null || echo "    Sin datos suficientes"

# 6. SCHEDULERS Y CRON
echo
echo "⏱️  AUTOMATIZACIONES Y TAREAS PROGRAMADAS:"
echo "  - Crontab de usuario:"
crontab -l 2>/dev/null | grep -E "football-market" | while read -r cron_line; do
  echo "      $cron_line"
done || echo "      (Sin tareas en crontab)"

echo "  - Procesos en segundo plano activos:"
RUNNING_SCRIPTS="$(ps aux | grep -E "run-quote-scheduler|run-p2p|seed-p2p" | grep -v grep || true)"
if [[ -n "$RUNNING_SCRIPTS" ]]; then
  echo "$RUNNING_SCRIPTS" | awk '{print "      PID " $2 ": " $11 " " $12}'
else
  echo "      (No hay procesos de generación en bucle continuo; funcionando vía cron cada 15m)"
fi

echo "=============================================================================="
