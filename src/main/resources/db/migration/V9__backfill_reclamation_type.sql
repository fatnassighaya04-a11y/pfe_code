-- ================================================================
-- MIGRATION V9 : Backfill du type de réclamation
-- ================================================================

UPDATE reclamations
SET reclamation_type = 'AUTRE'
WHERE reclamation_type IS NULL OR TRIM(reclamation_type) = '';

ALTER TABLE reclamations
    ALTER COLUMN reclamation_type SET DEFAULT 'AUTRE';

ALTER TABLE reclamations
    ALTER COLUMN reclamation_type SET NOT NULL;