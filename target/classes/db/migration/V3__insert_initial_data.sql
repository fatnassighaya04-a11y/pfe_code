-- ================================================================
-- MIGRATION V3 : Données initiales
-- Fichier : src/main/resources/db/migration/V3__insert_initial_data.sql
--
-- Crée un utilisateur ADMIN par défaut pour le premier démarrage
-- MOT DE PASSE : "Admin@1234" encodé en BCrypt strength 12
-- IMPORTANT : Changer ce mot de passe immédiatement en production !
-- ================================================================

INSERT INTO users (id, username, email, password_hash, role, is_active, is_locked)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@docextraction.com',
    -- Hash BCrypt de "Admin@1234" (strength 12)
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewYFqKGMRRJMCiIe',
    'ADMIN',
    TRUE,
    FALSE
)
-- Si l'admin existe déjà (re-migration), ne rien faire
ON CONFLICT (email) DO NOTHING;

-- Créer aussi un utilisateur de test pour le développement
INSERT INTO users (id, username, email, password_hash, role, is_active, is_locked)
VALUES (
    gen_random_uuid(),
    'operateur_test',
    'operateur@docextraction.com',
    -- Hash BCrypt de "Test@1234"
    '$2a$12$eImiTXuWVxfM37uY4JANjQe5KOCF8S5iBHsKKFiOOqkgSBi5GQBXO',
    'OPERATEUR',
    TRUE,
    FALSE
)
ON CONFLICT (email) DO NOTHING;
