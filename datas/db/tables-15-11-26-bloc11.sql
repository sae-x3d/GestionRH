BEGIN;

-- ----------------------------------------------------------------------------
-- 0. Extension pour la contrainte de non-chevauchement des retenues
-- ----------------------------------------------------------------------------

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS btree_gist;
EXCEPTION WHEN insufficient_privilege THEN
    RAISE NOTICE 'btree_gist non installee (droits insuffisants) : le chevauchement des retenues reste controle par trigger uniquement.';
END $$;

-- ----------------------------------------------------------------------------
-- 1. Tables de reference
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS statuts_procedure_disciplinaire (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS gravites_incident (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

-- retenue_salaire    : la sanction retire du salaire (mise a pied).
-- necessite_periode  : date de debut et date de fin obligatoires.
CREATE TABLE IF NOT EXISTS types_sanction (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    retenue_salaire BOOLEAN NOT NULL DEFAULT FALSE,
    necessite_periode BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- Valeurs modifiables par la RH ou l'administration.
CREATE TABLE IF NOT EXISTS parametres_discipline (
    id SERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    libelle VARCHAR(180) NOT NULL,
    valeur VARCHAR(60) NOT NULL,
    unite VARCHAR(30) NOT NULL,
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------------------
-- 2. Tables metier
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS incidents (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    declarant_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    gravite_id INTEGER NOT NULL REFERENCES gravites_incident(id) ON DELETE RESTRICT,
    titre VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    date_faits DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_incidents_titre CHECK (LENGTH(BTRIM(titre)) > 0),
    CONSTRAINT ck_incidents_description CHECK (LENGTH(BTRIM(description)) > 0)
);

-- Une procedure par incident.
CREATE TABLE IF NOT EXISTS procedures_disciplinaires (
    id SERIAL PRIMARY KEY,
    incident_id INTEGER NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_procedure_disciplinaire(id) ON DELETE RESTRICT,
    ouverte_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ouverte_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cloturee_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Une demande d'explication par procedure.
CREATE TABLE IF NOT EXISTS demandes_explication (
    id SERIAL PRIMARY KEY,
    procedure_id INTEGER NOT NULL UNIQUE REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,
    message TEXT NOT NULL,
    date_limite DATE NOT NULL,
    demandee_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    demandee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_demande_explication_message CHECK (LENGTH(BTRIM(message)) > 0)
);

-- Une reponse par demande.
CREATE TABLE IF NOT EXISTS reponses_explication (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL UNIQUE REFERENCES demandes_explication(id) ON DELETE RESTRICT,
    texte TEXT NOT NULL,
    repondue_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    repondue_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reponse_explication_texte CHECK (LENGTH(BTRIM(texte)) > 0)
);

-- Une sanction (ou un avertissement) par procedure. Une procedure classee sans
-- suite n'a pas de ligne ici (statut SANS_SUITE sur la procedure).
-- nombre_jours = duree CALENDAIRE de la periode (date_fin - date_debut + 1).
-- Les jours effectivement retenus sur le salaire sont dans retenues_sanction.
-- La contrainte de periode et les regles liees au type : voir section 4.
CREATE TABLE IF NOT EXISTS sanctions (
    id SERIAL PRIMARY KEY,
    procedure_id INTEGER NOT NULL UNIQUE REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,
    type_id INTEGER NOT NULL REFERENCES types_sanction(id) ON DELETE RESTRICT,
    motif TEXT NOT NULL,
    date_debut DATE,
    date_fin DATE,
    nombre_jours INTEGER,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sanctions_motif CHECK (LENGTH(BTRIM(motif)) > 0)
);

-- Pieces justificatives rattachees a un incident, et eventuellement a la
-- reponse de l'employe (la reponse doit etre celle du meme incident : trigger).
-- Non visibles dans documents_employe.
CREATE TABLE IF NOT EXISTS pieces_disciplinaires (
    id SERIAL PRIMARY KEY,
    incident_id INTEGER NOT NULL REFERENCES incidents(id) ON DELETE RESTRICT,
    reponse_id INTEGER REFERENCES reponses_explication(id) ON DELETE RESTRICT,
    cle_stockage VARCHAR(500) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
    ajoutee_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ajoutee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pieces_disciplinaires_fichiers CHECK (
        LENGTH(BTRIM(cle_stockage)) > 0
        AND LENGTH(BTRIM(nom_fichier)) > 0
        AND LENGTH(BTRIM(type_mime)) > 0
    )
);

-- Journal de toute consultation ou modification de donnees disciplinaires.
-- Ajout seul : voir section 4 (triggers).
CREATE TABLE IF NOT EXISTS journal_acces_discipline (
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    origine VARCHAR(12) NOT NULL DEFAULT 'UTILISATEUR',
    action VARCHAR(20) NOT NULL,
    objet_type VARCHAR(40) NOT NULL,
    objet_id INTEGER,
    employe_id INTEGER REFERENCES employes(id) ON DELETE RESTRICT,
    detail VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_journal_discipline_action CHECK (
        action IN ('LECTURE', 'CREATION', 'MODIFICATION', 'DECISION', 'TELECHARGEMENT')
    ),
    CONSTRAINT ck_journal_discipline_informations CHECK (
        LENGTH(BTRIM(objet_type)) > 0
        AND (detail IS NULL OR LENGTH(BTRIM(detail)) > 0)
    )
);

-- ----------------------------------------------------------------------------
-- 3. Lien avec la paie
-- ----------------------------------------------------------------------------

ALTER TABLE variables_paie
    ADD COLUMN IF NOT EXISTS sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT;

ALTER TABLE lignes_bulletin_paie
    ADD COLUMN IF NOT EXISTS sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT;

-- Cle cible de la cle etrangere composite de retenues_sanction.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_variables_paie_id_sanction'
    ) THEN
        ALTER TABLE variables_paie
            ADD CONSTRAINT uq_variables_paie_id_sanction UNIQUE (id, sanction_id);
    END IF;
END $$;

-- Retenue de salaire calculee par tranche de periode (une mise a pied a cheval
-- sur deux mois donne deux lignes). variable_paie_id est renseigne quand la
-- retenue est integree a la paie. Une variable ne sert qu'a une seule retenue.
CREATE TABLE IF NOT EXISTS retenues_sanction (
    id SERIAL PRIMARY KEY,
    sanction_id INTEGER NOT NULL REFERENCES sanctions(id) ON DELETE RESTRICT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INTEGER NOT NULL CHECK (nombre_jours > 0),
    taux_journalier NUMERIC(14, 2) NOT NULL CHECK (taux_journalier >= 0),
    montant NUMERIC(14, 2) NOT NULL CHECK (montant >= 0),
    variable_paie_id INTEGER UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_retenues_sanction_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_retenues_sanction_jours CHECK (
        nombre_jours <= (date_fin - date_debut + 1)
    ),
    CONSTRAINT ck_retenues_sanction_montant CHECK (
        montant = ROUND(taux_journalier * nombre_jours, 2)
    )
);

-- La variable de paie rattachee doit porter la meme sanction que la retenue.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_retenues_sanction_variable_sanction'
    ) THEN
        ALTER TABLE retenues_sanction
            ADD CONSTRAINT fk_retenues_sanction_variable_sanction
            FOREIGN KEY (variable_paie_id, sanction_id)
            REFERENCES variables_paie(id, sanction_id) ON DELETE RESTRICT;
    END IF;
END $$;

-- Deux tranches d'une meme sanction ne peuvent pas couvrir les memes jours.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ex_retenues_sanction_chevauchement'
    ) THEN
        BEGIN
            ALTER TABLE retenues_sanction
                ADD CONSTRAINT ex_retenues_sanction_chevauchement
                EXCLUDE USING gist (
                    sanction_id WITH =,
                    daterange(date_debut, date_fin, '[]') WITH &&
                );
        EXCEPTION WHEN undefined_object OR feature_not_supported OR insufficient_privilege THEN
            RAISE NOTICE 'Contrainte d''exclusion non creee (btree_gist absente) : le trigger fn_valider_retenue_sanction controle le chevauchement.';
        END;
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 4. Contraintes et triggers de coherence
-- ----------------------------------------------------------------------------

-- 4.1 Parametres : valeurs utilisables par le service.
ALTER TABLE parametres_discipline DROP CONSTRAINT IF EXISTS ck_parametres_discipline_valeur;
ALTER TABLE parametres_discipline ADD CONSTRAINT ck_parametres_discipline_valeur CHECK (
    CASE code
        WHEN 'MODE_COMPTAGE_RETENUE' THEN valeur IN ('CALENDAIRE', 'OUVRE')
        WHEN 'DELAI_REPONSE_JOURS' THEN valeur ~ '^[1-9][0-9]{0,2}$'
        WHEN 'DIVISEUR_TAUX_JOURNALIER' THEN valeur ~ '^[1-9][0-9]{0,2}$'
        ELSE TRUE
    END
);

-- 4.2 Sanctions (remarque 1.1)
-- Periode : soit absente, soit complete. nombre_jours represente les jours effectifs
-- (jours ouvres en mode OUVRE, jours calendaires en mode CALENDAIRE) : il doit rester
-- strictement positif et ne jamais depasser la duree calendaire de la periode.
ALTER TABLE sanctions DROP CONSTRAINT IF EXISTS ck_sanctions_periode;
ALTER TABLE sanctions ADD CONSTRAINT ck_sanctions_periode CHECK (
    (date_debut IS NULL AND date_fin IS NULL AND nombre_jours IS NULL)
    OR (
        date_debut IS NOT NULL AND date_fin IS NOT NULL AND date_fin >= date_debut
        AND nombre_jours IS NOT NULL
        AND nombre_jours > 0
        AND nombre_jours <= (date_fin - date_debut + 1)
    )
);

-- Lien avec le type : mise a pied => periode obligatoire,
-- avertissement ou blame => aucune periode.
CREATE OR REPLACE FUNCTION fn_valider_sanction() RETURNS trigger AS $$
DECLARE
    v_necessite BOOLEAN;
    v_code VARCHAR(40);
BEGIN
    SELECT necessite_periode, code INTO v_necessite, v_code
    FROM types_sanction WHERE id = NEW.type_id;

    IF v_necessite AND (NEW.date_debut IS NULL OR NEW.date_fin IS NULL OR NEW.nombre_jours IS NULL) THEN
        RAISE EXCEPTION 'La sanction % exige une date de debut, une date de fin et un nombre de jours.', v_code
            USING ERRCODE = 'check_violation';
    END IF;
    IF NOT v_necessite AND (NEW.date_debut IS NOT NULL OR NEW.date_fin IS NOT NULL OR NEW.nombre_jours IS NOT NULL) THEN
        RAISE EXCEPTION 'La sanction % ne comporte pas de periode.', v_code
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sanctions_valider ON sanctions;
CREATE TRIGGER trg_sanctions_valider
    BEFORE INSERT OR UPDATE ON sanctions
    FOR EACH ROW EXECUTE FUNCTION fn_valider_sanction();

-- 4.3 Pieces justificatives (remarque 1.2)
-- La reponse liee doit appartenir a la procedure du meme incident.
CREATE OR REPLACE FUNCTION fn_valider_piece_disciplinaire() RETURNS trigger AS $$
DECLARE
    v_incident INTEGER;
BEGIN
    IF NEW.reponse_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT pr.incident_id INTO v_incident
    FROM reponses_explication r
    JOIN demandes_explication d ON d.id = r.demande_id
    JOIN procedures_disciplinaires pr ON pr.id = d.procedure_id
    WHERE r.id = NEW.reponse_id;

    IF v_incident IS DISTINCT FROM NEW.incident_id THEN
        RAISE EXCEPTION 'La reponse % n''appartient pas a l''incident %.', NEW.reponse_id, NEW.incident_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pieces_disciplinaires_valider ON pieces_disciplinaires;
CREATE TRIGGER trg_pieces_disciplinaires_valider
    BEFORE INSERT OR UPDATE OF incident_id, reponse_id ON pieces_disciplinaires
    FOR EACH ROW EXECUTE FUNCTION fn_valider_piece_disciplinaire();

-- 4.4 Retenues (remarque 1.3)
CREATE OR REPLACE FUNCTION fn_valider_retenue_sanction() RETURNS trigger AS $$
DECLARE
    s RECORD;
    v RECORD;
    v_mode VARCHAR(60);
    v_duree INTEGER;
BEGIN
    SELECT sa.date_debut, sa.date_fin, ts.retenue_salaire, ts.code, i.employe_id
    INTO s
    FROM sanctions sa
    JOIN types_sanction ts ON ts.id = sa.type_id
    JOIN procedures_disciplinaires pr ON pr.id = sa.procedure_id
    JOIN incidents i ON i.id = pr.incident_id
    WHERE sa.id = NEW.sanction_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sanction % introuvable.', NEW.sanction_id USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF NOT s.retenue_salaire OR s.date_debut IS NULL THEN
        RAISE EXCEPTION 'La sanction % n''entraine aucune retenue de salaire.', s.code
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.date_debut < s.date_debut OR NEW.date_fin > s.date_fin THEN
        RAISE EXCEPTION 'La tranche % - % depasse la periode de la sanction (% - %).',
            NEW.date_debut, NEW.date_fin, s.date_debut, s.date_fin
            USING ERRCODE = 'check_violation';
    END IF;

    v_duree := NEW.date_fin - NEW.date_debut + 1;
    SELECT valeur INTO v_mode FROM parametres_discipline WHERE code = 'MODE_COMPTAGE_RETENUE';
    IF COALESCE(v_mode, 'CALENDAIRE') = 'CALENDAIRE' AND NEW.nombre_jours <> v_duree THEN
        RAISE EXCEPTION 'En comptage calendaire, la tranche % - % compte % jour(s), pas %.',
            NEW.date_debut, NEW.date_fin, v_duree, NEW.nombre_jours
            USING ERRCODE = 'check_violation';
    END IF;

    IF EXISTS (
        SELECT 1 FROM retenues_sanction r
        WHERE r.sanction_id = NEW.sanction_id
          AND r.id IS DISTINCT FROM NEW.id
          AND r.date_debut <= NEW.date_fin
          AND r.date_fin >= NEW.date_debut
    ) THEN
        RAISE EXCEPTION 'La tranche % - % chevauche une autre retenue de la sanction %.',
            NEW.date_debut, NEW.date_fin, NEW.sanction_id
            USING ERRCODE = 'exclusion_violation';
    END IF;

    IF NEW.variable_paie_id IS NOT NULL THEN
        SELECT vp.employe_id, vp.montant, r.code AS rubrique
        INTO v
        FROM variables_paie vp
        JOIN rubriques_paie r ON r.id = vp.rubrique_id
        WHERE vp.id = NEW.variable_paie_id;

        IF v.rubrique IS DISTINCT FROM 'RETENUE_MISE_A_PIED' THEN
            RAISE EXCEPTION 'La variable de paie % n''est pas une retenue de mise a pied.', NEW.variable_paie_id
                USING ERRCODE = 'check_violation';
        END IF;
        IF v.employe_id IS DISTINCT FROM s.employe_id THEN
            RAISE EXCEPTION 'La variable de paie % concerne un autre employe que la sanction.', NEW.variable_paie_id
                USING ERRCODE = 'check_violation';
        END IF;
        IF v.montant IS DISTINCT FROM NEW.montant THEN
            RAISE EXCEPTION 'Le montant de la variable de paie (%) differe de celui de la retenue (%).', v.montant, NEW.montant
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_retenues_sanction_valider ON retenues_sanction;
CREATE TRIGGER trg_retenues_sanction_valider
    BEFORE INSERT OR UPDATE ON retenues_sanction
    FOR EACH ROW EXECUTE FUNCTION fn_valider_retenue_sanction();

-- 4.5 Journal (remarque 1.4) : ajout seul.
ALTER TABLE journal_acces_discipline DROP CONSTRAINT IF EXISTS ck_journal_discipline_origine;
ALTER TABLE journal_acces_discipline ADD CONSTRAINT ck_journal_discipline_origine CHECK (
    origine IN ('UTILISATEUR', 'SYSTEME')
    AND (origine = 'SYSTEME' OR utilisateur_id IS NOT NULL)
);

CREATE OR REPLACE FUNCTION fn_journal_discipline_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Le journal disciplinaire est en ajout seul : % interdit.', TG_OP
        USING ERRCODE = 'restrict_violation';
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_journal_discipline_immuable ON journal_acces_discipline;
CREATE TRIGGER trg_journal_discipline_immuable
    BEFORE UPDATE OR DELETE ON journal_acces_discipline
    FOR EACH ROW EXECUTE FUNCTION fn_journal_discipline_immuable();

DROP TRIGGER IF EXISTS trg_journal_discipline_truncate ON journal_acces_discipline;
CREATE TRIGGER trg_journal_discipline_truncate
    BEFORE TRUNCATE ON journal_acces_discipline
    FOR EACH STATEMENT EXECUTE FUNCTION fn_journal_discipline_immuable();

REVOKE UPDATE, DELETE, TRUNCATE ON journal_acces_discipline FROM PUBLIC;

-- ----------------------------------------------------------------------------
-- 5. Index
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_incidents_employe ON incidents(employe_id, date_faits DESC);
CREATE INDEX IF NOT EXISTS idx_incidents_declarant ON incidents(declarant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_procedures_statut ON procedures_disciplinaires(statut_id, ouverte_at DESC);
CREATE INDEX IF NOT EXISTS idx_demandes_explication_limite ON demandes_explication(date_limite);
CREATE INDEX IF NOT EXISTS idx_sanctions_periode ON sanctions(date_debut, date_fin) WHERE date_debut IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_incident ON pieces_disciplinaires(incident_id);
CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_reponse ON pieces_disciplinaires(reponse_id) WHERE reponse_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_journal_discipline_employe ON journal_acces_discipline(employe_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_journal_discipline_utilisateur ON journal_acces_discipline(utilisateur_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_journal_discipline_objet ON journal_acces_discipline(objet_type, objet_id);
CREATE INDEX IF NOT EXISTS idx_variables_paie_sanction ON variables_paie(sanction_id) WHERE sanction_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 6. Donnees de reference et parametres
-- ----------------------------------------------------------------------------

INSERT INTO statuts_procedure_disciplinaire (code, libelle) VALUES
    ('OUVERTE', 'Procedure ouverte'),
    ('EXPLICATION_DEMANDEE', 'Demande d''explication'),
    ('EXPLICATION_RECUE', 'Explication recue'),
    ('DECIDEE', 'Decision prise'),
    ('SANS_SUITE', 'Classee sans suite'),
    ('CLOTUREE', 'Cloturee')
ON CONFLICT (code) DO NOTHING;

INSERT INTO gravites_incident (code, libelle) VALUES
    ('LEGERE', 'Legere'),
    ('MOYENNE', 'Moyenne'),
    ('GRAVE', 'Grave')
ON CONFLICT (code) DO NOTHING;

INSERT INTO types_sanction (code, libelle, retenue_salaire, necessite_periode) VALUES
    ('AVERTISSEMENT', 'Avertissement', FALSE, FALSE),
    ('BLAME', 'Blame', FALSE, FALSE),
    ('MISE_A_PIED', 'Mise a pied', TRUE, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO parametres_discipline (code, libelle, valeur, unite) VALUES
    ('DELAI_REPONSE_JOURS', 'Delai accorde a l''employe pour repondre a la demande d''explication', '5', 'jour'),
    ('DIVISEUR_TAUX_JOURNALIER', 'Diviseur du salaire mensuel pour obtenir le taux journalier', '30', 'jour'),
    ('MODE_COMPTAGE_RETENUE', 'Jours comptes pour la retenue de mise a pied (CALENDAIRE ou OUVRE)', 'CALENDAIRE', 'mode')
ON CONFLICT (code) DO NOTHING;

-- Jour de planning pendant lequel l'employe ne travaille pas (mise a pied).
INSERT INTO types_jour_planning (code, libelle, compte_effectif) VALUES
    ('MISE_A_PIED', 'Mise a pied', FALSE)
ON CONFLICT (code) DO NOTHING;

-- Rubrique de paie : retenue de salaire due a une mise a pied.
INSERT INTO rubriques_paie (
    code, libelle, categorie_id, mode_calcul_id, date_debut,
    assujettie_irsa, assujettie_cnaps, assujettie_organisme_sante,
    saisie_manuelle_autorisee, source_regle
)
SELECT 'RETENUE_MISE_A_PIED', 'Retenue sur mise a pied', c.id, m.id, DATE '2026-01-01',
       TRUE, TRUE, TRUE, FALSE, 'Sanction disciplinaire validee (mise a pied)'
FROM categories_rubrique_paie c
JOIN modes_calcul_rubrique_paie m ON m.code = 'REGLE_BASEE'
WHERE c.code = 'RETENUE_SALARIALE'
ON CONFLICT (code) DO NOTHING;

INSERT INTO types_notifications (code, libelle) VALUES
    ('DISCIPLINE_INCIDENT_DECLARE', 'Incident disciplinaire declare'),
    ('DISCIPLINE_EXPLICATION_DEMANDEE', 'Demande d''explication'),
    ('DISCIPLINE_REPONSE_RECUE', 'Reponse a une demande d''explication'),
    ('DISCIPLINE_DELAI_EXPIRE', 'Delai de reponse expire'),
    ('DISCIPLINE_DECISION', 'Decision disciplinaire')
ON CONFLICT (code) DO NOTHING;

COMMIT;