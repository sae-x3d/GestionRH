-- Bloc 9 : gestion de carriere.
-- A importer apres tables-14-11-26.sql et data-14-11-26.sql.
-- Les historiques sont append-only : une correction se fait par un nouveau mouvement,
-- jamais par la modification ou la suppression d'une ligne existante.
BEGIN;

-- Referentiel des mouvements de carriere. Un meme type peut qualifier une
-- affectation (promotion, mutation) et la remuneration qui l'accompagne.
CREATE TABLE IF NOT EXISTS types_mouvement_carriere (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    categorie VARCHAR(20) NOT NULL CHECK (categorie IN ('AFFECTATION', 'SALAIRE', 'MIXTE')),
    ordre SMALLINT NOT NULL DEFAULT 0,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO types_mouvement_carriere (code, libelle, categorie, ordre) VALUES
    ('AFFECTATION_INITIALE', 'Affectation initiale', 'AFFECTATION', 10),
    ('PROMOTION', 'Promotion', 'MIXTE', 20),
    ('MUTATION', 'Mutation', 'MIXTE', 30),
    ('CHANGEMENT_POSTE', 'Changement de poste', 'AFFECTATION', 40),
    ('SALAIRE_INITIAL', 'Salaire initial', 'SALAIRE', 50),
    ('CHANGEMENT_SALAIRE', 'Changement de salaire', 'SALAIRE', 60)
ON CONFLICT (code) DO UPDATE
SET libelle = EXCLUDED.libelle,
    categorie = EXCLUDED.categorie,
    ordre = EXCLUDED.ordre;

-- L'historique d'affectation existant est enrichi de snapshots de departement.
-- Ces snapshots restent valides meme si un poste est deplace ou renomme plus tard.
ALTER TABLE historique_affectation_employe
    ADD COLUMN IF NOT EXISTS type_mouvement_id INTEGER REFERENCES types_mouvement_carriere(id);
ALTER TABLE historique_affectation_employe
    ADD COLUMN IF NOT EXISTS ancien_departement_id INTEGER REFERENCES departements(id);
ALTER TABLE historique_affectation_employe
    ADD COLUMN IF NOT EXISTS nouveau_departement_id INTEGER REFERENCES departements(id);

-- Historique des remunerations. Le contrat actif reste la source de la paie
-- courante ; cet historique conserve chaque valeur et sa date d'effet.
CREATE TABLE IF NOT EXISTS historique_salaire_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) CHECK (ancien_salaire_base IS NULL OR ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    type_mouvement_id INTEGER NOT NULL REFERENCES types_mouvement_carriere(id) ON DELETE RESTRICT,
    contrat_id INTEGER REFERENCES contrats(id) ON DELETE RESTRICT,
    date_effet DATE NOT NULL,
    motif TEXT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Une promotion est un mouvement dans le meme departement avec un poste
-- different et une remuneration strictement superieure.
CREATE TABLE IF NOT EXISTS promotions (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    nouveau_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) NOT NULL CHECK (ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    date_effet DATE NOT NULL,
    motif TEXT NOT NULL,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    historique_affectation_id INTEGER NOT NULL UNIQUE REFERENCES historique_affectation_employe(id) ON DELETE RESTRICT,
    historique_salaire_id INTEGER NOT NULL UNIQUE REFERENCES historique_salaire_employe(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_promotions_postes CHECK (ancien_poste_id <> nouveau_poste_id),
    CONSTRAINT ck_promotions_salaire CHECK (nouveau_salaire_base > ancien_salaire_base)
);

-- Une mutation change de departement et de poste. Le salaire peut rester
-- inchange (historique_salaire_id NULL) ou etre ajuste dans la fourchette cible.
CREATE TABLE IF NOT EXISTS mutations (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    nouveau_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    ancien_departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    nouveau_departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) NOT NULL CHECK (ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    date_effet DATE NOT NULL,
    motif TEXT NOT NULL,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    historique_affectation_id INTEGER NOT NULL UNIQUE REFERENCES historique_affectation_employe(id) ON DELETE RESTRICT,
    historique_salaire_id INTEGER UNIQUE REFERENCES historique_salaire_employe(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mutations_postes CHECK (ancien_poste_id <> nouveau_poste_id),
    CONSTRAINT ck_mutations_departements CHECK (ancien_departement_id <> nouveau_departement_id)
);

CREATE INDEX IF NOT EXISTS idx_historique_salaire_employe_chrono
    ON historique_salaire_employe(employe_id, date_effet DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_promotions_employe_chrono
    ON promotions(employe_id, date_effet DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_mutations_employe_chrono
    ON mutations(employe_id, date_effet DESC, id DESC);

-- Backfill des lignes d'affectation existantes (trigger d'embauche et ancienne
-- mise a jour de poste par le gestionnaire des comptes).
UPDATE historique_affectation_employe h
SET nouveau_departement_id = p.departement_id
FROM postes p
WHERE p.id = h.nouveau_poste_id
  AND h.nouveau_departement_id IS NULL;

UPDATE historique_affectation_employe h
SET ancien_departement_id = p.departement_id
FROM postes p
WHERE p.id = h.ancien_poste_id
  AND h.ancien_departement_id IS NULL;

UPDATE historique_affectation_employe h
SET type_mouvement_id = t.id
FROM types_mouvement_carriere t
WHERE h.type_mouvement_id IS NULL
  AND t.code = CASE
      WHEN h.ancien_poste_id IS NULL THEN 'AFFECTATION_INITIALE'
      WHEN h.ancien_departement_id IS NOT DISTINCT FROM h.nouveau_departement_id THEN 'CHANGEMENT_POSTE'
      ELSE 'MUTATION'
  END;

-- Backfill du salaire initial depuis le contrat actif. L'insertion est
-- idempotente et ne cree qu'un seul salaire initial par salarie.
INSERT INTO historique_salaire_employe
    (employe_id, nouveau_salaire_base, type_mouvement_id, contrat_id, date_effet, motif)
SELECT e.id, contrat.salaire_base_mensuel, t.id, contrat.id,
       GREATEST(e.date_embauche, contrat.date_debut),
       'Salaire initial repris du contrat actif'
FROM employes e
JOIN LATERAL (
    SELECT ct.id, ct.salaire_base_mensuel, ct.date_debut
    FROM candidatures ca
    JOIN contrats ct ON ct.candidature_id = ca.id
    JOIN statuts_contrat sc ON sc.id = ct.statut_id
    WHERE ca.candidat_id = e.user_id AND sc.code = 'ACTIF'
    ORDER BY ct.date_debut DESC, ct.id DESC
    LIMIT 1
) contrat ON TRUE
JOIN types_mouvement_carriere t ON t.code = 'SALAIRE_INITIAL'
WHERE NOT EXISTS (
    SELECT 1 FROM historique_salaire_employe h WHERE h.employe_id = e.id
);

-- Les nouveaux contrats creent automatiquement le salaire initial du salarie.
CREATE OR REPLACE FUNCTION initialiser_historique_salaire_employe()
RETURNS TRIGGER AS $$
DECLARE
    employe_cible_id INTEGER;
    date_embauche_employe DATE;
BEGIN
    SELECT e.id, e.date_embauche
      INTO employe_cible_id, date_embauche_employe
      FROM candidatures c
      JOIN employes e ON e.user_id = c.candidat_id
     WHERE c.id = NEW.candidature_id;

    IF employe_cible_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM historique_salaire_employe WHERE employe_id = employe_cible_id) THEN
        INSERT INTO historique_salaire_employe
            (employe_id, nouveau_salaire_base, type_mouvement_id, contrat_id, date_effet, motif)
        SELECT employe_cible_id, NEW.salaire_base_mensuel, t.id, NEW.id,
               GREATEST(NEW.date_debut, date_embauche_employe),
               'Salaire initial repris du contrat'
        FROM types_mouvement_carriere t
        WHERE t.code = 'SALAIRE_INITIAL';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_initialiser_historique_salaire ON contrats;
CREATE TRIGGER trg_initialiser_historique_salaire
AFTER INSERT ON contrats
FOR EACH ROW EXECUTE FUNCTION initialiser_historique_salaire_employe();

-- La creation automatique du dossier doit aussi qualifier l'affectation
-- initiale et enregistrer le snapshot de departement.
CREATE OR REPLACE FUNCTION initialiser_situation_administrative_employe()
RETURNS TRIGGER AS $$
DECLARE statut_actif_id INTEGER;
BEGIN
    SELECT id INTO statut_actif_id FROM statuts_administratifs_employe WHERE code = 'ACTIF';
    IF statut_actif_id IS NOT NULL THEN
        INSERT INTO situation_administrative_employe (employe_id, statut_id, date_effet, motif)
        VALUES (NEW.id, statut_actif_id, NEW.date_embauche, 'Création automatique du dossier salarié')
        ON CONFLICT (employe_id) DO NOTHING;
        INSERT INTO historique_statut_employe (employe_id, statut_id, date_effet, motif)
        VALUES (NEW.id, statut_actif_id, NEW.date_embauche, 'Création automatique du dossier salarié');
        INSERT INTO historique_affectation_employe
            (employe_id, nouveau_poste_id, nouveau_departement_id, provenance_id,
             type_mouvement_id, date_effet, motif)
        SELECT NEW.id, NEW.poste_id, p.departement_id, NEW.provenance_id,
               t.id, NEW.date_embauche, 'Affectation initiale'
        FROM postes p
        JOIN types_mouvement_carriere t ON t.code = 'AFFECTATION_INITIALE'
        WHERE p.id = NEW.poste_id;
        INSERT INTO informations_personnelles_employe (employe_id) VALUES (NEW.id) ON CONFLICT (employe_id) DO NOTHING;
        INSERT INTO evenements_dossier_employe (employe_id, type_evenement_id, objet_type, objet_id, date_effet, resume)
        SELECT NEW.id, type.id, 'EMPLOYE', NEW.id, NEW.date_embauche, 'Création du dossier administratif salarié'
        FROM types_evenements_dossier type WHERE type.code = 'DOSSIER_CREE';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_initialiser_situation_administrative ON employes;
CREATE TRIGGER trg_initialiser_situation_administrative
AFTER INSERT ON employes
FOR EACH ROW EXECUTE FUNCTION initialiser_situation_administrative_employe();

-- Referentiels applicatifs necessaires aux notifications et a la chronologie
-- du dossier salarie.
INSERT INTO types_notifications (code, libelle) VALUES
    ('CARRIERE_STATUT', 'Mise a jour de la carriere')
ON CONFLICT (code) DO NOTHING;
INSERT INTO types_evenements_dossier (code, libelle) VALUES
    ('CARRIERE_EVENEMENT', 'Mouvement de carriere')
ON CONFLICT (code) DO NOTHING;

-- Contraintes de coherence ajoutees apres backfill pour rester idempotent.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_historique_affectation_mouvement') THEN
        ALTER TABLE historique_affectation_employe
            ADD CONSTRAINT ck_historique_affectation_mouvement CHECK (type_mouvement_id IS NOT NULL);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_historique_affectation_departement') THEN
        ALTER TABLE historique_affectation_employe
            ADD CONSTRAINT ck_historique_affectation_departement CHECK (nouveau_departement_id IS NOT NULL);
    END IF;
END $$;

-- Chronologie : une date d'effet ne peut pas preceder l'embauche ni repeter
-- une date deja historisee pour le meme salarie et le meme type d'historique.
CREATE OR REPLACE FUNCTION valider_chronologie_affectation_employe()
RETURNS TRIGGER AS $$
DECLARE
    date_embauche_employe DATE;
    derniere_date DATE;
BEGIN
    SELECT date_embauche INTO date_embauche_employe FROM employes WHERE id = NEW.employe_id;
    IF date_embauche_employe IS NULL THEN
        RAISE EXCEPTION 'Salarie % introuvable pour l''historique d''affectation.', NEW.employe_id;
    END IF;
    IF NEW.date_effet < date_embauche_employe THEN
        RAISE EXCEPTION 'La date d''effet % precede la date d''embauche %.', NEW.date_effet, date_embauche_employe;
    END IF;
    SELECT MAX(date_effet) INTO derniere_date
      FROM historique_affectation_employe
     WHERE employe_id = NEW.employe_id;
    IF derniere_date IS NOT NULL AND NEW.date_effet <= derniere_date THEN
        RAISE EXCEPTION 'La date d''effet % doit etre strictement posterieure au dernier mouvement d''affectation %.',
            NEW.date_effet, derniere_date;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION valider_chronologie_historique_salaire()
RETURNS TRIGGER AS $$
DECLARE
    date_embauche_employe DATE;
    derniere_date DATE;
BEGIN
    SELECT date_embauche INTO date_embauche_employe FROM employes WHERE id = NEW.employe_id;
    IF date_embauche_employe IS NULL THEN
        RAISE EXCEPTION 'Salarie % introuvable pour l''historique de salaire.', NEW.employe_id;
    END IF;
    IF NEW.date_effet < date_embauche_employe THEN
        RAISE EXCEPTION 'La date d''effet % precede la date d''embauche %.', NEW.date_effet, date_embauche_employe;
    END IF;
    SELECT MAX(date_effet) INTO derniere_date
      FROM historique_salaire_employe
     WHERE employe_id = NEW.employe_id;
    IF derniere_date IS NOT NULL AND NEW.date_effet <= derniere_date THEN
        RAISE EXCEPTION 'La date d''effet % doit etre strictement posterieure au dernier salaire historise %.',
            NEW.date_effet, derniere_date;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_valider_chronologie_affectation ON historique_affectation_employe;
CREATE TRIGGER trg_valider_chronologie_affectation
BEFORE INSERT ON historique_affectation_employe
FOR EACH ROW EXECUTE FUNCTION valider_chronologie_affectation_employe();

DROP TRIGGER IF EXISTS trg_valider_chronologie_salaire ON historique_salaire_employe;
CREATE TRIGGER trg_valider_chronologie_salaire
BEFORE INSERT ON historique_salaire_employe
FOR EACH ROW EXECUTE FUNCTION valider_chronologie_historique_salaire();

-- Ecriture append-only : toute tentative de reecriture est refusee.
CREATE OR REPLACE FUNCTION empecher_modification_historique_carriere()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Les historiques de carriere sont en lecture seule. Creez un nouveau mouvement au lieu de modifier ou supprimer une ligne.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_historique_affectation_immuable ON historique_affectation_employe;
CREATE TRIGGER trg_historique_affectation_immuable
BEFORE UPDATE OR DELETE ON historique_affectation_employe
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_historique_salaire_immuable ON historique_salaire_employe;
CREATE TRIGGER trg_historique_salaire_immuable
BEFORE UPDATE OR DELETE ON historique_salaire_employe
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_promotion_immuable ON promotions;
CREATE TRIGGER trg_promotion_immuable
BEFORE UPDATE OR DELETE ON promotions
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_mutation_immuable ON mutations;
CREATE TRIGGER trg_mutation_immuable
BEFORE UPDATE OR DELETE ON mutations
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

COMMIT;
