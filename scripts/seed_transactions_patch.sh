#!/usr/bin/env bash
# =============================================================================
# seed_transactions_patch.sh
#
# Section à AJOUTER à la fin de scripts/seed.sh (avant le bloc "Logout"),
# ou à exécuter en standalone après le seed principal.
#
# Pré-requis:
#   - Le seed principal a déjà tourné (estates, buildings, units, leases, categories)
#   - Les maps ESTATE_ID_MAP, BUILDING_ID_MAP, UNIT_ID_MAP sont disponibles
#     → Ce fichier est conçu pour être sourcé / inclus dans seed.sh
#
# Usage standalone:
#   ./scripts/seed_transactions_patch.sh [BASE_URL] [ADMIN_USER] [ADMIN_PASS] [DATA_DIR]
# =============================================================================

# ─── Si exécuté standalone, re-authentifier et reconstruire les maps ──────────
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set -euo pipefail

  BASE_URL="${1:-http://localhost:8090}"
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
    log_error "$label — HTTP $status — $body"
  }

  # Login
  log_section "Authenticating as '$ADMIN_USER' on $BASE_URL"
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" \
    -c "$COOKIE_JAR")
  [ "$HTTP_STATUS" != "200" ] && { echo "Login failed ($HTTP_STATUS)"; exit 1; }
  log_ok "Authenticated"

  get_resource() { curl -s -X GET "$BASE_URL$1" -H "Content-Type: application/json" -b "$COOKIE_JAR"; }

  # ── Rebuild ESTATE_ID_MAP ──────────────────────────────────────────────────
  declare -A ESTATE_ID_MAP
  ESTATES_FILE="$DATA_DIR/estates.json"
  if [ -f "$ESTATES_FILE" ]; then
    TOTAL_E=$(jq length "$ESTATES_FILE")
    for i in $(seq 0 $(( TOTAL_E - 1 ))); do
      ENAME=$(jq -r ".[$i].name" "$ESTATES_FILE")
      EID=$(get_resource "/api/v1/admin/estates?search=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$ENAME")&size=10" \
        | jq -r --arg n "$ENAME" '.content[]? | select(.name == $n) | .id' | head -1)
      [ -n "$EID" ] && ESTATE_ID_MAP["$ENAME"]="$EID"
      log_info "Estate '$ENAME' → id=$EID"
    done
  fi

  # ── Rebuild BUILDING_ID_MAP ────────────────────────────────────────────────
  declare -A BUILDING_ID_MAP
  BUILDINGS_FILE="$DATA_DIR/buildings.json"
  TOTAL_B=$(jq length "$BUILDINGS_FILE")
  for i in $(seq 0 $(( TOTAL_B - 1 ))); do
    BNAME=$(jq -r ".[$i].name" "$BUILDINGS_FILE")
    ESTATE_NAME=$(jq -r ".[$i].estateName" "$BUILDINGS_FILE")
    ESTATE_ID="${ESTATE_ID_MAP[$ESTATE_NAME]:-}"
    [ -z "$ESTATE_ID" ] && continue
    ENC=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$BNAME")
    BID=$(get_resource "/api/v1/estates/$ESTATE_ID/buildings?search=$ENC&size=20" \
      | jq -r --arg n "$BNAME" '.content[]? | select(.name == $n) | .id' | head -1)
    [ -n "$BID" ] && BUILDING_ID_MAP["$BNAME"]="$BID"
    log_info "Building '$BNAME' → id=$BID"
  done

  # ── Rebuild UNIT_ID_MAP ────────────────────────────────────────────────────
  declare -A UNIT_ID_MAP
  UNITS_FILE="$DATA_DIR/housing_units.json"
  TOTAL_U=$(jq length "$UNITS_FILE")
  for i in $(seq 0 $(( TOTAL_U - 1 ))); do
    BNAME=$(jq -r ".[$i].buildingName" "$UNITS_FILE")
    UNUM=$(jq -r ".[$i].unitNumber" "$UNITS_FILE")
    ESTATE_NAME=$(jq -r ".[$i].estateName" "$UNITS_FILE")
    ESTATE_ID="${ESTATE_ID_MAP[$ESTATE_NAME]:-}"
    BID="${BUILDING_ID_MAP[$BNAME]:-}"
    [ -z "$ESTATE_ID" ] || [ -z "$BID" ] && continue
    UID=$(get_resource "/api/v1/estates/$ESTATE_ID/buildings/$BID/units" \
      | jq -r --arg u "$UNUM" '.[]? | select(.unitNumber == $u) | .id' | head -1)
    [ -n "$UID" ] && UNIT_ID_MAP["$BNAME|$UNUM"]="$UID"
  done
  log_info "Unit map rebuilt: ${#UNIT_ID_MAP[@]} entries"
fi

# =============================================================================
# SEEDING TRANSACTIONS
# =============================================================================

TRANSACTIONS_FILE="$DATA_DIR/transactions.json"
if [ ! -f "$TRANSACTIONS_FILE" ]; then
  log_warn "No transactions.json found in $DATA_DIR — skipping"
  # shellcheck disable=SC2317
  return 0 2>/dev/null || exit 0
fi

log_section "12 — Seeding financial transactions"
TOTAL_TX=$(jq length "$TRANSACTIONS_FILE")
log_info "Total transactions to seed: $TOTAL_TX"

CREATED_TX=0; SKIPPED_TX=0; ERRORS_TX=0

# ── Build category/subcategory lookup per estate ─────────────────────────────
# Cache: CAT_ID_MAP["estateName|categoryName"] = categoryId
# Cache: SUB_ID_MAP["estateName|subcategoryName|categoryId"] = subcategoryId
declare -A CAT_ID_MAP
declare -A SUB_ID_MAP

get_or_cache_category() {
  local estate_name="$1" cat_name="$2"
  local estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$estate_id" ] && echo "" && return
  local key="$estate_name|$cat_name"
  if [ -z "${CAT_ID_MAP[$key]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$estate_id/tag-categories" \
      | jq -r --arg n "$cat_name" '.[] | select(.name == $n) | .id' | head -1)
    CAT_ID_MAP["$key"]="${result:-}"
  fi
  echo "${CAT_ID_MAP[$key]}"
}

get_or_cache_subcategory() {
  local estate_name="$1" sub_name="$2" cat_id="$3"
  local estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$estate_id" ] && echo "" && return
  local key="$estate_name|$sub_name|$cat_id"
  if [ -z "${SUB_ID_MAP[$key]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$estate_id/tag-subcategories?categoryId=$cat_id" \
      | jq -r --arg n "$sub_name" '.[] | select(.name == $n) | .id' | head -1)
    SUB_ID_MAP["$key"]="${result:-}"
  fi
  echo "${SUB_ID_MAP[$key]}"
}

# ── Get bank account IDs per estate (use first CURRENT account) ──────────────
declare -A BANK_ACCOUNT_ID_MAP
get_or_cache_bank_account() {
  local estate_name="$1"
  local estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$estate_id" ] && echo "" && return
  if [ -z "${BANK_ACCOUNT_ID_MAP[$estate_name]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$estate_id/bank-accounts" \
      | jq -r '[.[] | select(.type == "CURRENT")] | .[0].id // (.[] | .id)' | head -1)
    BANK_ACCOUNT_ID_MAP["$estate_name"]="${result:-}"
  fi
  echo "${BANK_ACCOUNT_ID_MAP[$estate_name]}"
}

# ── Process each transaction ─────────────────────────────────────────────────
BATCH=50  # log progress every N transactions

for i in $(seq 0 $(( TOTAL_TX - 1 ))); do
  TX=$(jq -c ".[$i]" "$TRANSACTIONS_FILE")

  TX_DATE=$(echo "$TX"       | jq -r '.transactionDate')
  TX_ACCT=$(echo "$TX"       | jq -r '.accountingMonth')
  TX_AMT=$(echo "$TX"        | jq -r '.amount')
  TX_DIR=$(echo "$TX"        | jq -r '.direction')
  TX_DESC=$(echo "$TX"       | jq -r '.description')
  TX_STATUS=$(echo "$TX"     | jq -r '.status')
  TX_SOURCE=$(echo "$TX"     | jq -r '.source')
  TX_REF=$(echo "$TX"        | jq -r '._ref')
  ESTATE_NAME=$(echo "$TX"   | jq -r '.estateName')
  CAT_NAME=$(echo "$TX"      | jq -r '.categoryName')
  SUB_NAME=$(echo "$TX"      | jq -r '.subcategoryName')
  BNAME=$(echo "$TX"         | jq -r '.buildingName // empty')
  UNUM=$(echo "$TX"          | jq -r '.unitNumber // empty')

  # Estate
  ESTATE_ID="${ESTATE_ID_MAP[$ESTATE_NAME]:-}"
  if [ -z "$ESTATE_ID" ]; then
    log_error "TX $TX_REF: estate '$ESTATE_NAME' not found — skipped"
    ERRORS_TX=$(( ERRORS_TX+1 )); continue
  fi

  # Resolve building id
  BID=""
  if [ -n "$BNAME" ] && [ "$BNAME" != "null" ]; then
    BID="${BUILDING_ID_MAP[$BNAME]:-}"
    if [ -z "$BID" ]; then
      log_warn "TX $TX_REF: building '$BNAME' not resolved — continuing without buildingId"
    fi
  fi

  # Resolve unit id
  UNIT_ID=""
  if [ -n "$UNUM" ] && [ "$UNUM" != "null" ] && [ -n "$BNAME" ] && [ "$BNAME" != "null" ]; then
    UNIT_ID="${UNIT_ID_MAP[$BNAME|$UNUM]:-}"
  fi

  # Resolve category/subcategory
  CAT_ID=$(get_or_cache_category "$ESTATE_NAME" "$CAT_NAME")
  SUB_ID=""
  if [ -n "$CAT_ID" ]; then
    SUB_ID=$(get_or_cache_subcategory "$ESTATE_NAME" "$SUB_NAME" "$CAT_ID")
  fi

  # Bank account
  BANK_ID=$(get_or_cache_bank_account "$ESTATE_NAME")

  # Build payload
  PAYLOAD=$(jq -n \
    --arg  date    "$TX_DATE" \
    --arg  acct    "$TX_ACCT" \
    --arg  amount  "$TX_AMT" \
    --arg  dir     "$TX_DIR" \
    --arg  desc    "$TX_DESC" \
    --arg  status  "$TX_STATUS" \
    --arg  source  "$TX_SOURCE" \
    '{
      transactionDate: $date,
      accountingMonth: $acct,
      amount:          ($amount | tonumber),
      direction:       $dir,
      description:     $desc,
      status:          $status,
      source:          $source
    }')

  [ -n "$BANK_ID"   ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$BANK_ID"   '. + {bankAccountId: $v}')
  [ -n "$BID"       ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$BID"        '. + {buildingId: $v}')
  [ -n "$UNIT_ID"   ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$UNIT_ID"   '. + {housingUnitId: $v}')
  [ -n "$SUB_ID"    ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$SUB_ID"    '. + {subcategoryId: $v}')

  # POST
  TS=$(curl -s -o /tmp/tx_r.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/estates/$ESTATE_ID/transactions" \
    -H "Content-Type: application/json" -b "$COOKIE_JAR" \
    -d "$PAYLOAD")

  case "$TS" in
    200|201)
      CREATED_TX=$(( CREATED_TX+1 ))
      # Progress log every BATCH
      if (( CREATED_TX % BATCH == 0 )); then
        log_info "  ... $CREATED_TX / $TOTAL_TX transactions inserted"
      fi
      ;;
    409)
      SKIPPED_TX=$(( SKIPPED_TX+1 ))
      ;;
    *)
      log_failure "TX $TX_REF ($TX_DATE $TX_DESC)" "$TS" /tmp/tx_r.json
      ERRORS_TX=$(( ERRORS_TX+1 ))
      ;;
  esac
done

log_ok "Transactions: $CREATED_TX created, $SKIPPED_TX skipped, $ERRORS_TX errors"

# Logout si exécuté standalone
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  curl -s -o /dev/null -X POST "$BASE_URL/api/v1/auth/logout" -b "$COOKIE_JAR" || true
  echo ""
  log_ok "Transaction seed completed!"
fi
