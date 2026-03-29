#!/usr/bin/env bash
# =============================================================================
# seed.sh — Injects full demo or real data via the REST API
#
# Correct routes (from controllers):
#   POST /api/v1/users
#   POST /api/v1/persons
#   POST /api/v1/persons/{personId}/bank-accounts               ← PersonBankAccountController
#   POST /api/v1/buildings
#   POST /api/v1/units                                          ← HousingUnitController
#   GET  /api/v1/buildings/{id}/units                           ← for 409 recovery
#   POST /api/v1/housing-units/{unitId}/boilers                 ← BoilerController
#   POST /api/v1/boilers/{boilerId}/services                    ← BoilerServiceController
#   POST /api/v1/housing-units/{unitId}/meters                  ← MeterController
#   POST /api/v1/buildings/{buildingId}/fire-extinguishers      ← FireExtinguisherController
#   POST /api/v1/fire-extinguishers/{id}/revisions
#   POST /api/v1/leases
#   POST /api/v1/leases/{id}/tenants
#   PATCH /api/v1/leases/{id}/status
#
# Usage:
#   ./scripts/seed.sh [BASE_URL] [ADMIN_USER] [ADMIN_PASS] [DATA_DIR]
#
# Defaults:
#   BASE_URL   = http://localhost:8081
#   ADMIN_USER = admin
#   ADMIN_PASS = admin123
#   DATA_DIR   = scripts/demo-data
#
# Examples:
#   make seed-demo                   → demo data on dev
#   make seed-real                   → real data on prod
#   ./scripts/seed.sh http://localhost:8081 admin admin123 scripts/real-data
#
# persons.json supports an optional `bankAccounts` array per person:
#   "bankAccounts": [
#     { "iban": "BE12 3456 7890 1234", "label": "Compte principal", "isPrimary": true }
#   ]
# =============================================================================

set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
ADMIN_USER="${2:-admin}"
ADMIN_PASS="${3:-admin123}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="${4:-$SCRIPT_DIR/demo-data}"

COOKIE_JAR=$(mktemp)
trap 'rm -f "$COOKIE_JAR"' EXIT

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
log_info()    { echo -e "  ${CYAN}ℹ${NC}  $*"; }
log_ok()      { echo -e "  ${GREEN}✔${NC}  $*"; }
log_warn()    { echo -e "  ${YELLOW}⚠${NC}  $*"; }
log_error()   { echo -e "  ${RED}✘${NC}  $*"; }
log_section() { echo -e "\n${CYAN}▶ $*${NC}"; }

log_failure() {
  local label="$1" status="$2" file="$3"
  local body; body=$(cat "$file" 2>/dev/null || echo "(empty)")
  log_error "$label — HTTP $status"
  log_error "  Body: $body"
}

if ! command -v jq &> /dev/null; then
  log_error "jq is required. Install: brew install jq (macOS) or apt install jq (Linux)"
  exit 1
fi

# ─── Pre-flight: check for duplicate names ────────────────────────────────────
log_section "Pre-flight checks"
PREFLIGHT_OK=true

BUILDING_NAMES=$(jq -r '.[].name' "$DATA_DIR/buildings.json" | sort)
BUILDING_DUPES=$(echo "$BUILDING_NAMES" | uniq -d)
if [ -n "$BUILDING_DUPES" ]; then
  log_error "Duplicate building names in buildings.json — names must be unique:"
  echo "$BUILDING_DUPES" | while read -r d; do log_error "  → '$d'"; done
  PREFLIGHT_OK=false
fi

UNIT_BNAMES=$(jq -r '.[].buildingName' "$DATA_DIR/housing_units.json" | sort -u)
while IFS= read -r bname; do
  if ! jq -e --arg n "$bname" '.[] | select(.name == $n)' "$DATA_DIR/buildings.json" > /dev/null 2>&1; then
    log_error "housing_units.json references unknown building: '$bname'"
    PREFLIGHT_OK=false
  fi
done <<< "$UNIT_BNAMES"

METER_BNAMES=$(jq -r '.[].buildingName' "$DATA_DIR/meters.json" | sort -u)
while IFS= read -r bname; do
  if ! jq -e --arg n "$bname" '.[] | select(.name == $n)' "$DATA_DIR/buildings.json" > /dev/null 2>&1; then
    log_error "meters.json references unknown building: '$bname'"
    PREFLIGHT_OK=false
  fi
done <<< "$METER_BNAMES"

if [ "$PREFLIGHT_OK" = false ]; then
  log_error "Pre-flight failed — fix the issues above before running the seed."
  exit 1
fi
log_ok "Pre-flight checks passed"

get_resource() {
  curl -s -X GET "$BASE_URL$1" -H "Content-Type: application/json" -b "$COOKIE_JAR"
}

# ─── Login ────────────────────────────────────────────────────────────────────
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
log_ok "Authenticated"

# ─── 1. Users ─────────────────────────────────────────────────────────────────
USERS_FILE="$DATA_DIR/users.json"
if [ -f "$USERS_FILE" ]; then
  log_section "1/8 — Seeding users"
  TOTAL=$(jq length "$USERS_FILE"); CREATED=0; SKIPPED=0
  for i in $(seq 0 $(( TOTAL - 1 ))); do
    USER=$(jq -c ".[$i]" "$USERS_FILE")
    USERNAME=$(echo "$USER" | jq -r '.username')
    S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
      -X POST "$BASE_URL/api/v1/users" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$USER")
    case "$S" in
      200|201) log_ok "User: $USERNAME"; CREATED=$(( CREATED+1 ));;
      409)     log_warn "User: $USERNAME (already exists)"; SKIPPED=$(( SKIPPED+1 ));;
      *)       log_failure "User: $USERNAME" "$S" /tmp/sr.json;;
    esac
  done
  log_info "Users: $CREATED created, $SKIPPED skipped"
fi

# ─── 2. Persons + bank accounts ───────────────────────────────────────────────
log_section "2/8 — Seeding persons (+ bank accounts)"
PERSONS_FILE="$DATA_DIR/persons.json"
TOTAL=$(jq length "$PERSONS_FILE")
declare -A PERSON_ID_MAP    # indexed by nationalId
declare -A PERSON_EMAIL_MAP # indexed by email (lowercase)

seed_bank_accounts() {
  local person_id="$1"
  local person_json="$2"
  local BANK_ACCOUNTS BA_COUNT BA IBAN BAS
  BANK_ACCOUNTS=$(echo "$person_json" | jq '.bankAccounts // []')
  BA_COUNT=$(echo "$BANK_ACCOUNTS" | jq length)
  for k in $(seq 0 $(( BA_COUNT - 1 ))); do
    BA=$(echo "$BANK_ACCOUNTS" | jq -c ".[$k]")
    IBAN=$(echo "$BA" | jq -r '.iban')
    BAS=$(curl -s -o /tmp/ba.json -w "%{http_code}" \
      -X POST "$BASE_URL/api/v1/persons/$person_id/bank-accounts" \
      -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$BA")
    case "$BAS" in
      200|201) log_ok "  Bank account: $IBAN";;
      409)     log_warn "  Bank account: $IBAN (already exists)";;
      *)       log_failure "  Bank account: $IBAN" "$BAS" /tmp/ba.json;;
    esac
  done
}

for i in $(seq 0 $(( TOTAL - 1 ))); do
  P=$(jq -c ".[$i]" "$PERSONS_FILE")
  NID=$(echo "$P"   | jq -r '.nationalId // empty')
  EMAIL=$(echo "$P" | jq -r '.email // empty' | tr '[:upper:]' '[:lower:]')
  NAME=$(echo "$P"  | jq -r '"\(.firstName) \(.lastName)"')

  # Strip bankAccounts before POSTing — not part of CreatePersonRequest
  PAYLOAD=$(echo "$P" | jq 'del(.bankAccounts)')

  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/persons" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  case "$S" in
    200|201)
      ID=$(jq -r '.id' /tmp/sr.json)
      [ -n "$NID" ]   && PERSON_ID_MAP["$NID"]="$ID"
      [ -n "$EMAIL" ] && PERSON_EMAIL_MAP["$EMAIL"]="$ID"
      log_ok "Person: $NAME (id=$ID)"
      seed_bank_accounts "$ID" "$P"
      ;;
    409)
      # Recover id: try nationalId search first, then email
      ID=""
      if [ -n "$NID" ]; then
        SEARCH=$(get_resource "/api/v1/persons?search=$NID&size=5")
        ID=$(echo "$SEARCH" | jq -r '.content[]? | select(.nationalId == "'"$NID"'") | .id' | head -1)
      fi
      if [ -z "$ID" ] && [ -n "$EMAIL" ]; then
        ENC_EMAIL=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$EMAIL" 2>/dev/null || echo "$EMAIL")
        SEARCH=$(get_resource "/api/v1/persons?search=$ENC_EMAIL&size=10")
        ID=$(echo "$SEARCH" | jq -r --arg em "$EMAIL" '.content[]? | select((.email // "" | ascii_downcase) == $em) | .id' | head -1)
      fi
      [ -n "$NID" ]   && PERSON_ID_MAP["$NID"]="${ID:-}"
      [ -n "$EMAIL" ] && PERSON_EMAIL_MAP["$EMAIL"]="${ID:-}"
      log_warn "Person: $NAME (already exists${ID:+, id=$ID})"
      # Attempt bank accounts in case they were missed on a previous run
      [ -n "$ID" ] && seed_bank_accounts "$ID" "$P"
      ;;
    *)
      log_failure "Person: $NAME" "$S" /tmp/sr.json
      ;;
  esac
done

# ─── 3. Buildings ─────────────────────────────────────────────────────────────
log_section "3/8 — Seeding buildings"
BUILDINGS_FILE="$DATA_DIR/buildings.json"
TOTAL=$(jq length "$BUILDINGS_FILE")
declare -A BUILDING_ID_MAP

for i in $(seq 0 $(( TOTAL - 1 ))); do
  B=$(jq -c ".[$i]" "$BUILDINGS_FILE")
  BNAME=$(echo "$B" | jq -r '.name')
  OWNER_NID=$(echo "$B" | jq -r '.ownerPersonNationalId // empty')
  PAYLOAD=$(echo "$B" | jq 'del(.ownerPersonNationalId)')
  if [ -n "$OWNER_NID" ] && [ -n "${PERSON_ID_MAP[$OWNER_NID]+_}" ] && [ -n "${PERSON_ID_MAP[$OWNER_NID]}" ]; then
    PAYLOAD=$(echo "$PAYLOAD" | jq --argjson oid "${PERSON_ID_MAP[$OWNER_NID]}" '. + {ownerId: $oid}')
  fi
  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/buildings" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  case "$S" in
    200|201)
      ID=$(jq -r '.id' /tmp/sr.json)
      BUILDING_ID_MAP["$BNAME"]="$ID"
      log_ok "Building: $BNAME (id=$ID)"
      ;;
    409)
      ENC=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$BNAME" 2>/dev/null || echo "$BNAME")
      ID=$(get_resource "/api/v1/buildings?search=$ENC&size=20" | jq -r '.content[]? | select(.name == "'"$BNAME"'") | .id' | head -1)
      if [ -z "$ID" ]; then
        log_error "Building: '$BNAME' already exists but id could not be resolved — dependent resources will be skipped"
      else
        BUILDING_ID_MAP["$BNAME"]="$ID"
        log_warn "Building: $BNAME (already exists, id=$ID)"
      fi
      ;;
    *)
      log_failure "Building: $BNAME" "$S" /tmp/sr.json
      ;;
  esac
done

# ─── 4. Housing Units ─────────────────────────────────────────────────────────
log_section "4/8 — Seeding housing units"
UNITS_FILE="$DATA_DIR/housing_units.json"
TOTAL=$(jq length "$UNITS_FILE")
declare -A UNIT_ID_MAP

for i in $(seq 0 $(( TOTAL - 1 ))); do
  U=$(jq -c ".[$i]" "$UNITS_FILE")
  BNAME=$(echo "$U" | jq -r '.buildingName')
  UNIT_NUM=$(echo "$U" | jq -r '.unitNumber')
  OWNER_NID=$(echo "$U" | jq -r '.ownerPersonNationalId // empty')
  BID="${BUILDING_ID_MAP[$BNAME]:-}"
  if [ -z "$BID" ]; then log_error "Unit $UNIT_NUM: building '$BNAME' not resolved — skipped"; continue; fi

  PAYLOAD=$(echo "$U" | jq 'del(.buildingName) | del(.ownerPersonNationalId)' \
    | jq --argjson bid "$BID" '. + {buildingId: $bid}')
  if [ -n "$OWNER_NID" ] && [ -n "${PERSON_ID_MAP[$OWNER_NID]+_}" ] && [ -n "${PERSON_ID_MAP[$OWNER_NID]}" ]; then
    PAYLOAD=$(echo "$PAYLOAD" | jq --argjson oid "${PERSON_ID_MAP[$OWNER_NID]}" '. + {ownerId: $oid}')
  fi

  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/units" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  KEY="$BNAME|$UNIT_NUM"
  case "$S" in
    200|201)
      ID=$(jq -r '.id' /tmp/sr.json)
      UNIT_ID_MAP["$KEY"]="$ID"
      log_ok "Unit: $BNAME/$UNIT_NUM (id=$ID)"
      ;;
    409)
      ID=$(get_resource "/api/v1/buildings/$BID/units" | jq -r '.[]? | select(.unitNumber == "'"$UNIT_NUM"'") | .id' | head -1)
      UNIT_ID_MAP["$KEY"]="${ID:-}"
      log_warn "Unit: $BNAME/$UNIT_NUM (already exists${ID:+, id=$ID})"
      ;;
    *)
      log_failure "Unit: $BNAME/$UNIT_NUM" "$S" /tmp/sr.json
      ;;
  esac
done

# ─── 5. Boilers ───────────────────────────────────────────────────────────────
log_section "5/8 — Seeding boilers"
BOILERS_FILE="$DATA_DIR/boilers.json"
TOTAL=$(jq length "$BOILERS_FILE")

for i in $(seq 0 $(( TOTAL - 1 ))); do
  B=$(jq -c ".[$i]" "$BOILERS_FILE")
  BNAME=$(echo "$B" | jq -r '.buildingName')
  UNIT_NUM=$(echo "$B" | jq -r '.unitNumber')
  BRAND=$(echo "$B" | jq -r '.brand // "?"')
  KEY="$BNAME|$UNIT_NUM"
  UNIT_ID="${UNIT_ID_MAP[$KEY]:-}"
  if [ -z "$UNIT_ID" ]; then log_error "Boiler $BRAND: unit '$KEY' not resolved — skipped"; continue; fi

  PAYLOAD=$(echo "$B" | jq 'del(.buildingName) | del(.unitNumber) | del(.ownerType) | del(.services)')
  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/housing-units/$UNIT_ID/boilers" \
    -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  case "$S" in
    200|201)
      BOILER_ID=$(jq -r '.id' /tmp/sr.json)
      log_ok "Boiler: $BRAND @ $BNAME/$UNIT_NUM (id=$BOILER_ID)"

      SERVICES=$(echo "$B" | jq '.services // []')
      SVC_COUNT=$(echo "$SERVICES" | jq length)
      for j in $(seq 0 $(( SVC_COUNT - 1 ))); do
        SVC=$(echo "$SERVICES" | jq -c ".[$j]")
        SVC_DATE=$(echo "$SVC" | jq -r '.serviceDate')
        SS=$(curl -s -o /tmp/svr.json -w "%{http_code}" \
          -X POST "$BASE_URL/api/v1/boilers/$BOILER_ID/services" \
          -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$SVC")
        if [ "$SS" = "200" ] || [ "$SS" = "201" ]; then
          log_ok "  Service $SVC_DATE"
        else
          log_failure "  Service $SVC_DATE" "$SS" /tmp/svr.json
        fi
      done
      ;;
    409) log_warn "Boiler: $BRAND @ $BNAME/$UNIT_NUM (already exists)";;
    *)   log_failure "Boiler: $BRAND @ $BNAME/$UNIT_NUM" "$S" /tmp/sr.json;;
  esac
done

# ─── 6. Meters ────────────────────────────────────────────────────────────────
log_section "6/8 — Seeding meters"
METERS_FILE="$DATA_DIR/meters.json"
TOTAL=$(jq length "$METERS_FILE")

for i in $(seq 0 $(( TOTAL - 1 ))); do
  M=$(jq -c ".[$i]" "$METERS_FILE")
  BNAME=$(echo "$M" | jq -r '.buildingName')
  OWNER_TYPE=$(echo "$M" | jq -r '.ownerType // "HOUSING_UNIT"')
  MTYPE=$(echo "$M" | jq -r '.type')
  MNUM=$(echo "$M" | jq -r '.meterNumber')

  PAYLOAD=$(echo "$M" | jq 'del(.buildingName) | del(.unitNumber) | del(.ownerType)')

  if [ "$OWNER_TYPE" = "BUILDING" ]; then
    BID="${BUILDING_ID_MAP[$BNAME]:-}"
    if [ -z "$BID" ]; then
      log_error "Meter $MTYPE/$MNUM: building '$BNAME' not resolved — skipped"
      continue
    fi
    ENDPOINT="$BASE_URL/api/v1/buildings/$BID/meters"
  else
    UNIT_NUM=$(echo "$M" | jq -r '.unitNumber')
    KEY="$BNAME|$UNIT_NUM"
    UNIT_ID="${UNIT_ID_MAP[$KEY]:-}"
    if [ -z "$UNIT_ID" ]; then
      log_error "Meter $MTYPE/$MNUM: unit '$KEY' not resolved — skipped"
      continue
    fi
    ENDPOINT="$BASE_URL/api/v1/housing-units/$UNIT_ID/meters"
  fi

  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  case "$S" in
    200|201) log_ok "Meter: $MTYPE $MNUM (id=$(jq -r '.id' /tmp/sr.json))";;
    409)     log_warn "Meter: $MTYPE $MNUM (already exists)";;
    *)       log_failure "Meter: $MTYPE/$MNUM" "$S" /tmp/sr.json;;
  esac
done

# ─── 7. Fire Extinguishers ────────────────────────────────────────────────────
log_section "7/8 — Seeding fire extinguishers"
FE_FILE="$DATA_DIR/fire_extinguishers.json"
TOTAL=$(jq length "$FE_FILE")

for i in $(seq 0 $(( TOTAL - 1 ))); do
  FE=$(jq -c ".[$i]" "$FE_FILE")
  BNAME=$(echo "$FE" | jq -r '.buildingName')
  FENUM=$(echo "$FE" | jq -r '.identificationNumber')
  BID="${BUILDING_ID_MAP[$BNAME]:-}"
  if [ -z "$BID" ]; then log_error "Extinguisher $FENUM: building '$BNAME' not resolved — skipped"; continue; fi

  PAYLOAD=$(echo "$FE" | jq 'del(.buildingName) | del(.revisions)')
  S=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/buildings/$BID/fire-extinguishers" \
    -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")
  case "$S" in
    200|201)
      FE_ID=$(jq -r '.id' /tmp/sr.json)
      log_ok "Extinguisher: $FENUM @ $BNAME (id=$FE_ID)"
      REVISIONS=$(echo "$FE" | jq '.revisions // []')
      REV_COUNT=$(echo "$REVISIONS" | jq length)
      for j in $(seq 0 $(( REV_COUNT - 1 ))); do
        REV=$(echo "$REVISIONS" | jq -c ".[$j]")
        REV_DATE=$(echo "$REV" | jq -r '.revisionDate')
        RS=$(curl -s -o /tmp/rr.json -w "%{http_code}" \
          -X POST "$BASE_URL/api/v1/fire-extinguishers/$FE_ID/revisions" \
          -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$REV")
        if [ "$RS" = "200" ] || [ "$RS" = "201" ]; then
          log_ok "  Revision $REV_DATE"
        else
          log_failure "  Revision $REV_DATE" "$RS" /tmp/rr.json
        fi
      done
      ;;
    409) log_warn "Extinguisher: $FENUM @ $BNAME (already exists)";;
    *)   log_failure "Extinguisher: $FENUM @ $BNAME" "$S" /tmp/sr.json;;
  esac
done

# ─── 8. Leases ────────────────────────────────────────────────────────────────
# Strategy to avoid unique constraint on (housing_unit_id, status):
#   FINISHED → create as DRAFT, PATCH → ACTIVE → PATCH → FINISHED
#   ACTIVE   → create with ?activate=true (bypasses DRAFT entirely)
# Each FINISHED lease must be fully committed before the next one starts.
log_section "8/8 — Seeding leases"
LEASES_FILE="$DATA_DIR/leases.json"
TOTAL=$(jq length "$LEASES_FILE")
CREATED=0; SKIPPED=0; ERRORS=0

for i in $(seq 0 $(( TOTAL - 1 ))); do
  L=$(jq -c ".[$i]" "$LEASES_FILE")
  BNAME=$(echo "$L" | jq -r '.buildingName // empty')
  [ -z "$BNAME" ] && continue

  UNIT_NUM=$(echo "$L" | jq -r '.unitNumber')
  STATUS_VAL=$(echo "$L" | jq -r '.status')
  LABEL="Lease ($STATUS_VAL) $BNAME/$UNIT_NUM"
  KEY="$BNAME|$UNIT_NUM"
  UNIT_ID="${UNIT_ID_MAP[$KEY]:-}"
  if [ -z "$UNIT_ID" ]; then
    log_error "$LABEL: unit not resolved — skipped"
    ERRORS=$(( ERRORS+1 )); continue
  fi

  # ── Resolve tenants ────────────────────────────────────────────────────────
  # Tenants may use `email` instead of `nationalId`.
  # Resolution order: nationalId → local email map → live API search.
  TENANTS_JSON=$(echo "$L" | jq '.tenants // []')
  TENANT_COUNT=$(echo "$TENANTS_JSON" | jq length)
  RESOLVED_TENANTS='[]'
  for j in $(seq 0 $(( TENANT_COUNT - 1 ))); do
    T=$(echo "$TENANTS_JSON" | jq -c ".[$j]")
    TNID=$(echo "$T"   | jq -r '.nationalId // empty')
    TEMAIL=$(echo "$T" | jq -r '.email // empty' | tr '[:upper:]' '[:lower:]')
    TROLE=$(echo "$T"  | jq -r '.role')

    TID=""

    # 1. Try nationalId
    if [ -n "$TNID" ] && [ "$TNID" != "null" ]; then
      TID="${PERSON_ID_MAP[$TNID]:-}"
    fi

    # 2. Fallback: local email map
    if [ -z "$TID" ] && [ -n "$TEMAIL" ]; then
      TID="${PERSON_EMAIL_MAP[$TEMAIL]:-}"
    fi

    # 3. Fallback: live API search by email, with caching
    if [ -z "$TID" ] && [ -n "$TEMAIL" ]; then
      ENC_EMAIL=$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$TEMAIL" 2>/dev/null || echo "$TEMAIL")
      SEARCH=$(get_resource "/api/v1/persons?search=$ENC_EMAIL&size=10")
      TID=$(echo "$SEARCH" | jq -r --arg em "$TEMAIL" '.content[]? | select((.email // "" | ascii_downcase) == $em) | .id' | head -1)
      [ -n "$TID" ] && PERSON_EMAIL_MAP["$TEMAIL"]="$TID"
    fi

    if [ -z "$TID" ]; then
      log_warn "  Tenant not resolved (nationalId='$TNID', email='$TEMAIL') — skipped"
      continue
    fi

    RESOLVED_TENANTS=$(echo "$RESOLVED_TENANTS" | \
      jq --argjson pid "$TID" --arg role "$TROLE" '. + [{personId: $pid, role: $role}]')
  done

  PAYLOAD=$(echo "$L" \
    | jq 'del(.buildingName) | del(.unitNumber) | del(.tenants) | del(._comment)' \
    | jq --argjson uid "$UNIT_ID" '. + {housingUnitId: $uid}' \
    | jq --argjson tenants "$RESOLVED_TENANTS" '. + {tenants: $tenants}' \
    | jq '. + {status: "DRAFT"}')

  if [ "$STATUS_VAL" = "ACTIVE" ]; then
    CREATE_URL="$BASE_URL/api/v1/leases?activate=true"
  else
    CREATE_URL="$BASE_URL/api/v1/leases"
  fi

  LS=$(curl -s -o /tmp/sr.json -w "%{http_code}" \
    -X POST "$CREATE_URL" -H "Content-Type: application/json" -b "$COOKIE_JAR" -d "$PAYLOAD")

  case "$LS" in
    200|201)
      LEASE_ID=$(jq -r '.id' /tmp/sr.json)
      log_ok "$LABEL (id=$LEASE_ID)"
      CREATED=$(( CREATED+1 ))

      # FINISHED: DRAFT → ACTIVE → FINISHED
      if [ "$STATUS_VAL" = "FINISHED" ]; then
        SS=$(curl -s -o /tmp/ss.json -w "%{http_code}" \
          -X PATCH "$BASE_URL/api/v1/leases/$LEASE_ID/status" \
          -H "Content-Type: application/json" -b "$COOKIE_JAR" \
          -d '{"targetStatus":"ACTIVE"}')
        if [ "$SS" = "200" ] || [ "$SS" = "201" ]; then
          SS2=$(curl -s -o /tmp/ss2.json -w "%{http_code}" \
            -X PATCH "$BASE_URL/api/v1/leases/$LEASE_ID/status" \
            -H "Content-Type: application/json" -b "$COOKIE_JAR" \
            -d '{"targetStatus":"FINISHED"}')
          if [ "$SS2" = "200" ] || [ "$SS2" = "201" ]; then
            log_ok "  Status → FINISHED"
          else
            log_failure "  Status → FINISHED (step 2)" "$SS2" /tmp/ss2.json
            log_error "  !! Next lease for $BNAME/$UNIT_NUM may fail (slot still occupied)"
          fi
        else
          log_failure "  Status → ACTIVE (step 1 toward FINISHED)" "$SS" /tmp/ss.json
          log_error "  !! Next lease for $BNAME/$UNIT_NUM may fail (slot still occupied)"
        fi
      fi
      ;;
    409)
      log_warn "$LABEL (already exists)"
      SKIPPED=$(( SKIPPED+1 ))
      ;;
    *)
      log_failure "$LABEL" "$LS" /tmp/sr.json
      ERRORS=$(( ERRORS+1 ))
      ;;
  esac
done
log_info "Leases: $CREATED created, $SKIPPED skipped, $ERRORS errors"

# ─── Logout ───────────────────────────────────────────────────────────────────
curl -s -o /dev/null -X POST "$BASE_URL/api/v1/auth/logout" -b "$COOKIE_JAR" || true

echo ""
log_ok "Full seed completed!"
echo ""
echo "  Summary:"
echo "    Buildings          : $(jq length "$DATA_DIR/buildings.json")"
echo "    Housing units      : $(jq length "$DATA_DIR/housing_units.json")"
echo "    Persons            : $(jq length "$DATA_DIR/persons.json")"
echo "    Boilers            : $(jq length "$DATA_DIR/boilers.json")"
echo "    Meters             : $(jq length "$DATA_DIR/meters.json")"
echo "    Fire extinguishers : $(jq length "$DATA_DIR/fire_extinguishers.json")"
echo "    Leases             : $(jq 'map(select(.buildingName)) | length' "$DATA_DIR/leases.json")"