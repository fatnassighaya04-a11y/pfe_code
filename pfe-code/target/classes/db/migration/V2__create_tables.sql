-- ================================================================
-- MIGRATION V2 : Création de toutes les tables
-- Fichier : src/main/resources/db/migration/V2__create_tables.sql
-- ================================================================

-- ----------------------------------------------------------------
-- TABLE : users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    -- Clé primaire UUID (généré par Java, pas par PostgreSQL)
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    username        VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL UNIQUE,

    -- Jamais le mot de passe en clair ! Toujours un hash BCrypt
    password_hash   VARCHAR(255)    NOT NULL,

    -- Utilise le type ENUM créé en V1
    role            user_role       NOT NULL DEFAULT 'OPERATEUR',

    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_locked       BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER         NOT NULL DEFAULT 0,
    last_login      TIMESTAMP WITH TIME ZONE,

    -- Champs d'audit (gérés par Hibernate automatiquement)
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE,

    -- Soft delete : NULL = actif, non-NULL = supprimé logiquement
    deleted_at      TIMESTAMP WITH TIME ZONE
);

-- Index pour accélérer les recherches fréquentes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------
-- TABLE : documents
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS documents (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Nom affiché à l'utilisateur (tel qu'il a uploadé)
    original_filename   VARCHAR(500)    NOT NULL,

    -- Nom sur disque (UUID renommé, pour la sécurité)
    stored_filename     VARCHAR(500)    NOT NULL,

    -- Chemin complet sur le serveur
    file_path           TEXT            NOT NULL,

    file_type           file_type       NOT NULL,

    -- Taille en octets
    file_size           BIGINT          NOT NULL,

    -- Type MIME vérifié par Apache Tika
    mime_type           VARCHAR(100)    NOT NULL,

    -- Hash SHA-256 pour détecter les doublons
    checksum            VARCHAR(64),

    status              document_status NOT NULL DEFAULT 'UPLOADED',
    document_type       document_type   NOT NULL DEFAULT 'AUTRE',

    -- Clé étrangère vers la table users
    uploaded_by         UUID            NOT NULL REFERENCES users(id),

    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE,
    deleted_at          TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_document_type ON documents(document_type);
CREATE INDEX IF NOT EXISTS idx_documents_checksum ON documents(checksum);
CREATE INDEX IF NOT EXISTS idx_documents_deleted_at ON documents(deleted_at) WHERE deleted_at IS NULL;

-- ----------------------------------------------------------------
-- TABLE : extractions
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS extractions (
    id                      UUID                PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Lien vers le document source
    document_id             UUID                NOT NULL REFERENCES documents(id) ON DELETE CASCADE,

    -- Données extraites en format JSON flexible
    -- JSONB = JSON binaire, indexable, plus rapide que JSON
    extracted_data          JSONB,

    -- Texte brut OCR avant traitement
    raw_text                TEXT,

    -- Score entre 0.00 et 100.00
    confidence_score        DECIMAL(5,2)        CHECK (confidence_score >= 0 AND confidence_score <= 100),

    -- Version du modèle utilisé (ex: "gemini-1.5-flash")
    extraction_model        VARCHAR(100),

    -- Durée du traitement en ms
    extraction_duration_ms  INTEGER,

    status                  extraction_status   NOT NULL DEFAULT 'PENDING',

    -- Message d'erreur si status = FAILED
    error_message           TEXT,

    -- Validation manuelle
    validated_by            UUID                REFERENCES users(id),
    validated_at            TIMESTAMP WITH TIME ZONE,
    validation_notes        TEXT,

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE,
    deleted_at              TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_extractions_document_id ON extractions(document_id);
CREATE INDEX IF NOT EXISTS idx_extractions_status ON extractions(status);
CREATE INDEX IF NOT EXISTS idx_extractions_confidence ON extractions(confidence_score);
-- Index GIN sur JSONB pour les recherches dans les données extraites
CREATE INDEX IF NOT EXISTS idx_extractions_data_gin ON extractions USING GIN (extracted_data);

-- ----------------------------------------------------------------
-- TABLE : refresh_tokens
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Le token en lui-même (longue chaîne)
    token           TEXT        NOT NULL UNIQUE,

    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    expires_at      TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Révoqué lors du logout ou de la rotation
    is_revoked      BOOLEAN     NOT NULL DEFAULT FALSE,

    -- IP source (pour sécurité)
    created_from_ip VARCHAR(45),

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);

-- ----------------------------------------------------------------
-- TABLE : audit_logs
-- Pas de soft delete ici — les logs ne se suppriment JAMAIS
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),

    -- L'utilisateur (peut être NULL pour actions non authentifiées)
    user_id         UUID        REFERENCES users(id),

    -- Action effectuée : ex "DOCUMENT_UPLOAD", "USER_LOGIN_FAILED"
    action          VARCHAR(100) NOT NULL,

    -- Type et ID de la ressource concernée
    resource_type   VARCHAR(50),
    resource_id     VARCHAR(36),

    -- IP source
    ip_address      VARCHAR(45),

    -- "SUCCESS" ou "FAILURE"
    result          VARCHAR(20),

    -- Détails supplémentaires
    details         TEXT,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_logs(resource_type, resource_id);
