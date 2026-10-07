#!/usr/bin/env bash

set -e

JSON_MODE=false
DRY_RUN=false
ALLOW_EXISTING=false
SHORT_NAME=""
BRANCH_NUMBER=""
USE_TIMESTAMP=false
NUMBER_EXPLICIT=false
REUSE=false
SPEC_ACTION="created"

# Branch types allowed by the project constitution's branch naming rule.
# Defaults are set before option parsing so an explicit --type wins.
BRANCH_TYPE="feat"
ALLOWED_BRANCH_TYPES="feat fix refactor test docs chore perf ci"

ARGS=()
i=1
while [ $i -le $# ]; do
    arg="${!i}"
    case "$arg" in
        --json)
            JSON_MODE=true
            ;;
        --dry-run)
            DRY_RUN=true
            ;;
        --allow-existing-branch)
            ALLOW_EXISTING=true
            ;;
        --short-name)
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --short-name requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            # Check if the next argument is another option (starts with --)
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --short-name requires a value' >&2
                exit 1
            fi
            SHORT_NAME="$next_arg"
            ;;
        --number)
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --number requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --number requires a value' >&2
                exit 1
            fi
            BRANCH_NUMBER="$next_arg"
            if [ -n "$BRANCH_NUMBER" ]; then
                NUMBER_EXPLICIT=true
            fi
            ;;
        --timestamp)
            USE_TIMESTAMP=true
            ;;
        --type)
            if [ $((i + 1)) -gt $# ]; then
                echo 'Error: --type requires a value' >&2
                exit 1
            fi
            i=$((i + 1))
            next_arg="${!i}"
            if [[ "$next_arg" == --* ]]; then
                echo 'Error: --type requires a value' >&2
                exit 1
            fi
            BRANCH_TYPE="$next_arg"
            ;;
        --reuse)
            REUSE=true
            ;;
        --help|-h)
            echo "Usage: $0 [--json] [--dry-run] [--allow-existing-branch] [--short-name <name>] [--number N] [--type <type>] [--reuse] [--timestamp] <feature_description>"
            echo ""
            echo "Options:"
            echo "  --json              Output in JSON format"
            echo "  --dry-run           Compute feature name and paths without creating directories or files"
            echo "  --allow-existing-branch  Reuse an existing feature directory if it already exists"
            echo "  --short-name <name> Provide a custom short name (2-4 words) for the feature"
            echo "  --number N          Prefer a feature number (auto-corrected if its specs prefix exists)"
            echo "  --type <type>       Branch type prefix (default: feat)"
            echo "  --reuse             Resume an existing spec matched by --short-name"
            echo "  --timestamp         Use timestamp prefix (YYYYMMDD-HHMMSS) instead of sequential numbering"
            echo "  --help, -h          Show this help message"
            echo ""
            echo "The spec directory is named <spec-id>-<scope> and is independent of the"
            echo "branch type. The branch is named <type>/<spec-id>-<scope>."
            echo ""
            echo "--reuse never creates a new spec or a new identifier: it resolves an"
            echo "existing spec directory and reuses its id, directory and branch."
            echo ""
            echo "Examples:"
            echo "  $0 'Add user authentication system' --short-name 'user-auth'"
            echo "  $0 'Implement OAuth2 integration for API' --number 5"
            echo "  $0 --reuse --short-name 'user-auth' 'Continue the authentication work'"
            echo "  $0 --timestamp --short-name 'user-auth' 'Add user authentication'"
            exit 0
            ;;
        *)
            ARGS+=("$arg")
            ;;
    esac
    i=$((i + 1))
done

FEATURE_DESCRIPTION="${ARGS[*]}"
if [ -z "$FEATURE_DESCRIPTION" ]; then
    echo "Usage: $0 [--json] [--dry-run] [--allow-existing-branch] [--short-name <name>] [--number N] [--type <type>] [--timestamp] <feature_description>" >&2
    exit 1
fi

if [[ ! " $ALLOWED_BRANCH_TYPES " =~ " $BRANCH_TYPE " ]]; then
    echo "Error: --type must be one of: $ALLOWED_BRANCH_TYPES" >&2
    exit 1
fi

# Trim whitespace and validate description is not empty (e.g., user passed only whitespace)
FEATURE_DESCRIPTION=$(echo "$FEATURE_DESCRIPTION" | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')
if [ -z "$FEATURE_DESCRIPTION" ]; then
    echo "Error: Feature description cannot be empty or contain only whitespace" >&2
    exit 1
fi

MAX_FEATURE_NUMBER=9223372036854775807
MAX_BRANCH_LENGTH=244

is_feature_number_in_range() {
    local value="$1"
    local normalized="${value#"${value%%[!0]*}"}"
    [ -n "$normalized" ] || normalized=0
    [ ${#normalized} -lt ${#MAX_FEATURE_NUMBER} ] && return 0
    [ ${#normalized} -gt ${#MAX_FEATURE_NUMBER} ] && return 1
    # Equal-length digit strings must be compared without arithmetic overflow.
    # shellcheck disable=SC2071
    [[ "$normalized" < "$MAX_FEATURE_NUMBER" || "$normalized" == "$MAX_FEATURE_NUMBER" ]]
}

# Function to get highest number from specs directory
get_highest_from_specs() {
    local specs_dir="$1"
    local highest=0

    if [ -d "$specs_dir" ]; then
        for dir in "$specs_dir"/*; do
            [ -d "$dir" ] || continue
            dirname=$(basename "$dir")
            # Match sequential prefixes (>=3 digits), but skip timestamp dirs.
            if echo "$dirname" | grep -Eq '^[0-9]{3,}-' && ! echo "$dirname" | grep -Eq '^[0-9]{8}-[0-9]{6}-'; then
                number=$(echo "$dirname" | grep -Eo '^[0-9]+')
                if is_feature_number_in_range "$number"; then
                    number=$((10#$number))
                    if [ "$number" -gt "$highest" ]; then
                        highest=$number
                    fi
                fi
            fi
        done
    fi

    echo "$highest"
}

# ---------------------------------------------------------------------------
# Spec lookup for --reuse
#
# Resolution is deterministic: a spec is selected only by exact directory-name
# equality or by exact equality of the scope that follows the "<spec-id>-"
# prefix. There is deliberately no fuzzy or substring matching, so a wrong
# spec can never be picked by accident. Zero matches and multiple matches are
# both hard errors.
# ---------------------------------------------------------------------------

# Print the "<spec-id>" part of a spec directory name, or nothing when absent.
spec_id_from_dirname() {
    local name="$1"
    if [[ "$name" =~ ^([0-9]+)- ]]; then
        printf '%s' "${BASH_REMATCH[1]}"
    fi
}

# Print the "<scope>" part of a spec directory name, i.e. everything after the
# "<spec-id>-" prefix. Timestamp-based directories carry a two-part numeric
# prefix, so the scope starts after the second hyphen group.
spec_scope_from_dirname() {
    local name="$1"
    # Sequential: "<digits>-<scope>"
    if [[ "$name" =~ ^([0-9]+)-(.*)$ ]]; then
        printf '%s' "${BASH_REMATCH[2]}"
        return 0
    fi
    # Timestamp: "<YYYYMMDD>-<HHMMSS>-<scope>"
    if [[ "$name" =~ ^[0-9]{8}-[0-9]{6}-(.*)$ ]]; then
        printf '%s' "${BASH_REMATCH[1]}"
        return 0
    fi
    # Not an id-prefixed directory: treat the whole name as the scope.
    printf '%s' "$name"
}

# Locate an existing spec matching the requested token.
#
# Matching, in order of precedence:
#   1. exact directory-name equality (the token may already be "<spec-id>-<scope>")
#   2. exact scope equality against every spec directory, optionally narrowed to
#      one spec id via wanted_id so an ambiguous scope can be disambiguated
#
# Sets REUSE_NAME and REUSE_ID on success. On ambiguity, lists the candidates in
# REUSE_CONFLICTS and returns 2. On no match, returns 1. The caller must not
# pick a candidate in the ambiguous case.
resolve_existing_spec() {
    local specs_dir="$1"
    local token="$2"
    local wanted_id="${3:-}"
    local name scope match id
    local exact=""
    local -a scope_matches=()

    REUSE_NAME=""
    REUSE_ID=""
    REUSE_CONFLICTS=""

    [ -d "$specs_dir" ] || return 1

    # Deterministic order regardless of the shell's glob collation.
    while IFS= read -r name; do
        [ -n "$name" ] || continue

        if [ "$name" = "$token" ]; then
            exact="$name"
            break
        fi

        scope=$(spec_scope_from_dirname "$name")
        if [ "$scope" = "$token" ]; then
            if [ -n "$wanted_id" ]; then
                id=$(spec_id_from_dirname "$name")
                # Compare numerically so 004 matches a requested "4".
                if [ -z "$id" ] || [ "$((10#$id))" -ne "$((10#$wanted_id))" ]; then
                    continue
                fi
            fi
            scope_matches+=("$name")
        fi
    done < <(find "$specs_dir" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null | sort)

    if [ -n "$exact" ]; then
        REUSE_NAME="$exact"
        REUSE_ID=$(spec_id_from_dirname "$exact")
        return 0
    fi

    if [ ${#scope_matches[@]} -eq 0 ]; then
        return 1
    fi

    if [ ${#scope_matches[@]} -gt 1 ]; then
        REUSE_CONFLICTS=$(printf '%s\n' "${scope_matches[@]}")
        return 2
    fi

    match="${scope_matches[0]}"
    REUSE_NAME="$match"
    REUSE_ID=$(spec_id_from_dirname "$match")
    return 0
}

# Return success when a spec directory owns the given numeric prefix.
spec_prefix_exists() {
    local specs_dir="$1"
    local feature_num="$2"

    for spec_path in "$specs_dir/${feature_num}-"*; do
        [ -d "$spec_path" ] && return 0
    done
    return 1
}

# Function to clean and format a branch name
clean_branch_name() {
    local name="$1"
    echo "$name" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g' | sed 's/-\+/-/g' | sed 's/^-//' | sed 's/-$//'
}

# Fit a spec name within GitHub's ref-name limit.
# The spec name is "<spec-id>-<scope>" and carries no branch type prefix: the
# directory name stays independent of how the branch is named.
fit_spec_name() {
    local feature_num="$1"
    local scope="$2"
    local spec_name="${feature_num}-${scope}"

    if [ ${#spec_name} -gt $MAX_BRANCH_LENGTH ]; then
        local prefix_length=$(( ${#feature_num} + 1 ))
        local max_suffix_length=$((MAX_BRANCH_LENGTH - prefix_length))
        local truncated_suffix
        truncated_suffix=$(printf '%s' "$scope" | cut -c "1-$max_suffix_length" | sed 's/-$//')
        spec_name="${feature_num}-${truncated_suffix}"
    fi

    printf '%s' "$spec_name"
}

# Build the git branch name from the type prefix and the spec name.
# "<type>/<spec-id>-<scope>" per the project constitution's branch naming rule.
# The type prefix is accounted for in the length budget so the final branch
# still respects GitHub's limit.
build_branch_name() {
    local branch_type="$1"
    local spec_name="$2"
    local branch_name="${branch_type}/${spec_name}"

    if [ ${#branch_name} -gt $MAX_BRANCH_LENGTH ]; then
        local type_length=$(( ${#branch_type} + 1 ))
        local max_spec_length=$((MAX_BRANCH_LENGTH - type_length))
        local truncated_spec
        truncated_spec=$(printf '%s' "$spec_name" | cut -c "1-$max_spec_length" | sed 's/-$//')
        branch_name="${branch_type}/${truncated_spec}"
    fi

    printf '%s' "$branch_name"
}

# Quote a value for POSIX shell reuse, byte-identical to Python's shlex.quote
# so the persistence hints match the Python variant exactly (printf %q output
# differs between bash versions and from shlex.quote for spaces/metachars).
shell_quote() {
    local value="$1" LC_ALL=C
    if [[ "$value" =~ ^[A-Za-z0-9_@%+=:,./-]+$ ]]; then
        printf '%s' "$value"
    else
        local q="'\"'\"'"
        printf "'%s'" "${value//\'/$q}"
    fi
}

# ---------------------------------------------------------------------------
# Associated git branch management
#
# The spec directory is "<spec-id>-<scope>" and the branch is
# "<type>/<spec-id>-<scope>". Ensuring the branch is idempotent: an existing
# local or remote branch of that name is reused, never duplicated, and never
# a reason to mint a different spec identifier.
# ---------------------------------------------------------------------------

git_repo_available() {
    git rev-parse --git-dir >/dev/null 2>&1
}

git_current_branch() {
    git symbolic-ref --quiet --short HEAD 2>/dev/null || printf ''
}

branch_exists_local() {
    git show-ref --verify --quiet "refs/heads/$1"
}

# Print the first remote whose origin has the branch, or nothing.
# Checking every configured remote keeps the lookup working when the branch
# was pushed somewhere other than "origin".
find_remote_with_branch() {
    local branch="$1" remote
    while read -r remote; do
        [ -n "$remote" ] || continue
        if git ls-remote --exit-code --heads "$remote" "refs/heads/$branch" >/dev/null 2>&1; then
            printf '%s' "$remote"
            return 0
        fi
    done < <(git remote 2>/dev/null)
    return 1
}

# Whether tracked files differ from HEAD or the index. Untracked files are
# excluded: they are not lost by a checkout, and including them would make
# this check trip over the spec directory this script creates moments later.
working_tree_has_tracked_changes() {
    [ -n "$(git status --porcelain --untracked-files=no 2>/dev/null)" ]
}

# Decide what has to happen for the associated branch and report the outcome
# in BRANCH_STATUS plus the action in BRANCH_ACTION. Performs no git write:
# the caller executes the resolved action.
plan_associated_branch() {
    local branch="$1"
    BRANCH_STATUS="skipped"
    BRANCH_ACTION="none"
    BRANCH_REMOTE=""

    if ! git_repo_available; then
        BRANCH_STATUS="skipped-no-git-repo"
        return 0
    fi

    local current
    current=$(git_current_branch)

    if [ "$current" = "$branch" ]; then
        # Already on the associated branch: nothing to create or switch.
        BRANCH_STATUS="already-current"
        return 0
    fi

    if [ -z "$current" ]; then
        # Detached HEAD: branching here would pin the spec to an arbitrary
        # commit, so leave the working tree alone.
        BRANCH_STATUS="skipped-detached-head"
        return 0
    fi

    if branch_exists_local "$branch"; then
        BRANCH_ACTION="switch-local"
    else
        local remote
        if remote=$(find_remote_with_branch "$branch"); then
            BRANCH_REMOTE="$remote"
            BRANCH_ACTION="track-remote"
        else
            BRANCH_ACTION="create"
        fi
    fi

    if working_tree_has_tracked_changes; then
        # Switching now would carry the user's pending work onto another
        # branch. Leave the working tree exactly as it is; the caller reports
        # the abort. Nothing is stashed or discarded.
        BRANCH_ACTION="none"
        BRANCH_STATUS="skipped-dirty-tree"
    fi
    return 0
}

# Execute the action planned by plan_associated_branch. Never discards or
# stashes local work; any git failure degrades to a warning so the spec
# itself still gets created.
apply_associated_branch() {
    local branch="$1"
    local remote="$2"
    local action="$3"
    local current

    case "$action" in
        create)
            current=$(git_current_branch)
            if git checkout -b "$branch" >/dev/null 2>&1; then
                BRANCH_STATUS="created"
                printf '[specify] Created branch %s from %s\n' "$branch" "$current" >&2
            else
                BRANCH_STATUS="create-failed"
                printf '[specify] Warning: could not create branch %s; continuing without switching\n' "$branch" >&2
            fi
            ;;
        switch-local)
            if git checkout "$branch" >/dev/null 2>&1; then
                BRANCH_STATUS="reused-local"
                printf '[specify] Switched to existing branch %s\n' "$branch" >&2
            else
                BRANCH_STATUS="switch-failed"
                printf '[specify] Warning: could not switch to branch %s\n' "$branch" >&2
            fi
            ;;
        track-remote)
            # Materialize the remote-tracking ref, then track it, so no parallel
            # branch is created when the branch only exists on the remote.
            if git fetch --quiet "$remote" \
                "refs/heads/$branch:refs/remotes/$remote/$branch" >/dev/null 2>&1 &&
                git checkout -b "$branch" --track "$remote/$branch" >/dev/null 2>&1; then
                BRANCH_STATUS="tracked-remote"
                printf '[specify] Created local branch %s tracking %s/%s\n' "$branch" "$remote" "$branch" >&2
            else
                BRANCH_STATUS="track-failed"
                printf '[specify] Warning: could not create local branch %s from %s/%s\n' "$branch" "$remote" "$branch" >&2
            fi
            ;;
        *)
            ;;
    esac
    return 0
}

# Resolve repository root using common.sh functions which prioritize .specify
SCRIPT_DIR="$(CDPATH="" cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT=$(get_repo_root) || exit 1

cd "$REPO_ROOT"

SPECS_DIR="$REPO_ROOT/specs"
if [ "$DRY_RUN" != true ]; then
    mkdir -p "$SPECS_DIR"
fi

# Function to generate the scope suffix with stop word filtering
generate_scope() {
    local description="$1"

    # Common stop words to filter out
    local stop_words="^(i|a|an|the|to|for|of|in|on|at|by|with|from|is|are|was|were|be|been|being|have|has|had|do|does|did|will|would|should|could|can|may|might|must|shall|this|that|these|those|my|your|our|their|want|need|add|get|set)$"

    # Convert to lowercase and split into words
    local clean_name=$(printf '%s' "$description" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/ /g')

    # Filter words: remove stop words and words shorter than 3 chars (unless they're uppercase acronyms in original)
    local meaningful_words=()
    for word in $clean_name; do
        # Skip empty words
        [ -z "$word" ] && continue

        # Keep words that are NOT stop words AND (length >= 3 OR are potential acronyms)
        if ! echo "$word" | grep -qiE "$stop_words"; then
            if [ ${#word} -ge 3 ]; then
                meaningful_words+=("$word")
            # Keep short words that appear as an uppercase acronym in the original.
            # Uppercase via tr and match with grep -w (both portable) rather than
            # bash's 4+ "^^" case expansion (breaks on macOS bash 3.2) and \b (non-POSIX).
            elif printf '%s' "$description" | grep -qw -- "$(printf '%s' "$word" | tr '[:lower:]' '[:upper:]')"; then
                meaningful_words+=("$word")
            fi
        fi
    done

    # If we have meaningful words, use first 3-4 of them
    if [ ${#meaningful_words[@]} -gt 0 ]; then
        local max_words=3
        if [ ${#meaningful_words[@]} -eq 4 ]; then max_words=4; fi

        local result=""
        local count=0
        for word in "${meaningful_words[@]}"; do
            if [ $count -ge $max_words ]; then break; fi
            if [ -n "$result" ]; then result="$result-"; fi
            result="$result$word"
            count=$((count + 1))
        done
        echo "$result"
    else
        # Fallback to original logic if no meaningful words found
        local cleaned=$(clean_branch_name "$description")
        echo "$cleaned" | tr '-' '\n' | grep -v '^$' | head -3 | tr '\n' '-' | sed 's/-$//'
    fi
}

# Resolve the scope portion shared by the spec name and the branch name
if [ -n "$SHORT_NAME" ]; then
    # Use provided short name, just clean it up
    BRANCH_SUFFIX=$(clean_branch_name "$SHORT_NAME")
else
    # Generate from description with smart filtering
    BRANCH_SUFFIX=$(generate_scope "$FEATURE_DESCRIPTION")
fi

# Warn if --number and --timestamp are both specified
if [ "$USE_TIMESTAMP" = true ] && [ -n "$BRANCH_NUMBER" ]; then
    >&2 echo "[specify] Warning: --number is ignored when --timestamp is used"
    BRANCH_NUMBER=""
fi

# ---------------------------------------------------------------------------
# --reuse: resume an existing spec instead of creating a new one.
# Resolved before any identifier is computed, so reuse never allocates a
# number and never goes through the auto-numbering path.
# ---------------------------------------------------------------------------
if [ "$REUSE" = true ]; then
    if [ "$USE_TIMESTAMP" = true ]; then
        echo "Error: --reuse cannot be combined with --timestamp: a timestamp allocates a new spec identity" >&2
        exit 1
    fi

    # `|| resolve_status=$?` keeps `set -e` from aborting before the status
    # below can be turned into a proper diagnostic.
    resolve_status=0
    resolve_existing_spec "$SPECS_DIR" "$BRANCH_SUFFIX" || resolve_status=$?

    # An ambiguous scope plus an explicit --number is the documented way to
    # disambiguate: retry narrowed to that id instead of guessing.
    if [ "$resolve_status" -eq 2 ] && [ -n "$BRANCH_NUMBER" ]; then
        resolve_status=0
        resolve_existing_spec "$SPECS_DIR" "$BRANCH_SUFFIX" "$BRANCH_NUMBER" || resolve_status=$?
    fi

    if [ "$resolve_status" -eq 2 ]; then
        echo "Error: '--reuse --short-name $BRANCH_SUFFIX' matches more than one spec:" >&2
        printf '%s\n' "$REUSE_CONFLICTS" | sed 's/^/  /' >&2
        echo "Pass the full spec name, or disambiguate with --number <id>." >&2
        exit 1
    elif [ "$resolve_status" -ne 0 ]; then
        if [ -n "$BRANCH_NUMBER" ]; then
            echo "Error: '--number $BRANCH_NUMBER' contradicts '--reuse --short-name $BRANCH_SUFFIX': no spec matches both." >&2
        else
            echo "Error: no existing spec matches '--short-name $BRANCH_SUFFIX'." >&2
        fi
        echo "Looked for an exact directory name or an exact scope match under $SPECS_DIR." >&2
        echo "Run without --reuse to create a new spec." >&2
        exit 1
    fi

    # --number, when given, must agree with the spec that was found.
    if [ -n "$BRANCH_NUMBER" ]; then
        if [ -z "$REUSE_ID" ]; then
            echo "Error: --reuse matched spec '$REUSE_NAME', which has no <spec-id> prefix to validate --number $BRANCH_NUMBER against" >&2
            exit 1
        fi
        if [ "$((10#$REUSE_ID))" -ne "$((10#$BRANCH_NUMBER))" ]; then
            echo "Error: --number $BRANCH_NUMBER contradicts '--reuse --short-name $BRANCH_SUFFIX', which matched '$REUSE_NAME' (spec id $REUSE_ID)" >&2
            exit 1
        fi
    fi

    # Reuse: the id, name and directory come from the existing spec.
    FEATURE_NUM="$REUSE_ID"
    SPEC_NAME="$REUSE_NAME"
    SPEC_ACTION="reused"
    ORIGINAL_SPEC_NAME="$SPEC_NAME"
else

# Determine the spec identifier prefix
if [ "$USE_TIMESTAMP" = true ]; then
    FEATURE_NUM=$(date +%Y%m%d-%H%M%S)
else
    if [ -n "$BRANCH_NUMBER" ] && [[ ! "$BRANCH_NUMBER" =~ ^[0-9]+$ ]]; then
        echo "Error: --number must be an unsigned integer, got '$BRANCH_NUMBER'" >&2
        exit 1
    fi

    # Bash arithmetic is signed 64-bit; reject digit strings that would wrap.
    if [ -n "$BRANCH_NUMBER" ] && ! is_feature_number_in_range "$BRANCH_NUMBER"; then
        echo "Error: --number must be between 0 and $MAX_FEATURE_NUMBER, got '$BRANCH_NUMBER'" >&2
        exit 1
    fi

    # Determine branch number from existing feature directories
    if [ -z "$BRANCH_NUMBER" ]; then
        HIGHEST=$(get_highest_from_specs "$SPECS_DIR")
        if [ "$HIGHEST" -eq "$MAX_FEATURE_NUMBER" ]; then
            echo "Error: feature number must be between 0 and $MAX_FEATURE_NUMBER, got '9223372036854775808'" >&2
            exit 1
        fi
        BRANCH_NUMBER=$((HIGHEST + 1))
    fi

    # Force base-10 interpretation to prevent octal conversion (e.g., 010 → 8 in octal, but should be 10 in decimal)
    FEATURE_NUM=$(printf "%03d" "$((10#$BRANCH_NUMBER))")

    # Treat an explicit number as a preference when its prefix is already used
    # by a feature directory. Auto-detected numbers are already conflict-free.
    if [ "$NUMBER_EXPLICIT" = true ]; then
        SPEC_CONFLICT=false
        REQUESTED_SPEC_NAME=$(fit_spec_name "$FEATURE_NUM" "$BRANCH_SUFFIX")
        REQUESTED_DIR="$SPECS_DIR/$REQUESTED_SPEC_NAME"
        if [ "$ALLOW_EXISTING" != true ] || [ ! -d "$REQUESTED_DIR" ]; then
            spec_prefix_exists "$SPECS_DIR" "$FEATURE_NUM" && SPEC_CONFLICT=true
        fi

        if [ "$SPEC_CONFLICT" = true ]; then
            REQUESTED_NUM="$FEATURE_NUM"
            HIGHEST=$(get_highest_from_specs "$SPECS_DIR")
            BRANCH_NUMBER=$HIGHEST
            while true; do
                if [ "$BRANCH_NUMBER" -eq "$MAX_FEATURE_NUMBER" ]; then
                    echo "Error: feature number must be between 0 and $MAX_FEATURE_NUMBER, got '9223372036854775808'" >&2
                    exit 1
                fi
                BRANCH_NUMBER=$((BRANCH_NUMBER + 1))
                FEATURE_NUM=$(printf "%03d" "$((10#$BRANCH_NUMBER))")
                spec_prefix_exists "$SPECS_DIR" "$FEATURE_NUM" || break
            done
            >&2 echo "[specify] Warning: --number $REQUESTED_NUM conflicts with an existing spec directory; using $FEATURE_NUM instead"
        fi
    fi

fi

# GitHub enforces a 244-byte limit on ref names.
# Spec name first: it drives the directory and is the branch name minus the
# type prefix, so the type is not double-counted in the budget.
ORIGINAL_SPEC_NAME="${FEATURE_NUM}-${BRANCH_SUFFIX}"
SPEC_NAME=$(fit_spec_name "$FEATURE_NUM" "$BRANCH_SUFFIX")
if [ "$SPEC_NAME" != "$ORIGINAL_SPEC_NAME" ]; then
    >&2 echo "[specify] Warning: Spec name exceeded GitHub's 244-byte limit"
    >&2 echo "[specify] Original: $ORIGINAL_SPEC_NAME (${#ORIGINAL_SPEC_NAME} bytes)"
    >&2 echo "[specify] Truncated to: $SPEC_NAME (${#SPEC_NAME} bytes)"
fi

fi  # end --reuse branch

# Branch name: the spec name under the configured type prefix.
BRANCH_NAME=$(build_branch_name "$BRANCH_TYPE" "$SPEC_NAME")

# The spec directory never carries the branch type prefix.
FEATURE_DIR="$SPECS_DIR/$SPEC_NAME"
SPEC_FILE="$FEATURE_DIR/spec.md"

# Ensure the associated branch exists before the directory is created, so a
# refusal to switch is reported against a working tree the script has not yet
# dirtied with its own output.
plan_associated_branch "$BRANCH_NAME"
if [ "$DRY_RUN" != true ]; then
    apply_associated_branch "$BRANCH_NAME" "$BRANCH_REMOTE" "$BRANCH_ACTION"
else
    case "$BRANCH_STATUS" in
        already-current)
            ;;
        skipped-dirty-tree)
            >&2 echo "[specify] Would switch to $BRANCH_NAME, but tracked changes are pending"
            ;;
        skipped-detached-head)
            >&2 echo "[specify] Would create $BRANCH_NAME, but HEAD is detached"
            ;;
        skipped-no-git-repo)
            >&2 echo "[specify] Would create $BRANCH_NAME, but this is not a git repository"
            ;;
        *)
            >&2 echo "[specify] Would $BRANCH_ACTION branch $BRANCH_NAME"
            ;;
    esac
fi

# An existing branch is never a collision: the spec id is kept and the abort
# is reported without touching the user's work.
if [ "$BRANCH_STATUS" = "skipped-dirty-tree" ]; then
    >&2 echo "[specify] Branch step aborted: local changes left untouched. Commit or stash them, then re-run to move to $BRANCH_NAME."
fi

if [ "$DRY_RUN" != true ]; then
    # In --reuse the directory already exists by definition, so the
    # collision guard belongs to the creation flow only.
    if [ "$REUSE" != true ] && [ -d "$FEATURE_DIR" ] && [ "$ALLOW_EXISTING" != true ]; then
        if [ "$USE_TIMESTAMP" = true ]; then
            >&2 echo "Error: Feature directory '$FEATURE_DIR' already exists. Rerun to get a new timestamp or use a different --short-name."
        else
            >&2 echo "Error: Feature directory '$FEATURE_DIR' already exists. Please use a different feature name or specify a different number with --number."
        fi
        exit 1
    fi

    NEEDS_SPEC=false
    SPEC_TEMPLATE_FOUND=false
    SPEC_TEMPLATE_CONTENT=""
    # --reuse never writes into the reused spec: no spec.md is generated and
    # no artifact is touched.
    if [ "$REUSE" != true ] && [ ! -f "$SPEC_FILE" ]; then
        NEEDS_SPEC=true
        if SPEC_TEMPLATE_CONTENT=$(resolve_template_content "spec-template" "$REPO_ROOT"; status=$?; printf x; exit "$status"); then
            SPEC_TEMPLATE_CONTENT="${SPEC_TEMPLATE_CONTENT%x}"
            SPEC_TEMPLATE_FOUND=true
        else
            resolve_status=$?
            if [ "$resolve_status" -ne 1 ]; then
                exit "$resolve_status"
            fi
        fi
    fi

    if [ "$REUSE" = true ]; then
        if [ ! -f "$SPEC_FILE" ]; then
            >&2 echo "[specify] Warning: reused spec '$SPEC_NAME' has no spec.md; nothing was created"
        fi
    else
        mkdir -p "$FEATURE_DIR"
    fi

    if [ "$NEEDS_SPEC" = true ]; then
        if [ "$SPEC_TEMPLATE_FOUND" = true ]; then
            printf '%s' "$SPEC_TEMPLATE_CONTENT" > "$SPEC_FILE"
        else
            echo "Warning: Spec template not found; created empty spec file" >&2
            touch "$SPEC_FILE"
        fi
    fi

    # Persist to .specify/feature.json so downstream commands can find the feature
    _persist_feature_json "$REPO_ROOT" "$FEATURE_DIR"

    # Inform the user how to set feature state in their own shell.
    # SPECIFY_FEATURE holds the branch name; consumers that need the spec
    # name derive it by stripping the type prefix.
    printf '# To persist: export SPECIFY_FEATURE=%s\n' "$(shell_quote "$BRANCH_NAME")" >&2
    printf '#              export SPECIFY_FEATURE_DIRECTORY=%s\n' "$(shell_quote "$FEATURE_DIR")" >&2
fi

if $JSON_MODE; then
    if command -v jq >/dev/null 2>&1; then
        if [ "$DRY_RUN" = true ]; then
            jq -cn \
                --arg branch_name "$BRANCH_NAME" \
                --arg spec_name "$SPEC_NAME" \
                --arg spec_file "$SPEC_FILE" \
                --arg feature_num "$FEATURE_NUM" \
                --arg branch_status "$BRANCH_STATUS" \
                --arg spec_action "$SPEC_ACTION" \
                '{BRANCH_NAME:$branch_name,SPEC_NAME:$spec_name,SPEC_FILE:$spec_file,FEATURE_NUM:$feature_num,BRANCH_STATUS:$branch_status,SPEC_ACTION:$spec_action,DRY_RUN:true}'
        else
            jq -cn \
                --arg branch_name "$BRANCH_NAME" \
                --arg spec_name "$SPEC_NAME" \
                --arg spec_file "$SPEC_FILE" \
                --arg feature_num "$FEATURE_NUM" \
                --arg branch_status "$BRANCH_STATUS" \
                --arg spec_action "$SPEC_ACTION" \
                '{BRANCH_NAME:$branch_name,SPEC_NAME:$spec_name,SPEC_FILE:$spec_file,FEATURE_NUM:$feature_num,BRANCH_STATUS:$branch_status,SPEC_ACTION:$spec_action}'
        fi
    else
        if [ "$DRY_RUN" = true ]; then
            printf '{"BRANCH_NAME":"%s","SPEC_NAME":"%s","SPEC_FILE":"%s","FEATURE_NUM":"%s","BRANCH_STATUS":"%s","SPEC_ACTION":"%s","DRY_RUN":true}\n' "$(json_escape "$BRANCH_NAME")" "$(json_escape "$SPEC_NAME")" "$(json_escape "$SPEC_FILE")" "$(json_escape "$FEATURE_NUM")" "$(json_escape "$BRANCH_STATUS")" "$(json_escape "$SPEC_ACTION")"
        else
            printf '{"BRANCH_NAME":"%s","SPEC_NAME":"%s","SPEC_FILE":"%s","FEATURE_NUM":"%s","BRANCH_STATUS":"%s","SPEC_ACTION":"%s"}\n' "$(json_escape "$BRANCH_NAME")" "$(json_escape "$SPEC_NAME")" "$(json_escape "$SPEC_FILE")" "$(json_escape "$FEATURE_NUM")" "$(json_escape "$BRANCH_STATUS")" "$(json_escape "$SPEC_ACTION")"
        fi
    fi
else
    echo "BRANCH_NAME: $BRANCH_NAME"
    echo "SPEC_NAME: $SPEC_NAME"
    echo "SPEC_FILE: $SPEC_FILE"
    echo "FEATURE_NUM: $FEATURE_NUM"
    echo "BRANCH_STATUS: $BRANCH_STATUS"
    echo "SPEC_ACTION: $SPEC_ACTION"
    if [ "$DRY_RUN" != true ]; then
        printf '# To persist in your shell: export SPECIFY_FEATURE=%s\n' "$(shell_quote "$BRANCH_NAME")"
        printf '#                           export SPECIFY_FEATURE_DIRECTORY=%s\n' "$(shell_quote "$FEATURE_DIR")"
    fi
fi
