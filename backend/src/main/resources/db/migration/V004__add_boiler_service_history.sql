-- ============================================================
-- V004 — Boiler service history (US064 / US065 / US066)
-- Branch: develop
-- ============================================================

CREATE TABLE boiler_service (
    id           BIGSERIAL    PRIMARY KEY,
    boiler_id    BIGINT       NOT NULL REFERENCES boiler(id) ON DELETE RESTRICT,
    service_date DATE         NOT NULL,
    valid_until  DATE         NOT NULL,
    notes        TEXT         NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_boiler_service_boiler ON boiler_service(boiler_id);
CREATE INDEX idx_boiler_service_date   ON boiler_service(boiler_id, service_date DESC);

-- Migrate existing flat data: boilers that already have dates get one seed record.
INSERT INTO boiler_service (boiler_id, service_date, valid_until, notes)
SELECT id, last_service_date, next_service_date, 'Migrated from initial record'
FROM boiler
WHERE last_service_date IS NOT NULL
  AND next_service_date IS NOT NULL;