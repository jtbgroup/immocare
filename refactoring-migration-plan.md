# ImmoCare — Plan de migration : réorganisation UC/US/Flyway

## Contexte et justification

### Pourquoi ce refactoring ?

L'architecture multi-tenant (Estates) a été introduite tardivement dans le projet (UC004_ESTATE_PLACEHOLDER, migrations V017→V021), ce qui a généré plusieurs problèmes structurels :

1. **4 migrations de backfill** (V018, V019, V020, V021) pour ajouter `estate_id` sur des tables déjà créées — complexes, risquées, avec des stratégies de placeholder.
2. **Numérotation des US découplée des UC** — impossible de savoir à quel UC appartient `UC003.001` sans consulter la doc.
3. **Saut de numéro** — V016 n'existait pas, créant une discontinuité dans la séquence Flyway.
4. **UC004_ESTATE_PLACEHOLDER fragmenté en 6 phases** de migration au lieu d'une seule migration propre.

### Principe de la correction

En déplaçant la gestion des Estates en **UC004** (juste après Manage Users), toutes les tables métier créées ensuite reçoivent `estate_id NOT NULL` **dès leur création**. Plus aucun backfill n'est nécessaire.

---

## Règles de numérotation — nouvelles conventions

### Use Cases
```
UC001, UC002, UC004, ... (séquence continue, deux chiffres minimum)
```

### User Stories
```
UCNNN.SSS  →  ex: UC004.001, UC004.002, ...
```
- `NNN` = numéro du UC parent (3 chiffres)
- `SSS` = séquence au sein du UC (3 chiffres, commence à 001)
- Avantage : renommer un UC ne nécessite de changer que le préfixe, pas toute la numérotation globale

### Fichiers Flyway
```
VNNN__ucNNN_slug.sql  →  ex: V003__uc003_manage_estates.sql
```
- Un fichier par UC maximum
- Le numéro V = le numéro UC

### Fichiers de documentation
```
use-cases/UC-NNN-slug.md
user-stories/UC-NNN.SSS-slug.md
prompts/UC-NNN-slug-prompt.md
```

---

## Table de correspondance complète

### Use Cases

| Nouveau | Ancien | Titre |
|---------|--------|-------|
| UC001 | UC001 | Authentication *(inchangé)* |
| UC002 | UC002 | Manage Users *(inchangé)* |
| UC004 | UC004_ESTATE_PLACEHOLDER | Manage Estates *(déplacé)* |
| UC005 | UC004 | Manage Persons |
| UC006 | UC005 | Manage Buildings |
| UC007 | UC006 | Manage Housing Units |
| UC008 | UC007 | Manage Rooms |
| UC009 | UC008 | Manage PEB Scores |
| UC010 | UC009 | Manage Meters |
| UC011 | UC010 | Manage Rents |
| UC012 | UC011 | Manage Boilers |
| UC014 | UC012 | Manage Fire Extinguishers |
| UC013 | UC013 | Manage Platform Config *(inchangé)* |
| UC015 | UC014 | Manage Leases |
| UC016 | UC015 | Manage Financial Transactions |
| UC004_ESTATE_PLACEHOLDER | UC016 | Import Parser Strategies |

### Fichiers Flyway

| Nouveau fichier | Ancien fichier | Changements schema |
|----------------|----------------|--------------------|
| V001__uc001_authentication.sql | V001__uc001_authentication.sql | *(inchangé — sauf suppression de `role`, ajout `is_platform_admin`)* |
| V002__uc002_manage_users.sql | V002__uc002_manage_users.sql | *(inchangé)* |
| V003__uc003_manage_estates.sql | V003__uc003_manage_estates.sql | Fusion V017 uniquement — `estate` + `estate_member` |
| V004__uc004_manage_persons.sql | V004__uc004_manage_persons.sql | `person` + `person_bank_account` WITH `estate_id NOT NULL` |
| V005__uc005_manage_buildings.sql | V005__uc005_manage_buildings.sql | `building` WITH `estate_id NOT NULL` |
| V006__uc006_manage_housing_units.sql | V006__uc006_manage_housing_units.sql | `housing_unit` *(estate via building)* |
| V007__uc007_manage_rooms.sql | V007__uc007_manage_rooms.sql | `room` *(inchangé structurellement)* |
| V008__uc008_manage_peb_scores.sql | V008__uc008_manage_peb_scores.sql | `peb_score_history` *(inchangé)* |
| V009__uc009_manage_meters.sql | V009__uc009_manage_meters.sql | `meter` *(inchangé)* |
| V010__uc010_manage_rents.sql | V010__uc010_manage_rents.sql | `rent_history` *(inchangé)* |
| V011__uc011_manage_boilers.sql | V011__uc011_manage_boilers.sql | `boiler` + `boiler_service` + `boiler_service_validity_rule` WITH `estate_id NOT NULL` |
| V012__uc012_manage_fire_extinguishers.sql | V012__uc012_manage_fire_extinguishers.sql | `fire_extinguisher` + `fire_extinguisher_revision` *(inchangé)* |
| V013__uc013_manage_platform_config.sql | V013__uc013_manage_platform_config.sql | `platform_config` WITH `estate_id NOT NULL` + PK composite dès la création |
| V014__uc014_manage_leases.sql | V014__uc014_manage_leases.sql | `lease` + `lease_tenant` + `lease_rent_adjustment` *(inchangé)* |
| V015__uc015_manage_financial_transactions.sql | V015__uc015_manage_financial_transactions.sql | Toutes tables financières WITH `estate_id NOT NULL` dès création |
| V016__uc016_import_parser_strategies.sql | V016__uc016_import_parser_strategies.sql | Seeds `import_parser` *(inchangé)* |
| *(supprimés)* | V018, V019, V020, V021 | Backfills estate — plus nécessaires |

### User Stories

| Nouveau | Ancien | Titre |
|---------|--------|-------|
| **UC001** | | |
| UC001.001 | UC009.001 | Login |
| UC001.002 | UC009.002 | Logout |
| UC001.003 | UC009.003 | Get Current User |
| **UC002** | | |
| UC002.001 | UC002.001 | View User List |
| UC002.002 | UC002.002 | Create User |
| UC002.003 | UC002.003 | Edit User |
| UC002.004 | UC002.004 | Change User Password |
| UC002.005 | UC002.005 | Delete User |
| **UC004** | | |
| UC004.001 | UC003.001 | Create Estate |
| UC004.002 | UC003.002 | Edit Estate |
| UC004.003 | UC003.003 | Delete Estate |
| UC004.004 | UC003.004 | List All Estates |
| UC004.005 | UC003.005 | Assign First Manager |
| UC004.006 | UC003.006 | View Estate Members |
| UC004.007 | UC003.007 | Add Member to Estate |
| UC004.008 | UC003.008 | Edit Member Role |
| UC004.009 | UC003.009 | Remove Member |
| UC004.010 | UC003.010 | Select Active Estate |
| UC004.011 | UC003.011 | View Estate Dashboard |
| UC004.012 | UC003.012 | View My Estates |
| UC004.013 | UC003.013 | Enforce Estate-scoped Access |
| **UC005** | | |
| UC005.001 | UC004.001 | View Persons List |
| UC005.002 | UC004.002 | Create Person |
| UC005.003 | UC004.003 | Edit Person |
| UC005.004 | UC004.004 | Delete Person |
| UC005.005 | UC004.005 | Assign Person as Owner |
| UC005.006 | UC004.006 | Person Picker |
| UC005.007 | UC004.007 | Manage Person Bank Accounts |
| **UC006** | | |
| UC006.001 | UC005.001 | Create Building |
| UC006.002 | UC005.002 | Edit Building |
| UC006.003 | UC005.003 | Delete Building |
| UC006.004 | UC005.004 | View Buildings List |
| UC006.005 | UC005.005 | Search Buildings |
| **UC007** | | |
| UC007.001 | UC006.001 | Create Housing Unit |
| UC007.002 | UC006.002 | Edit Housing Unit |
| UC007.003 | UC006.003 | Delete Housing Unit |
| UC007.004 | UC006.004 | View Housing Unit Details |
| UC007.005 | UC006.005 | Add Terrace |
| UC007.006 | UC006.006 | Add Garden |
| **UC008** | | |
| UC008.001 | UC007.001 | Add Room |
| UC008.002 | UC007.002 | Edit Room |
| UC008.003 | UC007.003 | Delete Room |
| UC008.004 | UC007.004 | Batch Create Rooms |
| UC008.005 | UC007.005 | View Room Composition |
| **UC009** | | |
| UC009.001 | UC008.001 | Add PEB Score |
| UC009.002 | UC008.002 | View PEB Score History |
| UC009.003 | UC008.003 | Check PEB Certificate Validity |
| UC009.004 | UC008.004 | Track PEB Score Improvements |
| **UC010** | | |
| UC010.001 | UC009.001 | View Meters of Housing Unit |
| UC010.002 | UC009.002 | View Meters of Building |
| UC010.003 | UC009.003 | Add Meter to Housing Unit |
| UC010.004 | UC009.004 | Add Meter to Building |
| UC010.005 | UC009.005 | Replace a Meter |
| UC010.006 | UC009.006 | Remove a Meter |
| UC010.007 | UC009.007 | View Meter History |
| **UC011** | | |
| UC011.001 | UC010.001 | Set Initial Rent |
| UC011.002 | UC010.002 | Edit a Rent Record |
| UC011.003 | UC010.003 | View Rent History |
| UC011.004 | UC010.004 | Track Rent Increases |
| UC011.005 | UC010.005 | Add Notes to Rent Changes |
| **UC012** | | |
| UC012.001 | UC011.001 | Add Boiler to Housing Unit |
| UC012.002 | UC011.002 | View Active Boiler |
| UC012.003 | UC011.003 | Replace Boiler |
| UC012.004 | UC011.004 | View Boiler History |
| UC012.005 | UC011.005 | Add Boiler Service Record |
| UC012.006 | UC011.006 | View Boiler Service History |
| UC012.007 | UC011.007 | View Boiler Service Validity Alert |
| **UC014** | | |
| UC014.001 | UC012.001 | Add Fire Extinguisher |
| UC014.002 | UC012.002 | Edit Fire Extinguisher |
| UC014.003 | UC012.003 | Delete Fire Extinguisher |
| UC014.004 | UC012.004 | View Fire Extinguishers List |
| UC014.005 | UC012.005 | Add Revision Record |
| UC014.006 | UC012.006 | View Revision History |
| UC014.007 | UC012.007 | Delete Revision Record |
| **UC013** | | |
| UC013.001 | UC013.001 | View Platform Settings |
| UC013.002 | UC013.002 | Add Boiler Service Validity Rule |
| UC013.003 | UC013.003 | View Validity Rules History |
| UC013.004 | UC013.004 | Update General Settings |
| UC013.005 | UC012.001 | Manage Asset Type Mappings |
| **UC015** | | |
| UC015.001 | UC014.001 | View Lease for Housing Unit |
| UC015.002 | UC014.002 | Create Lease (Draft) |
| UC015.003 | UC014.003 | Activate Lease |
| UC015.004 | UC014.004 | Edit Lease |
| UC015.005 | UC014.005 | Finish Lease |
| UC015.006 | UC014.006 | Cancel Lease |
| UC015.007 | UC014.007 | Record Indexation |
| UC015.008 | UC014.008 | View Indexation History |
| UC015.009 | UC014.009 | Add Tenant to Lease |
| UC015.010 | UC014.010 | Remove Tenant from Lease |
| UC015.011 | UC014.011 | View Lease Alerts |
| **UC016** | | |
| UC016.001 | UC015.001 | View Transaction List |
| UC016.002 | UC015.002 | Create Transaction Manually |
| UC016.003 | UC015.003 | Edit Transaction |
| UC016.004 | UC015.004 | Delete Transaction |
| UC016.005 | UC015.005 | Classify Transaction |
| UC016.006 | UC015.006 | Link Transaction to Assets |
| UC016.007 | UC015.007 | Import Transactions |
| UC016.008 | UC015.008 | Review Imported Transactions |
| UC016.009 | UC015.009 | Manage Tag Catalog |
| UC016.010 | UC015.010 | Manage Bank Account Catalog |
| UC016.011 | UC015.011 | View Financial Summary |
| UC016.012 | UC015.012 | Export Transactions CSV |
| **UC004_ESTATE_PLACEHOLDER** | | |
| UC004_ESTATE_PLACEHOLDER.001 | UC016.001 | Select Parser on Import |
| UC004_ESTATE_PLACEHOLDER.002 | UC016.002 | View Parser Registry |

### Fichiers de documentation

| Nouveau chemin | Ancien chemin |
|---------------|---------------|
| `docs/analysis/use-cases/UC-001-authentication.md` | `docs/analysis/use-cases/UC001-authentication.md` |
| `docs/analysis/use-cases/UC-002-manage-users.md` | `docs/analysis/use-cases/UC002_manage_users.md` |
| `docs/analysis/use-cases/UC-003-manage-estates.md` | `docs/analysis/use-cases/UC004_manage_estates.md` |
| `docs/analysis/use-cases/UC-004-manage-persons.md` | `docs/analysis/use-cases/UC005_manage_persons.md` |
| `docs/analysis/use-cases/UC-005-manage-buildings.md` | `docs/analysis/use-cases/UC006_manage_buildings.md` |
| `docs/analysis/use-cases/UC-006-manage-housing-units.md` | `docs/analysis/use-cases/UC007_manage_housing_units.md` |
| `docs/analysis/use-cases/UC-007-manage-rooms.md` | `docs/analysis/use-cases/UC008_manage_rooms.md` |
| `docs/analysis/use-cases/UC-008-manage-peb-scores.md` | `docs/analysis/use-cases/UC009_manage_peb_scores.md` |
| `docs/analysis/use-cases/UC-009-manage-meters.md` | `docs/analysis/use-cases/UC010_manage_meters.md` |
| `docs/analysis/use-cases/UC-010-manage-rents.md` | `docs/analysis/use-cases/UC011_manage_rents.md` |
| `docs/analysis/use-cases/UC-011-manage-boilers.md` | `docs/analysis/use-cases/UC012_manage_boilers.md` |
| `docs/analysis/use-cases/UC-012-manage-fire-extinguishers.md` | `docs/analysis/use-cases/UC014_manage_fire_extinguishers.md` |
| `docs/analysis/use-cases/UC-013-manage-platform-config.md` | `docs/analysis/use-cases/UC013_manage_platform_config.md` |
| `docs/analysis/use-cases/UC-014-manage-leases.md` | `docs/analysis/use-cases/UC015_manage_leases.md` |
| `docs/analysis/use-cases/UC-015-manage-financial-transactions.md` | `docs/analysis/use-cases/UC016_manage_financial_transactions.md` |
| `docs/analysis/use-cases/UC-016-import-parser-strategies.md` | `docs/analysis/use-cases/UC004_ESTATE_PLACEHOLDER_import_parser_strategies.md` |

---

## Impact sur le code source

### Ce qui change

| Élément | Exemple avant | Exemple après |
|---------|--------------|---------------|
| Commentaires Flyway | `-- Use case: UC004_ESTATE_PLACEHOLDER` | `-- Use case: UC004` |
| Références dans les prompts | `UC004_ESTATE_PLACEHOLDER`, `UC003.001` | `UC004`, `UC004.001` |
| Références dans les use cases | `UC014`, `UC014.001` | `UC015`, `UC015.001` |
| Références dans les user stories | `UC009`, `UC009.001` | `UC010`, `UC010.001` |
| Commentaires Java | `// UC011 - Manage Boilers` | `// UC012 - Manage Boilers` |
| Commentaires SQL | `-- V010` | `-- V011` |

### Ce qui ne change pas

- Les noms de classes Java (ex: `BoilerService`, `LeaseController`)
- Les endpoints API (ex: `/api/v1/buildings`)
- Les noms de tables PostgreSQL
- Les noms de packages Java (`com.immocare`)
- La logique métier

---

## Gains obtenus

| Critère | Avant | Après |
|---------|-------|-------|
| Fichiers Flyway | 21 (V001→V021, V016 manquant) | 16 (V001→V016, séquence continue) |
| Migrations de backfill estate | 4 | 0 |
| Lisibilité US | `UC003.001` → chercher dans la doc | `UC004.001` → UC004, story 1 |
| Cohérence UC ↔ Flyway | Partielle | Totale |
| Tables sans estate_id à la création | bank_account, tag_category, person, building, boiler_service_validity_rule, platform_config | Aucune |

---

## Notes importantes pour la régénération du code

Lors de la régénération des fichiers Flyway concernés, tenir compte des
évolutions de schéma suivantes par rapport aux fichiers originaux :

### V001 — Authentication
- Supprimer `role VARCHAR(20)` de `app_user`
- Ajouter `is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE`
- Le seed admin reçoit `is_platform_admin = TRUE`

### V003 — Manage Estates
- Contient uniquement `estate` et `estate_member`
- Ne contient PAS les `ALTER TABLE` des autres entités (ils disparaissent)

### V004 — Manage Persons
- `person` et `person_bank_account` incluent `estate_id UUID NOT NULL REFERENCES estate(id)`

### V005 — Manage Buildings
- `building` inclut `estate_id UUID NOT NULL REFERENCES estate(id)`

### V011 — Manage Boilers
- `boiler_service_validity_rule` inclut `estate_id UUID NOT NULL REFERENCES estate(id)`
- Contrainte unique : `UNIQUE (estate_id, valid_from)` dès la création

### V013 — Manage Platform Config
- `platform_config` a une PK composite `(estate_id, config_key)` dès la création
- `estate_id UUID NOT NULL REFERENCES estate(id)`
- Plus de migration corrective nécessaire

### V015 — Manage Financial Transactions
- `bank_account`, `tag_category`, `financial_transaction`, `import_batch`
  incluent tous `estate_id UUID NOT NULL REFERENCES estate(id)` dès la création
- Suppression de la séquence `financial_transaction_ref_seq` globale
  (la référence est générée par estate dans le code)





TODO: 
  1. Review any 'CHECK_MANUALLY' flags in the codebase (US071 collision)
  2. Rewrite Flyway files that need schema changes (V001, V003→V005, V011, V013, V015)
  3. Update Spring Boot entity/service references to new UC numbers in comments
  4. Run: mvn flyway:validate (after rewriting SQL files)
  5. Run: ng build (to verify frontend)