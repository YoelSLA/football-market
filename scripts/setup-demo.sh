#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# INICIALIZACIÓN "ONE-CLICK" DEL ENTORNO DEMO (FOOTBALL MARKET)
# =============================================================================
# Prepara y deja la aplicación 100% lista para una presentación o demo en vivo:
#   1. Verifica la infraestructura (PostgreSQL, Backend API).
#   2. Inicializa el mercado si está vacío.
#   3. Corre un recálculo de cotizaciones con clima bursátil dinámico.
#   4. Genera ofertas P2P variadas con cantidades aleatorias.
#   5. Ejecuta compras simuladas regulares para encender el volumen y el feed.
#   6. Fondea el portfolio del usuario principal (yoel@gmail.com).
#   7. Imprime el reporte de estado completo del mercado.
#
# Uso:
#   ./scripts/setup-demo.sh
# =============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-football-market-postgres}"

echo "=============================================================================="
echo "          🚀 INICIALIZANDO ENTORNO COMPLETO PARA DEMO EN VIVO                "
echo "=============================================================================="

# 1. Comprobar servicios
echo
echo "--- Paso 1: Verificando infraestructura básica ---"
if ! docker ps --filter "name=$DB_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$DB_CONTAINER"; then
  echo "Error: El contenedor $DB_CONTAINER no está corriendo. Iniciando con docker compose..."
  docker compose up -d postgres
  sleep 3
fi

BACKEND_UP=false
for i in {1..10}; do
  if [[ "$(curl -s -o /dev/null -w '%{http_code}' "$API_BASE_URL/actuator/health" 2>/dev/null || echo "000")" == "200" ]]; then
    BACKEND_UP=true
    break
  fi
  echo "Esperando que el backend esté listo ($i/10)..."
  sleep 3
done

if [[ "$BACKEND_UP" != "true" ]]; then
  echo "Error: El backend en $API_BASE_URL no responde. Inícialo con ./gradlew bootRun antes de ejecutar este script." >&2
  exit 1
fi
echo "✔ Infraestructura lista (PostgreSQL + Backend Spring Boot)."

# 2. Inicialización de cotizaciones de mercado (si hiciera falta)
echo
echo "--- Paso 2: Verificando inicialización del mercado ---"
QUOTES_COUNT="$(docker exec -i "$DB_CONTAINER" psql -U postgres -d football_market_dev -t -A -c "SELECT COUNT(*) FROM player_quotes;" 2>/dev/null || echo 0)"
if (( QUOTES_COUNT == 0 )); then
  echo "Mercado no inicializado. Registrando operador e inicializando cotizaciones..."
  TEMP_TOKEN="$(curl -s -X POST "$API_BASE_URL/api/auth/login" -H "Content-Type: application/json" -d '{"email":"quote.automation@example.test","password":"QuoteScheduler2026!"}' | jq -r '.token // empty')"
  curl -s -X POST "$API_BASE_URL/api/quotes/initialize" -H "Authorization: Bearer $TEMP_TOKEN" -H "Content-Type: application/json" -d '{}' >/dev/null
  echo "✔ Mercado inicializado exitosamente."
else
  echo "✔ Mercado ya inicializado ($QUOTES_COUNT cotizaciones en base de datos)."
fi

# 3. Recálculo dinámico de cotizaciones
echo
echo "--- Paso 3: Ejecutando recálculo dinámico de cotizaciones ---"
"$SCRIPT_DIR/recalculate-quotes.sh"

# 4. Siembra de ofertas en mercado P2P
echo
echo "--- Paso 4: Sembrando ofertas P2P con cantidades variadas ---"
P2P_SELLER_COUNT=8 P2P_PLAYER_POOL_SIZE=4 "$SCRIPT_DIR/seed-p2p-market.sh"

# 5. Simulación de compras y volumen
echo
echo "--- Paso 5: Simulando transacciones para encender el Live Feed y Volumen 24h ---"
P2P_TRADES_COUNT=5 OFFICIAL_TRADES_COUNT=2 "$SCRIPT_DIR/simulate-trades.sh"

# 6. Fondear el usuario principal para que su Dashboard esté completo
echo
echo "--- Paso 6: Fondeando el portfolio del usuario principal (yoel@gmail.com) ---"
"$SCRIPT_DIR/seed-user-portfolio.sh" "yoel@gmail.com" 25000

# 7. Diagnóstico final
echo
echo "--- Paso 7: Diagnóstico y resumen final ---"
"$SCRIPT_DIR/market-status.sh"

echo
echo "=============================================================================="
echo "✔ ENTORNO LISTO PARA DEMOSTRACIÓN. ¡Puedes abrir http://localhost:5173!"
echo "=============================================================================="
