-- Bloc 9 : evaluation, competences et formation.
-- Script additif a importer apres tables-14-11-26.sql.
-- Il ne modifie pas tables-14-11-26.sql et peut etre rejoue sans recreer les tables.

BEGIN;

-- Le bareme est porte par le poste : chaque poste definit son amplitude de notation.
ALTER TABLE postes ADD COLUMN IF NOT EXISTS niveau_min SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE postes ADD COLUMN IF NOT EXISTS niveau_max SMALLINT NOT NULL DEFAULT 5;
ALTER TABLE postes DROP CONSTRAINT IF EXISTS ck_postes_bareme_competence;
ALTER TABLE postes ADD CONSTRAINT ck_postes_bareme_competence
    CHECK (niveau_min >= 0 AND niveau_max > niveau_min);

CREATE TABLE IF NOT EXISTS competences (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_competences_code CHECK (code = UPPER(code) AND code ~ '^[A-Z0-9_]{2,40}$')
);

CREATE TABLE IF NOT EXISTS criteres_poste (
    id SERIAL PRIMARY KEY,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL DEFAULT 1,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire VARCHAR(1000),
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (poste_id, competence_id),
    CONSTRAINT ck_criteres_poste_niveaux CHECK (niveau_attendu >= 0),
    CONSTRAINT ck_criteres_poste_seuil CHECK (seuil_ecart >= 0)
);

CREATE TABLE IF NOT EXISTS evaluations_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    evaluateur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    date_evaluation DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'SOUMISE',
    commentaire VARCHAR(1500),
    bareme_min SMALLINT NOT NULL,
    bareme_max SMALLINT NOT NULL,
    soumise_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validee_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_evaluations_statut CHECK (statut IN ('SOUMISE', 'VALIDEE')),
    CONSTRAINT ck_evaluations_bareme CHECK (bareme_min >= 0 AND bareme_max > bareme_min),
    CONSTRAINT ck_evaluations_validation CHECK (
        (statut = 'SOUMISE' AND valide_par_id IS NULL AND validee_at IS NULL)
        OR (statut = 'VALIDEE' AND valide_par_id IS NOT NULL AND validee_at IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS evaluation_lignes (
    id SERIAL PRIMARY KEY,
    evaluation_id INTEGER NOT NULL REFERENCES evaluations_employe(id) ON DELETE RESTRICT,
    critere_id INTEGER NOT NULL REFERENCES criteres_poste(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    niveau_acquis SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL,
    ecart SMALLINT NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (evaluation_id, critere_id),
    CONSTRAINT ck_evaluation_lignes_niveaux CHECK (niveau_attendu >= 0 AND niveau_acquis >= 0),
    CONSTRAINT ck_evaluation_lignes_seuil CHECK (seuil_ecart >= 0),
    CONSTRAINT ck_evaluation_lignes_ecart CHECK (ecart = GREATEST(0, niveau_attendu - niveau_acquis))
);

CREATE TABLE IF NOT EXISTS besoins_formation (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    evaluation_ligne_id INTEGER NOT NULL UNIQUE REFERENCES evaluation_lignes(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    niveau_acquis_initial SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL,
    ecart_initial SMALLINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cloture_at TIMESTAMPTZ,
    CONSTRAINT ck_besoins_formation_statut CHECK (statut IN ('OUVERT', 'PLANIFIE', 'CLOTURE', 'ANNULE')),
    CONSTRAINT ck_besoins_formation_ecart CHECK (ecart_initial >= seuil_ecart),
    CONSTRAINT ck_besoins_formation_niveaux CHECK (niveau_attendu >= 0 AND niveau_acquis_initial >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_besoin_formation_actif
    ON besoins_formation (employe_id, competence_id)
    WHERE statut IN ('OUVERT', 'PLANIFIE');

CREATE TABLE IF NOT EXISTS formations (
    id SERIAL PRIMARY KEY,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    titre VARCHAR(200) NOT NULL,
    description VARCHAR(1500),
    organisme VARCHAR(150),
    date_debut DATE,
    date_fin DATE,
    niveau_cible SMALLINT,
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE',
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_formations_statut CHECK (statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE')),
    CONSTRAINT ck_formations_dates CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_formations_niveau_cible CHECK (niveau_cible IS NULL OR niveau_cible >= 0)
);

CREATE TABLE IF NOT EXISTS participations_formation (
    id SERIAL PRIMARY KEY,
    formation_id INTEGER NOT NULL REFERENCES formations(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    besoin_id INTEGER NOT NULL REFERENCES besoins_formation(id) ON DELETE RESTRICT,
    statut VARCHAR(20) NOT NULL DEFAULT 'INSCRIT',
    inscrit_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    termine_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (formation_id, employe_id),
    CONSTRAINT ck_participations_statut CHECK (statut IN ('INSCRIT', 'TERMINE', 'ABANDONNE'))
);

CREATE TABLE IF NOT EXISTS mesures_formation (
    id SERIAL PRIMARY KEY,
    participation_id INTEGER NOT NULL UNIQUE REFERENCES participations_formation(id) ON DELETE RESTRICT,
    niveau_avant SMALLINT NOT NULL,
    niveau_apres SMALLINT NOT NULL,
    manque SMALLINT NOT NULL,
    acquis SMALLINT NOT NULL,
    pourcentage NUMERIC(5, 2) NOT NULL,
    mesure_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    mesure_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    commentaire VARCHAR(1000),
    CONSTRAINT ck_mesures_niveaux CHECK (niveau_avant >= 0 AND niveau_apres >= 0),
    CONSTRAINT ck_mesures_manque CHECK (manque >= 0),
    CONSTRAINT ck_mesures_acquis CHECK (acquis >= 0 AND acquis <= manque),
    CONSTRAINT ck_mesures_pourcentage CHECK (pourcentage >= 0 AND pourcentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_competences_actif ON competences(actif, libelle);
CREATE INDEX IF NOT EXISTS idx_criteres_poste_poste ON criteres_poste(poste_id, competence_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_employe_employe_date ON evaluations_employe(employe_id, date_evaluation DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_evaluations_employe_statut ON evaluations_employe(statut, date_evaluation DESC);
CREATE INDEX IF NOT EXISTS idx_evaluation_lignes_evaluation ON evaluation_lignes(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_besoins_formation_statut ON besoins_formation(statut, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_besoins_formation_employe ON besoins_formation(employe_id, competence_id);
CREATE INDEX IF NOT EXISTS idx_formations_competence_statut ON formations(competence_id, statut, date_debut DESC);
CREATE INDEX IF NOT EXISTS idx_participations_formation_employe ON participations_formation(employe_id, statut);
CREATE INDEX IF NOT EXISTS idx_participations_formation_formation ON participations_formation(formation_id, statut);

COMMIT;
