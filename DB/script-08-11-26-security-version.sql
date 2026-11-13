BEGIN;

-- ==========================================
-- VERSION DE SECURITE DES SESSIONS
-- ==========================================

-- Utilisee par SecurityVersionFilter pour invalider les sessions lorsqu'un
-- utilisateur change de mot de passe ou de role.
ALTER TABLE utilisateur
    ADD COLUMN IF NOT EXISTS security_version INTEGER NOT NULL DEFAULT 0;

COMMIT;
