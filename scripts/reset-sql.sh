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
  --set ON_ERROR_STOP=0 \
  <<SQL

-- ============================================================
-- Reset DB — truncate order respects FK constraints
-- Leaf tables first, root tables last.
-- app_user, platform_config, import_parser,
-- boiler_service_validity_rule kept (seed/config data).
-- estate and estate_member will be truncated (not admin-specific).
-- Uses exception handling for non-existent tables.
-- ============================================================
 
-- ─── UC015 — Financial transactions (deepest leaves first) ───────────────────
DO $$ BEGIN TRUNCATE TABLE transaction_asset_link CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE accounting_month_rule  CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE tag_learning_rule      CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE financial_transaction  CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE import_batch           CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE bank_account           CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE tag_subcategory        CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE tag_category           CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC012 — Fire extinguishers ───────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE fire_extinguisher_revision CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE fire_extinguisher          CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC011 — Boilers ──────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE boiler_service CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE boiler         CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC009 — Meters ───────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE meter CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC014 — Leases ───────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE lease_rent_adjustment CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE lease_tenant          CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE lease                 CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC010 — Rent history ─────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE rent_history CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC008 — PEB scores ───────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE peb_score_history CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC007 — Rooms ────────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE room CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC006 — Housing units ────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE housing_unit CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC005 — Buildings ────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE building CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC004 — Persons & bank accounts ─────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE person_bank_account CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE person              CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
 
-- ─── UC002 — Users (keep admin) ───────────────────────────────────────────────
DELETE FROM app_user WHERE username != '$ADMIN_USER';
UPDATE app_user SET is_platform_admin = true WHERE username = '$ADMIN_USER';

-- ─── UC004_ESTATE_PLACEHOLDER — Estates ──────────────────────────────────────────────────────────
DO $$ BEGIN TRUNCATE TABLE estate_member CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;
DO $$ BEGIN TRUNCATE TABLE estate        CASCADE; EXCEPTION WHEN UNDEFINED_TABLE THEN NULL; END $$;

SQL

unset PGPASSWORD

log_ok "Reset complete — '$ADMIN_USER' kept."
echo ""
echo "  Next step: make seed-demo"