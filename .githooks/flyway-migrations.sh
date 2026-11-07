#!/bin/bash

set -euo pipefail

root=$(git rev-parse --show-toplevel)
base_ref=${1:-origin/dev}
target_ref=${2:-HEAD}
migration_path=backend/src/main/resources/db/migration
rebuild_marker=V20261107030001__create_users.sql

if ! git rev-parse --verify "$base_ref" >/dev/null 2>&1; then
    echo "No se puede validar Flyway: no existe la referencia base $base_ref." >&2
    exit 1
fi

base_files=$(git ls-tree -r --name-only "$base_ref" -- "$migration_path")
target_files=$(git ls-tree -r --name-only "$target_ref" -- "$migration_path")

# Excepción autocontenida para el único corte aprobado. Deja de aplicar cuando dev contiene
# el marcador del historial reconstruido; desde ese momento toda migración integrada es inmutable.
if ! grep -q "/$rebuild_marker$" <<< "$base_files" \
    && grep -q "/$rebuild_marker$" <<< "$target_files"; then
    echo "Reconstrucción inicial de Flyway detectada; la inmutabilidad regirá después de integrarla en dev."
    exit 0
fi

errors=()
while IFS=$'\t' read -r status first second; do
    [[ -z "$status" ]] && continue
    case "$status" in
        A)
            ;;
        M|D|R*|C*)
            errors+=("Migración integrada alterada ($status): $first${second:+ -> $second}")
            ;;
    esac
done < <(git diff --name-status --find-renames --find-copies "$base_ref...$target_ref" -- "$migration_path")

max_base=$(sed -nE 's#^.*/V([0-9]{14})__.*\.sql$#\1#p' <<< "$base_files" | sort -n | tail -1)
while IFS= read -r added; do
    [[ -z "$added" ]] && continue
    filename=${added##*/}
    if [[ ! "$filename" =~ ^V([0-9]{14})__[a-z][a-z0-9]*(_[a-z0-9]+)*\.sql$ ]]; then
        errors+=("Nombre nuevo inválido: $filename")
        continue
    fi
    version=${BASH_REMATCH[1]}
    if [[ -n "$max_base" && "$version" -le "$max_base" ]]; then
        errors+=("La nueva migración $filename no es posterior a la máxima integrada V$max_base.")
    fi
done < <(git diff --diff-filter=A --name-only "$base_ref...$target_ref" -- "$migration_path")

if (( ${#errors[@]} > 0 )); then
    printf '%s\n' "${errors[@]}" >&2
    echo "Las migraciones integradas en dev son inmutables; corregí con una migración posterior." >&2
    exit 1
fi

echo "Historial Flyway compatible con $base_ref: solo migraciones nuevas y posteriores."
