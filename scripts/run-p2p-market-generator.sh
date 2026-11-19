#!/usr/bin/env bash

set -euo pipefail

# =============================================================================
# GENERADOR PERIÓDICO DE MERCADO P2P
# =============================================================================
# Uso normal (segundo plano):
#   ./scripts/run-p2p-market-generator.sh
#
# Uso para DEMO EN VIVO (ráfagas rápidas de actividad cada 15-30 segundos):
#   P2P_INTERVAL_SECONDS=15 ./scripts/run-p2p-market-generator.sh
#
# Uso para siembra inicial previa a la demo (ejecutar una sola vez):
#   P2P_PLAYER_POOL_SIZE=8 P2P_SELLER_COUNT=16 ./scripts/seed-p2p-market.sh
# =============================================================================

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INTERVAL_SECONDS="${P2P_INTERVAL_SECONDS:-10800}"
LOG_FILE="${P2P_LOG_FILE:-$SCRIPT_DIR/p2p-generator.log}"

exec > >(tee -a "$LOG_FILE") 2>&1

if ! [[ "$INTERVAL_SECONDS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Error: P2P_INTERVAL_SECONDS debe ser un entero positivo." >&2
  exit 1
fi

while true; do
  echo "[$(date --iso-8601=seconds)] Iniciando una tanda de publicaciones P2P."
  if ! "$SCRIPT_DIR/seed-p2p-market.sh"; then
    echo "[$(date --iso-8601=seconds)] La tanda falló; se reintentará en $INTERVAL_SECONDS segundos." >&2
  fi
  sleep "$INTERVAL_SECONDS"
done
