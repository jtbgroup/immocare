#!/usr/bin/env bash
# =============================================================================
# seed-demo.sh — Injects demo data via the REST API
#
# Usage:
#   ./scripts/seed-demo.sh [BASE_URL] [ADMIN_USER] [ADMIN_PASS]
#
# Defaults (dev):
#   BASE_URL   = http://localhost:8081
#   ADMIN_USER = admin
#   ADMIN_PASS = admin123
#
# Prerequisites:
#   - jq must be installed  (brew install jq / apt install jq)
#   - The application must be running and reachable at BASE_URL
#
# All data goes through the REST API so model validation rules are enforced.
# This file (and scripts/demo-data/*.json) is the single source of truth
# and is intended to also serve as Cypress fixtures in the future.
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
ADMIN_USER="${2:-admin}"
ADMIN_PASS="${3:-admin123}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$SCRIPT_DIR/demo-data"

# 4th argument: custom users JSON file (absolute or relative to CWD)
# Defaults to scripts/demo-data/users.json
USERS_FILE="${4:-$DATA_DIR/users.json}"
PROJECTS_FILE="${5:-$DATA_DIR/projects.json}"

COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT

# ─── Colors ──────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()    { echo -e "  ${CYAN}ℹ${NC}  $*"; }
log_ok()      { echo -e "  ${GREEN}✔${NC}  $*"; }
log_warn()    { echo -e "  ${YELLOW}⚠${NC}  $*"; }
log_error()   { echo -e "  ${RED}✘${NC}  $*"; }
log_section() { echo -e "\n${CYAN}▶ $*${NC}"; }

# ─── Dependency check ────────────────────────────────────────────────────────
if ! command -v jq &> /dev/null; then
  log_error "jq is required but not installed."
  echo "       Install it with: brew install jq  (macOS) or  apt install jq  (Linux)"
  exit 1
fi

# ─── Admin login ─────────────────────────────────────────────────────────────
log_section "Authenticating as '$ADMIN_USER' on $BASE_URL"

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
  -c "$COOKIE_JAR")

if [ "$HTTP_STATUS" != "200" ]; then
  log_error "Login failed (HTTP $HTTP_STATUS). Is the app running at $BASE_URL?"
  exit 1
fi

log_ok "Authenticated successfully"

# ─── Seed users ──────────────────────────────────────────────────────────────
log_section "Seeding users from $USERS_FILE"

if [ ! -f "$USERS_FILE" ]; then
  log_warn "No users.json found at $USERS_FILE — skipping"
else
  TOTAL=$(jq length "$USERS_FILE")
  CREATED=0
  SKIPPED=0

  for i in $(seq 0 $(( TOTAL - 1 ))); do
    USER=$(jq -c ".[$i]" "$USERS_FILE")
    USERNAME=$(echo "$USER" | jq -r '.username')

    # Ensure the payload uses "roles" (array) — rename "role" to "roles" if needed
    # and wrap scalar value in array if necessary
    NORMALIZED_USER=$(echo "$USER" | jq '
      if has("role") and (.role | type) == "string" then
        . + { "roles": [.role] } | del(.role)
      elif has("roles") and (.roles | type) == "string" then
        . + { "roles": [.roles] }
      else
        .
      end
    ')

    HTTP_STATUS=$(curl -s -o /tmp/seed_response.json -w "%{http_code}" \
      -X POST "$BASE_URL/api/users" \
      -H "Content-Type: application/json" \
      -b "$COOKIE_JAR" \
      -d "$NORMALIZED_USER")

    case "$HTTP_STATUS" in
      200|201)
        log_ok "Created user: $USERNAME"
        CREATED=$(( CREATED + 1 ))
        ;;
      409)
        log_warn "Skipped user: $USERNAME (already exists)"
        SKIPPED=$(( SKIPPED + 1 ))
        ;;
      *)
        REASON=$(jq -r '.message // "unknown error"' /tmp/seed_response.json 2>/dev/null || echo "unknown error")
        log_error "Failed to create user: $USERNAME (HTTP $HTTP_STATUS — $REASON)"
        ;;
    esac
  done

  echo ""
  log_info "Users: $CREATED created, $SKIPPED skipped (total: $TOTAL)"
fi

# ─── Fetch user list (needed to resolve usernames → IDs for projects) ─────────
log_section "Fetching user list for project manager resolution"

USERS_RESPONSE=$(curl -s \
  -X GET "$BASE_URL/api/users" \
  -H "Content-Type: application/json" \
  -b "$COOKIE_JAR")

# ─── Seed projects ────────────────────────────────────────────────────────────
log_section "Seeding projects from $PROJECTS_FILE"

if [ ! -f "$PROJECTS_FILE" ]; then
  log_warn "No projects.json found at $PROJECTS_FILE — skipping"
else
  TOTAL=$(jq length "$PROJECTS_FILE")
  CREATED=0
  SKIPPED=0

  for i in $(seq 0 $(( TOTAL - 1 ))); do
    PROJECT=$(jq -c ".[$i]" "$PROJECTS_FILE")
    REFERENCE=$(echo "$PROJECT" | jq -r '.reference')
    PM_USERNAME=$(echo "$PROJECT" | jq -r '.projectManagerUsername')

    # Resolve project manager username → ID
    PM_ID=$(echo "$USERS_RESPONSE" | jq -r --arg uname "$PM_USERNAME" \
      '.[] | select(.username == $uname) | .id')

    if [ -z "$PM_ID" ] || [ "$PM_ID" = "null" ]; then
      log_error "Project manager '$PM_USERNAME' not found — skipping project: $REFERENCE"
      continue
    fi

    # Build the payload: replace projectManagerUsername with projectManagerId,
    # remove null optional fields to keep the payload clean
    PAYLOAD=$(echo "$PROJECT" | jq --arg pmId "$PM_ID" '
      . + { "projectManagerId": $pmId }
      | del(.projectManagerUsername)
      | del(.[] | nulls)
      | if .pordBia == null then del(.pordBia) else . end
      | if .pordProject == null then del(.pordProject) else . end
    ')

    HTTP_STATUS=$(curl -s -o /tmp/seed_response.json -w "%{http_code}" \
      -X POST "$BASE_URL/api/projects" \
      -H "Content-Type: application/json" \
      -b "$COOKIE_JAR" \
      -d "$PAYLOAD")

    case "$HTTP_STATUS" in
      200|201)
        log_ok "Created project: $REFERENCE"
        CREATED=$(( CREATED + 1 ))
        ;;
      409)
        log_warn "Skipped project: $REFERENCE (reference already exists)"
        SKIPPED=$(( SKIPPED + 1 ))
        ;;
      *)
        REASON=$(jq -r '.message // "unknown error"' /tmp/seed_response.json 2>/dev/null || echo "unknown error")
        log_error "Failed to create project: $REFERENCE (HTTP $HTTP_STATUS — $REASON)"
        ;;
    esac
  done

  echo ""
  log_info "Projects: $CREATED created, $SKIPPED skipped (total: $TOTAL)"
fi

# ─── Logout ──────────────────────────────────────────────────────────────────
curl -s -o /dev/null \
  -X POST "$BASE_URL/api/auth/logout" \
  -b "$COOKIE_JAR"

echo ""
log_ok "Done. Demo data injected via API."