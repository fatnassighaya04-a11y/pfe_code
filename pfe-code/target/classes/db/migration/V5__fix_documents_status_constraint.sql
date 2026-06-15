-- ================================================================
-- MIGRATION V5 : Corrige la contrainte sur documents.status
-- Autorise tous les statuts utilisés par le code métier actuel.
-- ================================================================

DO $$
BEGIN
    ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check;
EXCEPTION
    WHEN undefined_object THEN NULL;
END $$;

ALTER TABLE documents
    ADD CONSTRAINT documents_status_check
    CHECK (status IN (
        'PENDING',
        'UPLOADED',
        'PROCESSING',
        'COMPLETED',
        'VALIDATED',
        'REJECTED',
        'FAILED',
        'ARCHIVED'
    ));
