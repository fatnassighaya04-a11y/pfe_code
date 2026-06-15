-- ================================================================
-- MIGRATION V8 : Ajout du type de réclamation
-- ================================================================

ALTER TABLE reclamations
    ADD COLUMN IF NOT EXISTS reclamation_type VARCHAR(100);