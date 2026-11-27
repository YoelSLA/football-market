#!/usr/bin/env bash

set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DEMO_PASSWORD="${P2P_DEMO_PASSWORD:-P2pDemo2026!}"
SELLER_COUNT="${P2P_SELLER_COUNT:-12}"
PLAYER_POOL_SIZE="${P2P_PLAYER_POOL_SIZE:-3}"
INTERVAL_SECONDS="${P2P_INTERVAL_SECONDS:-10800}"
P2P_MIN_TOKENS="${P2P_MIN_TOKENS:-1}"
P2P_MAX_TOKENS="${P2P_MAX_TOKENS:-5}"
MAX_INITIAL_CREDITS_CENTS=10000

# Variación dinámica de ratio de ventas entre 40% y 60%
P2P_SELLS_RATIO_PERCENT="${P2P_SELLS_RATIO_PERCENT:-$(( 40 + (RANDOM % 21) ))}"
BUYBACK_SELLER_COUNT=$(( (SELLER_COUNT * P2P_SELLS_RATIO_PERCENT + 50) / 100 ))
if (( BUYBACK_SELLER_COUNT < 1 && P2P_SELLS_RATIO_PERCENT > 0 )); then
  BUYBACK_SELLER_COUNT=1
fi
COUNT_BUYS=0
COUNT_SELLS=0

for dependency in curl jq; do
  if ! command -v "$dependency" >/dev/null 2>&1; then
    echo "Error: $dependency es obligatorio." >&2
    exit 1
  fi
done

for setting in SELLER_COUNT PLAYER_POOL_SIZE INTERVAL_SECONDS; do
  value="${!setting}"
  if ! [[ "$value" =~ ^[1-9][0-9]*$ ]]; then
    echo "Error: $setting debe ser un entero positivo." >&2
    exit 1
  fi
done

if (( SELLER_COUNT < PLAYER_POOL_SIZE )); then
  echo "Error: P2P_SELLER_COUNT debe ser igual o mayor que P2P_PLAYER_POOL_SIZE." >&2
  exit 1
fi

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
  HTTP_STATUS="$(curl "${args[@]}")"
}

require_status() {
  local expected="$1"
  local operation="$2"

  if [[ "$HTTP_STATUS" != "$expected" ]]; then
    echo "Error durante $operation (HTTP $HTTP_STATUS):" >&2
    jq . "$RESPONSE_BODY" 2>/dev/null >&2 || sed -n '1,20p' "$RESPONSE_BODY" >&2
    exit 1
  fi
}

register_or_login() {
  local username="$1"
  local email="$2"
  local register_body
  local login_body

  register_body="$(jq -n \
    --arg username "$username" \
    --arg email "$email" \
    --arg password "$DEMO_PASSWORD" \
    '{username: $username, email: $email, password: $password}')"
  request POST "$API_BASE_URL/api/auth/register" '' "$register_body"
  if [[ "$HTTP_STATUS" != "201" && "$HTTP_STATUS" != "409" ]]; then
    require_status 201 "el registro de $email"
  fi

  login_body="$(jq -n \
    --arg email "$email" \
    --arg password "$DEMO_PASSWORD" \
    '{email: $email, password: $password}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
  require_status 200 "el inicio de sesión de $email"
  jq -er '.token' "$RESPONSE_BODY"
}

request GET "$API_BASE_URL/actuator/health"
require_status 200 "la comprobación del backend"

# Una clave estable por franja vuelve idempotente cada ejecución programada.
BATCH_ID="$(($(date -u +%s) / INTERVAL_SECONDS))"
BATCH_PREFIX="p2p.$BATCH_ID"

declare -a SELLER_TOKENS=()
for seller_number in $(seq 1 "$SELLER_COUNT"); do
  seller_email="$BATCH_PREFIX.seller${seller_number}@example.test"
  SELLER_TOKENS+=("$(register_or_login "$BATCH_PREFIX.s${seller_number}" "$seller_email")")
done

BUYER_EMAIL="$BATCH_PREFIX.buyer@example.test"
BUYER_TOKEN="$(register_or_login "$BATCH_PREFIX.buyer" "$BUYER_EMAIL")"

CATALOG_QUERY="size=$PLAYER_POOL_SIZE&hasQuote=true&maxPrice=$MAX_INITIAL_CREDITS_CENTS&sort=price,asc"
request GET "$API_BASE_URL/api/market/official?page=0&$CATALOG_QUERY" "${SELLER_TOKENS[0]}"
require_status 200 "la consulta inicial del mercado oficial"
TOTAL_PAGES="$(jq -er '.totalPages' "$RESPONSE_BODY")"
if (( TOTAL_PAGES == 0 )); then
  echo "Error: no hay jugadores cotizados por hasta 100 créditos." >&2
  exit 1
fi

# Cambia de grupo cada tres horas, pero repite jugadores dentro de la tanda.
CATALOG_PAGE=$((BATCH_ID % TOTAL_PAGES))
request GET "$API_BASE_URL/api/market/official?page=$CATALOG_PAGE&$CATALOG_QUERY" "${SELLER_TOKENS[0]}"
require_status 200 "la página $CATALOG_PAGE del mercado oficial"
CATALOG="$(<"$RESPONSE_BODY")"
AVAILABLE_PLAYERS="$(jq '.content | length' <<<"$CATALOG")"
if (( AVAILABLE_PLAYERS == 0 )); then
  echo "Error: la página seleccionada del mercado oficial quedó vacía." >&2
  exit 1
fi

echo "[$(date --iso-8601=seconds)] Tanda P2P $BATCH_ID en $API_BASE_URL (página oficial $CATALOG_PAGE)"
echo

declare -a CREATED_LISTINGS=()

for seller_index in $(seq 0 $((SELLER_COUNT - 1))); do
  seller_number=$((seller_index + 1))
  seller_email="$BATCH_PREFIX.seller${seller_number}@example.test"
  seller_token="${SELLER_TOKENS[$seller_index]}"

  request GET "$API_BASE_URL/api/listings/mine?status=ACTIVE" "$seller_token"
  require_status 200 "la consulta de publicaciones de $seller_email"
  if (( $(jq '.listings | length' "$RESPONSE_BODY") > 0 )); then
    echo "- $seller_email ya tiene una publicación activa; no se duplicó."
    continue
  fi

  player_index=$((seller_index % AVAILABLE_PLAYERS))
  player_id="$(jq -er ".content[$player_index].id" <<<"$CATALOG")"
  player_name="$(jq -er ".content[$player_index].name" <<<"$CATALOG")"
  official_price="$(jq -er ".content[$player_index].quote.price // 100" <<<"$CATALOG")"

  # Cantidad aleatoria de tokens a comprar y publicar (simula oferta realista):
  max_affordable=$(( MAX_INITIAL_CREDITS_CENTS / official_price ))
  max_allowed=$(( max_affordable < P2P_MAX_TOKENS ? max_affordable : P2P_MAX_TOKENS ))

  if (( max_allowed <= P2P_MIN_TOKENS )); then
    token_quantity=$(( max_allowed > 0 ? max_allowed : 1 ))
  else
    # Distribución realista: 60% probabilidad de 1-2 tokens, 30% de 3-4, 10% de 5+
    distribution_roll=$(( RANDOM % 100 ))
    if (( distribution_roll < 60 )); then
      upper=$(( max_allowed < 2 ? max_allowed : 2 ))
      token_quantity=$(( (RANDOM % (upper - P2P_MIN_TOKENS + 1)) + P2P_MIN_TOKENS ))
    elif (( distribution_roll < 90 )); then
      lower=$(( max_allowed < 3 ? max_allowed : 3 ))
      upper=$(( max_allowed < 4 ? max_allowed : 4 ))
      token_quantity=$(( (RANDOM % (upper - lower + 1)) + lower ))
    else
      token_quantity="$max_allowed"
    fi
  fi

  # Si este vendedor participará en liquidaciones oficiales (buyback), compra tokens adicionales para liquidar
  extra_buyback_qty=0
  if (( seller_index < BUYBACK_SELLER_COUNT )); then
    extra_buyback_qty=$(( (RANDOM % 2) + 1 ))
  fi
  total_buy_qty=$(( token_quantity + extra_buyback_qty ))

  buy_body="$(jq -n --argjson playerId "$player_id" --argjson quantity "$total_buy_qty" \
    '{playerId: $playerId, quantity: $quantity}')"
  request POST "$API_BASE_URL/api/orders/buy" "$seller_token" "$buy_body"
  require_status 200 "la compra oficial de $seller_email"
  COUNT_BUYS=$(( COUNT_BUYS + 1 ))

  # Publicación P2P con precio flexible (85% - 115% de la cotización según SPEC-026)
  variation_factor=$(( 85 + (RANDOM % 31) ))
  flexible_unit_price=$(( (official_price * variation_factor) / 100 ))
  if (( flexible_unit_price < 1 )); then flexible_unit_price=1; fi

  sell_body="$(jq -n \
    --argjson playerId "$player_id" \
    --argjson quantity "$token_quantity" \
    --argjson unitPrice "$flexible_unit_price" \
    '{playerId: $playerId, quantity: $quantity, unitPrice: $unitPrice}')"
  request POST "$API_BASE_URL/api/orders/sell" "$seller_token" "$sell_body"

  # Fallback si el backend no aceptara unitPrice
  if [[ "$HTTP_STATUS" != "201" ]]; then
    sell_body="$(jq -n --argjson playerId "$player_id" --argjson quantity "$token_quantity" \
      '{playerId: $playerId, quantity: $quantity}')"
    request POST "$API_BASE_URL/api/orders/sell" "$seller_token" "$sell_body"
  fi
  require_status 201 "la publicación P2P de $seller_email"

  listing_id="$(jq -er '.listingId' "$RESPONSE_BODY")"
  echo "- Listing $listing_id: $seller_email ofrece $token_quantity token(s) de $player_name."
  CREATED_LISTINGS+=("$listing_id")
done

# -----------------------------------------------------------------------------
# MOVIMIENTOS Y LIQUIDACIÓN REAL EN EL MERCADO P2P (ALIMENTA EL DASHBOARD)
# -----------------------------------------------------------------------------
# Un comprador regular (NO superusuario) liquida un porcentaje de los listings
# (por defecto 40%), logrando:
#  1. Publicaciones que pasan a estado 'SOLD' (visible en historial).
#  2. Registros reales en 'market_transactions' (compras/ventas secundarias).
#  3. Datos para el Tablero de Tendencias y Volumen 24h del Dashboard.
#  4. Ofertas remanentes en 'ACTIVE' (60%) para que el usuario compre en la UI.
P2P_BUY_RATIO_PERCENT="${P2P_BUY_RATIO_PERCENT:-40}"
if (( P2P_BUY_RATIO_PERCENT > 0 && ${#CREATED_LISTINGS[@]} > 0 )); then
  echo
  echo "=== [TRADES] Simulando compras P2P de la tanda para generar transacciones y volumen ==="
  for listing_to_buy in "${CREATED_LISTINGS[@]}"; do
    if (( (RANDOM % 100) < P2P_BUY_RATIO_PERCENT )); then
      buy_p2p_body="$(jq -n --argjson listingId "$listing_to_buy" '{listingId: $listingId}')"
      request POST "$API_BASE_URL/api/orders/buy" "$BUYER_TOKEN" "$buy_p2p_body"
      if [[ "$HTTP_STATUS" == "200" ]]; then
        echo "  ✔ [TRADE P2P] Listing $listing_to_buy comprado por $BUYER_EMAIL (generó transacción)"
        COUNT_BUYS=$(( COUNT_BUYS + 1 ))
      else
        echo "  ✖ [TRADE P2P] Falló compra del listing $listing_to_buy (HTTP $HTTP_STATUS)"
      fi
    else
      echo "  ➜ [OFERTA ABIERTA] Listing $listing_to_buy queda activo para comprar en vivo"
    fi
  done
fi

# -----------------------------------------------------------------------------
# LIQUIDACIÓN INMEDIATA OFICIAL (BUYBACK - SPEC-026 / PR #111)
# -----------------------------------------------------------------------------
# Vendedores liquidan tokens de vuelta a la casa oficial con spread del 10%.
# Genera ventas oficiales que equilibran dinámicamente las métricas de sentimiento
# entre el 40% y el 60% de ventas (variación aleatoria).
if (( BUYBACK_SELLER_COUNT > 0 && ${#SELLER_TOKENS[@]} > 0 )); then
  echo
  echo "=== [BUYBACK] Liquidaciones oficiales a la Casa (Objetivo ventas: $BUYBACK_SELLER_COUNT de $SELLER_COUNT vendedores) ==="
  for s_idx in $(seq 0 $(( BUYBACK_SELLER_COUNT - 1 ))); do
    sample_seller_token="${SELLER_TOKENS[$s_idx]}"
    sample_seller_email="$BATCH_PREFIX.seller$((s_idx + 1))@example.test"
    request GET "$API_BASE_URL/api/portfolio/me" "$sample_seller_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      p_candidate="$(jq -c '.positions[]? | select(.availableQuantity > 0)' "$RESPONSE_BODY" | head -n 1)"
      if [[ -n "$p_candidate" ]]; then
        sample_p_id="$(jq -r '.playerId' <<<"$p_candidate")"
        sample_p_name="$(jq -r '.playerName' <<<"$p_candidate")"
        sample_p_avail="$(jq -r '.availableQuantity' <<<"$p_candidate")"
        bb_qty=$(( sample_p_avail > 2 ? 2 : sample_p_avail ))
        buyback_body="$(jq -n --argjson playerId "$sample_p_id" --argjson quantity "$bb_qty" \
          '{playerId: $playerId, quantity: $quantity}')"
        request POST "$API_BASE_URL/api/orders/sell-official" "$sample_seller_token" "$buyback_body"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          echo "  ✔ [BUYBACK] $sample_seller_email liquidó $bb_qty token(s) de $sample_p_name a la casa oficial."
          COUNT_SELLS=$(( COUNT_SELLS + 1 ))
        fi
      fi
    fi
  done

  # El comprador también liquida 1 token de su inventario a la casa si tiene stock
  request GET "$API_BASE_URL/api/portfolio/me" "$BUYER_TOKEN"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    buyer_cand="$(jq -c '.positions[]? | select(.availableQuantity > 0)' "$RESPONSE_BODY" | head -n 1)"
    if [[ -n "$buyer_cand" ]]; then
      b_pid="$(jq -r '.playerId' <<<"$buyer_cand")"
      b_pname="$(jq -r '.playerName' <<<"$buyer_cand")"
      bb_body="$(jq -n --argjson playerId "$b_pid" --argjson quantity 1 '{playerId: $playerId, quantity: $quantity}')"
      request POST "$API_BASE_URL/api/orders/sell-official" "$BUYER_TOKEN" "$bb_body"
      if [[ "$HTTP_STATUS" == "200" ]]; then
        echo "  ✔ [BUYBACK] $BUYER_EMAIL liquidó 1 token de $b_pname a la casa oficial."
        COUNT_SELLS=$(( COUNT_SELLS + 1 ))
      fi
    fi
  fi
fi

total_tanda_trades=$(( COUNT_BUYS + COUNT_SELLS ))
tanda_buy_pct=0 tanda_sell_pct=0
if (( total_tanda_trades > 0 )); then
  tanda_buy_pct=$(( (COUNT_BUYS * 100) / total_tanda_trades ))
  tanda_sell_pct=$(( (COUNT_SELLS * 100) / total_tanda_trades ))
fi
echo
echo "=============================================================================="
echo "📊 Balance de Operaciones de la Tanda P2P:"
echo "   • Compras: $COUNT_BUYS ($tanda_buy_pct%) | Ventas: $COUNT_SELLS ($tanda_sell_pct%) [Objetivo: $P2P_SELLS_RATIO_PERCENT%]"
echo "=============================================================================="

# -----------------------------------------------------------------------------
# [PENDIENTE - ISSUES #96 Y #97 / SPEC-028 Y SPEC-029: HOT PLAYERS DE TENDENCIAS]
# Para que el endpoint GET /api/market/trends muestre jugadores líderes
# en 'topHotPlayers' y volumen significativo en la demo:
#
# target_hot_player_id="$(jq -er '.content[0].id' <<<"$CATALOG")"
# target_hot_player_name="$(jq -er '.content[0].name' <<<"$CATALOG")"
# echo "  ➜ [HOT PLAYER] Inyectando compras concentradas sobre $target_hot_player_name para elevar volumen..."
# for i in {1..4}; do
#   hot_buy_body="$(jq -n --argjson playerId "$target_hot_player_id" '{playerId: $playerId, quantity: 2}')"
#   request POST "$API_BASE_URL/api/orders/buy" "$BUYER_TOKEN" "$hot_buy_body"
# done
# -----------------------------------------------------------------------------

echo
echo "Comprador de esta tanda: $BUYER_EMAIL"
echo "Contraseña común: $DEMO_PASSWORD"
echo "Cada jugador elegido recibe varias publicaciones de vendedores diferentes."
