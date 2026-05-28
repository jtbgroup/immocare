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
      ENC=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$ENAME")
      EID=$(get_resource "/api/v1/admin/estates?search=$ENC&size=10" \
        | jq -r --arg n "$ENAME" '.content[]? | select(.name == $n) | .id' | head -1)
      if [ -n "$EID" ]; then
        ESTATE_ID_MAP["$ENAME"]="$EID"
        log_info "Estate '$ENAME' → id=$EID"
      else
        log_error "Estate '$ENAME' not found in API"
      fi
    done
  fi

  # ── Rebuild BUILDING_ID_MAP ────────────────────────────────────────────────
  declare -A BUILDING_ID_MAP
  BUILDINGS_FILE="$DATA_DIR/buildings.json"
  TOTAL_B=$(jq length "$BUILDINGS_FILE")
  for i in $(seq 0 $(( TOTAL_B - 1 ))); do
    BNAME=$(jq -r ".[$i].name" "$BUILDINGS_FILE")
    ESTATE_NAME_B=$(jq -r ".[$i].estateName" "$BUILDINGS_FILE")
    local_estate_id="${ESTATE_ID_MAP[$ESTATE_NAME_B]:-}"
    [ -z "$local_estate_id" ] && log_error "Building '$BNAME': estate '$ESTATE_NAME_B' not found" && continue
    ENC=$(python3 -c "import urllib.parse,sys;print(urllib.parse.quote(sys.argv[1]))" "$BNAME")
    BID=$(get_resource "/api/v1/estates/$local_estate_id/buildings?search=$ENC&size=20" \
      | jq -r --arg n "$BNAME" '.content[]? | select(.name == $n) | .id' | head -1)
    if [ -n "$BID" ]; then
      BUILDING_ID_MAP["$BNAME"]="$BID"
    else
      log_error "Building '$BNAME' not found in API"
    fi
  done
  log_info "Building map rebuilt: ${#BUILDING_ID_MAP[@]} entries"

  # ── Rebuild UNIT_ID_MAP ────────────────────────────────────────────────────
  declare -A UNIT_ID_MAP
  UNITS_FILE="$DATA_DIR/housing_units.json"
  TOTAL_U=$(jq length "$UNITS_FILE")
  for i in $(seq 0 $(( TOTAL_U - 1 ))); do
    BNAME=$(jq -r ".[$i].buildingName" "$UNITS_FILE")
    UNUM=$(jq -r ".[$i].unitNumber" "$UNITS_FILE")
    ESTATE_NAME_U=$(jq -r ".[$i].estateName" "$UNITS_FILE")
    local_estate_id="${ESTATE_ID_MAP[$ESTATE_NAME_U]:-}"
    local_bid="${BUILDING_ID_MAP[$BNAME]:-}"
    [ -z "$local_estate_id" ] || [ -z "$local_bid" ] && continue
    UNIT_DB_ID=$(get_resource "/api/v1/estates/$local_estate_id/buildings/$local_bid/units" \
      | jq -r --arg u "$UNUM" '.[]? | select(.unitNumber == $u) | .id' | head -1)
    [ -n "$UNIT_DB_ID" ] && UNIT_ID_MAP["$BNAME|$UNUM"]="$UNIT_DB_ID"
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

# ── Build category/subcategory/bank-account lookup per estate ─────────────────
# Toutes les fonctions de cache utilisent ESTATE_ID_MAP[$estate_name]
# pour résoudre l'estate_id localement — jamais la variable globale ESTATE_ID.

declare -A CAT_ID_MAP
declare -A SUB_ID_MAP
declare -A BANK_ACCOUNT_ID_MAP

get_or_cache_category() {
  local estate_name="$1"
  local cat_name="$2"
  local local_estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$local_estate_id" ] && echo "" && return
  local key="$estate_name|$cat_name"
  if [ -z "${CAT_ID_MAP[$key]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$local_estate_id/tag-categories" \
      | jq -r --arg n "$cat_name" '.[] | select(.name == $n) | .id' | head -1)
    CAT_ID_MAP["$key"]="${result:-}"
  fi
  echo "${CAT_ID_MAP[$key]}"
}

get_or_cache_subcategory() {
  local estate_name="$1"
  local sub_name="$2"
  local cat_id="$3"
  local local_estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$local_estate_id" ] && echo "" && return
  local key="$estate_name|$sub_name|$cat_id"
  if [ -z "${SUB_ID_MAP[$key]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$local_estate_id/tag-subcategories?categoryId=$cat_id" \
      | jq -r --arg n "$sub_name" '.[] | select(.name == $n) | .id' | head -1)
    SUB_ID_MAP["$key"]="${result:-}"
  fi
  echo "${SUB_ID_MAP[$key]}"
}

get_or_cache_bank_account() {
  local estate_name="$1"
  local local_estate_id="${ESTATE_ID_MAP[$estate_name]:-}"
  [ -z "$local_estate_id" ] && echo "" && return
  if [ -z "${BANK_ACCOUNT_ID_MAP[$estate_name]+_}" ]; then
    local result
    result=$(get_resource "/api/v1/estates/$local_estate_id/bank-accounts" \
      | jq -r '[.[] | select(.type == "CURRENT")] | .[0].id // (.[] | .id)' | head -1)
    BANK_ACCOUNT_ID_MAP["$estate_name"]="${result:-}"
  fi
  echo "${BANK_ACCOUNT_ID_MAP[$estate_name]}"
}

# ── Process each transaction ─────────────────────────────────────────────────
BATCH=50  # log progress every N transactions

for i in $(seq 0 $(( TOTAL_TX - 1 ))); do
  TX=$(jq -c ".[$i]" "$TRANSACTIONS_FILE")

  TX_DATE=$(echo "$TX"     | jq -r '.transactionDate')
  TX_ACCT=$(echo "$TX"     | jq -r '.accountingMonth')
  TX_AMT=$(echo "$TX"      | jq -r '.amount')
  TX_DIR=$(echo "$TX"      | jq -r '.direction')
  TX_DESC=$(echo "$TX"     | jq -r '.description // ""')
  TX_STATUS=$(echo "$TX"   | jq -r '.status')
  TX_REF=$(echo "$TX"      | jq -r '._ref')
  TX_COUNTER=$(echo "$TX"  | jq -r '.counterpartyAccount // empty')
  ESTATE_NAME=$(echo "$TX" | jq -r '.estateName')
  CAT_NAME=$(echo "$TX"    | jq -r '.categoryName // empty')
  SUB_NAME=$(echo "$TX"    | jq -r '.subcategoryName // empty')
  BNAME=$(echo "$TX"       | jq -r '.buildingName // empty')
  UNUM=$(echo "$TX"        | jq -r '.unitNumber // empty')

  # ── Résolution de l'estate pour CETTE transaction ──────────────────────────
  # On utilise une variable locale tx_estate_id pour éviter tout écrasement
  # de la variable globale ESTATE_ID utilisée éventuellement ailleurs.
  tx_estate_id="${ESTATE_ID_MAP[$ESTATE_NAME]:-}"
  if [ -z "$tx_estate_id" ]; then
    log_error "TX $TX_REF: estate '$ESTATE_NAME' not found — skipped"
    ERRORS_TX=$(( ERRORS_TX+1 )); continue
  fi

  # ── Résolution du building ─────────────────────────────────────────────────
  tx_building_id=""
  if [ -n "$BNAME" ] && [ "$BNAME" != "null" ]; then
    tx_building_id="${BUILDING_ID_MAP[$BNAME]:-}"
    if [ -z "$tx_building_id" ]; then
      log_warn "TX $TX_REF: building '$BNAME' not resolved — continuing without buildingId"
    fi
  fi

  # ── Résolution de l'unité ──────────────────────────────────────────────────
  tx_unit_id=""
  if [ -n "$UNUM" ] && [ "$UNUM" != "null" ] && [ -n "$BNAME" ] && [ "$BNAME" != "null" ]; then
    tx_unit_id="${UNIT_ID_MAP[$BNAME|$UNUM]:-}"
  fi

  # ── Résolution category/subcategory ───────────────────────────────────────
  tx_cat_id=""
  tx_sub_id=""
  if [ -n "$CAT_NAME" ] && [ "$CAT_NAME" != "null" ]; then
    tx_cat_id=$(get_or_cache_category "$ESTATE_NAME" "$CAT_NAME")
    if [ -n "$tx_cat_id" ] && [ -n "$SUB_NAME" ] && [ "$SUB_NAME" != "null" ]; then
      tx_sub_id=$(get_or_cache_subcategory "$ESTATE_NAME" "$SUB_NAME" "$tx_cat_id")
    fi
  fi

  # ── Résolution du compte bancaire ─────────────────────────────────────────
  tx_bank_id=$(get_or_cache_bank_account "$ESTATE_NAME")

  # ── Construction du payload ───────────────────────────────────────────────
  PAYLOAD=$(jq -n \
    --arg  date    "$TX_DATE" \
    --arg  acct    "$TX_ACCT" \
    --arg  amount  "$TX_AMT" \
    --arg  dir     "$TX_DIR" \
    --arg  desc    "$TX_DESC" \
    '{
      transactionDate: $date,
      accountingMonth: $acct,
      amount:          ($amount | tonumber),
      direction:       $dir,
      description:     $desc
    }')

  # Ajout des champs optionnels uniquement s'ils sont résolus
  [ -n "$tx_bank_id"     ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$tx_bank_id"     '. + {bankAccountId: $v}')
  [ -n "$tx_building_id" ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$tx_building_id" '. + {buildingId: $v}')
  [ -n "$tx_unit_id"     ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$tx_unit_id"     '. + {housingUnitId: $v}')
  [ -n "$tx_sub_id"      ] && PAYLOAD=$(echo "$PAYLOAD" | jq --argjson v "$tx_sub_id"      '. + {subcategoryId: $v}')
  if [ -n "$TX_COUNTER" ] && [ "$TX_COUNTER" != "null" ]; then
    PAYLOAD=$(echo "$PAYLOAD" | jq --arg v "$TX_COUNTER" '. + {counterpartyAccount: $v}')
  fi

  # ── POST vers l'estate correct ─────────────────────────────────────────────
  TS=$(curl -s -o /tmp/tx_r.json -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/estates/$tx_estate_id/transactions" \
    -H "Content-Type: application/json" -b "$COOKIE_JAR" \
    -d "$PAYLOAD")

  case "$TS" in
    200|201)
      TX_ID=$(jq -r '.id' /tmp/tx_r.json)
      TX_REF_CREATED=$(jq -r '.reference' /tmp/tx_r.json)
      log_ok "TX $TX_REF → $TX_REF_CREATED (id=$TX_ID) [$ESTATE_NAME] [$TX_DIR ${TX_AMT}€] $TX_DATE — $TX_DESC"
      CREATED_TX=$(( CREATED_TX+1 ))
      if (( CREATED_TX % BATCH == 0 )); then
        log_info "  ... $CREATED_TX / $TOTAL_TX transactions inserted"
      fi
      ;;
    409)
      SKIPPED_TX=$(( SKIPPED_TX+1 ))
      log_warn "TX $TX_REF: already exists — $TX_DATE $TX_DESC"
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