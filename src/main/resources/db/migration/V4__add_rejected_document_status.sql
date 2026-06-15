-- ================================================================
-- MIGRATION V4 : Ajout du statut REJECTED à document_status
-- Corrige les bases existantes qui ont été créées avant l'ajout
-- de REJECTED dans le code Java.
-- ================================================================

DO $$
BEGIN
    ALTER TYPE document_status ADD VALUE IF NOT EXISTS 'REJECTED';
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
