#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# SIEMBRA / FONDEO DE PORTFOLIO DE USUARIO (DEMO & TESTING)
# =============================================================================
# Acredita saldo de créditos y una cartera diversificada de tokens de jugadores
# (Delanteros, Mediocampistas, Defensores, Arqueros) a un usuario para que su
# Portfolio, Donut Chart y Cartas Sorare en el Dashboard se vean completos.
#
# Uso por defecto (usuario yoel@gmail.com):
#   ./scripts/seed-user-portfolio.sh
#
# Para otro usuario o con créditos personalizados:
#   ./scripts/seed-user-portfolio.sh nicolas@gmail.com 30000
# =============================================================================

TARGET_EMAIL="${1:-yoel@gmail.com}"
ADD_CREDITS_CENTS="${2:-20000}" # 200 créditos por defecto

DB_CONTAINER="${DB_CONTAINER:-football-market-postgres}"
DB_NAME="${DB_NAME:-football_market_dev}"
DB_USER="${DB_USER:-postgres}"

psql_cmd() {
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$1"
}

psql_scalar() {
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1" 2>/dev/null || echo ""
}

USER_ID="$(psql_scalar "SELECT id FROM users WHERE email = '$TARGET_EMAIL';")"

if [[ -z "$USER_ID" ]]; then
  echo "Error: No se encontró ningún usuario con el email: $TARGET_EMAIL" >&2
  echo "Asegúrate de que el usuario esté registrado en la aplicación." >&2
  exit 1
fi

USERNAME="$(psql_scalar "SELECT username FROM users WHERE id = $USER_ID;")"

echo "=============================================================================="
echo "[$(date --iso-8601=seconds)] Fondeando Portfolio de Usuario"
echo "  - Usuario: $USERNAME ($TARGET_EMAIL, ID: $USER_ID)"
echo "  - Créditos a acreditar: ₵$(awk -v c="$ADD_CREDITS_CENTS" 'BEGIN{printf "%.2f", c/100}') ($ADD_CREDITS_CENTS centésimas)"
echo "=============================================================================="

# 1. Acreditar saldo en user_credits
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
INSERT INTO user_credits (user_id, available_credits, updated_at)
VALUES ($USER_ID, $ADD_CREDITS_CENTS, NOW())
ON CONFLICT (user_id)
DO UPDATE SET available_credits = user_credits.available_credits + $ADD_CREDITS_CENTS, updated_at = NOW();
" >/dev/null

NEW_BALANCE="$(psql_scalar "SELECT available_credits FROM user_credits WHERE user_id = $USER_ID;")"
echo "✔ Saldo actualizado: $(awk -v b="$NEW_BALANCE" 'BEGIN{printf "₵%.2f", b/100}')"

# 2. Asignar tokens de jugadores destacados en varias posiciones
# Jugadores seleccionados:
# - Lamine Yamal (Offence)
# - Raphinha (Offence)
# - Declan Rice (Midfield)
# - Joshua Kimmich (Midfield)
# - Giorgio Cittadini (Defence)
# - David Raya (Goalkeeper)
declare -a PLAYER_NAMES=(
  "Lamine Yamal"
  "Raphinha"
  "Declan Rice"
  "Joshua Kimmich"
  "Giorgio Cittadini"
  "David Raya"
)

declare -a QUANTITIES=(
  5
  4
  6
  5
  4
  5
)

echo
echo "--- Asignando tokens de jugadores para Sorare Grid y Donut Chart ---"
for i in "${!PLAYER_NAMES[@]}"; do
  p_name="${PLAYER_NAMES[$i]}"
  qty="${QUANTITIES[$i]}"

  p_id="$(psql_scalar "SELECT id FROM players WHERE name = '$p_name' LIMIT 1;")"
  if [[ -n "$p_id" ]]; then
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
    INSERT INTO player_tokens (player_id, owner_user_id, quantity, updated_at)
    VALUES ($p_id, $USER_ID, $qty, NOW())
    ON CONFLICT (player_id, owner_user_id)
    DO UPDATE SET quantity = player_tokens.quantity + EXCLUDED.quantity, updated_at = NOW();
    " >/dev/null
    echo "  ✔ $p_name (ID: $p_id): +$qty tokens"
  else
    echo "  ○ No se encontró $p_name en la BD"
  fi
done

echo
echo "=============================================================================="
echo "Portfolio de $TARGET_EMAIL listo:"
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
SELECT p.name AS jugador, p.position AS posicion, pt.quantity AS cantidad_tokens,
       ROUND(COALESCE(q.price, 100) / 100.0, 2) AS precio_unitario,
       ROUND((pt.quantity * COALESCE(q.price, 100)) / 100.0, 2) AS valor_total
FROM player_tokens pt
JOIN players p ON p.id = pt.player_id
LEFT JOIN (
  SELECT DISTINCT ON (player_id) player_id, price
  FROM player_quotes
  ORDER BY player_id, effective_date DESC, calculated_at DESC
) q ON q.player_id = p.id
WHERE pt.owner_user_id = $USER_ID
ORDER BY valor_total DESC;
"
