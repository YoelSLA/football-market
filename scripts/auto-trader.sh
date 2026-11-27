#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# AUTO TRADER & MOTOR DE MERCADO (YOEL, NICOLAS & TRADERS DEMO)
# =============================================================================
# Genera compras y ventas automáticas involucrando a los usuarios reales
# yoel@gmail.com y nicolas@gmail.com, además de compradores/vendedores demo,
# y recalcula dinámicamente las cotizaciones para mantener el Dashboard vivo.
#
# Flujo de cada ciclo:
#   1. Autenticación de Yoel, Nicolas y Comprador Regular Demo.
#   2. Publicaciones P2P orgánicas: Yoel y Nicolas ponen a la venta tokens de su portfolio.
#   3. Compras cruzadas P2P:
#      - Nicolas compra publicaciones de Yoel.
#      - Yoel compra publicaciones de Nicolas.
#   4. Absorción de liquidez demo: El mercado demo compra publicaciones remanentes
#      para asegurar que Yoel y Nicolas cobren sus créditos y roten inventario.
#   5. Compras en mercado primario (Oficial): Yoel y Nicolas adquieren nuevos tokens
#      para enriquecer y diversificar su portfolio.
#   6. Recálculo dinámico de cotizaciones: Ajusta precios de los 1861 jugadores con
#      climas bursátiles aleatorios (Calmo, Moderado, Volátil).
#   7. Balance e impacto: Reporta el saldo y posiciones actualizadas de cada uno.
#
# Uso:
#   ./scripts/auto-trader.sh                        # Ejecuta 1 ciclo completo y sale
#   ./scripts/auto-trader.sh --once                 # Idem (ideal para tareas cron)
#   ./scripts/auto-trader.sh --loop                 # Bucle continuo (cada 60s por defecto)
#   ./scripts/auto-trader.sh --loop --interval 30   # Bucle continuo cada 30 segundos
#   ./scripts/auto-trader.sh --rounds 3             # Ejecuta 3 ciclos y finaliza
#   ./scripts/auto-trader.sh --no-recalc            # Ejecuta compras/ventas sin recalcular
# =============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
LOG_FILE="${AUTO_TRADER_LOG:-$SCRIPT_DIR/auto-trader.log}"

# Credenciales de usuarios reales
YOEL_EMAIL="${YOEL_EMAIL:-yoel@gmail.com}"
YOEL_PASSWORD="${YOEL_PASSWORD:-12345678}"
YOEL_USER_ID=2

NICOLAS_EMAIL="${NICOLAS_EMAIL:-nicolas@gmail.com}"
NICOLAS_PASSWORD="${NICOLAS_PASSWORD:-12345678}"
NICOLAS_USER_ID=3

# Credenciales de comprador demo auxiliar (para absorción de liquidez)
DEMO_BUYER_PASSWORD="${DEMO_BUYER_PASSWORD:-TraderDemo2026!}"

# Parámetros operativos
MODE="once"
INTERVAL_SECONDS=60
TOTAL_ROUNDS=1
RECALCULATE_QUOTES=true

# Parseo de argumentos de línea de comandos
while [[ $# -gt 0 ]]; do
  case "$1" in
    --once)
      MODE="once"
      TOTAL_ROUNDS=1
      shift
      ;;
    --loop)
      MODE="loop"
      shift
      ;;
    --interval)
      INTERVAL_SECONDS="$2"
      shift 2
      ;;
    --rounds)
      MODE="rounds"
      TOTAL_ROUNDS="$2"
      shift 2
      ;;
    --no-recalc)
      RECALCULATE_QUOTES=false
      shift
      ;;
    --with-recalc)
      RECALCULATE_QUOTES=true
      shift
      ;;
    -h|--help)
      echo "Uso: $0 [--once | --loop | --rounds N] [--interval SEG] [--no-recalc]"
      exit 0
      ;;
    *)
      echo "Opción desconocida: $1" >&2
      exit 1
      ;;
  esac
done

for dependency in curl jq awk; do
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

format_currency() {
  local cents="${1:-0}"
  awk -v c="$cents" 'BEGIN { printf "₵%.2f", c/100 }'
}

# 1. Comprobar que el backend esté listo
check_backend() {
  request GET "$API_BASE_URL/actuator/health"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "Error: El backend no responde en $API_BASE_URL (HTTP $HTTP_STATUS)." >&2
    return 1
  fi
  return 0
}

# 2. Login de un usuario
login_user() {
  local email="$1"
  local password="$2"
  local body
  body="$(jq -n --arg e "$email" --arg p "$password" '{email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$body"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "Error al autenticar $email (HTTP $HTTP_STATUS)" >&2
    return 1
  fi
  jq -er '.token' "$RESPONSE_BODY"
}

# 3. Obtener o registrar comprador demo
get_or_create_demo_buyer() {
  local idx="${1:-1}"
  local email="trader.buyer${idx}@example.test"
  local username="trader_buyer_${idx}"

  local reg_body
  reg_body="$(jq -n --arg u "$username" --arg e "$email" --arg p "$DEMO_BUYER_PASSWORD" \
    '{username: $u, email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/register" '' "$reg_body"

  local login_body
  login_body="$(jq -n --arg e "$email" --arg p "$DEMO_BUYER_PASSWORD" \
    '{email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
  if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "Error al autenticar comprador demo $email (HTTP $HTTP_STATUS)" >&2
    return 1
  fi
  jq -er '.token' "$RESPONSE_BODY"
}

# 4. Ejecución de un ciclo completo de trading
run_trading_cycle() {
  local cycle_num="$1"
  echo "=============================================================================="
  echo "🚀 [$(date '+%Y-%m-%d %H:%M:%S')] CICLO DE TRADING AUTOMÁTICO #$cycle_num"
  echo "   Operadores activos: Yoel (yoel@gmail.com) y Nicolas (nicolas@gmail.com)"
  echo "=============================================================================="

  # A. Login de actores
  local yoel_token nicolas_token demo_buyer_token
  yoel_token="$(login_user "$YOEL_EMAIL" "$YOEL_PASSWORD")"
  nicolas_token="$(login_user "$NICOLAS_EMAIL" "$NICOLAS_PASSWORD")"
  demo_buyer_token="$(get_or_create_demo_buyer "$(( (RANDOM % 5) + 1 ))")"

  # B. Publicación de venta P2P: YOEL
  echo
  echo "--- 1. Gestión de ventas P2P de Yoel ---"
  request GET "$API_BASE_URL/api/listings/mine?status=ACTIVE" "$yoel_token"
  local yoel_active_orders=0
  if [[ "$HTTP_STATUS" == "200" ]]; then
    yoel_active_orders="$(jq -r '.summary.activeOrders // 0' "$RESPONSE_BODY")"
  fi

  if (( yoel_active_orders >= 2 )); then
    echo "  ➜ Yoel ya tiene $yoel_active_orders publicaciones activas; no satura el mercado."
  else
    request GET "$API_BASE_URL/api/portfolio/me" "$yoel_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local yoel_candidates
      yoel_candidates="$(jq -r '.positions[] | select(.availableQuantity > 0) | "\(.playerId)|\(.playerName)|\(.availableQuantity)|\(.currentUnitPrice)"' "$RESPONSE_BODY")"
      if [[ -n "$yoel_candidates" ]]; then
        local chosen_line
        chosen_line="$(shuf -n 1 <<<"$yoel_candidates")"
        IFS='|' read -r p_id p_name p_qty p_price <<<"$chosen_line"

        local sell_body
        sell_body="$(jq -n --argjson id "$p_id" --argjson q 1 '{playerId: $id, quantity: $q}')"
        request POST "$API_BASE_URL/api/orders/sell" "$yoel_token" "$sell_body"
        if [[ "$HTTP_STATUS" == "201" ]]; then
          local new_listing_id
          new_listing_id="$(jq -r '.listingId' "$RESPONSE_BODY")"
          echo "  ✔ [VENTA YOEL] Publicó 1 token de $p_name en P2P (Listing #$new_listing_id a $(format_currency "$p_price"))"
        else
          echo "  ✖ [FALLO VENTA YOEL] No se pudo publicar $p_name (HTTP $HTTP_STATUS)"
        fi
      else
        echo "  [INFO] Yoel no tiene tokens disponibles para vender en este momento."
      fi
    fi
  fi

  # C. Publicación de venta P2P: NICOLAS
  echo
  echo "--- 2. Gestión de ventas P2P de Nicolas ---"
  request GET "$API_BASE_URL/api/listings/mine?status=ACTIVE" "$nicolas_token"
  local nicolas_active_orders=0
  if [[ "$HTTP_STATUS" == "200" ]]; then
    nicolas_active_orders="$(jq -r '.summary.activeOrders // 0' "$RESPONSE_BODY")"
  fi

  if (( nicolas_active_orders >= 2 )); then
    echo "  ➜ Nicolas ya tiene $nicolas_active_orders publicaciones activas; no satura el mercado."
  else
    request GET "$API_BASE_URL/api/portfolio/me" "$nicolas_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local nic_candidates
      nic_candidates="$(jq -r '.positions[] | select(.availableQuantity > 0) | "\(.playerId)|\(.playerName)|\(.availableQuantity)|\(.currentUnitPrice)"' "$RESPONSE_BODY")"
      if [[ -n "$nic_candidates" ]]; then
        local chosen_nic_line
        chosen_nic_line="$(shuf -n 1 <<<"$nic_candidates")"
        IFS='|' read -r p_id p_name p_qty p_price <<<"$chosen_nic_line"

        local sell_body
        sell_body="$(jq -n --argjson id "$p_id" --argjson q 1 '{playerId: $id, quantity: $q}')"
        request POST "$API_BASE_URL/api/orders/sell" "$nicolas_token" "$sell_body"
        if [[ "$HTTP_STATUS" == "201" ]]; then
          local new_listing_id
          new_listing_id="$(jq -r '.listingId' "$RESPONSE_BODY")"
          echo "  ✔ [VENTA NICOLAS] Publicó 1 token de $p_name en P2P (Listing #$new_listing_id a $(format_currency "$p_price"))"
        else
          echo "  ✖ [FALLO VENTA NICOLAS] No se pudo publicar $p_name (HTTP $HTTP_STATUS)"
        fi
      else
        echo "  [INFO] Nicolas no tiene tokens disponibles para vender en este momento."
      fi
    fi
  fi

  # D. Compras cruzadas P2P (Nicolas compra a Yoel)
  echo
  echo "--- 3. Compras cruzadas P2P (Nicolas -> Yoel) ---"
  request GET "$API_BASE_URL/api/listings" "$nicolas_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local yoel_listings
    yoel_listings="$(jq -r --argjson sellerId "$YOEL_USER_ID" \
      '.listings[]? | select(.sellerId == $sellerId and .status == "ACTIVE") | "\(.listingId)|\(.playerId)|\(.unitPrice)|\(.totalAmount)"' "$RESPONSE_BODY")"
    if [[ -n "$yoel_listings" ]]; then
      local first_yoel_listing
      first_yoel_listing="$(head -n 1 <<<"$yoel_listings")"
      IFS='|' read -r l_id p_id u_price t_amount <<<"$first_yoel_listing"

      local buy_body
      buy_body="$(jq -n --argjson id "$l_id" '{listingId: $id}')"
      request POST "$API_BASE_URL/api/orders/buy" "$nicolas_token" "$buy_body"
      if [[ "$HTTP_STATUS" == "200" ]]; then
        local tx_id
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        echo "  ✔ [TRADE CRUZADO] Transacción #$tx_id: Nicolas compró Listing #$l_id de Yoel (Total: $(format_currency "$t_amount"))"
      else
        echo "  ✖ [FALLO TRADE CRUZADO] Nicolas no pudo comprar Listing #$l_id de Yoel (HTTP $HTTP_STATUS)"
      fi
    else
      echo "  ➜ No hay publicaciones activas de Yoel disponibles para Nicolas."
    fi
  fi

  # E. Compras cruzadas P2P (Yoel compra a Nicolas)
  echo
  echo "--- 4. Compras cruzadas P2P (Yoel -> Nicolas) ---"
  request GET "$API_BASE_URL/api/listings" "$yoel_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local nicolas_listings
    nicolas_listings="$(jq -r --argjson sellerId "$NICOLAS_USER_ID" \
      '.listings[]? | select(.sellerId == $sellerId and .status == "ACTIVE") | "\(.listingId)|\(.playerId)|\(.unitPrice)|\(.totalAmount)"' "$RESPONSE_BODY")"
    if [[ -n "$nicolas_listings" ]]; then
      local first_nic_listing
      first_nic_listing="$(head -n 1 <<<"$nicolas_listings")"
      IFS='|' read -r l_id p_id u_price t_amount <<<"$first_nic_listing"

      local buy_body
      buy_body="$(jq -n --argjson id "$l_id" '{listingId: $id}')"
      request POST "$API_BASE_URL/api/orders/buy" "$yoel_token" "$buy_body"
      if [[ "$HTTP_STATUS" == "200" ]]; then
        local tx_id
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        echo "  ✔ [TRADE CRUZADO] Transacción #$tx_id: Yoel compró Listing #$l_id de Nicolas (Total: $(format_currency "$t_amount"))"
      else
        echo "  ✖ [FALLO TRADE CRUZADO] Yoel no pudo comprar Listing #$l_id de Nicolas (HTTP $HTTP_STATUS)"
      fi
    else
      echo "  ➜ No hay publicaciones activas de Nicolas disponibles para Yoel."
    fi
  fi

  # F. Liquidez de mercado (Comprador Demo absorbe ofertas remanentes)
  echo
  echo "--- 5. Absorción de liquidez demo (Rotación de mercado) ---"
  request GET "$API_BASE_URL/api/listings" "$demo_buyer_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local remaining_listings
    remaining_listings="$(jq -r '.listings[]? | select(.status == "ACTIVE") | "\(.listingId)|\(.sellerId)|\(.totalAmount)"' "$RESPONSE_BODY")"
    if [[ -n "$remaining_listings" ]]; then
      local count=0
      while IFS='|' read -r l_id s_id t_amt; do
        [[ -n "$l_id" ]] || continue
        if (( count >= 2 )); then break; fi

        local buy_body
        buy_body="$(jq -n --argjson id "$l_id" '{listingId: $id}')"
        request POST "$API_BASE_URL/api/orders/buy" "$demo_buyer_token" "$buy_body"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          local tx_id
          tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
          local seller_name="Vendedor #$s_id"
          if [[ "$s_id" == "$YOEL_USER_ID" ]]; then seller_name="Yoel"; fi
          if [[ "$s_id" == "$NICOLAS_USER_ID" ]]; then seller_name="Nicolas"; fi
          echo "  ✔ [LIQUIDEZ DEMO] Transacción #$tx_id: Comprador demo liquidó Listing #$l_id ($seller_name acreditó $(format_currency "$t_amt"))"
          count=$(( count + 1 ))
        fi
      done <<<"$remaining_listings"
    else
      echo "  ➜ No hay publicaciones activas remanentes en el mercado."
    fi
  fi

  # G. Compras de enriquecimiento en Mercado Primario (Oficial)
  echo
  echo "--- 6. Compras primarias (Mercado Oficial) ---"
  # 50% probabilidad de que Yoel compre 1 jugador del catálogo oficial
  if (( (RANDOM % 100) < 60 )); then
    request GET "$API_BASE_URL/api/market/official?page=$(( RANDOM % 5 ))&size=5&hasQuote=true" "$yoel_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local candidate_player
      candidate_player="$(jq -c '.content[0] // empty' "$RESPONSE_BODY")"
      if [[ -n "$candidate_player" ]]; then
        local p_id p_name p_price
        p_id="$(jq -r '.id' <<<"$candidate_player")"
        p_name="$(jq -r '.name' <<<"$candidate_player")"
        p_price="$(jq -r '.quote.price // 100' <<<"$candidate_player")"

        local buy_body
        buy_body="$(jq -n --argjson id "$p_id" --argjson q 1 '{playerId: $id, quantity: $q}')"
        request POST "$API_BASE_URL/api/orders/buy" "$yoel_token" "$buy_body"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          local tx_id
          tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
          echo "  ✔ [OFICIAL YOEL] Transacción #$tx_id: Yoel compró 1 token de $p_name al superusuario por $(format_currency "$p_price")"
        fi
      fi
    fi
  fi

  # 50% probabilidad de que Nicolas compre 1 jugador del catálogo oficial
  if (( (RANDOM % 100) < 60 )); then
    request GET "$API_BASE_URL/api/market/official?page=$(( RANDOM % 5 ))&size=5&hasQuote=true" "$nicolas_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local candidate_player
      candidate_player="$(jq -c '.content[0] // empty' "$RESPONSE_BODY")"
      if [[ -n "$candidate_player" ]]; then
        local p_id p_name p_price
        p_id="$(jq -r '.id' <<<"$candidate_player")"
        p_name="$(jq -r '.name' <<<"$candidate_player")"
        p_price="$(jq -r '.quote.price // 100' <<<"$candidate_player")"

        local buy_body
        buy_body="$(jq -n --argjson id "$p_id" --argjson q 1 '{playerId: $id, quantity: $q}')"
        request POST "$API_BASE_URL/api/orders/buy" "$nicolas_token" "$buy_body"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          local tx_id
          tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
          echo "  ✔ [OFICIAL NICOLAS] Transacción #$tx_id: Nicolas compró 1 token de $p_name al superusuario por $(format_currency "$p_price")"
        fi
      fi
    fi
  fi

  # H. Recálculo dinámico de cotizaciones
  if [[ "$RECALCULATE_QUOTES" == "true" ]]; then
    echo
    echo "--- 7. Recálculo dinámico de cotizaciones (Moviendo precios) ---"
    if [[ -x "$SCRIPT_DIR/recalculate-quotes.sh" ]]; then
      "$SCRIPT_DIR/recalculate-quotes.sh"
    else
      echo "  [AVISO] No se encontró el ejecutable $SCRIPT_DIR/recalculate-quotes.sh"
    fi
  fi

  # I. Resumen del ciclo
  echo
  echo "--- 8. Estado actualizado de portfolios y mercado ---"
  request GET "$API_BASE_URL/api/auth/me" "$yoel_token"
  local yoel_cr=0
  if [[ "$HTTP_STATUS" == "200" ]]; then
    yoel_cr="$(jq -r '.availableCredits // 0' "$RESPONSE_BODY")"
  fi

  request GET "$API_BASE_URL/api/auth/me" "$nicolas_token"
  local nic_cr=0
  if [[ "$HTTP_STATUS" == "200" ]]; then
    nic_cr="$(jq -r '.availableCredits // 0' "$RESPONSE_BODY")"
  fi

  echo "  👤 Yoel (yoel@gmail.com): ₵${yoel_cr}.00 créditos disponibles"
  echo "  👤 Nicolas (nicolas@gmail.com): ₵${nic_cr}.00 créditos disponibles"
  echo "=============================================================================="
  echo
}

# 5. Control de ejecución según modo
if ! check_backend; then
  exit 1
fi

case "$MODE" in
  once)
    run_trading_cycle 1
    ;;
  rounds)
    for (( round=1; round<=TOTAL_ROUNDS; round++ )); do
      run_trading_cycle "$round"
      if (( round < TOTAL_ROUNDS )); then
        echo "Pausa de $INTERVAL_SECONDS segundos antes de la siguiente ronda..."
        sleep "$INTERVAL_SECONDS"
      fi
    done
    ;;
  loop)
    echo "Iniciando motor de trading continuo (cada $INTERVAL_SECONDS segundos). Presiona Ctrl+C para detener."
    round=1
    while true; do
      run_trading_cycle "$round"
      echo "Próximo ciclo en $INTERVAL_SECONDS segundos..."
      sleep "$INTERVAL_SECONDS"
      round=$(( round + 1 ))
    done
    ;;
esac
