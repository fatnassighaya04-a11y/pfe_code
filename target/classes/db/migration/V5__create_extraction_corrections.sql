-- =====================================================
-- Table de traçabilité des corrections IA → humain
-- Permet d'analyser sur quels champs Gemini se trompe
-- =====================================================

CREATE TABLE IF NOT EXISTS extraction_corrections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    extraction_id   UUID NOT NULL REFERENCES extractions(id) ON DELETE CASCADE,
    document_type   VARCHAR(50) NOT NULL,
    field_name      VARCHAR(100) NOT NULL,
    ai_value        TEXT,
    human_value     TEXT,
    corrected_by    UUID REFERENCES users(id),

    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_corrections_field      ON extraction_corrections(field_name);
CREATE INDEX IF NOT EXISTS idx_corrections_doc_type   ON extraction_corrections(document_type);
CREATE INDEX IF NOT EXISTS idx_corrections_extraction ON extraction_corrections(extraction_id);
