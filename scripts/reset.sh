#!/usr/bin/env bash
# =============================================================================
# reset.sh — Clears all data except the admin user
#
# Deletes in reverse dependency order:
#   leases → boiler services → boilers → fire extinguisher revisions
#   → fire extinguishers → meters → rooms → peb scores → rent history
#   → housing units → buildings → persons → users (except admin)
#
# Usage:
#   ./scripts/reset.sh [BASE_URL] [ADMIN_USER] [ADMIN_PASS]
#
# Defaults:
#   BASE_URL   = http://localhost:8081
#   ADMIN_USER = admin
#   ADMIN_PASS = admin123
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
ADMIN_USER="${2:-admin}"
ADMIN_PASS="${3:-admin123}"

COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info()    { echo -e "  ${CYAN}ℹ${NC}  $*"; }
log_ok()      { echo -e "  ${GREEN}✔${NC}  $*"; }
log_warn()    { echo -e "  ${YELLOW}⚠${NC}  $*"; }
log_error()   { echo -e "  ${RED}✘${NC}  $*"; }
log_section() { echo -e "\n${CYAN}▶ $*${NC}"; }

get_resource() {
  curl -s -X GET "$BASE_URL$1" -H "Content-Type: application/json" -b "$COOKIE_JAR"
}

delete_resource() {
  local endpoint="$1"
  local label="$2"
  local status
  status=$(curl -s -o /tmp/reset_response.json -w "%{http_code}" \
    -X DELETE "$BASE_URL$endpoint" \
    -H "Content-Type: application/json" \
    -b "$COOKIE_JAR")
  case "$status" in
    200|204) log_ok "Deleted: $label";;
    404)     log_warn "Not found: $label";;
    *)       BODY=$(cat /tmp/reset_response.json 2>/dev/null)
             log_error "Failed: $label — HTTP $status: $BODY";;
  esac
}

# ─── Confirmation ─────────────────────────────────────────────────────────────
echo -e "${RED}"
echo "  ██████╗ ███████╗███████╗███████╗████████╗"
echo "  ██╔══██╗██╔════╝██╔════╝██╔════╝╚══██╔══╝"
echo "  ██████╔╝█████╗  ███████╗█████╗     ██║   "
echo "  ██╔══██╗██╔══╝  ╚════██║██╔══╝     ██║   "
echo "  ██║  ██║███████╗███████║███████╗   ██║   "
echo "  ╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝   ╚═╝   "
echo -e "${NC}"
echo "  This will DELETE ALL DATA on $BASE_URL"
echo "  except the '$ADMIN_USER' user."
echo ""
read -r -p "  Type 'yes' to confirm: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "  Aborted."
  exit 0
fi

# ─── Login ────────────────────────────────────────────────────────────────────
log_section "Authenticating as '$ADMIN_USER' on $BASE_URL"
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
  -c "$COOKIE_JAR")
if [ "$HTTP_STATUS" != "200" ]; then
  log_error "Login failed (HTTP $HTTP_STATUS)"
  exit 1
fi
log_ok "Authenticated"

# ─── 1. Leases ────────────────────────────────────────────────────────────────
log_section "1/9 — Deleting leases"
LEASES=$(get_resource "/api/v1/leases?status=ACTIVE&status=DRAFT&status=FINISHED&status=CANCELLED&size=1000")
LEASE_IDS=$(echo "$LEASES" | jq -r '.content[]?.id // empty')
COUNT=0
for id in $LEASE_IDS; do
  # Cancel first if ACTIVE or DRAFT (service may require it)
  STATUS_VAL=$(get_resource "/api/v1/leases/$id" | jq -r '.status')
  if [ "$STATUS_VAL" = "ACTIVE" ]; then
    curl -s -o /dev/null -X PATCH "$BASE_URL/api/v1/leases/$id/status" \
      -H "Content-Type: application/json" -b "$COOKIE_JAR" \
      -d '{"targetStatus":"CANCELLED"}' || true
  elif [ "$STATUS_VAL" = "DRAFT" ]; then
    curl -s -o /dev/null -X PATCH "$BASE_URL/api/v1/leases/$id/status" \
      -H "Content-Type: application/json" -b "$COOKIE_JAR" \
      -d '{"targetStatus":"CANCELLED"}' || true
  fi
  delete_resource "/api/v1/leases/$id" "Lease #$id"
  COUNT=$(( COUNT+1 ))
done
log_info "Leases deleted: $COUNT"

# ─── 2. Boilers ───────────────────────────────────────────────────────────────
log_section "2/9 — Deleting boilers"
# Fetch all boilers via buildings and units
BOILER_IDS=$(get_resource "/api/v1/boilers/alerts" | jq -r '.[].id // empty')
# Also fetch via buildings list
BUILDINGS=$(get_resource "/api/v1/buildings?size=1000")
BUILDING_IDS=$(echo "$BUILDINGS" | jq -r '.content[]?.id // empty')
ALL_BOILER_IDS=""
for bid in $BUILDING_IDS; do
  IDS=$(get_resource "/api/v1/buildings/$bid/boilers" | jq -r '.[].id // empty')
  ALL_BOILER_IDS="$ALL_BOILER_IDS $IDS"
  UNITS=$(get_resource "/api/v1/buildings/$bid/units" | jq -r '.[].id // empty')
  for uid in $UNITS; do
    IDS=$(get_resource "/api/v1/housing-units/$uid/boilers" | jq -r '.[].id // empty')
    ALL_BOILER_IDS="$ALL_BOILER_IDS $IDS"
  done
done
COUNT=0
for id in $ALL_BOILER_IDS; do
  [ -z "$id" ] && continue
  delete_resource "/api/v1/boilers/$id" "Boiler #$id"
  COUNT=$(( COUNT+1 ))
done
log_info "Boilers deleted: $COUNT"

# ─── 3. Fire Extinguishers ────────────────────────────────────────────────────
log_section "3/9 — Deleting fire extinguishers"
COUNT=0
for bid in $BUILDING_IDS; do
  FE_IDS=$(get_resource "/api/v1/buildings/$bid/fire-extinguishers" | jq -r '.[].id // empty')
  for id in $FE_IDS; do
    [ -z "$id" ] && continue
    delete_resource "/api/v1/fire-extinguishers/$id" "FireExtinguisher #$id"
    COUNT=$(( COUNT+1 ))
  done
done
log_info "Fire extinguishers deleted: $COUNT"

# ─── 4. Meters ────────────────────────────────────────────────────────────────
log_section "4/9 — Deleting meters"
COUNT=0
for bid in $BUILDING_IDS; do
  # Building meters
  METER_IDS=$(get_resource "/api/v1/buildings/$bid/meters" | jq -r '.[].id // empty')
  for id in $METER_IDS; do
    [ -z "$id" ] && continue
    # Close meter with today's date (removeMeter requires endDate)
    TODAY=$(date +%Y-%m-%d)
    curl -s -o /dev/null -X DELETE "$BASE_URL/api/v1/buildings/$bid/meters/$id" \
      -H "Content-Type: application/json" -b "$COOKIE_JAR" \
      -d "{\"endDate\":\"$TODAY\"}" || true
    log_ok "Closed: Meter #$id (building)"
    COUNT=$(( COUNT+1 ))
  done
  # Unit meters
  UNITS=$(get_resource "/api/v1/buildings/$bid/units" | jq -r '.[].id // empty')
  for uid in $UNITS; do
    METER_IDS=$(get_resource "/api/v1/housing-units/$uid/meters" | jq -r '.[].id // empty')
    for id in $METER_IDS; do
      [ -z "$id" ] && continue
      TODAY=$(date +%Y-%m-%d)
      curl -s -o /dev/null -X DELETE "$BASE_URL/api/v1/housing-units/$uid/meters/$id" \
        -H "Content-Type: application/json" -b "$COOKIE_JAR" \
        -d "{\"endDate\":\"$TODAY\"}" || true
      log_ok "Closed: Meter #$id (unit)"
      COUNT=$(( COUNT+1 ))
    done
  done
done
log_info "Meters closed: $COUNT"

# ─── 5. Housing Units ─────────────────────────────────────────────────────────
log_section "5/9 — Deleting housing units"
COUNT=0
for bid in $BUILDING_IDS; do
  UNIT_IDS=$(get_resource "/api/v1/buildings/$bid/units" | jq -r '.[].id // empty')
  for id in $UNIT_IDS; do
    [ -z "$id" ] && continue
    delete_resource "/api/v1/units/$id" "HousingUnit #$id"
    COUNT=$(( COUNT+1 ))
  done
done
log_info "Housing units deleted: $COUNT"

# ─── 6. Buildings ─────────────────────────────────────────────────────────────
log_section "6/9 — Deleting buildings"
COUNT=0
for id in $BUILDING_IDS; do
  [ -z "$id" ] && continue
  delete_resource "/api/v1/buildings/$id" "Building #$id"
  COUNT=$(( COUNT+1 ))
done
log_info "Buildings deleted: $COUNT"

# ─── 7. Persons ───────────────────────────────────────────────────────────────
log_section "7/9 — Deleting persons"
PERSONS=$(get_resource "/api/v1/persons?size=1000")
PERSON_IDS=$(echo "$PERSONS" | jq -r '.content[]?.id // empty')
COUNT=0
for id in $PERSON_IDS; do
  [ -z "$id" ] && continue
  delete_resource "/api/v1/persons/$id" "Person #$id"
  COUNT=$(( COUNT+1 ))
done
log_info "Persons deleted: $COUNT"

# ─── 8. Users (except admin) ──────────────────────────────────────────────────
log_section "8/9 — Deleting users (except '$ADMIN_USER')"
USERS=$(get_resource "/api/v1/users")
COUNT=0
echo "$USERS" | jq -c '.[]' | while read -r user; do
  id=$(echo "$user" | jq -r '.id')
  username=$(echo "$user" | jq -r '.username')
  if [ "$username" = "$ADMIN_USER" ]; then
    log_warn "Skipped: $username (protected)"
    continue
  fi
  delete_resource "/api/v1/users/$id" "User: $username"
  COUNT=$(( COUNT+1 ))
done
log_info "Users deleted: $COUNT"

# ─── Logout ───────────────────────────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/v1/auth/logout" -b "$COOKIE_JAR" || true

echo ""
log_ok "Reset complete — only '$ADMIN_USER' remains."