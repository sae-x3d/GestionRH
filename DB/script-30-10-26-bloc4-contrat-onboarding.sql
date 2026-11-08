-- Bloc 4 : historique des contrats, accreditation, affectation, onboarding
-- et workflow de validation Manager -> RH -> Direction.
-- Script idempotent, a executer avant le demarrage de l'application.

BEGIN;

ALTER TABLE contrat ADD COLUMN IF NOT EXISTS duree_essai_mois integer;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS date_fin_essai date;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS id_contrat_precedent integer;

-- Un candidat peut desormais avoir plusieurs contrats (historique). On retire
-- uniquement l'unicite portant sur id_candidature, pas celle de numero_contrat.
DO $$
DECLARE
    contrainte record;
    colonne smallint;
BEGIN
    SELECT attnum INTO colonne
    FROM pg_attribute
    WHERE attrelid = 'contrat'::regclass AND attname = 'id_candidature';

    FOR contrainte IN
        SELECT conname, conkey
        FROM pg_constraint
        WHERE conrelid = 'contrat'::regclass AND contype = 'u'
    LOOP
        IF array_length(contrainte.conkey, 1) = 1 AND contrainte.conkey[1] = colonne THEN
            EXECUTE format('ALTER TABLE contrat DROP CONSTRAINT %I', contrainte.conname);
        END IF;
    END LOOP;
END $$;

ALTER TABLE contrat DROP CONSTRAINT IF EXISTS contrat_type_contrat_check;
ALTER TABLE contrat DROP CONSTRAINT IF EXISTS ck_contrat_cdd_dates;
ALTER TABLE contrat DROP CONSTRAINT IF EXISTS ck_contrat_cdi_sans_fin;
ALTER TABLE contrat DROP CONSTRAINT IF EXISTS ck_contrat_type;
ALTER TABLE contrat DROP CONSTRAINT IF EXISTS ck_contrat_dates;

ALTER TABLE contrat ADD CONSTRAINT ck_contrat_type
    CHECK (type_contrat IN ('CDI', 'CDD', 'ESSAI'));
ALTER TABLE contrat ADD CONSTRAINT ck_contrat_dates CHECK (
    (type_contrat = 'CDD' AND date_fin IS NOT NULL AND date_fin_essai IS NULL)
    OR (type_contrat = 'CDI' AND date_fin IS NULL AND date_fin_essai IS NULL)
    OR (type_contrat = 'ESSAI' AND date_fin IS NULL AND date_fin_essai IS NOT NULL)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_contrat_precedent'
    ) THEN
        ALTER TABLE contrat
            ADD CONSTRAINT fk_contrat_precedent
            FOREIGN KEY (id_contrat_precedent) REFERENCES contrat (id_contrat);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_contrat_candidature ON contrat (id_candidature);
CREATE INDEX IF NOT EXISTS idx_contrat_statut ON contrat (statut);

CREATE TABLE IF NOT EXISTS accreditation (
    id_accreditation serial PRIMARY KEY,
    id_user integer NOT NULL REFERENCES utilisateur (id_user),
    id_candidature integer REFERENCES candidature (id_candidature),
    type_accreditation varchar(150) NOT NULL,
    organisme varchar(200) NOT NULL,
    numero varchar(100),
    date_obtention date NOT NULL,
    date_expiration date,
    statut varchar(40) NOT NULL,
    document_nom_original varchar(255),
    document_chemin varchar(500),
    commentaire varchar(1000),
    id_verificateur integer REFERENCES utilisateur (id_user),
    date_verification timestamp,
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_accreditation_user ON accreditation (id_user);
CREATE INDEX IF NOT EXISTS idx_accreditation_candidature ON accreditation (id_candidature);
CREATE INDEX IF NOT EXISTS idx_accreditation_statut ON accreditation (statut);

CREATE TABLE IF NOT EXISTS affectation (
    id_affectation serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_poste integer NOT NULL REFERENCES poste (id_poste),
    id_dept integer NOT NULL REFERENCES departement (id_dept),
    id_manager integer REFERENCES utilisateur (id_user),
    date_debut date NOT NULL,
    date_fin date,
    statut varchar(30) NOT NULL,
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_affectation_employe ON affectation (id_emp);
CREATE INDEX IF NOT EXISTS idx_affectation_departement ON affectation (id_dept);
CREATE INDEX IF NOT EXISTS idx_affectation_manager ON affectation (id_manager);

CREATE TABLE IF NOT EXISTS onboarding (
    id_onboarding serial PRIMARY KEY,
    id_contrat integer NOT NULL UNIQUE REFERENCES contrat (id_contrat),
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_affectation integer REFERENCES affectation (id_affectation),
    statut varchar(40) NOT NULL,
    date_debut date NOT NULL,
    date_cloture date,
    decision_essai varchar(40),
    commentaire varchar(1000),
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_onboarding_statut ON onboarding (statut);
CREATE INDEX IF NOT EXISTS idx_onboarding_employe ON onboarding (id_emp);

CREATE TABLE IF NOT EXISTS onboarding_tache (
    id_tache serial PRIMARY KEY,
    id_onboarding integer NOT NULL REFERENCES onboarding (id_onboarding) ON DELETE CASCADE,
    libelle varchar(255) NOT NULL,
    ordre integer NOT NULL,
    obligatoire boolean NOT NULL DEFAULT false,
    fait boolean NOT NULL DEFAULT false,
    date_realisation date,
    commentaire varchar(500),
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_onboarding_tache_onboarding ON onboarding_tache (id_onboarding);

CREATE TABLE IF NOT EXISTS document (
    id_document serial PRIMARY KEY,
    id_onboarding integer NOT NULL REFERENCES onboarding (id_onboarding) ON DELETE CASCADE,
    type_document varchar(80) NOT NULL,
    nom_original varchar(255) NOT NULL,
    chemin varchar(500) NOT NULL,
    date_upload timestamp NOT NULL DEFAULT now(),
    valide boolean NOT NULL DEFAULT false,
    commentaire varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_document_onboarding ON document (id_onboarding);

CREATE TABLE IF NOT EXISTS validation_dossier (
    id_dossier serial PRIMARY KEY,
    type_objet varchar(30) NOT NULL,
    id_objet integer NOT NULL,
    numero_tentative integer NOT NULL DEFAULT 1,
    departement_id integer,
    statut varchar(30) NOT NULL,
    etape_courante integer NOT NULL DEFAULT 1,
    id_createur integer REFERENCES utilisateur (id_user),
    lien varchar(500),
    date_creation timestamp NOT NULL DEFAULT now(),
    date_cloture timestamp,
    CONSTRAINT uk_validation_dossier_objet
        UNIQUE (type_objet, id_objet, numero_tentative)
);
CREATE INDEX IF NOT EXISTS idx_validation_dossier_statut ON validation_dossier (statut);
CREATE INDEX IF NOT EXISTS idx_validation_dossier_objet ON validation_dossier (type_objet, id_objet);

CREATE TABLE IF NOT EXISTS validation_etape (
    id_etape serial PRIMARY KEY,
    id_dossier integer NOT NULL REFERENCES validation_dossier (id_dossier) ON DELETE CASCADE,
    ordre integer NOT NULL,
    role_requis varchar(30) NOT NULL,
    statut varchar(30) NOT NULL,
    id_decideur integer REFERENCES utilisateur (id_user),
    date_decision timestamp,
    commentaire varchar(1000),
    CONSTRAINT uk_validation_etape_ordre UNIQUE (id_dossier, ordre)
);

COMMIT;
