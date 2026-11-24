#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# MOTOR AUTÓNOMO DE MERCADO CONTINUO (FOOTBALL MARKET ENGINE)
# =============================================================================
# Simula el ecosistema completo del mercado de fútbol en tiempo real:
#   1. Cuota objetivo: EXACTAMENTE 10 TRANSACCIONES CONFIRMADAS cada 3 minutos.
#   2. Compras primarias en Mercado Oficial al Superusuario (1 a 10 tokens aleatorios).
#   3. Diversificación táctica balanceada por Ligas y Posiciones (Donut Chart).
#   4. Generación de presión y volumen sobre "Hot Players" (Tendencias y Ticker).
#   5. Inyección robusta y permanente de ofertas en Mercado P2P (15+ órdenes activas).
#   6. Precios P2P dinámicos y flexibles (85%–115% de cotización con Best Deals).
#   7. Compras cruzadas P2P controladas entre bots y usuarios reales (Yoel & Nicolas).
#   8. Cancelaciones esporádicas de órdenes activas (Prueba y alimenta SPEC-030).
#   9. Liquidación inmediata oficial / Buyback con spread oficial del 10% (SPEC-026).
#  10. Recálculo dinámico de cotizaciones controlado (MÁXIMO 4 AL DÍA: 00, 06, 12, 18 UTC).
#  11. Creación natural de "P2P Best Deals" (arbitraje con descuento tras recálculo).
#  12. Auto-recuperación de liquidez e inventario para que nunca se quede sin fondos ni stock.
#  13. Impresión en vivo ultra-detallada (jugador, club, posición, precios, carteras).
#  14. Auto-rotación y protección de logs (< 10 MB).
#  15. Gestión de demonio en segundo plano (start / stop / status / restart).
#
# Comandos de servicio:
#   ./scripts/market-engine.sh start             # Inicia como demonio en segundo plano
#   ./scripts/market-engine.sh status            # Estado del motor, PID y métricas
#   ./scripts/market-engine.sh stop              # Detiene el demonio ordenadamente
#   ./scripts/market-engine.sh restart           # Reinicia el motor en segundo plano
#
# Modos de ejecución directa (interactivos):
#   ./scripts/market-engine.sh                   # Bucle interactivo (10 transacciones cada 180s)
#   ./scripts/market-engine.sh --once            # Ejecuta 1 ciclo de 10 transacciones y sale
#   ./scripts/market-engine.sh --inject-p2p [N]  # Inyecta N ofertas P2P de inmediato y sale
#   ./scripts/market-engine.sh --fast            # Modo demo rápido (10 transacciones cada 20s)
#   ./scripts/market-engine.sh --burst           # Ráfaga inicial inmediata + bucle normal
#   ./scripts/market-engine.sh --interval 60     # Intervalo personalizado en segundos
#   ./scripts/market-engine.sh -t 15             # Cambia el objetivo a 15 transacciones por ciclo
# =============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/market-engine.pid"
LOG_FILE="${MARKET_ENGINE_LOG:-$SCRIPT_DIR/market-engine.log}"
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-football-market-postgres}"

# Parámetros operativos predeterminados
MODE="loop"
INTERVAL_SECONDS=180          # 3 minutos por defecto
TARGET_TRADES_PER_CYCLE=10    # EXACTAMENTE 10 transacciones confirmadas por ciclo
BURST_INITIAL_ROUNDS=0
TOTAL_ROUNDS=0                # 0 = infinito en modo loop
MIN_OFFICIAL_QTY=1            # Mínimo 1 token en mercado oficial
MAX_OFFICIAL_QTY=10           # Máximo 10 tokens en mercado oficial (según requerimiento)
MIN_P2P_QTY=1
MAX_P2P_QTY=5
BOT_COUNT=8
WITH_REAL_USERS=true
RECALCULATE_QUOTES=true
FORCE_RECALC_EVERY_CYCLE=false
DAEMON_ACTION=""
TARGET_ACTIVE_P2P=16           # Mantener al menos 16 órdenes activas en el libro P2P
INJECT_P2P_COUNT=16            # Cantidad por defecto para --inject-p2p

# Métricas acumuladas y contadores globales
CYCLE_VOLUME=0
CYCLE_TRADES=0
COUNT_OFFICIAL_BUYS=0
COUNT_OFFICIAL_SELLS=0
COUNT_P2P_TRADES=0
COUNT_P2P_LISTINGS=0
COUNT_P2P_CANCELS=0

# Credenciales de usuarios reales
YOEL_EMAIL="${YOEL_EMAIL:-yoel@gmail.com}"
YOEL_PASSWORD="${YOEL_PASSWORD:-12345678}"
YOEL_USER_ID=2

NICOLAS_EMAIL="${NICOLAS_EMAIL:-nicolas@gmail.com}"
NICOLAS_PASSWORD="${NICOLAS_PASSWORD:-12345678}"
NICOLAS_USER_ID=3

# Credenciales de traders bot
BOT_PASSWORD="${BOT_PASSWORD:-EngineTrader2026!}"

# Paleta de colores para consola interactiva
if [[ -t 1 ]]; then
  CLR_RESET="\033[0m"
  CLR_BOLD="\033[1m"
  CLR_DIM="\033[2m"
  CLR_CYAN="\033[36m"
  CLR_GREEN="\033[32m"
  CLR_YELLOW="\033[33m"
  CLR_RED="\033[31m"
  CLR_MAGENTA="\033[35m"
  CLR_BLUE="\033[34m"
else
  CLR_RESET=""
  CLR_BOLD=""
  CLR_DIM=""
  CLR_CYAN=""
  CLR_GREEN=""
  CLR_YELLOW=""
  CLR_RED=""
  CLR_MAGENTA=""
  CLR_BLUE=""
fi

# -----------------------------------------------------------------------------
# Parseo de argumentos de línea de comandos y comandos de servicio
# -----------------------------------------------------------------------------
while [[ $# -gt 0 ]]; do
  case "$1" in
    start|stop|status|restart)
      DAEMON_ACTION="$1"
      shift
      ;;
    --once)
      MODE="once"
      shift
      ;;
    --inject-p2p)
      MODE="inject_p2p"
      if [[ -n "${2:-}" && "$2" =~ ^[0-9]+$ ]]; then
        INJECT_P2P_COUNT="$2"
        shift 2
      else
        INJECT_P2P_COUNT=16
        shift
      fi
      ;;
    --loop)
      MODE="loop"
      shift
      ;;
    --fast|--demo)
      INTERVAL_SECONDS=20
      shift
      ;;
    --burst)
      BURST_INITIAL_ROUNDS=3
      shift
      ;;
    --interval)
      INTERVAL_SECONDS="$2"
      shift 2
      ;;
    -t|--trades-per-cycle)
      TARGET_TRADES_PER_CYCLE="$2"
      shift 2
      ;;
    --rounds)
      MODE="rounds"
      TOTAL_ROUNDS="$2"
      shift 2
      ;;
    --min-official-qty)
      MIN_OFFICIAL_QTY="$2"
      shift 2
      ;;
    --max-official-qty)
      MAX_OFFICIAL_QTY="$2"
      shift 2
      ;;
    --target-p2p)
      TARGET_ACTIVE_P2P="$2"
      shift 2
      ;;
    --pool-size)
      BOT_COUNT="$2"
      shift 2
      ;;
    --only-bots)
      WITH_REAL_USERS=false
      shift
      ;;
    --with-real-users)
      WITH_REAL_USERS=true
      shift
      ;;
    --no-recalc)
      RECALCULATE_QUOTES=false
      shift
      ;;
    --with-recalc)
      RECALCULATE_QUOTES=true
      shift
      ;;
    --force-recalc)
      FORCE_RECALC_EVERY_CYCLE=true
      shift
      ;;
    -h|--help)
      echo -e "${CLR_BOLD}Uso de Football Market Engine:${CLR_RESET}"
      echo "  $0 [start | stop | status | restart]"
      echo "  $0 [--once | --loop | --fast | --burst] [--interval SEG] [--rounds N]"
      echo "  $0 [--inject-p2p [N]]                        # Inyecta N ofertas P2P de inmediato y sale"
      echo "  $0 [-t 10 | --trades-per-cycle 10] [--min-official-qty 1] [--max-official-qty 10]"
      echo "  $0 [--target-p2p 16] [--pool-size 8] [--only-bots | --with-real-users]"
      exit 0
      ;;
    *)
      echo "Opción o comando desconocido: $1" >&2
      exit 1
      ;;
  esac
done

# -----------------------------------------------------------------------------
# Gestión de comandos de servicio (start, stop, status, restart)
# -----------------------------------------------------------------------------
get_daemon_pid() {
  if [[ -f "$PID_FILE" ]]; then
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null || echo "")"
    if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
      echo "$pid"
      return 0
    fi
  fi
  return 1
}

if [[ -n "$DAEMON_ACTION" ]]; then
  case "$DAEMON_ACTION" in
    start)
      if pid=$(get_daemon_pid); then
        echo -e "${CLR_YELLOW}El motor ya está corriendo en segundo plano (PID: $pid).${CLR_RESET}"
        exit 0
      fi
      echo -e "${CLR_CYAN}Iniciando Market Engine en segundo plano ($TARGET_TRADES_PER_CYCLE transacciones cada ${INTERVAL_SECONDS}s)...${CLR_RESET}"
      nohup "$0" --loop --interval "$INTERVAL_SECONDS" -t "$TARGET_TRADES_PER_CYCLE" >> "$LOG_FILE" 2>&1 &
      NEW_PID=$!
      echo "$NEW_PID" > "$PID_FILE"
      sleep 1
      if kill -0 "$NEW_PID" 2>/dev/null; then
        echo -e "${CLR_GREEN}✔ Market Engine iniciado exitosamente con PID $NEW_PID.${CLR_RESET}"
        echo "  - Objetivo: $TARGET_TRADES_PER_CYCLE transacciones cada ${INTERVAL_SECONDS} segundos."
        echo "  - Registro de actividad: $LOG_FILE"
        echo "  - Monitorea con: $0 status"
      else
        echo -e "${CLR_RED}✖ Falló el arranque en segundo plano. Revisa $LOG_FILE.${CLR_RESET}" >&2
        rm -f "$PID_FILE"
        exit 1
      fi
      exit 0
      ;;
    stop)
      if pid=$(get_daemon_pid); then
        echo -e "${CLR_YELLOW}Deteniendo Market Engine (PID: $pid)...${CLR_RESET}"
        kill "$pid" 2>/dev/null || true
        for i in {1..10}; do
          if ! kill -0 "$pid" 2>/dev/null; then break; fi
          sleep 0.5
        done
        kill -9 "$pid" 2>/dev/null || true
        rm -f "$PID_FILE"
        echo -e "${CLR_GREEN}✔ Market Engine detenido.${CLR_RESET}"
      else
        echo "Market Engine no se encuentra en ejecución."
        rm -f "$PID_FILE"
      fi
      exit 0
      ;;
    restart)
      "$0" stop
      sleep 1
      "$0" start
      exit 0
      ;;
    status)
      echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
      echo -e "${CLR_BOLD}               ESTADO DEL MOTOR DE MERCADO (MARKET ENGINE)                    ${CLR_RESET}"
      echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
      if pid=$(get_daemon_pid); then
        echo -e "  Estado: ${CLR_GREEN}ACTIVO (Corriendo en segundo plano)${CLR_RESET}"
        echo -e "  PID: ${CLR_BOLD}$pid${CLR_RESET}"
      else
        echo -e "  Estado: ${CLR_DIM}INACTIVO (No está corriendo en segundo plano)${CLR_RESET}"
      fi
      echo -e "  Cuota objetivo: ${CLR_BOLD}$TARGET_TRADES_PER_CYCLE transacciones por ciclo${CLR_RESET}"
      echo -e "  Log: $LOG_FILE"
      if [[ -f "$LOG_FILE" ]]; then
        echo -e "\n${CLR_CYAN}Últimas 12 líneas de actividad:${CLR_RESET}"
        tail -n 12 "$LOG_FILE" | sed 's/^/    /'
      fi
      echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
      exit 0
      ;;
  esac
fi

# -----------------------------------------------------------------------------
# Inicialización y validaciones del entorno interactivo
# -----------------------------------------------------------------------------
for dep in curl jq awk; do
  if ! command -v "$dep" >/dev/null 2>&1; then
    echo "Error: $dep es obligatorio para ejecutar este script." >&2
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
  awk -v c="$cents" 'BEGIN {
    if (c < 0) {
      printf "-₵%.2f", (-c)/100
    } else {
      printf "₵%.2f", c/100
    }
  }'
}

rotate_logs_if_needed() {
  if [[ -f "$LOG_FILE" ]]; then
    local size_bytes
    size_bytes=$(stat -c%s "$LOG_FILE" 2>/dev/null || echo 0)
    if (( size_bytes > 10485760 )); then # > 10 MB
      mv "$LOG_FILE" "${LOG_FILE}.$(date +%Y%m%d%H%M).bak"
      touch "$LOG_FILE"
      echo "[LOG ROTATION] Log rotado exitosamente tras superar 10 MB."
    fi
  fi
}

check_backend_ready() {
  request GET "$API_BASE_URL/actuator/health"
  [[ "$HTTP_STATUS" == "200" ]]
}

wait_for_backend() {
  while ! check_backend_ready; do
    echo -e "${CLR_YELLOW}[ESPERA] Backend no disponible en $API_BASE_URL (HTTP $HTTP_STATUS). Reintentando en 5s...${CLR_RESET}"
    sleep 5
  done
}

# -----------------------------------------------------------------------------
# Caché en memoria de Jugadores y Usuarios para salida enriquecida en vivo
# -----------------------------------------------------------------------------
declare -A PLAYER_NAMES=()
declare -A PLAYER_TEAMS=()
declare -A PLAYER_POSITIONS=()
declare -A USER_NAMES=(
  [1]="Superusuario (Casa)"
  [2]="Yoel (yoel@gmail.com)"
  [3]="Nicolas (nicolas@gmail.com)"
)

load_cache_data() {
  if docker ps --filter "name=$DB_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$DB_CONTAINER"; then
    # Cargar catálogo de jugadores desde PostgreSQL
    while IFS='|' read -r p_id p_name p_team p_pos; do
      [[ -z "$p_id" ]] && continue
      PLAYER_NAMES["$p_id"]="$p_name"
      PLAYER_TEAMS["$p_id"]="${p_team:-Club Independiente}"
      PLAYER_POSITIONS["$p_id"]="${p_pos:-Midfield}"
    done < <(docker exec -i "$DB_CONTAINER" psql -U postgres -d football_market_dev -t -A -F'|' -c "
      SELECT p.id, p.name, COALESCE(t.name, 'Club Independiente'), p.position 
      FROM players p 
      LEFT JOIN teams t ON t.id = p.team_id;
    " 2>/dev/null || true)

    # Cargar usuarios registrados desde PostgreSQL
    while IFS='|' read -r u_id u_name u_email; do
      [[ -z "$u_id" ]] && continue
      if [[ "$u_id" == "1" ]]; then
        USER_NAMES["$u_id"]="Superusuario (Casa)"
      elif [[ "$u_id" == "2" ]]; then
        USER_NAMES["$u_id"]="Yoel (yoel@gmail.com)"
      elif [[ "$u_id" == "3" ]]; then
        USER_NAMES["$u_id"]="Nicolas (nicolas@gmail.com)"
      elif [[ "$u_name" =~ ^eng_bot_([0-9]+)$ ]]; then
        USER_NAMES["$u_id"]="Bot #${BASH_REMATCH[1]}"
      else
        USER_NAMES["$u_id"]="$u_name"
      fi
    done < <(docker exec -i "$DB_CONTAINER" psql -U postgres -d football_market_dev -t -A -F'|' -c "
      SELECT id, username, email FROM users;
    " 2>/dev/null || true)
  fi
}

get_player_details() {
  local p_id="$1"
  local token="${2:-}"

  if [[ -n "${PLAYER_NAMES[$p_id]:-}" ]]; then
    echo "${PLAYER_NAMES[$p_id]}|${PLAYER_TEAMS[$p_id]}|${PLAYER_POSITIONS[$p_id]}"
    return 0
  fi

  # Fallback mediante llamada a API
  if [[ -n "$token" ]]; then
    request GET "$API_BASE_URL/api/players/$p_id" "$token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local name team pos
      name="$(jq -r '.name // empty' "$RESPONSE_BODY")"
      team="$(jq -r '.teamName // "Club Independiente"' "$RESPONSE_BODY")"
      pos="$(jq -r '.position // "Midfield"' "$RESPONSE_BODY")"
      if [[ -n "$name" ]]; then
        PLAYER_NAMES["$p_id"]="$name"
        PLAYER_TEAMS["$p_id"]="$team"
        PLAYER_POSITIONS["$p_id"]="$pos"
        echo "$name|$team|$pos"
        return 0
      fi
    fi
  fi
  echo "Jugador #$p_id|Club Independiente|Posición"
}

get_user_label() {
  local u_id="$1"
  if [[ -n "${USER_NAMES[$u_id]:-}" ]]; then
    echo "${USER_NAMES[$u_id]}"
  else
    echo "Usuario #$u_id"
  fi
}

# -----------------------------------------------------------------------------
# Autenticación y gestión de usuarios / bots
# -----------------------------------------------------------------------------
login_user() {
  local email="$1"
  local password="$2"
  local body
  body="$(jq -n --arg e "$email" --arg p "$password" '{email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$body"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    jq -er '.token' "$RESPONSE_BODY"
  else
    echo ""
  fi
}

register_or_login() {
  local username="$1"
  local email="$2"
  local password="$3"

  local reg_body
  reg_body="$(jq -n --arg u "$username" --arg e "$email" --arg p "$password" \
    '{username: $u, email: $e, password: $p}')"
  request POST "$API_BASE_URL/api/auth/register" '' "$reg_body"

  local login_body
  login_body="$(jq -n --arg email "$email" --arg password "$password" \
    '{email: $email, password: $password}')"
  request POST "$API_BASE_URL/api/auth/login" '' "$login_body"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    jq -er '.token' "$RESPONSE_BODY"
  else
    echo ""
  fi
}

get_or_create_bot() {
  local idx="$1"
  local username="eng_bot_${idx}"
  local email="eng_bot_${idx}@example.test"
  register_or_login "$username" "$email" "$BOT_PASSWORD"
}

# Auto-recuperación de liquidez dinámica
ensure_liquidity() {
  local token="$1"
  local email="$2"

  request GET "$API_BASE_URL/api/auth/me" "$token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local user_id credits
    user_id="$(jq -r '.id // empty' "$RESPONSE_BODY")"
    credits="$(jq -r '.availableCredits // 0' "$RESPONSE_BODY")"
    if (( credits < 20 )) && [[ -n "$user_id" ]]; then # < 20 créditos
      if docker ps --filter "name=$DB_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$DB_CONTAINER"; then
        docker exec -i "$DB_CONTAINER" psql -U postgres -d football_market_dev -c "
          INSERT INTO user_credits (user_id, available_credits, updated_at)
          VALUES ($user_id, 25000, NOW())
          ON CONFLICT (user_id)
          DO UPDATE SET available_credits = user_credits.available_credits + 25000, updated_at = NOW();
        " >/dev/null 2>&1 || true
        echo -e "  ${CLR_CYAN}💧 [LIQUIDEZ] Inyectados +₵250.00 créditos de soporte para $email (ID: $user_id).${CLR_RESET}"
      fi
    fi
  fi
}

# Abastecimiento proactivo de inventario en cartera de bots para alimentar oferta P2P continua
ensure_bot_inventory() {
  local bot_token="$1"
  local bot_name="$2"
  local min_positions="${3:-3}"

  request GET "$API_BASE_URL/api/portfolio/me" "$bot_token"
  local avail_count=0
  if [[ "$HTTP_STATUS" == "200" ]]; then
    avail_count="$(jq '[.positions[]? | select(.availableQuantity > 0)] | length' "$RESPONSE_BODY")"
  fi

  if (( avail_count < min_positions )); then
    local needed=$(( min_positions - avail_count ))
    for (( b=0; b<needed; b++ )); do
      local page=$(( (RANDOM % 15) + 1 ))
      declare -a POS_LIST=("Offence" "Midfield" "Defence" "Goalkeeper")
      local target_pos="${POS_LIST[$(( (RANDOM + b) % 4 ))]}"
      request GET "$API_BASE_URL/api/market/official?page=$page&size=10&hasQuote=true&position=$target_pos" "$bot_token"
      if [[ "$HTTP_STATUS" == "200" ]]; then
        local c_len
        c_len="$(jq '.content | length' "$RESPONSE_BODY" 2>/dev/null || echo 0)"
        if (( c_len > 0 )); then
          local rand_idx=$(( RANDOM % c_len ))
          local candidate
          candidate="$(jq -c ".content[$rand_idx] // empty" "$RESPONSE_BODY")"
          if [[ -n "$candidate" ]]; then
            local p_id p_price
            p_id="$(jq -r '.id' <<<"$candidate")"
            p_price="$(jq -r '.quote.price // 100' <<<"$candidate")"
            local buy_qty=$(( (RANDOM % 4) + 3 )) # compra 3 a 6 tokens para tener stock
            local buy_body
            buy_body="$(jq -n --argjson id "$p_id" --argjson q "$buy_qty" '{playerId: $id, quantity: $q}')"
            request POST "$API_BASE_URL/api/orders/buy" "$bot_token" "$buy_body" >/dev/null 2>&1 || true
          fi
        fi
      fi
    done
  fi
}

# -----------------------------------------------------------------------------
# Fases de Operación del Mercado
# -----------------------------------------------------------------------------

# 1. Compras Primarias en Mercado Oficial (1 a 10 tokens aleatorios con diversidad táctica)
operate_official_buys() {
  local buyer_token="$1"
  local buyer_name="$2"
  local round_num="$3"

  # Diversificación por posición para enriquecer el Donut Chart del Dashboard
  declare -a POSITIONS=("Offence" "Midfield" "Defence" "Goalkeeper")
  local target_position="${POSITIONS[$(( round_num % 4 ))]}"

  # Lista de "Hot Players" estrella (30% de probabilidad de compra concentrada)
  declare -a HOT_STARS=("Yamal" "Mbappé" "Haaland" "Kane" "Vinicius" "Salah" "Bellingham" "Pedri" "Musiala")

  local catalog_query
  if (( (RANDOM % 100) < 30 )); then
    local star_name="${HOT_STARS[$(( RANDOM % ${#HOT_STARS[@]} ))]}"
    catalog_query="size=5&hasQuote=true&search=$star_name"
  else
    catalog_query="size=5&hasQuote=true&position=$target_position&page=$(( RANDOM % 4 ))"
  fi

  request GET "$API_BASE_URL/api/market/official?$catalog_query" "$buyer_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local candidate
    candidate="$(jq -c '.content[0] // empty' "$RESPONSE_BODY")"
    if [[ -n "$candidate" ]]; then
      local p_id p_name p_pos p_price p_team
      p_id="$(jq -r '.id' <<<"$candidate")"
      p_name="$(jq -r '.name' <<<"$candidate")"
      p_pos="$(jq -r '.position' <<<"$candidate")"
      p_price="$(jq -r '.quote.price // 100' <<<"$candidate")"
      p_team="$(jq -r '.teamName // empty' <<<"$candidate")"
      if [[ -z "$p_team" || "$p_team" == "null" ]]; then
        p_team="${PLAYER_TEAMS[$p_id]:-Club Independiente}"
      fi

      # Cantidad aleatoria solicitada estrictamente entre 1 y 10 tokens
      local qty=$(( (RANDOM % (MAX_OFFICIAL_QTY - MIN_OFFICIAL_QTY + 1)) + MIN_OFFICIAL_QTY ))

      # Verificar que el comprador disponga de saldo suficiente para la cantidad
      request GET "$API_BASE_URL/api/auth/me" "$buyer_token"
      local available_cents=10000
      if [[ "$HTTP_STATUS" == "200" ]]; then
        local user_credits_val
        user_credits_val="$(jq -r '.availableCredits // 100' "$RESPONSE_BODY")"
        available_cents=$(( user_credits_val * 100 ))
      fi

      local total_cost=$(( p_price * qty ))
      if (( total_cost > available_cents )); then
        qty=$(( available_cents / p_price ))
      fi

      if (( qty >= 1 )); then
        local buy_body
        buy_body="$(jq -n --argjson id "$p_id" --argjson q "$qty" '{playerId: $id, quantity: $q}')"
        request POST "$API_BASE_URL/api/orders/buy" "$buyer_token" "$buy_body"

        if [[ "$HTTP_STATUS" == "200" ]]; then
          local tx_id total_spent
          tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
          total_spent="$(jq -r '.totalAmount // 0' "$RESPONSE_BODY")"
          local remaining_cents=$(( available_cents - total_spent ))
          if (( remaining_cents < 0 )); then remaining_cents=0; fi

          echo -e "  ${CLR_GREEN}✔ [OFICIAL COMPRA]${CLR_RESET} Transacción #${CLR_BOLD}$tx_id${CLR_RESET}: $buyer_name compró ${CLR_BOLD}$qty token(s)${CLR_RESET} de ${CLR_BOLD}$p_name${CLR_RESET} ($p_team | $p_pos)"
          echo -e "      ↳ Vendedor: Superusuario (Casa) | Cotización: $(format_currency "$p_price") c/u | Total: ${CLR_BOLD}$(format_currency "$total_spent")${CLR_RESET} | Saldo post-compra: $(format_currency "$remaining_cents")"
          CYCLE_VOLUME=$(( CYCLE_VOLUME + total_spent ))
          CYCLE_TRADES=$(( CYCLE_TRADES + 1 ))
          COUNT_OFFICIAL_BUYS=$(( COUNT_OFFICIAL_BUYS + 1 ))
          return 0
        fi
      fi
    fi
  fi
  return 1
}

# 2. Publicación de Ventas en Mercado P2P (Cantidades aleatorias y PRECIOS FLEXIBLES - SPEC-026)
operate_p2p_sell() {
  local seller_token="$1"
  local seller_name="$2"
  local max_orders="${3:-1}"
  local published_count=0

  request GET "$API_BASE_URL/api/portfolio/me" "$seller_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local candidates
    candidates="$(jq -r '.positions[]? | select(.availableQuantity > 0) | "\(.playerId)|\(.playerName)|\(.availableQuantity)|\(.currentUnitPrice)"' "$RESPONSE_BODY")"

    if [[ -n "$candidates" ]]; then
      while IFS='|' read -r p_id p_name p_avail p_price && (( published_count < max_orders )); do
        [[ -z "$p_id" ]] && continue

        local p_details
        p_details="$(get_player_details "$p_id" "$seller_token")"
        IFS='|' read -r full_p_name p_team p_pos <<<"$p_details"
        [[ -n "$full_p_name" && "$full_p_name" != "Jugador #$p_id" ]] || full_p_name="$p_name"

        # Cantidad aleatoria a poner a la venta (entre 1 y min(p_avail, MAX_P2P_QTY))
        local max_sell=$p_avail
        if (( max_sell > MAX_P2P_QTY )); then max_sell=$MAX_P2P_QTY; fi
        local sell_qty=$(( (RANDOM % max_sell) + 1 ))

        # Margen flexible de precio P2P: entre 85% y 115% de la cotización oficial vigente
        local variation_pct=$(( 85 + (RANDOM % 31) ))
        local flexible_price=$(( (p_price * variation_pct) / 100 ))
        if (( flexible_price < 1 )); then flexible_price=1; fi

        local sell_body
        sell_body="$(jq -n --argjson id "$p_id" --argjson q "$sell_qty" --argjson u "$flexible_price" \
          '{playerId: $id, quantity: $q, unitPrice: $u}')"
        request POST "$API_BASE_URL/api/orders/sell" "$seller_token" "$sell_body"

        # Fallback transparente si el backend aún no fue reiniciado con el campo unitPrice
        if [[ "$HTTP_STATUS" != "201" ]]; then
          sell_body="$(jq -n --argjson id "$p_id" --argjson q "$sell_qty" '{playerId: $id, quantity: $q}')"
          request POST "$API_BASE_URL/api/orders/sell" "$seller_token" "$sell_body"
          flexible_price="$p_price"
        fi

        if [[ "$HTTP_STATUS" == "201" ]]; then
          local new_listing_id
          new_listing_id="$(jq -r '.listingId' "$RESPONSE_BODY")"
          local diff_label=""
          if (( flexible_price < p_price )); then
            local disc_pct
            disc_pct="$(awk -v o="$p_price" -v f="$flexible_price" 'BEGIN{printf "%.0f%%", (1 - f/o)*100}')"
            diff_label=" ${CLR_GREEN}[⭐ Oferta Best Deal: -$disc_pct]${CLR_RESET}"
          elif (( flexible_price > p_price )); then
            local mark_pct
            mark_pct="$(awk -v o="$p_price" -v f="$flexible_price" 'BEGIN{printf "%.0f%%", (f/o - 1)*100}')"
            diff_label=" ${CLR_YELLOW}[+${mark_pct} Sobreprecio]${CLR_RESET}"
          else
            diff_label=" ${CLR_DIM}[Al valor oficial]${CLR_RESET}"
          fi
          local lot_total=$(( sell_qty * flexible_price ))
          echo -e "  ${CLR_CYAN}✔ [P2P PUBLICACIÓN]${CLR_RESET} Listing #${CLR_BOLD}$new_listing_id${CLR_RESET}: $seller_name puso a la venta ${CLR_BOLD}$sell_qty token(s)${CLR_RESET} de ${CLR_BOLD}$full_p_name${CLR_RESET} ($p_team | $p_pos)"
          echo -e "      ↳ Precio: ${CLR_BOLD}$(format_currency "$flexible_price")${CLR_RESET} c/u (Cotiz base: $(format_currency "$p_price"))$diff_label | Lote total: $(format_currency "$lot_total")"
          COUNT_P2P_LISTINGS=$(( COUNT_P2P_LISTINGS + 1 ))
          published_count=$(( published_count + 1 ))
        fi
      done < <(shuf <<<"$candidates")
    fi
  fi
  (( published_count > 0 ))
}

# 3. Liquidación Inmediata Oficial / Buyback a la Casa (SPEC-026 / PR #111)
operate_official_sell() {
  local seller_token="$1"
  local seller_name="$2"

  request GET "$API_BASE_URL/api/portfolio/me" "$seller_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local candidates
    candidates="$(jq -r '.positions[]? | select(.availableQuantity > 0) | "\(.playerId)|\(.playerName)|\(.availableQuantity)|\(.currentUnitPrice)"' "$RESPONSE_BODY")"

    if [[ -n "$candidates" ]]; then
      local chosen_line
      chosen_line="$(shuf -n 1 <<<"$candidates")"
      IFS='|' read -r p_id p_name p_avail p_price <<<"$chosen_line"

      local p_details
      p_details="$(get_player_details "$p_id" "$seller_token")"
      IFS='|' read -r full_p_name p_team p_pos <<<"$p_details"
      [[ -n "$full_p_name" && "$full_p_name" != "Jugador #$p_id" ]] || full_p_name="$p_name"

      # Liquidar entre 1 y min(p_avail, 3) tokens al superusuario
      local max_sell=$p_avail
      if (( max_sell > 3 )); then max_sell=3; fi
      local sell_qty=$(( (RANDOM % max_sell) + 1 ))

      local buyback_body
      buyback_body="$(jq -n --argjson id "$p_id" --argjson q "$sell_qty" '{playerId: $id, quantity: $q}')"
      request POST "$API_BASE_URL/api/orders/sell-official" "$seller_token" "$buyback_body"

      if [[ "$HTTP_STATUS" == "200" ]]; then
        local tx_id total_amount disc_pct net_unit
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        total_amount="$(jq -r '.totalAmount // 0' "$RESPONSE_BODY")"
        disc_pct="$(jq -r '.discountPercentage // 10' "$RESPONSE_BODY")"
        net_unit="$(jq -r '.unitPrice // 0' "$RESPONSE_BODY")"
        echo -e "  ${CLR_YELLOW}✔ [LIQUIDACIÓN OFICIAL]${CLR_RESET} Transacción #${CLR_BOLD}$tx_id${CLR_RESET}: $seller_name liquidó ${CLR_BOLD}$sell_qty token(s)${CLR_RESET} de ${CLR_BOLD}$full_p_name${CLR_RESET} ($p_team | $p_pos)"
        echo -e "      ↳ Comprador: Superusuario (Casa) | Cotización: $(format_currency "$p_price") | Spread casa: -${disc_pct}% | Neto: $(format_currency "$net_unit") c/u | Total cobrado: ${CLR_BOLD}$(format_currency "$total_amount")${CLR_RESET}"
        CYCLE_VOLUME=$(( CYCLE_VOLUME + total_amount ))
        CYCLE_TRADES=$(( CYCLE_TRADES + 1 ))
        COUNT_OFFICIAL_SELLS=$(( COUNT_OFFICIAL_SELLS + 1 ))
        return 0
      elif [[ "$HTTP_STATUS" == "404" ]]; then
        return 1
      fi
    fi
  fi
  return 1
}

# 4. Compras Cruzadas en Mercado P2P (Liquidación orgánica y rotación de inventario)
operate_p2p_buy() {
  local buyer_token="$1"
  local buyer_name="$2"
  local buyer_id="$3"

  request GET "$API_BASE_URL/api/listings" "$buyer_token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local eligible_listings
    eligible_listings="$(jq -r --argjson myId "$buyer_id" \
      '.listings[]? | select(.status == "ACTIVE" and .sellerId != $myId) | "\(.listingId)|\(.sellerId)|\(.playerId)|\(.quantity)|\(.unitPrice)|\(.totalAmount)"' "$RESPONSE_BODY")"

    if [[ -n "$eligible_listings" ]]; then
      local target_line
      target_line="$(shuf -n 1 <<<"$eligible_listings")"
      IFS='|' read -r l_id s_id p_id l_qty u_price t_amount <<<"$target_line"

      if [[ -z "$u_price" || "$u_price" == "null" || "$u_price" == "0" ]]; then
        if (( l_qty > 0 )); then
          u_price=$(( t_amount / l_qty ))
        else
          u_price="$t_amount"
        fi
      fi

      local p_details
      p_details="$(get_player_details "$p_id" "$buyer_token")"
      IFS='|' read -r full_p_name p_team p_pos <<<"$p_details"

      local seller_label
      seller_label="$(get_user_label "$s_id")"

      local buy_body
      buy_body="$(jq -n --argjson id "$l_id" '{listingId: $id}')"
      request POST "$API_BASE_URL/api/orders/buy" "$buyer_token" "$buy_body"

      if [[ "$HTTP_STATUS" == "200" ]]; then
        local tx_id
        tx_id="$(jq -r '.transactionId // empty' "$RESPONSE_BODY")"
        echo -e "  ${CLR_GREEN}✔ [P2P COMPRA]${CLR_RESET} Transacción #${CLR_BOLD}$tx_id${CLR_RESET}: $buyer_name adquirió Listing #${CLR_BOLD}$l_id${CLR_RESET} de ${CLR_BOLD}$seller_label${CLR_RESET}"
        echo -e "      ↳ Futbolista: ${CLR_BOLD}$full_p_name${CLR_RESET} ($p_team | $p_pos) | Cantidad: ${CLR_BOLD}$l_qty token(s)${CLR_RESET} a $(format_currency "$u_price") c/u | Total invertido: ${CLR_BOLD}$(format_currency "$t_amount")${CLR_RESET}"
        CYCLE_VOLUME=$(( CYCLE_VOLUME + t_amount ))
        CYCLE_TRADES=$(( CYCLE_TRADES + 1 ))
        COUNT_P2P_TRADES=$(( COUNT_P2P_TRADES + 1 ))
        return 0
      fi
    fi
  fi
  return 1
}

# 5. Cancelaciones Esporádicas en Mercado P2P (Puebla y alimenta SPEC-030 Mis Órdenes)
operate_p2p_cancellations() {
  local seller_token="$1"
  local seller_name="$2"

  if (( (RANDOM % 100) < 15 )); then
    request GET "$API_BASE_URL/api/listings/mine?status=ACTIVE" "$seller_token"
    if [[ "$HTTP_STATUS" == "200" ]]; then
      local first_listing
      first_listing="$(jq -c '.listings[0] // empty' "$RESPONSE_BODY")"
      if [[ -n "$first_listing" ]]; then
        local first_active_id p_id c_qty c_price
        first_active_id="$(jq -r '.listingId' <<<"$first_listing")"
        p_id="$(jq -r '.playerId' <<<"$first_listing")"
        c_qty="$(jq -r '.quantity' <<<"$first_listing")"
        c_price="$(jq -r '.unitPrice // .totalAmount' <<<"$first_listing")"

        local p_details
        p_details="$(get_player_details "$p_id" "$seller_token")"
        IFS='|' read -r full_p_name p_team p_pos <<<"$p_details"

        request POST "$API_BASE_URL/api/listings/$first_active_id/cancel" "$seller_token"
        if [[ "$HTTP_STATUS" == "200" ]]; then
          echo -e "  ${CLR_YELLOW}○ [P2P CANCELACIÓN]${CLR_RESET} $seller_name retiró su orden Listing #${CLR_BOLD}$first_active_id${CLR_RESET}: $c_qty token(s) de $full_p_name ($p_team) a $(format_currency "$c_price") c/u"
          COUNT_P2P_CANCELS=$(( COUNT_P2P_CANCELS + 1 ))
          return 0
        fi
      fi
    fi
  fi
  return 1
}

# 6. Recálculo de Cotizaciones Bursátiles (Control estricto: MÁXIMO 4 AL DÍA)
operate_quote_recalculation() {
  if [[ "$RECALCULATE_QUOTES" != "true" ]]; then
    return 0
  fi

  echo
  echo -e "${CLR_CYAN}--- 7. COTIZACIONES BURSÁTILES (POLÍTICA: MÁXIMO 4 AL DÍA) ---${CLR_RESET}"

  if [[ "$FORCE_RECALC_EVERY_CYCLE" == "true" ]]; then
    echo -e "  ${CLR_MAGENTA}📈 [COTIZACIONES (FORZADO)] Disparando recálculo dinámico sobre 1.861 futbolistas...${CLR_RESET}"
    if [[ -x "$SCRIPT_DIR/recalculate-quotes.sh" ]]; then
      "$SCRIPT_DIR/recalculate-quotes.sh" | sed 's/^/    /'
    fi
  else
    if [[ -x "$SCRIPT_DIR/run-quote-scheduler.sh" ]]; then
      local sched_out
      sched_out="$("$SCRIPT_DIR/run-quote-scheduler.sh" --once 2>&1 || true)"
      if grep -q "RECUPERACIÓN\|Recálculo finalizado exitosamente" <<<"$sched_out"; then
        echo -e "  ${CLR_MAGENTA}📈 [COTIZACIONES (NUEVA FRANJA)] Recálculo obligatorio ejecutado:${CLR_RESET}"
        grep -E "Clima bursátil|Estrategia|ID Ejecución|Fecha de cotización|Jugadores cotizados" <<<"$sched_out" | sed 's/^/    /' || true
      elif grep -q "AL DÍA" <<<"$sched_out"; then
        local al_dia_info
        al_dia_info="$(grep "AL DÍA" <<<"$sched_out" | head -n 1 | sed 's/.*\[AL DÍA\]/[AL DÍA]/')"
        echo -e "  ${CLR_DIM}📈 [COTIZACIONES] $al_dia_info (Límite: máx 4 al día respetado)${CLR_RESET}"
      else
        echo -e "  ${CLR_DIM}📈 [COTIZACIONES] Verificación completada (cotizaciones dentro de su franja horaria).${CLR_RESET}"
      fi
    fi
  fi
}

# 7. Mantenimiento y saneamiento de listings expirados
operate_maintenance() {
  if docker ps --filter "name=$DB_CONTAINER" --filter "status=running" --format "{{.Names}}" | grep -q "$DB_CONTAINER"; then
    local cleaned
    cleaned="$(docker exec -i "$DB_CONTAINER" psql -U postgres -d football_market_dev -t -A -c "
      WITH updated AS (
        UPDATE token_listings
        SET status = 'CANCELLED', closed_at = NOW()
        WHERE status = 'ACTIVE' AND expires_at <= NOW()
        RETURNING id
      )
      SELECT COUNT(*) FROM updated;
    " 2>/dev/null || echo 0)"
    if (( cleaned > 0 )); then
      echo -e "  ${CLR_DIM}🧹 [LIMPIEZA] Cerradas $cleaned publicaciones P2P vencidas.${CLR_RESET}"
    fi
  fi
  rotate_logs_if_needed
}

# 8. Impresión de tarjeta de cartera de usuario detallada
print_user_portfolio_card() {
  local user_label="$1"
  local email="$2"
  local token="$3"

  request GET "$API_BASE_URL/api/auth/me" "$token"
  local credits="0.00"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    credits="$(jq -r '.availableCredits // 0' "$RESPONSE_BODY")"
  fi

  request GET "$API_BASE_URL/api/portfolio/me" "$token"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    local pos_count total_tok avail_tok res_tok cur_val invested pnl pnl_pct
    pos_count="$(jq -r '.summary.totalPositions // 0' "$RESPONSE_BODY")"
    total_tok="$(jq -r '.summary.totalTokens // 0' "$RESPONSE_BODY")"
    avail_tok="$(jq -r '.summary.totalAvailableTokens // 0' "$RESPONSE_BODY")"
    res_tok="$(jq -r '.summary.totalReservedTokens // 0' "$RESPONSE_BODY")"
    cur_val="$(jq -r '.summary.totalCurrentValue // 0' "$RESPONSE_BODY")"
    invested="$(jq -r '.summary.totalInvested // 0' "$RESPONSE_BODY")"
    pnl="$(jq -r '.summary.totalProfitLoss // 0' "$RESPONSE_BODY")"
    pnl_pct="$(jq -r '.summary.totalProfitLossPercentage // 0' "$RESPONSE_BODY")"

    local pnl_color="$CLR_GREEN"
    local pnl_sign="+"
    if (( pnl < 0 )); then
      pnl_color="$CLR_RED"
      pnl_sign=""
    fi

    echo -e "  ┌── 👤 ${CLR_BOLD}PORTAFOLIO EN VIVO: $user_label ($email)${CLR_RESET}"
    echo -e "  │  • Saldo disponible:    ${CLR_BOLD}₵${credits}.00 créditos${CLR_RESET}"
    echo -e "  │  • Tokens en posesión:   ${CLR_BOLD}$total_tok tokens${CLR_RESET} ($avail_tok disponibles / $res_tok en venta P2P)"
    echo -e "  │  • Posiciones activas:   ${CLR_BOLD}$pos_count futbolistas${CLR_RESET}"
    echo -e "  │  • Valorización cartera: ${CLR_BOLD}$(format_currency "$cur_val")${CLR_RESET} (Inversión base: $(format_currency "$invested"))"
    echo -e "  │  • Rendimiento neto:     ${pnl_color}${CLR_BOLD}${pnl_sign}$(format_currency "$pnl") (${pnl_sign}${pnl_pct}%)${CLR_RESET}"
    echo -e "  └──────────────────────────────────────────────────────────────────────────"
  fi
}

# -----------------------------------------------------------------------------
# Inyección Directa de Lotes P2P (--inject-p2p)
# -----------------------------------------------------------------------------
inject_p2p_batch() {
  local target_qty="${1:-16}"
  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
  echo -e "${CLR_BOLD}🚀 INYECTANDO $target_qty PUBLICACIONES DE VENTA EN EL MERCADO P2P...${CLR_RESET}"
  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"

  wait_for_backend
  load_cache_data

  local created=0
  local bot_round=1
  while (( created < target_qty && bot_round <= 10 )); do
    for b_idx in $(seq 1 "$BOT_COUNT"); do
      if (( created >= target_qty )); then break; fi
      local b_tok
      b_tok="$(get_or_create_bot "$b_idx")"
      if [[ -n "$b_tok" ]]; then
        ensure_liquidity "$b_tok" "eng_bot_${b_idx}@example.test"
        ensure_bot_inventory "$b_tok" "Bot #$b_idx" 4
        if operate_p2p_sell "$b_tok" "Bot #$b_idx" 1; then
          created=$(( created + 1 ))
        fi
      fi
    done
    bot_round=$(( bot_round + 1 ))
  done

  local total_active=0
  request GET "$API_BASE_URL/api/listings" "$(get_or_create_bot 1)"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    total_active="$(jq '[.listings[]? | select(.status == "ACTIVE")] | length' "$RESPONSE_BODY")"
  fi

  echo
  echo -e "${CLR_GREEN}✔ Inyección completada: $created nuevas publicaciones P2P creadas.${CLR_RESET}"
  echo -e "  Total de órdenes activas disponibles en el mercado P2P: ${CLR_BOLD}$total_active${CLR_RESET}"
  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
}

# -----------------------------------------------------------------------------
# Ejecución del Ciclo Integral de Mercado (Objetivo: 10 Transacciones)
# -----------------------------------------------------------------------------
run_single_market_cycle() {
  local round_num="$1"
  CYCLE_VOLUME=0
  CYCLE_TRADES=0
  COUNT_OFFICIAL_BUYS=0
  COUNT_OFFICIAL_SELLS=0
  COUNT_P2P_TRADES=0
  COUNT_P2P_LISTINGS=0
  COUNT_P2P_CANCELS=0

  if (( ${#PLAYER_NAMES[@]} == 0 )); then
    load_cache_data
  fi

  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
  echo -e "${CLR_BOLD}🚀 [$(date '+%Y-%m-%d %H:%M:%S')] CICLO DE MERCADO INTEGRAL #${round_num}${CLR_RESET}"
  echo -e "   Intervalo: ${INTERVAL_SECONDS}s | ${CLR_BOLD}Objetivo estricto: $TARGET_TRADES_PER_CYCLE transacciones${CLR_RESET}"
  echo -e "   Rango compra oficial: ${MIN_OFFICIAL_QTY}–${MAX_OFFICIAL_QTY} tokens | P2P: ${MIN_P2P_QTY}–${MAX_P2P_QTY} tokens"
  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"

  # 1. Autenticación y preparación del pool de traders (Bots 1 a BOT_COUNT)
  declare -a BOT_TOKENS=()
  for (( b=1; b<=BOT_COUNT; b++ )); do
    local b_tok
    b_tok="$(get_or_create_bot "$b")"
    BOT_TOKENS+=("$b_tok")
    [[ -z "$b_tok" ]] || ensure_liquidity "$b_tok" "eng_bot_${b}@example.test"
  done

  local yoel_token="" nicolas_token=""
  if [[ "$WITH_REAL_USERS" == "true" ]]; then
    yoel_token="$(login_user "$YOEL_EMAIL" "$YOEL_PASSWORD")"
    nicolas_token="$(login_user "$NICOLAS_EMAIL" "$NICOLAS_PASSWORD")"
  fi

  [[ -z "$yoel_token" ]] || ensure_liquidity "$yoel_token" "$YOEL_EMAIL"
  [[ -z "$nicolas_token" ]] || ensure_liquidity "$nicolas_token" "$NICOLAS_EMAIL"

  # 2. Publicaciones de Venta en Mercado P2P (Inyección robusta y permanente de oferta)
  echo
  echo -e "${CLR_CYAN}--- 1. MERCADO P2P (PUBLICACIÓN ROBUSTA DE ÓRDENES DE VENTA) ---${CLR_RESET}"

  local current_active_p2p=0
  request GET "$API_BASE_URL/api/listings" "${BOT_TOKENS[0]}"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    current_active_p2p="$(jq '[.listings[]? | select(.status == "ACTIVE")] | length' "$RESPONSE_BODY")"
  fi
  echo -e "  📦 [PROFUNDIDAD P2P] Órdenes activas en cartelera: ${CLR_BOLD}$current_active_p2p${CLR_RESET} (Objetivo de liquidez: >=$TARGET_ACTIVE_P2P)"

  # Si la profundidad está por debajo del objetivo, inyectamos más órdenes por bot
  local orders_per_bot=1
  if (( current_active_p2p < TARGET_ACTIVE_P2P )); then
    orders_per_bot=2
    echo -e "  ⚡ [INYECCIÓN ACTIVA] Reabasteciendo el libro de órdenes P2P..."
  fi

  # Inyectar órdenes con todos los bots disponibles (Bot #1 al Bot #8)
  for (( b=0; b<${#BOT_TOKENS[@]}; b++ )); do
    local b_idx=$(( b + 1 ))
    local b_tok="${BOT_TOKENS[$b]}"
    if [[ -n "$b_tok" ]]; then
      ensure_bot_inventory "$b_tok" "Bot #$b_idx" 3
      operate_p2p_sell "$b_tok" "Bot #$b_idx" "$orders_per_bot" || true
    fi
  done

  if [[ "$WITH_REAL_USERS" == "true" && -n "$yoel_token" ]]; then
    operate_p2p_sell "$yoel_token" "Yoel" 1 || true
  fi
  if [[ "$WITH_REAL_USERS" == "true" && -n "$nicolas_token" ]]; then
    operate_p2p_sell "$nicolas_token" "Nicolas" 1 || true
  fi

  # 3. Compras Primarias en Mercado Oficial (1 a 10 tokens aleatorios)
  echo
  echo -e "${CLR_CYAN}--- 2. MERCADO OFICIAL (COMPRAS AL SUPERUSUARIO - 1 A 10 TOKENS) ---${CLR_RESET}"
  for (( b=0; b<3; b++ )); do
    local b_idx=$(( b + 1 ))
    local b_tok="${BOT_TOKENS[$b]}"
    [[ -z "$b_tok" ]] || operate_official_buys "$b_tok" "Bot #$b_idx" "$(( round_num + b ))" || true
  done
  if [[ "$WITH_REAL_USERS" == "true" && -n "$yoel_token" ]]; then
    operate_official_buys "$yoel_token" "Yoel" "$(( round_num + 3 ))" || true
  fi
  if [[ "$WITH_REAL_USERS" == "true" && -n "$nicolas_token" ]]; then
    operate_official_buys "$nicolas_token" "Nicolas" "$(( round_num + 4 ))" || true
  fi

  # 4. Liquidación Inmediata Oficial / Buyback a la Casa (SPEC-026 / PR #111)
  echo
  echo -e "${CLR_CYAN}--- 3. MERCADO OFICIAL (LIQUIDACIÓN / BUYBACK INMEDIATO CON SPREAD 10%) ---${CLR_RESET}"
  local sold_official=false
  if [[ -n "${BOT_TOKENS[0]:-}" ]] && operate_official_sell "${BOT_TOKENS[0]}" "Bot #1"; then
    sold_official=true
  fi
  if [[ "$WITH_REAL_USERS" == "true" && -n "$yoel_token" ]]; then
    if operate_official_sell "$yoel_token" "Yoel"; then
      sold_official=true
    fi
  fi
  if [[ "$sold_official" == "false" ]]; then
    if [[ "$HTTP_STATUS" == "404" ]]; then
      echo -e "  ${CLR_DIM}○ [LIQUIDACIÓN OFICIAL] Endpoint pendiente de activación (reiniciar backend para activar PR #111).${CLR_RESET}"
    else
      echo -e "  ${CLR_DIM}○ [LIQUIDACIÓN OFICIAL] Sin liquidaciones a la casa en este ciclo.${CLR_RESET}"
    fi
  fi

  # 5. Gestión y Cancelaciones Esporádicas (SPEC-030)
  echo
  echo -e "${CLR_CYAN}--- 4. GESTIÓN DE ÓRDENES (CANCELACIONES ESPORÁDICAS) ---${CLR_RESET}"
  local cancelled_p2p=false
  if [[ -n "${BOT_TOKENS[0]:-}" ]] && operate_p2p_cancellations "${BOT_TOKENS[0]}" "Bot #1"; then
    cancelled_p2p=true
  fi
  if [[ "$WITH_REAL_USERS" == "true" && -n "$yoel_token" ]]; then
    if operate_p2p_cancellations "$yoel_token" "Yoel"; then
      cancelled_p2p=true
    fi
  fi
  if [[ "$cancelled_p2p" == "false" ]]; then
    echo -e "  ${CLR_DIM}○ [P2P GESTIÓN] Sin cancelaciones en este ciclo (órdenes activas preservadas en el libro).${CLR_RESET}"
  fi

  # 6. Compras Cruzadas en Mercado P2P (Absorción controlada: máx 3 trades para preservar stock vivo)
  echo
  echo -e "${CLR_CYAN}--- 5. MERCADO P2P (COMPRAS CRUZADAS Y ABSORCIÓN) ---${CLR_RESET}"
  local p2p_trades_done=0
  local max_p2p_trades=3

  # Rotar compradores entre bots 4 a 8
  for (( b=3; b<${#BOT_TOKENS[@]}; b++ )); do
    if (( p2p_trades_done >= max_p2p_trades )); then break; fi
    local b_idx=$(( b + 1 ))
    local b_tok="${BOT_TOKENS[$b]}"
    if [[ -n "$b_tok" ]] && operate_p2p_buy "$b_tok" "Bot #$b_idx" "$(( 100 + b_idx ))"; then
      p2p_trades_done=$(( p2p_trades_done + 1 ))
    fi
  done

  if [[ "$WITH_REAL_USERS" == "true" && -n "$yoel_token" ]] && (( p2p_trades_done < max_p2p_trades )); then
    if operate_p2p_buy "$yoel_token" "Yoel" "$YOEL_USER_ID"; then
      p2p_trades_done=$(( p2p_trades_done + 1 ))
    fi
  fi
  if [[ "$WITH_REAL_USERS" == "true" && -n "$nicolas_token" ]] && (( p2p_trades_done < max_p2p_trades )); then
    if operate_p2p_buy "$nicolas_token" "Nicolas" "$NICOLAS_USER_ID"; then
      p2p_trades_done=$(( p2p_trades_done + 1 ))
    fi
  fi
  if (( p2p_trades_done == 0 )); then
    echo -e "  ${CLR_DIM}○ [P2P TRADES] Sin compras P2P en este turno (ofertas activas permanecen en cartelera).${CLR_RESET}"
  fi

  # 7. Compensación activa para garantizar EXACTAMENTE 10 transacciones
  if (( CYCLE_TRADES < TARGET_TRADES_PER_CYCLE )); then
    local missing=$(( TARGET_TRADES_PER_CYCLE - CYCLE_TRADES ))
    echo
    echo -e "${CLR_CYAN}--- 6. BALANCE Y CUOTA (COMPLETANDO HASTA EXACTAMENTE $TARGET_TRADES_PER_CYCLE TRADES) ---${CLR_RESET}"
    echo -e "  ⚖ [CUOTA] Ejecutadas $CYCLE_TRADES de $TARGET_TRADES_PER_CYCLE. Completando $missing transacción(es) oficial(es)..."

    local comp_round=1
    while (( CYCLE_TRADES < TARGET_TRADES_PER_CYCLE && comp_round <= 15 )); do
      local comp_bot_idx=$(( (RANDOM % BOT_COUNT) + 1 ))
      local comp_token="${BOT_TOKENS[$(( comp_bot_idx - 1 ))]}"
      if [[ -n "$comp_token" ]]; then
        ensure_liquidity "$comp_token" "eng_bot_${comp_bot_idx}@example.test"
        operate_official_buys "$comp_token" "Bot #$comp_bot_idx" "$(( round_num + comp_round + 20 ))" || true
      fi
      comp_round=$(( comp_round + 1 ))
    done
    if (( CYCLE_TRADES >= TARGET_TRADES_PER_CYCLE )); then
      echo -e "  ${CLR_GREEN}✔ [CUOTA ALCANZADA] Objetivo de $TARGET_TRADES_PER_CYCLE transacciones cumplido exitosamente.${CLR_RESET}"
    fi
  fi

  # 8. Recálculo bursátil de cotizaciones (Máximo 4 al día)
  operate_quote_recalculation

  # 9. Mantenimiento y saneamiento de libro de órdenes
  operate_maintenance

  # 10. Balance y Resumen Final Detallado del Ciclo
  echo
  echo -e "${CLR_CYAN}==============================================================================${CLR_RESET}"
  echo -e "${CLR_BOLD}📊 RESUMEN DETALLADO DEL CICLO #${round_num}${CLR_RESET}"
  echo -e "${CLR_CYAN}==============================================================================${CLR_RESET}"
  local cuota_badge="${CLR_GREEN}[100% Cuota cumplida]${CLR_RESET}"
  if (( CYCLE_TRADES < TARGET_TRADES_PER_CYCLE )); then
    cuota_badge="${CLR_YELLOW}[Parcial: $CYCLE_TRADES/$TARGET_TRADES_PER_CYCLE]${CLR_RESET}"
  fi
  echo -e "  🤝 Transacciones confirmadas: ${CLR_BOLD}${CLR_GREEN}$CYCLE_TRADES de $TARGET_TRADES_PER_CYCLE${CLR_RESET} $cuota_badge"
  echo -e "     • Compras Oficiales (Casa):          ${CLR_BOLD}$COUNT_OFFICIAL_BUYS${CLR_RESET} transacciones"
  echo -e "     • Liquidaciones Inmediatas (Casa):   ${CLR_BOLD}$COUNT_OFFICIAL_SELLS${CLR_RESET} transacciones"
  echo -e "     • Trades P2P cruzados entre partes:  ${CLR_BOLD}$COUNT_P2P_TRADES${CLR_RESET} transacciones"
  echo -e "  💰 Volumen total transaccionado:        ${CLR_BOLD}$(format_currency "$CYCLE_VOLUME")${CLR_RESET}"
  echo -e "  📋 Actividad de órdenes P2P:            ${CLR_BOLD}$COUNT_P2P_LISTINGS${CLR_RESET} publicadas | ${CLR_BOLD}$COUNT_P2P_CANCELS${CLR_RESET} retiradas"

  local ending_active_p2p=0
  request GET "$API_BASE_URL/api/listings" "${BOT_TOKENS[0]}"
  if [[ "$HTTP_STATUS" == "200" ]]; then
    ending_active_p2p="$(jq '[.listings[]? | select(.status == "ACTIVE")] | length' "$RESPONSE_BODY")"
  fi
  echo -e "  📦 Órdenes P2P activas en cartelera:    ${CLR_BOLD}${CLR_GREEN}$ending_active_p2p ofertas vivas disponibles${CLR_RESET}"
  echo

  if [[ "$WITH_REAL_USERS" == "true" ]]; then
    if [[ -n "$yoel_token" ]]; then
      print_user_portfolio_card "Yoel" "$YOEL_EMAIL" "$yoel_token"
    fi
    if [[ -n "$nicolas_token" ]]; then
      print_user_portfolio_card "Nicolas" "$NICOLAS_EMAIL" "$nicolas_token"
    fi
  fi
  echo -e "${CLR_BOLD}==============================================================================${CLR_RESET}"
  echo
}

# -----------------------------------------------------------------------------
# Temporizador con Cuenta Regresiva Dinámica para Consola en Vivo
# -----------------------------------------------------------------------------
wait_with_countdown() {
  local remaining="$1"
  local next_cycle="${2:-}"
  if [[ -t 1 ]]; then
    while (( remaining > 0 )); do
      local mins=$(( remaining / 60 ))
      local secs=$(( remaining % 60 ))
      local cycle_msg=""
      if [[ -n "$next_cycle" ]]; then
        cycle_msg=" (#$next_cycle)"
      fi
      printf "\r  ${CLR_YELLOW}⏳ Próximo ciclo de mercado%s en %02d:%02d...${CLR_RESET} ${CLR_DIM}(consola en vivo activa | Presiona Ctrl+C para detener)${CLR_RESET}   " "$cycle_msg" "$mins" "$secs"
      sleep 1
      remaining=$(( remaining - 1 ))
    done
    printf "\r%-85s\r" ""
  else
    echo "Próximo ciclo en $remaining segundos..."
    sleep "$remaining"
  fi
}

# -----------------------------------------------------------------------------
# Bucle de Control Principal
# -----------------------------------------------------------------------------
wait_for_backend
load_cache_data

# Si se solicitó ráfaga inicial (--burst), ejecutar 3 ciclos rápidos
if (( BURST_INITIAL_ROUNDS > 0 )); then
  echo -e "${CLR_MAGENTA}⚡ [BURST] Disparando ráfaga inicial de $BURST_INITIAL_ROUNDS ciclos para poblar el mercado...${CLR_RESET}"
  for (( b=1; b<=BURST_INITIAL_ROUNDS; b++ )); do
    run_single_market_cycle "BURST-$b"
    sleep 3
  done
  echo -e "${CLR_GREEN}✔ Ráfaga inicial completada. Continuando con intervalo estándar de ${INTERVAL_SECONDS}s.${CLR_RESET}"
fi

case "$MODE" in
  inject_p2p)
    inject_p2p_batch "$INJECT_P2P_COUNT"
    ;;
  once)
    run_single_market_cycle 1
    ;;
  rounds)
    for (( r=1; r<=TOTAL_ROUNDS; r++ )); do
      run_single_market_cycle "$r"
      if (( r < TOTAL_ROUNDS )); then
        wait_with_countdown "$INTERVAL_SECONDS" "$(( r + 1 ))"
      fi
    done
    ;;
  loop)
    echo -e "${CLR_BOLD}Iniciando bucle de mercado continuo ($TARGET_TRADES_PER_CYCLE transacciones cada $INTERVAL_SECONDS segundos). Presiona Ctrl+C para detener.${CLR_RESET}"
    round=1
    while true; do
      run_single_market_cycle "$round"
      wait_with_countdown "$INTERVAL_SECONDS" "$(( round + 1 ))"
      round=$(( round + 1 ))
    done
    ;;
esac
