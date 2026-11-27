#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# LIMPIEZA Y MANTENIMIENTO DE DATOS DE DEMO
# =============================================================================
# Cancela listings P2P vencidos, archiva o limpia logs voluminosos y mantiene
# la base de datos limpia de publicaciones obsoletas sin tocar usuarios ni
# cotizaciones históricas.
#
# Uso estándar:
#   ./scripts/clean-demo-data.sh
#
# Limpieza profunda del mercado secundario (cancela todas las ofertas activas):
#   ./scripts/clean-demo-data.sh --reset-p2p
# =============================================================================

RESET_P2P=false
if [[ "${1:-}" == "--reset-p2p" ]]; then
  RESET_P2P=true
fi

DB_CONTAINER="${DB_CONTAINER:-football-market-postgres}"
DB_NAME="${DB_NAME:-football_market_dev}"
DB_USER="${DB_USER:-postgres}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

psql_cmd() {
  docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$1"
}

echo "=============================================================================="
echo "[$(date --iso-8601=seconds)] Mantenimiento y Limpieza del Entorno Demo"
echo "=============================================================================="

# 1. Cancelar los listings vencidos (status = 'CANCELLED' con closed_at)
EXPIRED_COUNT="$(psql_cmd "
WITH updated AS (
  UPDATE token_listings
  SET status = 'CANCELLED', closed_at = NOW()
  WHERE status = 'ACTIVE' AND expires_at <= NOW()
  RETURNING id
)
SELECT COUNT(*) FROM updated;
")"
echo "✔ Publicaciones vencidas canceladas ordenadamente: $EXPIRED_COUNT"

# 2. Reset opcional de todas las publicaciones P2P activas
if [[ "$RESET_P2P" == "true" ]]; then
  RESET_COUNT="$(psql_cmd "
  WITH updated AS (
    UPDATE token_listings
    SET status = 'CANCELLED', closed_at = NOW()
    WHERE status = 'ACTIVE'
    RETURNING id
  )
  SELECT COUNT(*) FROM updated;
  ")"
  echo "✔ [RESET P2P] Ofertas P2P activas canceladas para reiniciar el catálogo: $RESET_COUNT"
fi

# 3. Mantenimiento y rotación de archivos de log (> 5 MB)
echo
echo "--- Verificación de tamaño de logs ---"
for log_file in "$SCRIPT_DIR"/*.log; do
  [[ -f "$log_file" ]] || continue
  log_size_bytes=$(stat -c%s "$log_file" 2>/dev/null || echo 0)
  log_name="$(basename "$log_file")"

  if (( log_size_bytes > 5242880 )); then # > 5 MB
    echo "  ⚠ $log_name supera los 5 MB ($(awk -v s="$log_size_bytes" 'BEGIN{printf "%.2f MB", s/1048576}')). Rotando..."
    mv "$log_file" "${log_file}.$(date +%Y%m%d%H%M).bak"
    touch "$log_file"
    echo "  ✔ $log_name rotado exitosamente."
  else
    echo "  ✔ $log_name dentro del límite ($(awk -v s="$log_size_bytes" 'BEGIN{printf "%.2f KB", s/1024}'))."
  fi
done

echo
echo "Mantenimiento completado exitosamente."
