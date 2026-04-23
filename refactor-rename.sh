#!/usr/bin/env bash
# =============================================================================
# ImmoCare — UC/US/Flyway renaming script
# =============================================================================
# Usage:
#   ./refactor-rename.sh [--dry-run] [--root /path/to/project]
#
# Options:
#   --dry-run   Show what would be done without making any changes
#   --root      Project root directory (default: current directory)
#
# What this script does:
#   1. Renames Flyway migration files (backend/src/main/resources/db/migration/)
#   2. Renames use-case documentation files (docs/analysis/use-cases/)
#   3. Renames user-story documentation files (docs/analysis/user-stories/)
#   4. Renames prompt documentation files (docs/analysis/prompts/ and docs/prompts/)
#   5. Replaces all UC/US references inside every text file in the project
#      (Java, TypeScript, SQL, Markdown, YAML, etc.)
# =============================================================================

set -euo pipefail

# ─── Defaults ────────────────────────────────────────────────────────────────
DRY_RUN=false
PROJECT_ROOT="$(pwd)"

# ─── Argument parsing ─────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)  DRY_RUN=true; shift ;;
    --root)     PROJECT_ROOT="$2"; shift 2 ;;
    *)          echo "Unknown option: $1"; exit 1 ;;
  esac
done

# ─── Helpers ─────────────────────────────────────────────────────────────────
COLOR_RESET="\033[0m"
COLOR_GREEN="\033[32m"
COLOR_YELLOW="\033[33m"
COLOR_CYAN="\033[36m"
COLOR_RED="\033[31m"

log_info()    { echo -e "${COLOR_CYAN}[INFO]${COLOR_RESET}  $*"; }
log_rename()  { echo -e "${COLOR_GREEN}[RENAME]${COLOR_RESET} $*"; }
log_replace() { echo -e "${COLOR_YELLOW}[REPLACE]${COLOR_RESET} $*"; }
log_dry()     { echo -e "${COLOR_YELLOW}[DRY-RUN]${COLOR_RESET} $*"; }
log_error()   { echo -e "${COLOR_RED}[ERROR]${COLOR_RESET}  $*" >&2; }

# Safe mv: logs and respects dry-run
safe_mv() {
  local src="$1" dst="$2"
  if [[ ! -e "$src" ]]; then
    log_error "Source not found, skipping: $src"
    return 0
  fi
  if $DRY_RUN; then
    log_dry "mv '$src' -> '$dst'"
  else
    mkdir -p "$(dirname "$dst")"
    mv "$src" "$dst"
    log_rename "'$src' -> '$dst'"
  fi
}

# Safe sed in-place: replaces all occurrences in a file
# Usage: safe_sed_file <file> <old_pattern> <new_string>
safe_sed_file() {
  local file="$1" old="$2" new="$3"
  if grep -qF "$old" "$file" 2>/dev/null; then
    if $DRY_RUN; then
      log_dry "  sed '$old' -> '$new' in $file"
    else
      sed -i "s|${old}|${new}|g" "$file"
      log_replace "  '$old' -> '$new' in $file"
    fi
  fi
}

# Apply a list of replacements to all matching text files in the project
# Usage: replace_in_all_files <old> <new>
replace_in_all_files() {
  local old="$1" new="$2"
  # Find all text files, excluding binary files, .git, node_modules, target, dist
  while IFS= read -r -d '' file; do
    safe_sed_file "$file" "$old" "$new"
  done < <(find "$PROJECT_ROOT" \
    -not \( -path "*/.git/*" -prune \) \
    -not \( -path "*/node_modules/*" -prune \) \
    -not \( -path "*/target/*" -prune \) \
    -not \( -path "*/dist/*" -prune \) \
    -not \( -path "*/.angular/*" -prune \) \
    -type f \
    \( -name "*.java" -o -name "*.kt" -o -name "*.ts" -o -name "*.tsx" \
       -o -name "*.html" -o -name "*.css" -o -name "*.scss" \
       -o -name "*.sql" -o -name "*.md" -o -name "*.yaml" -o -name "*.yml" \
       -o -name "*.json" -o -name "*.properties" -o -name "*.xml" \
       -o -name "*.sh" -o -name "*.txt" \) \
    -print0)
}

# ─── Sanity check ─────────────────────────────────────────────────────────────
if [[ ! -d "$PROJECT_ROOT" ]]; then
  log_error "Project root not found: $PROJECT_ROOT"
  exit 1
fi

log_info "Project root : $PROJECT_ROOT"
log_info "Dry run      : $DRY_RUN"
echo ""

# =============================================================================
# STEP 1 — FLYWAY MIGRATION FILES
# =============================================================================
log_info "=== STEP 1: Flyway migration files ==="

FLYWAY_DIR="$PROJECT_ROOT/backend/src/main/resources/db/migration"

# Mapping: old filename -> new filename
declare -A FLYWAY_MAP=(
  ["V001__uc001_authentication.sql"]="V001__uc001_authentication.sql"
  ["V002__uc002_manage_users.sql"]="V002__uc002_manage_users.sql"
  ["V004__uc004_manage_persons.sql"]="V004__uc004_manage_persons.sql"
  ["V005__uc005_manage_buildings.sql"]="V005__uc005_manage_buildings.sql"
  ["V006__uc006_manage_housing_units.sql"]="V006__uc006_manage_housing_units.sql"
  ["V007__uc007_manage_rooms.sql"]="V007__uc007_manage_rooms.sql"
  ["V008__uc008_manage_peb_scores.sql"]="V008__uc008_manage_peb_scores.sql"
  ["V009__uc009_manage_meters.sql"]="V009__uc009_manage_meters.sql"
  ["V010__uc010_manage_rents.sql"]="V010__uc010_manage_rents.sql"
  ["V011__uc011_manage_boilers.sql"]="V011__uc011_manage_boilers.sql"
  ["V012__uc012_manage_fire_extinguishers.sql"]="V012__uc012_manage_fire_extinguishers.sql"
  ["V013__uc013_manage_platform_config.sql"]="V013__uc013_manage_platform_config.sql"
  ["V014__uc014_manage_leases.sql"]="V014__uc014_manage_leases.sql"
  ["V015__uc015_manage_financial_transactions.sql"]="V015__uc015_manage_financial_transactions.sql"
  ["V016__uc016_import_parser_strategies.sql"]="V016__uc016_import_parser_strategies.sql"
  ["V003__uc003_manage_estates.sql"]="V003__uc003_manage_estates.sql"
)

# Files to DELETE (replaced by the new structure — backfill migrations)
FLYWAY_DELETE=(
  "V018__estate_scope_buildings.sql"
  "V019__estate_scope_persons.sql"
  "V020__estate_scope_financial.sql"
  "V021__estate_scope_config.sql"
)

# Renames must be done in reverse order to avoid collisions
# (e.g. V012 -> V014 before V014 -> V015)
# Strategy: move to temp names first, then to final names

if [[ -d "$FLYWAY_DIR" ]]; then
  # Phase A: rename to temp names to avoid conflicts
  for old in "${!FLYWAY_MAP[@]}"; do
    new="${FLYWAY_MAP[$old]}"
    src="$FLYWAY_DIR/$old"
    tmp="$FLYWAY_DIR/.tmp_${old}"
    if [[ "$old" != "$new" ]] && [[ -f "$src" ]]; then
      safe_mv "$src" "$tmp"
    fi
  done

  # Phase B: rename from temp to final names
  for old in "${!FLYWAY_MAP[@]}"; do
    new="${FLYWAY_MAP[$old]}"
    tmp="$FLYWAY_DIR/.tmp_${old}"
    dst="$FLYWAY_DIR/$new"
    if [[ "$old" != "$new" ]] && ([[ -f "$tmp" ]] || $DRY_RUN); then
      safe_mv "$tmp" "$dst"
    fi
  done

  # Phase C: delete backfill migrations
  for del in "${FLYWAY_DELETE[@]}"; do
    f="$FLYWAY_DIR/$del"
    if [[ -f "$f" ]]; then
      if $DRY_RUN; then
        log_dry "DELETE '$f'"
      else
        rm "$f"
        log_rename "DELETED '$f'"
      fi
    fi
  done
else
  log_error "Flyway directory not found: $FLYWAY_DIR — skipping"
fi

echo ""

# =============================================================================
# STEP 2 — DOCUMENTATION: USE CASE FILES
# =============================================================================
log_info "=== STEP 2: Use case files ==="

UC_DIR="$PROJECT_ROOT/docs/analysis/use-cases"

declare -A UC_FILE_MAP=(
  ["UC001-authentication.md"]="UC-001-authentication.md"
  ["UC001_authentication.md"]="UC-001-authentication.md"
  ["UC002_manage_users.md"]="UC-002-manage-users.md"
  ["UC005_manage_persons.md"]="UC-004-manage-persons.md"
  ["UC006_manage_buildings.md"]="UC-005-manage-buildings.md"
  ["UC007_manage_housing_units.md"]="UC-006-manage-housing-units.md"
  ["UC008_manage_rooms.md"]="UC-007-manage-rooms.md"
  ["UC009_manage_peb_scores.md"]="UC-008-manage-peb-scores.md"
  ["UC010_manage_meters.md"]="UC-009-manage-meters.md"
  ["UC011_manage_rents.md"]="UC-010-manage-rents.md"
  ["UC012_manage_boilers.md"]="UC-011-manage-boilers.md"
  ["UC014_manage_fire_extinguishers.md"]="UC-012-manage-fire-extinguishers.md"
  ["UC013_manage_platform_config.md"]="UC-013-manage-platform-config.md"
  ["UC015_manage_leases.md"]="UC-014-manage-leases.md"
  ["UC016_manage_financial_transactions.md"]="UC-015-manage-financial-transactions.md"
  ["UC004_ESTATE_PLACEHOLDER_import_parser_strategies.md"]="UC-016-import-parser-strategies.md"
  ["UC004_manage_estates.md"]="UC-003-manage-estates.md"
)

if [[ -d "$UC_DIR" ]]; then
  for old in "${!UC_FILE_MAP[@]}"; do
    new="${UC_FILE_MAP[$old]}"
    safe_mv "$UC_DIR/$old" "$UC_DIR/$new"
  done
else
  log_error "Use-cases directory not found: $UC_DIR — skipping"
fi

echo ""

# =============================================================================
# STEP 3 — DOCUMENTATION: USER STORY FILES
# =============================================================================
log_info "=== STEP 3: User story files ==="

US_DIR="$PROJECT_ROOT/docs/analysis/user-stories"

# Mapping: old filename -> new filename
declare -A US_FILE_MAP=(
  # UC001 — Authentication (were UC009.001-038 but those numbers clash with UC010 meters)
  # Note: UC009.001-038 in authentication context renamed to UC001.001-003
  # UC002 — Manage Users
  ["UC002.001-view-user-list.md"]="UC002.001-view-user-list.md"
  ["UC002.002-create-user.md"]="UC002.002-create-user.md"
  ["UC002.003-edit-user.md"]="UC002.003-edit-user.md"
  ["UC002.004-change-password.md"]="UC002.004-change-user-password.md"
  ["UC002.005-delete-user.md"]="UC002.005-delete-user.md"
  # UC004 — Manage Estates
  ["UC003.001-create-estate.md"]="UC004.001-create-estate.md"
  ["UC003.002-edit-estate.md"]="UC004.002-edit-estate.md"
  ["UC003.003-delete-estate.md"]="UC004.003-delete-estate.md"
  ["UC003.004-list-all-estates.md"]="UC004.004-list-all-estates.md"
  ["UC003.005-assign-first-manager.md"]="UC004.005-assign-first-manager.md"
  ["UC003.006-view-estate-members.md"]="UC004.006-view-estate-members.md"
  ["UC003.007-add-member.md"]="UC004.007-add-member-to-estate.md"
  ["UC003.008-edit-member-role.md"]="UC004.008-edit-member-role.md"
  ["UC003.009-remove-member.md"]="UC004.009-remove-member.md"
  ["UC003.010-select-active-estate.md"]="UC004.010-select-active-estate.md"
  ["UC003.011-view-estate-dashboard.md"]="UC004.011-view-estate-dashboard.md"
  ["UC003.012-view-my-estates.md"]="UC004.012-view-my-estates.md"
  ["UC003.013-enforce-estate-scoped-access.md"]="UC004.013-enforce-estate-scoped-access.md"
  # UC005 — Manage Persons
  ["UC004.001-view-persons-list.md"]="UC005.001-view-persons-list.md"
  ["UC004.002-create-person.md"]="UC005.002-create-person.md"
  ["UC004.003-edit-person.md"]="UC005.003-edit-person.md"
  ["UC004.004-delete-person.md"]="UC005.004-delete-person.md"
  ["UC004.005-assign-person-as-owner.md"]="UC005.005-assign-person-as-owner.md"
  ["UC004.006-person-picker.md"]="UC005.006-person-picker.md"
  ["UC004.007-manage-person-bank-accounts.md"]="UC005.007-manage-person-bank-accounts.md"
  # UC006 — Manage Buildings
  ["UC005.001-create-building.md"]="UC006.001-create-building.md"
  ["UC005.002-edit-building.md"]="UC006.002-edit-building.md"
  ["UC005.003-delete-building.md"]="UC006.003-delete-building.md"
  ["UC005.004-view-buildings-list.md"]="UC006.004-view-buildings-list.md"
  ["UC005.005-search-buildings.md"]="UC006.005-search-buildings.md"
  # UC007 — Manage Housing Units
  ["UC006.001-create-housing-unit.md"]="UC007.001-create-housing-unit.md"
  ["UC006.002-edit-housing-unit.md"]="UC007.002-edit-housing-unit.md"
  ["UC006.003-delete-housing-unit.md"]="UC007.003-delete-housing-unit.md"
  ["UC006.004-view-housing-unit-details.md"]="UC007.004-view-housing-unit-details.md"
  ["UC006.005-add-terrace.md"]="UC007.005-add-terrace.md"
  ["UC006.006-add-garden.md"]="UC007.006-add-garden.md"
  # UC008 — Manage Rooms
  ["UC007.001-add-room.md"]="UC008.001-add-room.md"
  ["UC007.002-edit-room.md"]="UC008.002-edit-room.md"
  ["UC007.003-delete-room.md"]="UC008.003-delete-room.md"
  ["UC007.004-batch-create-rooms.md"]="UC008.004-batch-create-rooms.md"
  ["UC007.005-view-room-composition.md"]="UC008.005-view-room-composition.md"
  # UC009 — Manage PEB Scores
  ["UC008.001-add-peb-score.md"]="UC009.001-add-peb-score.md"
  ["UC008.002-view-peb-history.md"]="UC009.002-view-peb-score-history.md"
  ["UC008.003-check-peb-validity.md"]="UC009.003-check-peb-certificate-validity.md"
  ["UC008.004-track-peb-improvements.md"]="UC009.004-track-peb-score-improvements.md"
  # UC010 — Manage Meters
  ["UC009.001-view-meters-housing-unit.md"]="UC010.001-view-meters-housing-unit.md"
  ["UC009.002-view-meters-building.md"]="UC010.002-view-meters-building.md"
  ["UC009.003-add-meter-housing-unit.md"]="UC010.003-add-meter-housing-unit.md"
  ["UC009.004-add-meter-building.md"]="UC010.004-add-meter-building.md"
  ["UC009.005-replace-meter.md"]="UC010.005-replace-meter.md"
  ["UC009.006-remove-meter.md"]="UC010.006-remove-meter.md"
  ["UC009.007-view-meter-history.md"]="UC010.007-view-meter-history.md"
  # UC011 — Manage Rents
  ["UC010.001-set-initial-rent.md"]="UC011.001-set-initial-rent.md"
  ["UC010.002-edit-rent-record.md"]="UC011.002-edit-rent-record.md"
  ["UC010.003-view-rent-history.md"]="UC011.003-view-rent-history.md"
  ["UC010.004-track-rent-increases.md"]="UC011.004-track-rent-increases.md"
  ["UC010.005-add-rent-notes.md"]="UC011.005-add-rent-notes.md"
  # UC012 — Manage Boilers
  ["UC011.001-add_boiler_to_housing_unit.md"]="UC012.001-add-boiler-to-housing-unit.md"
  ["UC011.002-view_active_boiler.md"]="UC012.002-view-active-boiler.md"
  ["UC011.003-replace_boiler.md"]="UC012.003-replace-boiler.md"
  ["UC011.004-view_boiler_history.md"]="UC012.004-view-boiler-history.md"
  ["UC011.005-add_boiler_service_record.md"]="UC012.005-add-boiler-service-record.md"
  ["UC011.006-view_boiler_service_history.md"]="UC012.006-view-boiler-service-history.md"
  ["UC011.007_view_boiler_service_validity_alert.md"]="UC012.007-view-boiler-service-validity-alert.md"
  # UC014 — Manage Fire Extinguishers
  ["UC012.001-add-fire-extinguisher.md"]="UC014.001-add-fire-extinguisher.md"
  ["UC012.002-edit-fire-extinguisher.md"]="UC014.002-edit-fire-extinguisher.md"
  ["UC012.003-delete-fire-extinguisher.md"]="UC014.003-delete-fire-extinguisher.md"
  ["UC012.004-view-fire-extinguishers-list.md"]="UC014.004-view-fire-extinguishers-list.md"
  ["UC012.005-add-revision-record.md"]="UC014.005-add-revision-record.md"
  ["UC012.006-view-revision-history.md"]="UC014.006-view-revision-history.md"
  ["UC012.007-delete-revision-record.md"]="UC014.007-delete-revision-record.md"
  # UC013 — Manage Platform Config
  ["UC013.001-view-platform-settings.md"]="UC013.001-view-platform-settings.md"
  ["UC013.002-add-boiler-service-validity-rule.md"]="UC013.002-add-boiler-service-validity-rule.md"
  ["UC013.003-view-boiler-service-validity-rules-history.md"]="UC013.003-view-boiler-service-validity-rules-history.md"
  ["UC013.004-update-general-settings.md"]="UC013.004-update-general-settings.md"
  # UC015 — Manage Leases
  ["UC014.001-view-lease-for-housing-unit.md"]="UC015.001-view-lease-for-housing-unit.md"
  ["UC014.002-create-lease-draft.md"]="UC015.002-create-lease-draft.md"
  ["UC014.003-activate-lease.md"]="UC015.003-activate-lease.md"
  ["UC014.004-edit-lease.md"]="UC015.004-edit-lease.md"
  ["UC014.005-finish-lease.md"]="UC015.005-finish-lease.md"
  ["UC014.006-cancel-lease.md"]="UC015.006-cancel-lease.md"
  ["UC014.007-record-indexation.md"]="UC015.007-record-indexation.md"
  ["UC014.008-view-indexation-history.md"]="UC015.008-view-indexation-history.md"
  ["UC014.009-add-tenant.md"]="UC015.009-add-tenant-to-lease.md"
  ["UC014.010-remove-tenant.md"]="UC015.010-remove-tenant-from-lease.md"
  ["UC014.011-view-lease-alerts.md"]="UC015.011-view-lease-alerts.md"
  # UC016 — Manage Financial Transactions
  ["UC015.001-view-transaction-list.md"]="UC016.001-view-transaction-list.md"
  ["UC015.002-create-transaction-manually.md"]="UC016.002-create-transaction-manually.md"
  ["UC015.003-edit-transaction.md"]="UC016.003-edit-transaction.md"
  ["UC015.004-delete-transaction.md"]="UC016.004-delete-transaction.md"
  ["UC015.005-classify-transaction.md"]="UC016.005-classify-transaction.md"
  ["UC015.006-link-transaction-to-assets.md"]="UC016.006-link-transaction-to-assets.md"
  ["UC015.007-import-transactions.md"]="UC016.007-import-transactions.md"
  ["UC015.007-import-transactions-csv.md"]="UC016.007b-import-transactions-csv.md"
  ["UC015.008-review-confirm-imported-transactions.md"]="UC016.008-review-confirm-imported-transactions.md"
  ["UC015.009-manage-tag-catalog.md"]="UC016.009-manage-tag-catalog.md"
  ["UC015.010-manage-bank-account-catalog.md"]="UC016.010-manage-bank-account-catalog.md"
  ["UC015.011-view-financial-summary-statistics.md"]="UC016.011-view-financial-summary-statistics.md"
  ["UC015.012-export-transactions-csv.md"]="UC016.012-export-transactions-csv.md"
  # UC004_ESTATE_PLACEHOLDER — Import Parser Strategies
  ["UC016.001-select-parser-on-import.md"]="UC004_ESTATE_PLACEHOLDER.001-select-parser-on-import.md"
  ["UC016.002-view-parser-registry.md"]="UC004_ESTATE_PLACEHOLDER.002-view-parser-registry.md"
)

if [[ -d "$US_DIR" ]]; then
  for old in "${!US_FILE_MAP[@]}"; do
    new="${US_FILE_MAP[$old]}"
    safe_mv "$US_DIR/$old" "$US_DIR/$new"
  done
else
  log_error "User-stories directory not found: $US_DIR — skipping"
fi

echo ""

# =============================================================================
# STEP 4 — DOCUMENTATION: PROMPT FILES
# =============================================================================
log_info "=== STEP 4: Prompt files ==="

# Check both possible prompt directories
for PROMPT_DIR in \
  "$PROJECT_ROOT/docs/analysis/prompts" \
  "$PROJECT_ROOT/docs/prompts"; do

  if [[ ! -d "$PROMPT_DIR" ]]; then
    continue
  fi

  log_info "Processing prompts in: $PROMPT_DIR"

  declare -A PROMPT_FILE_MAP=(
    ["UC001-authentication-prompt.md"]="UC-001-authentication-prompt.md"
    ["UC002-manage-users-prompt.md"]="UC-002-manage-users-prompt.md"
    ["UC005-manage-persons-prompt.md"]="UC-004-manage-persons-prompt.md"
    ["UC006-manage-buildings-prompt.md"]="UC-005-manage-buildings-prompt.md"
    ["UC007-manage-housing-units-prompt.md"]="UC-006-manage-housing-units-prompt.md"
    ["UC008-manage-rooms-prompt.md"]="UC-007-manage-rooms-prompt.md"
    ["UC009-manage-peb-scores-prompt.md"]="UC-008-manage-peb-scores-prompt.md"
    ["UC08-manage-meters-prompt.md"]="UC-009-manage-meters-prompt.md"
    ["UC011-manage-rents-prompt.md"]="UC-010-manage-rents-prompt.md"
    ["UC012-manage-boilers-prompt.md"]="UC-011-manage-boilers-prompt.md"
    ["UC014-manage-fire-extinguishers-prompt.md"]="UC-012-manage-fire-extinguishers-prompt.md"
    ["UC013-manage-platform-config-prompt.md"]="UC-013-manage-platform-config-prompt.md"
    ["UC015-manage-leases-prompt.md"]="UC-014-manage-leases-prompt.md"
    ["UC016-manage-financial-transactions-prompt.md"]="UC-015-manage-financial-transactions-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-global prompt.md"]="UC-003-manage-estates-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase1-implementation-prompt.md"]="UC-003-manage-estates-phase1-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase2-implementation-prompt.md"]="UC-003-manage-estates-phase2-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase3-implementation-prompt.md"]="UC-003-manage-estates-phase3-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase4-implementation-prompt.md"]="UC-003-manage-estates-phase4-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase5-implementation-prompt.md"]="UC-003-manage-estates-phase5-prompt.md"
    ["UC004_ESTATE_PLACEHOLDER-phase6-implementation-prompt.md"]="UC-003-manage-estates-phase6-prompt.md"
  )

  for old in "${!PROMPT_FILE_MAP[@]}"; do
    new="${PROMPT_FILE_MAP[$old]}"
    safe_mv "$PROMPT_DIR/$old" "$PROMPT_DIR/$new"
  done

  unset PROMPT_FILE_MAP
done

echo ""

# =============================================================================
# STEP 5 — CONTENT REPLACEMENT IN ALL FILES
# =============================================================================
log_info "=== STEP 5: Content replacement inside files ==="
log_info "This may take a moment for large codebases..."
echo ""

# ─── Flyway V-number references ──────────────────────────────────────────────
log_info "--- Flyway V-number references ---"
# Order matters: replace higher numbers first to avoid double-replacement
# V021 -> (deleted, no replacement needed in content)
# V020 -> (deleted)
# V019 -> (deleted)
# V018 -> (deleted)
replace_in_all_files "V003__uc003_manage_estates" "V003__uc003_manage_estates"
replace_in_all_files "V016__uc016_import_parser"     "V016__uc016_import_parser"
replace_in_all_files "V015__uc015_manage_financial"  "V015__uc015_manage_financial"
replace_in_all_files "V013__uc013_manage_platform"   "V013__uc013_manage_platform"
replace_in_all_files "V014__uc014_manage_leases"     "V014__uc014_manage_leases"
replace_in_all_files "V012__uc012_manage_fire"       "V012__uc012_manage_fire"
replace_in_all_files "V011__uc011_manage_boilers"    "V011__uc011_manage_boilers"
replace_in_all_files "V010__uc010_manage_rents"      "V010__uc010_manage_rents"
replace_in_all_files "V009__uc009_manage_meters"     "V009__uc009_manage_meters"
replace_in_all_files "V008__uc008_manage_peb"        "V008__uc008_manage_peb"
replace_in_all_files "V007__uc007_manage_rooms"      "V007__uc007_manage_rooms"
replace_in_all_files "V006__uc006_manage_housing"    "V006__uc006_manage_housing"
replace_in_all_files "V005__uc005_manage_buildings"  "V005__uc005_manage_buildings"
replace_in_all_files "V004__uc004_manage_persons"    "V004__uc004_manage_persons"

echo ""

# ─── UC references (long form with slug — do these first to avoid partial matches) ─
log_info "--- UC slug references ---"
replace_in_all_files "UC004_manage_estates"               "UC004_manage_estates"
replace_in_all_files "UC004-manage-estates"               "UC004-manage-estates"
replace_in_all_files "UC004_ESTATE_PLACEHOLDER_import_parser_strategies"     "UC004_ESTATE_PLACEHOLDER_import_parser_strategies"
replace_in_all_files "UC004_ESTATE_PLACEHOLDER-import-parser-strategies"     "UC004_ESTATE_PLACEHOLDER-import-parser-strategies"
replace_in_all_files "UC016_manage_financial_transactions" "UC016_manage_financial_transactions"
replace_in_all_files "UC016-manage-financial-transactions" "UC016-manage-financial-transactions"
replace_in_all_files "UC013_manage_platform_config"       "UC013_manage_platform_config"
replace_in_all_files "UC015_manage_leases"                "UC015_manage_leases"
replace_in_all_files "UC015-manage-leases"                "UC015-manage-leases"
replace_in_all_files "UC014_manage_fire_extinguishers"    "UC014_manage_fire_extinguishers"
replace_in_all_files "UC014-manage-fire-extinguishers"    "UC014-manage-fire-extinguishers"
replace_in_all_files "UC012_manage_boilers"               "UC012_manage_boilers"
replace_in_all_files "UC012-manage-boilers"               "UC012-manage-boilers"
replace_in_all_files "UC011_manage_rents"                 "UC011_manage_rents"
replace_in_all_files "UC011-manage-rents"                 "UC011-manage-rents"
replace_in_all_files "UC010_manage_meters"                "UC010_manage_meters"
replace_in_all_files "UC010-manage-meters"                "UC010-manage-meters"
replace_in_all_files "UC009_manage_peb_scores"            "UC009_manage_peb_scores"
replace_in_all_files "UC009-manage-peb-scores"            "UC009-manage-peb-scores"
replace_in_all_files "UC008_manage_rooms"                 "UC008_manage_rooms"
replace_in_all_files "UC008-manage-rooms"                 "UC008-manage-rooms"
replace_in_all_files "UC007_manage_housing_units"         "UC007_manage_housing_units"
replace_in_all_files "UC007-manage-housing-units"         "UC007-manage-housing-units"
replace_in_all_files "UC006_manage_buildings"             "UC006_manage_buildings"
replace_in_all_files "UC006-manage-buildings"             "UC006-manage-buildings"
replace_in_all_files "UC005_manage_persons"               "UC005_manage_persons"
replace_in_all_files "UC005-manage-persons"               "UC005-manage-persons"

echo ""

# ─── UC bare number references ────────────────────────────────────────────────
# These must be done AFTER slug replacements to avoid double-replacement.
# Use word-boundary-like patterns (UC followed by exactly 3 digits).
# Order: highest first to avoid UC004_ESTATE_PLACEHOLDER -> UC004 then UC004 -> UC005
log_info "--- UC bare number references ---"
replace_in_all_files "UC004_ESTATE_PLACEHOLDER" "UC004_ESTATE_PLACEHOLDER"  # temp to avoid collision
replace_in_all_files "UC016" "UC004_ESTATE_PLACEHOLDER"
replace_in_all_files "UC015" "UC016"
replace_in_all_files "UC013" "UC013"  # unchanged
replace_in_all_files "UC014" "UC015"
replace_in_all_files "UC012" "UC014"
replace_in_all_files "UC011" "UC012"
replace_in_all_files "UC010" "UC011"
replace_in_all_files "UC009" "UC010"
replace_in_all_files "UC008" "UC009"
replace_in_all_files "UC007" "UC008"
replace_in_all_files "UC006" "UC007"
replace_in_all_files "UC005" "UC006"
replace_in_all_files "UC004" "UC005"
replace_in_all_files "UC004_ESTATE_PLACEHOLDER" "UC004"  # resolve temp

echo ""

# ─── US references (new format UCnnn.sss) ─────────────────────────────────────
log_info "--- US new-format references ---"

# UC002 — Manage Users
replace_in_all_files "UC002.001" "UC002.001"
replace_in_all_files "UC002.002" "UC002.002"
replace_in_all_files "UC002.003" "UC002.003"
replace_in_all_files "UC002.004" "UC002.004"
replace_in_all_files "UC002.005" "UC002.005"

# UC004 — Manage Estates
replace_in_all_files "UC003.001" "UC004.001"
replace_in_all_files "UC003.002" "UC004.002"
replace_in_all_files "UC003.003" "UC004.003"
replace_in_all_files "UC003.004" "UC004.004"
replace_in_all_files "UC003.005" "UC004.005"
replace_in_all_files "UC003.006" "UC004.006"
replace_in_all_files "UC003.007" "UC004.007"
replace_in_all_files "UC003.008" "UC004.008"
replace_in_all_files "UC003.009" "UC004.009"
replace_in_all_files "UC003.010" "UC004.010"
replace_in_all_files "UC003.011" "UC004.011"
replace_in_all_files "UC003.012" "UC004.012"
replace_in_all_files "UC003.013" "UC004.013"

# UC005 — Manage Persons
replace_in_all_files "UC004.007" "UC005.007"
replace_in_all_files "UC004.001" "UC005.001"
replace_in_all_files "UC004.002" "UC005.002"
replace_in_all_files "UC004.003" "UC005.003"
replace_in_all_files "UC004.004" "UC005.004"
replace_in_all_files "UC004.005" "UC005.005"
replace_in_all_files "UC004.006" "UC005.006"

# UC006 — Manage Buildings
replace_in_all_files "UC005.001" "UC006.001"
replace_in_all_files "UC005.002" "UC006.002"
replace_in_all_files "UC005.003" "UC006.003"
replace_in_all_files "UC005.004" "UC006.004"
replace_in_all_files "UC005.005" "UC006.005"

# UC007 — Manage Housing Units
replace_in_all_files "UC006.001" "UC007.001"
replace_in_all_files "UC006.002" "UC007.002"
replace_in_all_files "UC006.003" "UC007.003"
replace_in_all_files "UC006.004" "UC007.004"
replace_in_all_files "UC006.005" "UC007.005"
replace_in_all_files "UC006.006" "UC007.006"

# UC008 — Manage Rooms
replace_in_all_files "UC007.001" "UC008.001"
replace_in_all_files "UC007.002" "UC008.002"
replace_in_all_files "UC007.003" "UC008.003"
replace_in_all_files "UC007.004" "UC008.004"
replace_in_all_files "UC007.005" "UC008.005"

# UC009 — Manage PEB Scores
replace_in_all_files "UC008.001" "UC009.001"
replace_in_all_files "UC008.002" "UC009.002"
replace_in_all_files "UC008.003" "UC009.003"
replace_in_all_files "UC008.004" "UC009.004"

# UC010 — Manage Meters (former UC009.001-042)
replace_in_all_files "UC009.001" "UC010.001"
replace_in_all_files "UC009.002" "UC010.002"
replace_in_all_files "UC009.003" "UC010.003"
replace_in_all_files "UC009.004" "UC010.004"
replace_in_all_files "UC009.005" "UC010.005"
replace_in_all_files "UC009.006" "UC010.006"
replace_in_all_files "UC009.007" "UC010.007"

# UC011 — Manage Rents
replace_in_all_files "UC010.001" "UC011.001"
replace_in_all_files "UC010.002" "UC011.002"
replace_in_all_files "UC010.003" "UC011.003"
replace_in_all_files "UC010.004" "UC011.004"
replace_in_all_files "UC010.005" "UC011.005"

# UC012 — Manage Boilers
replace_in_all_files "UC011.001" "UC012.001"
replace_in_all_files "UC011.002" "UC012.002"
replace_in_all_files "UC011.003" "UC012.003"
replace_in_all_files "UC011.004" "UC012.004"
replace_in_all_files "UC011.005" "UC012.005"
replace_in_all_files "UC011.006" "UC012.006"
replace_in_all_files "UC011.007" "UC012.007"

# UC014 — Manage Fire Extinguishers
replace_in_all_files "UC012.001" "UC014.001"
replace_in_all_files "UC012.002" "UC014.002"
replace_in_all_files "UC012.003" "UC014.003"
replace_in_all_files "UC012.004" "UC014.004"
replace_in_all_files "UC012.005" "UC014.005"
replace_in_all_files "UC012.006" "UC014.006"
replace_in_all_files "UC012.007" "UC014.007"

# UC013 — Manage Platform Config
replace_in_all_files "UC013.001" "UC013.001"
replace_in_all_files "UC013.002" "UC013.002"
replace_in_all_files "UC013.003" "UC013.003"
replace_in_all_files "UC013.004" "UC013.004"
# Note: UC012.001 mapped to UC014.001 (fire ext) above
# UC013 asset type mapping story was also UC012.001 — manual resolution needed
# => flag it
replace_in_all_files "UC012.001" "UC013.005_OR_UC014.001_CHECK_MANUALLY"

# UC015 — Manage Leases
replace_in_all_files "UC014.001" "UC015.001"
replace_in_all_files "UC014.002" "UC015.002"
replace_in_all_files "UC014.003" "UC015.003"
replace_in_all_files "UC014.004" "UC015.004"
replace_in_all_files "UC014.005" "UC015.005"
replace_in_all_files "UC014.006" "UC015.006"
replace_in_all_files "UC014.007" "UC015.007"
replace_in_all_files "UC014.008" "UC015.008"
replace_in_all_files "UC014.009" "UC015.009"
replace_in_all_files "UC014.010" "UC015.010"
replace_in_all_files "UC014.011" "UC015.011"

# UC016 — Manage Financial Transactions
replace_in_all_files "UC015.001" "UC016.001"
replace_in_all_files "UC015.002" "UC016.002"
replace_in_all_files "UC015.003" "UC016.003"
replace_in_all_files "UC015.004" "UC016.004"
replace_in_all_files "UC015.005" "UC016.005"
replace_in_all_files "UC015.006" "UC016.006"
replace_in_all_files "UC015.007" "UC016.007"
replace_in_all_files "UC015.008" "UC016.008"
replace_in_all_files "UC015.009" "UC016.009"
replace_in_all_files "UC015.010" "UC016.010"
replace_in_all_files "UC015.011" "UC016.011"
replace_in_all_files "UC015.012" "UC016.012"

# UC004_ESTATE_PLACEHOLDER — Import Parser Strategies
replace_in_all_files "UC016.001" "UC004_ESTATE_PLACEHOLDER.001"
replace_in_all_files "UC016.002" "UC004_ESTATE_PLACEHOLDER.002"

echo ""

# ─── Flyway comment headers ───────────────────────────────────────────────────
log_info "--- Flyway SQL comment headers ---"
replace_in_all_files "-- Use case: UC004_ESTATE_PLACEHOLDER" "-- Use case: UC004"
replace_in_all_files "-- Use case: UC016" "-- Use case: UC004_ESTATE_PLACEHOLDER"
replace_in_all_files "-- Use case: UC015" "-- Use case: UC016"
replace_in_all_files "-- Use case: UC014" "-- Use case: UC015"
replace_in_all_files "-- Use case: UC012" "-- Use case: UC014"
replace_in_all_files "-- Use case: UC011" "-- Use case: UC012"
replace_in_all_files "-- Use case: UC010" "-- Use case: UC011"
replace_in_all_files "-- Use case: UC009" "-- Use case: UC010"
replace_in_all_files "-- Use case: UC008" "-- Use case: UC009"
replace_in_all_files "-- Use case: UC007" "-- Use case: UC008"
replace_in_all_files "-- Use case: UC006" "-- Use case: UC007"
replace_in_all_files "-- Use case: UC005" "-- Use case: UC006"
replace_in_all_files "-- Use case: UC004" "-- Use case: UC005"

echo ""

# =============================================================================
# STEP 6 — POST-MIGRATION CHECKS
# =============================================================================
log_info "=== STEP 6: Post-migration checks ==="

# Check for any remaining old UC references that may need manual attention
log_info "Scanning for potentially unmapped references..."

PATTERNS_TO_CHECK=(
  "UC012.001"   # collision: UC014.001 vs UC013.005
  "UC004_ESTATE_PLACEHOLDER"  # should be gone
  "V018__" "V019__" "V020__" "V021__"  # backfill migrations
  "UC004_ESTATE_PLACEHOLDER.*phase"  # old phase-based prompt references
  "_OR_.*_CHECK_MANUALLY"  # explicit flags left by script
)

FOUND_ISSUES=false
for pattern in "${PATTERNS_TO_CHECK[@]}"; do
  results=$(grep -rl "$pattern" "$PROJECT_ROOT" \
    --include="*.java" --include="*.ts" --include="*.sql" \
    --include="*.md" --include="*.yaml" --include="*.yml" \
    2>/dev/null | grep -v "/.git/" | grep -v "/node_modules/" | grep -v "/target/" || true)
  if [[ -n "$results" ]]; then
    log_error "Found '$pattern' in:"
    echo "$results" | while read -r f; do echo "    $f"; done
    FOUND_ISSUES=true
  fi
done

if ! $FOUND_ISSUES; then
  log_info "No unmapped references found."
fi

echo ""

# =============================================================================
# SUMMARY
# =============================================================================
log_info "=== DONE ==="
if $DRY_RUN; then
  echo -e "${COLOR_YELLOW}Dry-run complete — no files were modified.${COLOR_RESET}"
  echo "Run without --dry-run to apply changes."
else
  echo -e "${COLOR_GREEN}Migration complete.${COLOR_RESET}"
  echo ""
  echo "Next steps:"
  echo "  1. Review any 'CHECK_MANUALLY' flags in the codebase (UC012.001 collision)"
  echo "  2. Rewrite Flyway files that need schema changes (V001, V003→V005, V011, V013, V015)"
  echo "  3. Update Spring Boot entity/service references to new UC numbers in comments"
  echo "  4. Run: mvn flyway:validate (after rewriting SQL files)"
  echo "  5. Run: ng build (to verify frontend)"
fi
