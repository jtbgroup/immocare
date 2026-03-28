#!/usr/bin/env bash
# =============================================================================
# seed.sh — Injects demo data via the REST API
#
# Usage:
#   ./scripts/seed.sh [BASE_URL] [ADMIN_USER] [ADMIN_PASS] [USERS_FILE]
#
# Defaults (dev):
#   BASE_URL   = http://localhost:8081
#   ADMIN_USER = admin
#   ADMIN_PASS = admin123
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
ADMIN_USER="${2:-admin}"
ADMIN_PASS="${3:-admin123}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$SCRIPT_DIR/demo-data"

USERS_FILE="${4:-$DATA_DIR/users.json}"

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
  -X POST "$BASE_URL/api/v1/auth/login" \
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
  log_warn "No users file found at $USERS_FILE — skipping"
else
  TOTAL=$(jq length "$USERS_FILE")
  CREATED=0
  SKIPPED=0

  for i in $(seq 0 $(( TOTAL - 1 ))); do
    USER=$(jq -c ".[$i]" "$USERS_FILE")
    USERNAME=$(echo "$USER" | jq -r '.username')

    HTTP_STATUS=$(curl -s -o /tmp/seed_response.json -w "%{http_code}" \
      -X POST "$BASE_URL/api/v1/users" \
      -H "Content-Type: application/json" \
      -b "$COOKIE_JAR" \
      -d "$USER")

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

# ─── Logout ──────────────────────────────────────────────────────────────────
curl -s -o /dev/null \
  -X POST "$BASE_URL/api/v1/auth/logout" \
  -b "$COOKIE_JAR"

echo ""
log_ok "Done. Demo data injected via API."