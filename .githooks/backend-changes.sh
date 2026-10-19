#!/bin/bash

# Lee los registros de pre-push. 0: validar backend; 1: omisión segura.
# Cualquier error, referencia nueva o caso desconocido exige validación.
backend_path_is_relevant() {
    case "$1" in
        backend/src/*|backend/gradle/*|backend/gradlew*|backend/*.gradle*|backend/gradle.properties)
            return 0 ;;
        .githooks/*|.github/workflows/*|scripts/*|compose*|Dockerfile*|.java-version|.tool-versions)
            return 0 ;;
        frontend/*|docs/*|specs/*|tp/*|*.md)
            return 1 ;;
        *)
            # No mantener una lista exhaustiva de configuración compartida.
            return 0 ;;
    esac
}

seen_record=false
while IFS=' ' read -r local_ref local_sha remote_ref remote_sha extra ||
    [[ -n "$local_ref$local_sha$remote_ref$remote_sha$extra" ]]; do
    seen_record=true
    if [[ -z "$local_ref" || -z "$remote_ref" || -n "$extra" ||
          ! "$local_sha" =~ ^([[:xdigit:]]{40}|[[:xdigit:]]{64})$ ||
          ! "$remote_sha" =~ ^([[:xdigit:]]{40}|[[:xdigit:]]{64})$ ]]; then
        exit 0
    fi
    # Eliminar una referencia no publica código nuevo.
    [[ "$local_sha" =~ ^0+$ ]] && continue
    [[ "$remote_sha" =~ ^0+$ ]] && exit 0

    # Solo comparar rangos conocidos y fast-forward; no hacer fetch desde un hook.
    git cat-file -e "${remote_sha}^{commit}" 2>/dev/null || exit 0
    git cat-file -e "${local_sha}^{commit}" 2>/dev/null || exit 0
    git merge-base --is-ancestor "$remote_sha" "$local_sha" 2>/dev/null || exit 0

    # Revisa cada commit, no solo el diff neto: incluye cambios luego revertidos.
    changes=$(git -c core.quotePath=true log --format= --name-only --no-renames \
        --diff-merges=separate "${remote_sha}..${local_sha}" --) || exit 0
    while IFS= read -r path; do
        [[ -z "$path" ]] && continue
        # Git entrecomilla nombres especiales; ante esa incertidumbre, validar.
        [[ "$path" == \"* ]] && exit 0
        backend_path_is_relevant "$path" && exit 0
    done <<< "$changes"
done

[[ "$seen_record" == true ]] || exit 0
exit 1
