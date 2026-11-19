#!/usr/bin/env bash

set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DEMO_PASSWORD="${P2P_DEMO_PASSWORD:-P2pDemo2026!}"
SELLER_COUNT="${P2P_SELLER_COUNT:-12}"
PLAYER_POOL_SIZE="${P2P_PLAYER_POOL_SIZE:-3}"
INTERVAL_SECONDS="${P2P_INTERVAL_SECONDS:-10800}"
MAX_INITIAL_CREDITS_CENTS=10000

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

  # Cantidad actual (1 token). Para la demo se podrá variar entre 1 y 3 tokens:
  token_quantity=1
  # [DEMO FEATURE #94] Descomentar para variar cantidad de tokens:
  # token_quantity=$(( (seller_index % 3) + 1 ))

  buy_body="$(jq -n --argjson playerId "$player_id" --argjson quantity "$token_quantity" \
    '{playerId: $playerId, quantity: $quantity}')"
  request POST "$API_BASE_URL/api/orders/buy" "$seller_token" "$buy_body"
  require_status 200 "la compra oficial de $seller_email"

  # Publicación P2P estándar actual (precio congelado por backend al 100% de la cotización):
  sell_body="$(jq -n --argjson playerId "$player_id" --argjson quantity "$token_quantity" \
    '{playerId: $playerId, quantity: $quantity}')"

  # ---------------------------------------------------------------------------
  # [PENDIENTE - ISSUE #94 / SPEC-026: PRECIO FLEXIBLE EN MERCADO P2P]
  # Cuando se implemente el campo opcional 'unitPrice' en OrderSellRequestDTO:
  # - Rango permitido: [quote.price * 0.8, quote.price * 1.2]
  # - Permite ofertas con descuento (ej. 85-95%) o sobreprecio (ej. 105-115%)
  # Descomentar el siguiente bloque cuando el endpoint acepte unitPrice:
  #
  # variation_factor=$(( 85 + (RANDOM % 31) )) # Entre 85% y 115% de la cotización
  # flexible_unit_price=$(( (official_price * variation_factor) / 100 ))
  # sell_body="$(jq -n \
  #   --argjson playerId "$player_id" \
  #   --argjson quantity "$token_quantity" \
  #   --argjson unitPrice "$flexible_unit_price" \
  #   '{playerId: $playerId, quantity: $quantity, unitPrice: $unitPrice}')"
  # ---------------------------------------------------------------------------

  request POST "$API_BASE_URL/api/orders/sell" "$seller_token" "$sell_body"
  require_status 201 "la publicación P2P de $seller_email"

  listing_id="$(jq -er '.listingId' "$RESPONSE_BODY")"
  echo "- Listing $listing_id: $seller_email ofrece $token_quantity token(s) de $player_name."
  CREATED_LISTINGS+=("$listing_id")
done

# -----------------------------------------------------------------------------
# [PENDIENTE - ISSUES #94, #96, #97 / SPEC-026, SPEC-028, SPEC-029]
# MOVIMIENTOS Y LIQUIDACIÓN REAL EN EL MERCADO P2P PARA LA DEMO
# -----------------------------------------------------------------------------
# Actualmente el script solo publica ofertas pero nadie las compra.
# Al activar este bloque, el comprador liquida un porcentaje de los listings
# (por ejemplo el 50%), logrando:
#  1. Publicaciones que pasan a estado 'SOLD' (visible en historial).
#  2. Registros reales en 'market_transactions' (compras/ventas secundarias).
#  3. Datos para el Tablero de Tendencias y Sentimiento (BULLISH/BEARISH).
#  4. Ofertas remanentes en 'ACTIVE' para que el usuario compre en vivo en la demo.
#
# BUY_RATIO_PERCENT=50
# echo
# echo "=== [DEMO] Simulando compras P2P para generar transacciones y sentimiento ==="
# for listing_to_buy in "${CREATED_LISTINGS[@]}"; do
#   if (( (RANDOM % 100) < BUY_RATIO_PERCENT )); then
#     buy_p2p_body="$(jq -n --argjson listingId "$listing_to_buy" '{listingId: $listingId}')"
#     request POST "$API_BASE_URL/api/orders/buy" "$BUYER_TOKEN" "$buy_p2p_body"
#     if [[ "$HTTP_STATUS" == "200" ]]; then
#       echo "  ✔ [TRADE P2P] Listing $listing_to_buy comprado por $BUYER_EMAIL (generó transacción)"
#     else
#       echo "  ✖ [TRADE P2P] Falló compra del listing $listing_to_buy (HTTP $HTTP_STATUS)"
#     fi
#   else
#     echo "  ➜ [OFERTA ABIERTA] Listing $listing_to_buy queda activo para comprar en vivo durante la demo"
#   fi
# done

# -----------------------------------------------------------------------------
# [PENDIENTE - ISSUE #94 / SPEC-026: VENTA INMEDIATA OFICIAL (BUYBACK)]
# Cuando se implemente POST /api/orders/sell-official (o similar):
# Permite simular que usuarios venden de vuelta a la casa con spread (10% desc.)
# para mostrar la función de liquidez inmediata.
#
# if (( ${#SELLER_TOKENS[@]} > 0 )); then
#   # Un vendedor vende inmediatamente 1 token a la casa oficial
#   sample_seller_token="${SELLER_TOKENS[0]}"
#   sample_player_id="$(jq -er '.content[0].id' <<<"$CATALOG")"
#   # buyback_body="$(jq -n --argjson playerId "$sample_player_id" --argjson quantity 1 \
#   #   '{playerId: $playerId, quantity: $quantity}')"
#   # request POST "$API_BASE_URL/api/orders/sell-official" "$sample_seller_token" "$buyback_body"
#   # echo "  ✔ [BUYBACK] Venta oficial ejecutada al superusuario con spread aplicado."
# fi

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
