BEGIN;

ALTER TABLE proposition_embauche
    ADD COLUMN IF NOT EXISTS poste_propose VARCHAR(150),
    ADD COLUMN IF NOT EXISTS departement_propose VARCHAR(150),
    ADD COLUMN IF NOT EXISTS lieu_travail VARCHAR(150),
    ADD COLUMN IF NOT EXISTS periode_essai_mois INTEGER,
    ADD COLUMN IF NOT EXISTS avantages TEXT,
    ADD COLUMN IF NOT EXISTS autres_conditions TEXT;

COMMIT;
