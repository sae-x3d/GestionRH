BEGIN;

-- Une candidature peut desormais recevoir plusieurs propositions d'embauche
-- (creation puis revisions successives apres negociation).
DO $$
DECLARE
    contrainte RECORD;
BEGIN
    FOR contrainte IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE rel.relname = 'proposition_embauche'
          AND con.contype = 'u'
          AND att.attname = 'id_candidature'
    LOOP
        EXECUTE format('ALTER TABLE proposition_embauche DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;
END $$;

ALTER TABLE proposition_embauche
    DROP CONSTRAINT IF EXISTS proposition_embauche_id_candidature_key;

ALTER TABLE proposition_embauche
    ADD COLUMN IF NOT EXISTS numero_revision INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS motif_revision TEXT;

UPDATE proposition_embauche
SET numero_revision = 1
WHERE numero_revision IS NULL;

CREATE INDEX IF NOT EXISTS idx_proposition_candidature_revision
    ON proposition_embauche(id_candidature, numero_revision DESC);

COMMIT;
