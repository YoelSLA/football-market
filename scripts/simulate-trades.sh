#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# SIMULADOR DE COMPRAS Y TRANSACCIONES P2P / OFICIALES
# =============================================================================
# Simula compradores regulares (NO superusuario) que adquieren publicaciones
# activas del mercado P2P y ejecutan compras oficiales para generar:
#   1. Registros en market_transactions (alimenta Live Activity Feed).
#   2. Volumen de negociación real en las últimas 24 horas.
#   3. Rotación de estados en publicaciones (ACTIVE -> SOLD).
#
# Uso individual:
#   ./scripts/simulate-trades.sh
#
# Con parámetros personalizados:
#   P2P_TRADES_COUNT=8 OFFICIAL_TRADES_COUNT=3 ./scripts/simulate-trades.sh
# =============================================================================

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
P2P_TRADES_COUNT="${P2P_TRADES_COUNT:-4}"
OFFICIAL_TRADES_COUNT="${OFFICIAL_TRADES_COUNT:-2}"
TOTAL_SIM_TARGET="${TOTAL_SIM_TARGET:-10}"
# Variación aleatoria de ventas entre 40% y 60%
SELLS_PCT=$(( 40 + (RANDOM % 21) ))
OFFICIAL_SELLS_COUNT="${OFFICIAL_SELLS_COUNT:-$(( (TOTAL_SIM_TARGET * SELLS_PCT + 50) / 100 ))}"
BUYER_PASSWORD="${BUYER_PASSWORD:-TraderDemo2026!}"
LOG_FILE="${TRADE_LOG_FILE:-$(dirname -- "${BASH_SOURCE[0]}")/trades.log}"
COUNT_SIM_BUYS=0
COUNT_SIM_SELLS=0

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

# 1. Comprobar que el backend esté listo
request GET "$API_BASE_URL/actuator/health"
if [[ "$HTTP_STATUS" != "200" ]]; then
  echo "Error: El backend no responde en $API_BASE_URL (HTTP $HTTP_STATUS)." >&2
  exit 1
fi

# 2. Obtener o registrar un comprador regular con saldo
get_or_create_buyer_token() {
  local buyer_index="${1:-1}"
  local email="trader.buyer${buyer_index}@example.test"
  local username="trader_buyer_${buyer_index}"

  # Registrar si no existe (recibe 100 créditos = 10.000 centésimas)
  local reg_body
  reg_body="$(jq -n --arg u "$username" --arg e "$email" --arg p "$BUYER_PASSWORD" \
    '{username: $u, email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/register" '' "$reg_body"

  # Iniciar sesión
  local login_body
  login_body="$(jq -n --arg e "$email" --arg p "$BUYER_PASSWORD" \
    '{email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "Error al autenticar comprador $email (HTTP $HTTP_STATUS)" >&2
    exit 1
  fi

  jq -er '.token' "$RESPONSE_BODY"
}

CURRENT_BUYER_INDEX=$(( (RANDOM % 10) + 1 ))
BUYER_TOKEN="$(get_or_create_buyer_token "$CURRENT_BUYER_INDEX")"
BUYER_EMAIL="trader.buyer${CURRENT_BUYER_INDEX}@example.test"

echo "=============================================================================="
echo "[$(date --iso-8601=seconds)] Simulación de Operaciones de Mercado"
echo "  - Comprador regular: $BUYER_EMAIL (Usuario estándar, NO superusuario)"
echo "  - Objetivo compras: $P2P_TRADES_COUNT P2P + $OFFICIAL_TRADES_COUNT oficiales ($(( 100 - SELLS_PCT ))%)"
echo "  - Objetivo ventas:  $OFFICIAL_SELLS_COUNT liquidaciones oficiales a la casa ($SELLS_PCT%)"
echo "=============================================================================="

TOTAL_VOLUME_CENTS=0
SUCCESSFUL_TRADES=0

# 3. Compras en Mercado Secundario (P2P)
request GET "$API_BASE_URL/api/listings" "$BUYER_TOKEN"
if [[ "$HTTP_STATUS" == "200" ]]; then
  ACTIVE_LISTINGS="$(jq '[.listings[] | select(.status == "ACTIVE")]' "$RESPONSE_BODY")"
  TOTAL_ACTIVE="$(jq 'length' <<<"$ACTIVE_LISTINGS")"

  if (( TOTAL_ACTIVE > 0 )); then
    echo
    echo "--- Ejecutando compras secundarias P2P (Listings activos disponibles: $TOTAL_ACTIVE) ---"

    # Barajar y tomar hasta P2P_TRADES_COUNT listings
    SELECTED_LISTINGS="$(jq -c --argjson count "$P2P_TRADES_COUNT" 'sort_by(empty) | .[0:$count][]' <<<"$ACTIVE_LISTINGS" 2>/dev/null || jq -c '.[0:4][]' <<<"$ACTIVE_LISTINGS")"

    while IFS= read -r listing_json; do
      [[ -n "$listing_json" ]] || continue
      listing_id="$(jq -r '.listingId' <<<"$listing_json")"
      player_id="$(jq -r '.playerId' <<<"$listing_json")"
      quantity="$(jq -r '.quantity' <<<"$listing_json")"
      unit_price="$(jq -r '.unitPrice' <<<"$listing_json")"
      total_amount="$(jq -r '.totalAmount' <<<"$listing_json")"

      buy_body="$(jq -n --argjson id "$listing_id" '{listingId: $id}')"
      request POST "$API_BASE_URL/api/orders/buy" "$BUYER_TOKEN" "$buy_body"

      if [[ "$HTTP_STATUS" == "200" ]]; then
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        echo "  ✔ [P2P TRADE] Transacción #$tx_id: Listing #$listing_id comprado ($quantity token(s) jugador #$player_id a ₵$(awk -v p="$unit_price" 'BEGIN{printf "%.2f", p/100}') c/u | Total: ₵$(awk -v t="$total_amount" 'BEGIN{printf "%.2f", t/100}'))"
        TOTAL_VOLUME_CENTS=$(( TOTAL_VOLUME_CENTS + total_amount ))
        SUCCESSFUL_TRADES=$(( SUCCESSFUL_TRADES + 1 ))
        COUNT_SIM_BUYS=$(( COUNT_SIM_BUYS + 1 ))
      elif [[ "$HTTP_STATUS" == "400" ]] && grep -q "INSUFFICIENT" "$RESPONSE_BODY" 2>/dev/null; then
        echo "  [AVISO] Saldo insuficiente en $BUYER_EMAIL. Rotando a nuevo comprador..."
        CURRENT_BUYER_INDEX=$(( CURRENT_BUYER_INDEX + 1 ))
        BUYER_TOKEN="$(get_or_create_buyer_token "$CURRENT_BUYER_INDEX")"
        BUYER_EMAIL="trader.buyer${CURRENT_BUYER_INDEX}@example.test"
      else
        echo "  ✖ [FALLO P2P] Listing #$listing_id no pudo completarse (HTTP $HTTP_STATUS)"
      fi
    done <<<"$SELECTED_LISTINGS"
  else
    echo "  [INFO] No hay publicaciones P2P activas disponibles para comprar."
  fi
fi

# 4. Compras en Mercado Oficial (Emisión Primaria)
if (( OFFICIAL_TRADES_COUNT > 0 )); then
  request GET "$API_BASE_URL/api/market/official?page=0&size=10&hasQuote=true" "$BUYER_TOKEN"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    echo
    echo "--- Ejecutando compras primarias (Mercado Oficial) ---"
    PLAYERS="$(jq -c '.content[0:2][]' "$RESPONSE_BODY")"

    while IFS= read -r player_json; do
      [[ -n "$player_json" ]] || continue
      p_id="$(jq -r '.id' <<<"$player_json")"
      p_name="$(jq -r '.name' <<<"$player_json")"
      p_price="$(jq -r '.quote.price // 100' <<<"$player_json")"
      buy_qty=$(( (RANDOM % 2) + 1 ))

      official_buy_body="$(jq -n --argjson id "$p_id" --argjson q "$buy_qty" \
        '{playerId: $id, quantity: $q}')"
      request POST "$API_BASE_URL/api/orders/buy" "$BUYER_TOKEN" "$official_buy_body"

      if [[ "$HTTP_STATUS" == "200" ]]; then
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        item_total=$(( p_price * buy_qty ))
        echo "  ✔ [OFICIAL TRADE] Transacción #$tx_id: Comprados $buy_qty token(s) de $p_name al superusuario por ₵$(awk -v t="$item_total" 'BEGIN{printf "%.2f", t/100}')"
        TOTAL_VOLUME_CENTS=$(( TOTAL_VOLUME_CENTS + item_total ))
        SUCCESSFUL_TRADES=$(( SUCCESSFUL_TRADES + 1 ))
        COUNT_SIM_BUYS=$(( COUNT_SIM_BUYS + 1 ))
      fi
    done <<<"$PLAYERS"
  fi
fi

# 5. Liquidaciones Oficiales a la Casa (Ventas / Buyback con spread 10% - SPEC-026)
if (( OFFICIAL_SELLS_COUNT > 0 )); then
  echo
  echo "--- Ejecutando liquidaciones oficiales a la casa (Ventas / Buyback: meta $OFFICIAL_SELLS_COUNT) ---"
  sells_done=0
  for b_idx in $(seq 1 10 | shuf); do
    if (( sells_done >= OFFICIAL_SELLS_COUNT )); then break; fi
    trader_token="$(get_or_create_buyer_token "$b_idx")"
    trader_email="trader.buyer${b_idx}@example.test"
    request GET "$API_BASE_URL/api/portfolio/me" "$trader_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      candidates="$(jq -c '[.positions[]? | select(.availableQuantity > 0)]' "$RESPONSE_BODY")"
      c_len="$(jq 'length' <<<"$candidates")"
      if (( c_len == 0 )); then
        # Abastecer 1 posición rápida para poder vender
        request GET "$API_BASE_URL/api/market/official?page=0&size=5&hasQuote=true" "$trader_token"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          cand_id="$(jq -r '.content[0].id // empty' "$RESPONSE_BODY")"
          if [[ -n "$cand_id" ]]; then
            b_body="$(jq -n --argjson id "$cand_id" --argjson q 2 '{playerId: $id, quantity: $q}')"
            request POST "$API_BASE_URL/api/orders/buy" "$trader_token" "$b_body" >/dev/null 2>&1 || true
            request GET "$API_BASE_URL/api/portfolio/me" "$trader_token"
            candidates="$(jq -c '[.positions[]? | select(.availableQuantity > 0)]' "$RESPONSE_BODY")"
            c_len="$(jq 'length' <<<"$candidates")"
          fi
        fi
      fi
      if (( c_len > 0 )); then
        cand="$(jq -c '.[0]' <<<"$candidates")"
        p_id="$(jq -r '.playerId' <<<"$cand")"
        p_name="$(jq -r '.playerName' <<<"$cand")"
        p_avail="$(jq -r '.availableQuantity' <<<"$cand")"
        sell_qty=$(( p_avail > 2 ? 2 : p_avail ))
        bb_body="$(jq -n --argjson playerId "$p_id" --argjson quantity "$sell_qty" '{playerId: $playerId, quantity: $quantity}')"
        request POST "$API_BASE_URL/api/orders/sell-official" "$trader_token" "$bb_body"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
          total_amt="$(jq -r '.totalAmount // 0' "$RESPONSE_BODY")"
          echo "  ✔ [LIQUIDACIÓN OFICIAL] Transacción #$tx_id: $trader_email liquidó $sell_qty token(s) de $p_name al superusuario por ₵$(awk -v t="$total_amt" 'BEGIN{printf "%.2f", t/100}')"
          TOTAL_VOLUME_CENTS=$(( TOTAL_VOLUME_CENTS + total_amt ))
          SUCCESSFUL_TRADES=$(( SUCCESSFUL_TRADES + 1 ))
          COUNT_SIM_SELLS=$(( COUNT_SIM_SELLS + 1 ))
          sells_done=$(( sells_done + 1 ))
        fi
      fi
    fi
  done
fi

echo
echo "=============================================================================="
echo "[$(date --iso-8601=seconds)] Resumen de la Simulación:"
echo "  - Operaciones completadas: $SUCCESSFUL_TRADES"
if (( SUCCESSFUL_TRADES > 0 )); then
  sim_buy_pct=$(( (COUNT_SIM_BUYS * 100) / SUCCESSFUL_TRADES ))
  sim_sell_pct=$(( (COUNT_SIM_SELLS * 100) / SUCCESSFUL_TRADES ))
  bias="NEUTRAL"
  if (( COUNT_SIM_BUYS > COUNT_SIM_SELLS )); then
    bias="BULLISH (Alcista)"
  elif (( COUNT_SIM_BUYS < COUNT_SIM_SELLS )); then
    bias="BEARISH (Bajista)"
  fi
  echo "  - Sentimiento generado: $COUNT_SIM_BUYS Compras ($sim_buy_pct%) vs $COUNT_SIM_SELLS Ventas ($sim_sell_pct%) [Sesgo: $bias]"
fi
echo "  - Volumen inyectado al Dashboard: ₵$(awk -v v="$TOTAL_VOLUME_CENTS" 'BEGIN{printf "%.2f", v/100}') ($TOTAL_VOLUME_CENTS centésimas)"
echo "=============================================================================="
