-- ================================================================
-- MIGRATION V1 : Création des types ENUM PostgreSQL
-- Fichier : src/main/resources/db/migration/V1__create_enums.sql
--
-- IMPORTANT : Les ENUM PostgreSQL doivent être créés AVANT les tables
--             qui les utilisent.
-- IF NOT EXISTS : évite l'erreur si le script est rejoué
-- ================================================================

-- Rôles utilisateurs
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('ADMIN', 'GESTIONNAIRE', 'OPERATEUR', 'LECTEUR');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Statuts d'un document
DO $$ BEGIN
    CREATE TYPE document_status AS ENUM ('UPLOADED', 'PROCESSING', 'COMPLETED', 'FAILED', 'REJECTED', 'ARCHIVED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Types de documents administratifs
DO $$ BEGIN
    CREATE TYPE document_type AS ENUM (
        'FACTURE', 'BON_COMMANDE', 'CONTRAT', 'FORMULAIRE',
        'DOCUMENT_TEXTE', 'TABLEAU_DONNEES', 'AUTRE'
    );
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Statuts d'une extraction IA
DO $$ BEGIN
    CREATE TYPE extraction_status AS ENUM ('PENDING', 'IN_PROGRESS', 'SUCCESS', 'PARTIAL', 'FAILED');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Types de fichiers supportés
DO $$ BEGIN
    CREATE TYPE file_type AS ENUM ('PDF', 'PNG', 'JPG', 'JPEG', 'TIFF', 'BMP', 'DOCX', 'XLSX', 'TXT');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;
