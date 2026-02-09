# ImmoCare - Data Dictionary

## Overview

This document provides detailed specifications for all entities, attributes, data types, constraints, and validation rules in the ImmoCare application.

---

## Entity: USER

**Purpose**: Authentication and user management

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | NO | - | Login username | admin |
| password_hash | VARCHAR(255) | NOT NULL | NO | - | BCrypt hashed password | $2a$10$... |
| email | VARCHAR(100) | UNIQUE, NOT NULL | NO | - | Email address | admin@immocare.com |
| role | VARCHAR(20) | NOT NULL | NO | ADMIN | User role | ADMIN |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |
| updated_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Last update timestamp | 2024-01-16 14:22:00 |

**Validation Rules**:
- `username`: 3-50 characters, alphanumeric + underscore
- `password`: Minimum 8 characters, must contain uppercase, lowercase, digit
- `email`: Valid email format (RFC 5322)
- `role`: Must be from enum (ADMIN)

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX (username)
- UNIQUE INDEX (email)

---

## Entity: BUILDING

**Purpose**: Physical buildings containing housing units

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| name | VARCHAR(100) | NOT NULL | NO | - | Building name | Résidence Soleil |
| street_address | VARCHAR(200) | NOT NULL | NO | - | Street address | 123 Rue de la Loi |
| postal_code | VARCHAR(20) | NOT NULL | NO | - | Postal code | 1000 |
| city | VARCHAR(100) | NOT NULL | NO | - | City name | Brussels |
| country | VARCHAR(100) | NOT NULL | NO | Belgium | Country name | Belgium |
| owner_name | VARCHAR(200) | - | YES | NULL | Building owner | Jean Dupont |
| created_by | BIGINT | FK(USER) | YES | NULL | User who created | 1 |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |
| updated_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Last update timestamp | 2024-01-16 14:22:00 |

**Validation Rules**:
- `name`: 1-100 characters
- `street_address`: 1-200 characters
- `postal_code`: 1-20 characters
- `city`: 1-100 characters
- `country`: 1-100 characters
- `owner_name`: 0-200 characters (optional)

**Indexes**:
- PRIMARY KEY (id)
- INDEX (created_by)
- INDEX (city) - for filtering by location

---

## Entity: HOUSING_UNIT

**Purpose**: Individual apartments or units within buildings

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| building_id | BIGINT | FK(BUILDING), NOT NULL | NO | - | Parent building | 1 |
| unit_number | VARCHAR(20) | NOT NULL | NO | - | Unit identifier | A101 |
| floor | INTEGER | NOT NULL | NO | - | Floor number | 1 |
| landing_number | VARCHAR(10) | - | YES | NULL | Landing/staircase | A |
| total_surface | DECIMAL(7,2) | CHECK > 0 | YES | NULL | Total surface (m²) | 85.50 |
| has_terrace | BOOLEAN | NOT NULL | NO | FALSE | Has terrace | true |
| terrace_surface | DECIMAL(7,2) | CHECK > 0 | YES | NULL | Terrace surface (m²) | 12.00 |
| terrace_orientation | VARCHAR(2) | - | YES | NULL | Terrace orientation | S |
| has_garden | BOOLEAN | NOT NULL | NO | FALSE | Has garden | false |
| garden_surface | DECIMAL(7,2) | CHECK > 0 | YES | NULL | Garden surface (m²) | NULL |
| garden_orientation | VARCHAR(2) | - | YES | NULL | Garden orientation | NULL |
| owner_name | VARCHAR(200) | - | YES | NULL | Unit-specific owner | Marie Martin |
| created_by | BIGINT | FK(USER) | YES | NULL | User who created | 1 |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |
| updated_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Last update timestamp | 2024-01-16 14:22:00 |

**Validation Rules**:
- `unit_number`: 1-20 characters, alphanumeric
- `floor`: Integer (-10 to 100)
- `landing_number`: 1-10 characters (optional)
- `total_surface`: Positive decimal, max 9999.99 m²
- `terrace_surface`: Required if `has_terrace = true`
- `terrace_orientation`: Must be N, S, E, W, NE, NW, SE, SW (if provided)
- `garden_surface`: Required if `has_garden = true`
- `garden_orientation`: Must be N, S, E, W, NE, NW, SE, SW (if provided)

**Business Logic**:
- If `owner_name` is NULL, inherit from `building.owner_name`
- `total_surface` can be calculated from sum of room surfaces or manually entered

**Indexes**:
- PRIMARY KEY (id)
- UNIQUE INDEX (building_id, unit_number)
- INDEX (building_id) - for filtering units by building
- INDEX (created_by)

---

## Entity: ROOM

**Purpose**: Individual rooms within housing units

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| housing_unit_id | BIGINT | FK(HOUSING_UNIT), NOT NULL | NO | - | Parent housing unit | 1 |
| room_type | VARCHAR(20) | NOT NULL | NO | - | Room type | BEDROOM |
| approximate_surface | DECIMAL(6,2) | CHECK > 0, NOT NULL | NO | - | Surface (m²) | 15.50 |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |
| updated_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Last update timestamp | 2024-01-16 14:22:00 |

**Validation Rules**:
- `room_type`: Must be from enum: LIVING_ROOM, BEDROOM, KITCHEN, BATHROOM, TOILET, HALLWAY, STORAGE, OFFICE, DINING_ROOM, OTHER
- `approximate_surface`: Positive decimal, max 999.99 m²

**Indexes**:
- PRIMARY KEY (id)
- INDEX (housing_unit_id) - for retrieving all rooms of a unit

**Room Type Enum Values**:
- `LIVING_ROOM`: Main living space
- `BEDROOM`: Sleeping room
- `KITCHEN`: Cooking area
- `BATHROOM`: Full bathroom with shower/bath
- `TOILET`: Separate WC
- `HALLWAY`: Entrance or corridor
- `STORAGE`: Storage room, closet, cellar
- `OFFICE`: Home office
- `DINING_ROOM`: Separate dining area
- `OTHER`: Any other type

---

## Entity: PEB_SCORE_HISTORY

**Purpose**: Energy Performance Certificate history

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| housing_unit_id | BIGINT | FK(HOUSING_UNIT), NOT NULL | NO | - | Parent housing unit | 1 |
| peb_score | VARCHAR(10) | NOT NULL | NO | - | Energy score | B |
| score_date | DATE | NOT NULL | NO | - | Score issue date | 2024-01-15 |
| certificate_number | VARCHAR(50) | - | YES | NULL | Certificate ID | PEB-2024-123456 |
| valid_until | DATE | - | YES | NULL | Expiration date | 2034-01-15 |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |

**Validation Rules**:
- `peb_score`: Must be from enum: A_PLUS_PLUS, A_PLUS, A, B, C, D, E, F, G
- `score_date`: Cannot be in future
- `valid_until`: Must be after `score_date` (if provided)
- `certificate_number`: 1-50 characters (optional)

**Indexes**:
- PRIMARY KEY (id)
- INDEX (housing_unit_id, score_date DESC) - for retrieving history
- INDEX (housing_unit_id, valid_until) - for checking validity

**PEB Score Enum Values**:
- `A_PLUS_PLUS`: Excellent (A++)
- `A_PLUS`: Very good (A+)
- `A`: Good
- `B`: Fairly good
- `C`: Average
- `D`: Mediocre
- `E`: Poor
- `F`: Very poor
- `G`: Extremely poor

**Query Pattern for Current Score**:
```sql
SELECT * FROM peb_score_history
WHERE housing_unit_id = ?
ORDER BY score_date DESC
LIMIT 1;
```

---

## Entity: RENT_HISTORY

**Purpose**: Indicative rent amounts over time

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| housing_unit_id | BIGINT | FK(HOUSING_UNIT), NOT NULL | NO | - | Parent housing unit | 1 |
| monthly_rent | DECIMAL(10,2) | CHECK > 0, NOT NULL | NO | - | Monthly rent (EUR) | 850.00 |
| effective_from | DATE | NOT NULL | NO | - | Start date | 2024-01-01 |
| effective_to | DATE | - | YES | NULL | End date | NULL |
| notes | TEXT | - | YES | NULL | Notes about change | Annual indexation |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |

**Validation Rules**:
- `monthly_rent`: Positive decimal, max 99,999,999.99 EUR
- `effective_from`: Cannot be in distant future (> 1 year)
- `effective_to`: Must be after `effective_from` (if provided)
- Only one record per housing unit should have `effective_to = NULL` (current rent)

**Indexes**:
- PRIMARY KEY (id)
- INDEX (housing_unit_id, effective_from DESC) - for retrieving history
- INDEX (housing_unit_id, effective_to) - for finding current rent

**Query Pattern for Current Rent**:
```sql
SELECT * FROM rent_history
WHERE housing_unit_id = ?
  AND effective_to IS NULL;
```

**Query Pattern for Rent at Specific Date**:
```sql
SELECT * FROM rent_history
WHERE housing_unit_id = ?
  AND effective_from <= ?
  AND (effective_to IS NULL OR effective_to >= ?);
```

---

## Entity: WATER_METER_HISTORY

**Purpose**: Water meter assignments over time

| Attribute | Data Type | Constraints | Nullable | Default | Description | Example |
|-----------|-----------|-------------|----------|---------|-------------|---------|
| id | BIGINT | PK, AUTO_INCREMENT | NO | - | Unique identifier | 1 |
| housing_unit_id | BIGINT | FK(HOUSING_UNIT), NOT NULL | NO | - | Parent housing unit | 1 |
| meter_number | VARCHAR(50) | NOT NULL | NO | - | Meter identifier | WM-2024-ABC123 |
| meter_location | VARCHAR(100) | - | YES | NULL | Physical location | Kitchen under sink |
| installation_date | DATE | NOT NULL | NO | - | Installation date | 2024-01-15 |
| removal_date | DATE | - | YES | NULL | Removal date | NULL |
| created_at | TIMESTAMP | NOT NULL | NO | CURRENT_TIMESTAMP | Creation timestamp | 2024-01-15 10:30:00 |

**Validation Rules**:
- `meter_number`: 1-50 characters, alphanumeric with hyphens
- `meter_location`: 0-100 characters (optional)
- `installation_date`: Cannot be in future
- `removal_date`: Must be after `installation_date` (if provided)
- Only one record per housing unit should have `removal_date = NULL` (active meter)

**Indexes**:
- PRIMARY KEY (id)
- INDEX (housing_unit_id, installation_date DESC) - for retrieving history
- INDEX (housing_unit_id, removal_date) - for finding active meter
- INDEX (meter_number) - for meter lookup

**Query Pattern for Active Meter**:
```sql
SELECT * FROM water_meter_history
WHERE housing_unit_id = ?
  AND removal_date IS NULL;
```

---

## Common Patterns

### Audit Timestamps

All main entities have:
- `created_at`: Set automatically on INSERT
- `updated_at`: Set automatically on INSERT and UPDATE

History tables have only:
- `created_at`: Set automatically on INSERT (no updates allowed)

### Foreign Key Actions

- **ON DELETE CASCADE**: Applied to child entities (housing units, rooms, history)
- **ON DELETE SET NULL**: Applied to `created_by` references (retain data after user deletion)

### NULL Handling

- **Required fields**: Use `NOT NULL` constraint
- **Optional fields**: Allow `NULL`
- **Empty strings**: Not allowed - use `NULL` for optional text fields

### Validation Approach

1. **Database level**: Constraints, CHECK, NOT NULL, UNIQUE
2. **Application level**: Spring Validation annotations (@NotNull, @Size, @Pattern, etc.)
3. **Business logic level**: Service layer validation

---

## Naming Conventions

- **Tables**: Uppercase with underscores (e.g., `HOUSING_UNIT`)
- **Columns**: Lowercase with underscores (e.g., `unit_number`)
- **Foreign Keys**: `{referenced_table}_id` (e.g., `building_id`)
- **Enums**: Uppercase with underscores (e.g., `A_PLUS_PLUS`)
- **Indexes**: `idx_{table}_{columns}` (e.g., `idx_housing_unit_building_id`)

---

## Data Type Mappings

### Java to PostgreSQL

| Java Type | PostgreSQL Type | Notes |
|-----------|-----------------|-------|
| Long | BIGINT | Primary keys, IDs |
| String | VARCHAR(n) | Text with length limit |
| String | TEXT | Unlimited text |
| Integer | INTEGER | Whole numbers |
| BigDecimal | DECIMAL(p,s) | Monetary values, surfaces |
| Boolean | BOOLEAN | True/false flags |
| LocalDate | DATE | Dates without time |
| LocalDateTime | TIMESTAMP | Dates with time |

---

**Last Updated**: 2024-01-15  
**Version**: 1.0  
**Status**: Draft for Review
