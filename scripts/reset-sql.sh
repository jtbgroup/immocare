#!/usr/bin/env bash
# =============================================================================
# reset-sql.sh — Wipes all data except the admin user, via direct SQL (psql)
#
# Usage:
#   ./scripts/reset-sql.sh [DB_HOST] [DB_PORT] [DB_NAME] [DB_USER] [DB_PASS] [ADMIN_USER]
#
# Defaults:
#   DB_HOST    = localhost
#   DB_PORT    = 5432
#   DB_NAME    = immocare
#   DB_USER    = immocare
#   DB_PASS    = immocare
#   ADMIN_USER = admin
# =============================================================================

set -euo pipefail

DB_HOST="${1:-localhost}"
DB_PORT="${2:-5432}"
DB_NAME="${3:-immocare}"
DB_USER="${4:-immocare}"
DB_PASS="${5:-immocare}"
ADMIN_USER="${6:-admin}"

GREEN='\033[0;32m'; RED='\033[0;31m'; CYAN='\033[0;36m'; NC='\033[0m'
log_ok()      { echo -e "  ${GREEN}✔${NC}  $*"; }
log_error()   { echo -e "  ${RED}✘${NC}  $*"; }
log_section() { echo -e "\n${CYAN}▶ $*${NC}"; }

if ! command -v psql &> /dev/null; then
  log_error "psql is required. Install: apt install postgresql-client (Linux) or brew install libpq (macOS)"
  exit 1
fi

echo -e "${RED}"
echo "  ██████╗ ███████╗███████╗███████╗████████╗"
echo "  ██╔══██╗██╔════╝██╔════╝██╔════╝╚══██╔══╝"
echo "  ██████╔╝█████╗  ███████╗█████╗     ██║   "
echo "  ██╔══██╗██╔══╝  ╚════██║██╔══╝     ██║   "
echo "  ██║  ██║███████╗███████║███████╗   ██║   "
echo "  ╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝   ╚═╝   "
echo -e "${NC}"
echo "  This will DELETE ALL DATA on:"
echo "    Host     : $DB_HOST:$DB_PORT"
echo "    Database : $DB_NAME"
echo ""
echo "  User '$ADMIN_USER' will be kept."
echo ""
read -r -p "  Type 'yes' to confirm: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "  Aborted."
  exit 0
fi

log_section "Running reset"

export PGPASSWORD="$DB_PASS"

psql \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --dbname="$DB_NAME" \
  --set ON_ERROR_STOP=1 \
  <<SQL

-- ============================================================
-- Reset DB — truncate order respects FK constraints
-- Leaf tables first, root tables last.
-- app_user, platform_config, import_parser,
-- boiler_service_validity_rule kept (seed/config data).
-- ============================================================
 
-- ─── UC014 — Financial transactions (deepest leaves first) ───────────────────
TRUNCATE TABLE transaction_asset_link CASCADE;
TRUNCATE TABLE accounting_month_rule  CASCADE;
TRUNCATE TABLE tag_learning_rule      CASCADE;
TRUNCATE TABLE financial_transaction  CASCADE;
TRUNCATE TABLE import_batch           CASCADE;
TRUNCATE TABLE bank_account           CASCADE;
TRUNCATE TABLE tag_subcategory        CASCADE;
TRUNCATE TABLE tag_category           CASCADE;
 
-- ─── UC011 — Fire extinguishers ───────────────────────────────────────────────
TRUNCATE TABLE fire_extinguisher_revision CASCADE;
TRUNCATE TABLE fire_extinguisher          CASCADE;
 
-- ─── UC010 — Boilers ──────────────────────────────────────────────────────────
TRUNCATE TABLE boiler_service CASCADE;
TRUNCATE TABLE boiler         CASCADE;
 
-- ─── UC008 — Meters ───────────────────────────────────────────────────────────
TRUNCATE TABLE meter CASCADE;
 
-- ─── UC012 — Leases ───────────────────────────────────────────────────────────
TRUNCATE TABLE lease_rent_adjustment CASCADE;
TRUNCATE TABLE lease_tenant          CASCADE;
TRUNCATE TABLE lease                 CASCADE;
 
-- ─── UC009 — Rent history ─────────────────────────────────────────────────────
TRUNCATE TABLE rent_history CASCADE;
 
-- ─── UC007 — PEB scores ───────────────────────────────────────────────────────
TRUNCATE TABLE peb_score_history CASCADE;
 
-- ─── UC006 — Rooms ────────────────────────────────────────────────────────────
TRUNCATE TABLE room CASCADE;
 
-- ─── UC005 — Housing units ────────────────────────────────────────────────────
TRUNCATE TABLE housing_unit CASCADE;
 
-- ─── UC004 — Buildings ────────────────────────────────────────────────────────
TRUNCATE TABLE building CASCADE;
 
-- ─── UC003 — Persons & bank accounts ─────────────────────────────────────────
TRUNCATE TABLE person_bank_account CASCADE;
TRUNCATE TABLE person              CASCADE;
 
-- ─── UC002 — Users (keep admin) ───────────────────────────────────────────────
DELETE FROM app_user WHERE username != '$ADMIN_USER';
 
-- ─── Reset sequences ──────────────────────────────────────────────────────────
ALTER SEQUENCE financial_transaction_ref_seq RESTART WITH 1;

SQL

unset PGPASSWORD

log_ok "Reset complete — '$ADMIN_USER' kept."
echo ""
echo "  Next step: make seed-demo"