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

-- retenue_salaire : la sanction retire du salaire (mise a pied).
-- necessite_periode : date de debut et date de fin obligatoires.
CREATE TABLE IF NOT EXISTS types_sanction (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    retenue_salaire BOOLEAN NOT NULL DEFAULT FALSE,
    necessite_periode BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

-- Valeurs modifiables par la RH ou l'administration (delai de reponse, etc.).
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
-- CREATE TABLE IF NOT EXISTS sanctions (
--     id SERIAL PRIMARY KEY,
--     procedure_id INTEGER NOT NULL UNIQUE REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,
--     type_id INTEGER NOT NULL REFERENCES types_sanction(id) ON DELETE RESTRICT,
--     motif TEXT NOT NULL,
--     date_debut DATE,
--     date_fin DATE,
--     nombre_jours INTEGER,
--     decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
--     decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT ck_sanctions_motif CHECK (LENGTH(BTRIM(motif)) > 0),
--     CONSTRAINT ck_sanctions_periode CHECK (
--         (date_debut IS NULL AND date_fin IS NULL AND nombre_jours IS NULL)
--         OR (date_debut IS NOT NULL AND date_fin IS NOT NULL AND date_fin >= date_debut
--             AND nombre_jours IS NOT NULL AND nombre_jours > 0)
--     )
-- );

-- Retenue de salaire calculee par tranche de periode (une mise a pied a cheval
-- sur deux mois donne deux lignes). variable_paie_id est renseigne quand la
-- retenue est integree a la paie (variables_paie).
-- CREATE TABLE IF NOT EXISTS retenues_sanction (
--     id SERIAL PRIMARY KEY,
--     sanction_id INTEGER NOT NULL REFERENCES sanctions(id) ON DELETE RESTRICT,
--     date_debut DATE NOT NULL,
--     date_fin DATE NOT NULL,
--     nombre_jours INTEGER NOT NULL CHECK (nombre_jours > 0),
--     taux_journalier NUMERIC(14, 2) NOT NULL CHECK (taux_journalier >= 0),
--     montant NUMERIC(14, 2) NOT NULL CHECK (montant >= 0),
--     variable_paie_id INTEGER,
--     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT ck_retenues_sanction_dates CHECK (date_fin >= date_debut),
--     UNIQUE (sanction_id, date_debut)
-- );

-- Pieces justificatives rattachees a un incident, et eventuellement a la
-- reponse de l'employe. Non visibles dans documents_employe.
-- CREATE TABLE IF NOT EXISTS pieces_disciplinaires (
--     id SERIAL PRIMARY KEY,
--     incident_id INTEGER NOT NULL REFERENCES incidents(id) ON DELETE RESTRICT,
--     reponse_id INTEGER REFERENCES reponses_explication(id) ON DELETE RESTRICT,
--     cle_stockage VARCHAR(500) NOT NULL,
--     nom_fichier VARCHAR(255) NOT NULL,
--     type_mime VARCHAR(100) NOT NULL,
--     taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
--     ajoutee_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
--     ajoutee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );

-- Journal de toute consultation ou modification de donnees disciplinaires.
-- CREATE TABLE IF NOT EXISTS journal_acces_discipline (
--     id SERIAL PRIMARY KEY,
--     utilisateur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
--     action VARCHAR(20) NOT NULL,
--     objet_type VARCHAR(40) NOT NULL,
--     objet_id INTEGER,
--     employe_id INTEGER REFERENCES employes(id) ON DELETE RESTRICT,
--     detail VARCHAR(300),
--     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT ck_journal_discipline_action CHECK (
--         action IN ('LECTURE', 'CREATION', 'MODIFICATION', 'DECISION', 'TELECHARGEMENT')
--     )
-- );

-- ============================================================================
-- BLOC 11 - DISCIPLINE
-- Tables : sanctions, retenues_sanction,
--          pieces_disciplinaires, journal_acces_discipline
-- ============================================================================


-- ============================================================================
-- 1. SANCTIONS
-- Une procedure disciplinaire peut avoir une seule sanction.
-- Une sanction peut necessiter ou non une periode selon son type.
-- ============================================================================

CREATE TABLE IF NOT EXISTS sanctions (
    id SERIAL PRIMARY KEY,

    procedure_id INTEGER NOT NULL UNIQUE
        REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,

    type_id INTEGER NOT NULL
        REFERENCES types_sanction(id) ON DELETE RESTRICT,

    motif TEXT NOT NULL,

    date_debut DATE,
    date_fin DATE,
    nombre_jours INTEGER,

    decide_par_id INTEGER NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,

    decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_sanctions_motif
        CHECK (LENGTH(BTRIM(motif)) > 0),

    CONSTRAINT ck_sanctions_periode CHECK (
        (
            date_debut IS NULL
            AND date_fin IS NULL
            AND nombre_jours IS NULL
        )
        OR
        (
            date_debut IS NOT NULL
            AND date_fin IS NOT NULL
            AND date_fin >= date_debut
            AND nombre_jours IS NOT NULL
            AND nombre_jours > 0
        )
    )
);


-- ============================================================================
-- 2. RETENUES_SANCTION
-- Une sanction peut avoir plusieurs retenues, notamment lorsqu'une mise
-- a pied traverse plusieurs periodes de paie.
-- Une variable de paie ne peut etre rattachee qu'a une seule retenue.
-- ============================================================================

CREATE TABLE IF NOT EXISTS retenues_sanction (
    id SERIAL PRIMARY KEY,

    sanction_id INTEGER NOT NULL
        REFERENCES sanctions(id) ON DELETE RESTRICT,

    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,

    nombre_jours INTEGER NOT NULL
        CHECK (nombre_jours > 0),

    taux_journalier NUMERIC(14, 2) NOT NULL
        CHECK (taux_journalier >= 0),

    montant NUMERIC(14, 2) NOT NULL
        CHECK (montant >= 0),

    variable_paie_id INTEGER UNIQUE
        REFERENCES variables_paie(id) ON DELETE RESTRICT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_retenues_sanction_dates
        CHECK (date_fin >= date_debut),

    CONSTRAINT ck_retenues_sanction_montant
        CHECK (
            montant = ROUND(taux_journalier * nombre_jours, 2)
        ),

    CONSTRAINT uq_retenues_sanction_periode
        UNIQUE (sanction_id, date_debut)
);


-- ============================================================================
-- 3. PIECES_DISCIPLINAIRES
-- Une piece appartient a un incident et peut eventuellement etre rattachee
-- a une reponse de l'employe.
-- ============================================================================

CREATE TABLE IF NOT EXISTS pieces_disciplinaires (
    id SERIAL PRIMARY KEY,

    incident_id INTEGER NOT NULL
        REFERENCES incidents(id) ON DELETE RESTRICT,

    reponse_id INTEGER
        REFERENCES reponses_explication(id) ON DELETE RESTRICT,

    cle_stockage VARCHAR(500) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,

    taille_octets BIGINT NOT NULL
        CHECK (taille_octets > 0),

    ajoutee_par_id INTEGER NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,

    ajoutee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_pieces_disciplinaires_fichiers CHECK (
        LENGTH(BTRIM(cle_stockage)) > 0
        AND LENGTH(BTRIM(nom_fichier)) > 0
        AND LENGTH(BTRIM(type_mime)) > 0
    )
);


-- ============================================================================
-- 4. JOURNAL_ACCES_DISCIPLINE
-- Historique des consultations et modifications des donnees disciplinaires.
-- ============================================================================

CREATE TABLE IF NOT EXISTS journal_acces_discipline (
    id SERIAL PRIMARY KEY,

    utilisateur_id INTEGER NOT NULL
        REFERENCES users(id) ON DELETE RESTRICT,

    action VARCHAR(20) NOT NULL,

    objet_type VARCHAR(40) NOT NULL,
    objet_id INTEGER,

    employe_id INTEGER
        REFERENCES employes(id) ON DELETE RESTRICT,

    detail VARCHAR(300),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_journal_discipline_action CHECK (
        action IN (
            'LECTURE',
            'CREATION',
            'MODIFICATION',
            'DECISION',
            'TELECHARGEMENT'
        )
    ),

    CONSTRAINT ck_journal_discipline_informations CHECK (
        LENGTH(BTRIM(objet_type)) > 0
        AND (detail IS NULL OR LENGTH(BTRIM(detail)) > 0)
    )
);


-- ============================================================================
-- 5. INDEX
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_sanctions_periode
ON sanctions(date_debut, date_fin)
WHERE date_debut IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_retenues_sanction_sanction
ON retenues_sanction(sanction_id);

CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_incident
ON pieces_disciplinaires(incident_id);

CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_reponse
ON pieces_disciplinaires(reponse_id)
WHERE reponse_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_journal_discipline_employe
ON journal_acces_discipline(employe_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_journal_discipline_utilisateur
ON journal_acces_discipline(utilisateur_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_journal_discipline_objet
ON journal_acces_discipline(objet_type, objet_id, created_at DESC);

-- ============================================================================
-- FIN BLOC 11 - DISCIPLINE
-- ============================================================================



CREATE INDEX IF NOT EXISTS idx_incidents_employe ON incidents(employe_id, date_faits DESC);
CREATE INDEX IF NOT EXISTS idx_incidents_declarant ON incidents(declarant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_procedures_statut ON procedures_disciplinaires(statut_id, ouverte_at DESC);
CREATE INDEX IF NOT EXISTS idx_demandes_explication_limite ON demandes_explication(date_limite);
-- CREATE INDEX IF NOT EXISTS idx_sanctions_periode ON sanctions(date_debut, date_fin) WHERE date_debut IS NOT NULL;
-- CREATE INDEX IF NOT EXISTS idx_retenues_sanction_sanction ON retenues_sanction(sanction_id);
-- CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_incident ON pieces_disciplinaires(incident_id);
-- CREATE INDEX IF NOT EXISTS idx_journal_discipline_employe ON journal_acces_discipline(employe_id, created_at DESC);
-- CREATE INDEX IF NOT EXISTS idx_journal_discipline_utilisateur ON journal_acces_discipline(utilisateur_id, created_at DESC);

-- ----------------------------------------------------------------------------
-- 3. Lien avec la paie (variables et lignes de bulletin, comme absence_id)
-- ----------------------------------------------------------------------------

ALTER TABLE variables_paie
    ADD COLUMN IF NOT EXISTS sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT;
ALTER TABLE lignes_bulletin_paie
    ADD COLUMN IF NOT EXISTS sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT;

-- DO $$
-- BEGIN
--     IF NOT EXISTS (
--         SELECT 1 FROM pg_constraint WHERE conname = 'fk_retenues_sanction_variable_paie'
--     ) THEN
--         ALTER TABLE retenues_sanction
--             ADD CONSTRAINT fk_retenues_sanction_variable_paie
--             FOREIGN KEY (variable_paie_id) REFERENCES variables_paie(id) ON DELETE RESTRICT;
--     END IF;
-- END $$;

CREATE INDEX IF NOT EXISTS idx_variables_paie_sanction ON variables_paie(sanction_id) WHERE sanction_id IS NOT NULL;

-- ----------------------------------------------------------------------------
-- 4. Donnees de reference et parametres
-- ----------------------------------------------------------------------------

INSERT INTO statuts_procedure_disciplinaire (code, libelle) VALUES
    ('OUVERTE', 'Procedure ouverte'),
    ('EXPLICATION_DEMANDEE', 'Explication demandee'),
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

-- ----------------------------------------------------------------------------
-- Annulation (a lancer a la main si besoin, dans cet ordre) :
--   ALTER TABLE retenues_sanction DROP CONSTRAINT IF EXISTS fk_retenues_sanction_variable_paie;
--   ALTER TABLE variables_paie DROP COLUMN IF EXISTS sanction_id;
--   ALTER TABLE lignes_bulletin_paie DROP COLUMN IF EXISTS sanction_id;
--   DROP TABLE IF EXISTS journal_acces_discipline, pieces_disciplinaires, retenues_sanction,
--       sanctions, reponses_explication, demandes_explication, procedures_disciplinaires,
--       incidents, parametres_discipline, types_sanction, gravites_incident,
--       statuts_procedure_disciplinaire;
--   DELETE FROM rubriques_paie WHERE code = 'RETENUE_MISE_A_PIED';
--   DELETE FROM types_jour_planning WHERE code = 'MISE_A_PIED';
--   DELETE FROM types_notifications WHERE code LIKE 'DISCIPLINE_%';
-- ----------------------------------------------------------------------------