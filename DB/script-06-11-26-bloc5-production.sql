-- Bloc 5 : complement production RH
-- Statut de poste, budget/effectif, evaluations, diffusions, GED, sorties,
-- paie, audit, alertes contrats et statut de demande POURVUE.
-- Script idempotent : peut etre rejoue sans perte de donnees.

BEGIN;

-- --------------------------------------------------------------------------
-- Colonnes ajoutees aux tables existantes
-- --------------------------------------------------------------------------
ALTER TABLE poste ADD COLUMN IF NOT EXISTS statut varchar(20) DEFAULT 'VACANT';
ALTER TABLE poste ADD COLUMN IF NOT EXISTS effectif integer;

ALTER TABLE departement ADD COLUMN IF NOT EXISTS effectif integer;
ALTER TABLE departement ADD COLUMN IF NOT EXISTS enveloppe_budgetaire integer;
ALTER TABLE departement ADD COLUMN IF NOT EXISTS budget_consomme integer;

ALTER TABLE emp ADD COLUMN IF NOT EXISTS matricule varchar(30);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS email varchar(150);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS statut varchar(20) DEFAULT 'ACTIF';
ALTER TABLE emp ADD COLUMN IF NOT EXISTS id_candidature integer REFERENCES candidature (id_candidature);

ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS email varchar(150);
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS actif boolean NOT NULL DEFAULT true;

ALTER TABLE entretien ADD COLUMN IF NOT EXISTS disponibilites varchar(500);

ALTER TABLE contrat ADD COLUMN IF NOT EXISTS nature varchar(30);
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS numero_avenant integer;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS alerte_fin_envoyee boolean NOT NULL DEFAULT false;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS alerte_essai_envoyee boolean NOT NULL DEFAULT false;
ALTER TABLE contrat ADD COLUMN IF NOT EXISTS commentaire varchar(1000);
UPDATE contrat SET nature = 'INITIAL' WHERE nature IS NULL;

ALTER TABLE onboarding_tache ADD COLUMN IF NOT EXISTS responsable varchar(40);
ALTER TABLE onboarding_tache ADD COLUMN IF NOT EXISTS date_echeance date;

CREATE UNIQUE INDEX IF NOT EXISTS uk_emp_matricule ON emp (matricule);
CREATE INDEX IF NOT EXISTS idx_emp_statut ON emp (statut);
CREATE INDEX IF NOT EXISTS idx_contrat_alerte_fin
    ON contrat (date_fin) WHERE alerte_fin_envoyee = false;
CREATE INDEX IF NOT EXISTS idx_contrat_alerte_essai
    ON contrat (date_fin_essai) WHERE alerte_essai_envoyee = false;

-- --------------------------------------------------------------------------
-- Statut de demande POURVUE
-- --------------------------------------------------------------------------
INSERT INTO demande_status (libelle_status)
SELECT 'POURVUE'
WHERE NOT EXISTS (
    SELECT 1 FROM demande_status WHERE libelle_status = 'POURVUE'
);

-- --------------------------------------------------------------------------
-- Evaluations de candidat (issues des entretiens / tests)
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evaluation_candidat (
    id_evaluation serial PRIMARY KEY,
    id_entretien integer NOT NULL REFERENCES entretien (id_entretien) ON DELETE CASCADE,
    note integer NOT NULL,
    grille text,
    commentaire varchar(1000),
    id_evaluateur integer REFERENCES utilisateur (id_user),
    date_evaluation timestamp NOT NULL DEFAULT now(),
    CONSTRAINT ck_evaluation_note CHECK (note BETWEEN 0 AND 100)
);
CREATE INDEX IF NOT EXISTS idx_evaluation_entretien ON evaluation_candidat (id_entretien);
CREATE INDEX IF NOT EXISTS idx_evaluation_evaluateur ON evaluation_candidat (id_evaluateur);

-- --------------------------------------------------------------------------
-- Diffusion multi-canaux des offres
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS diffusion_offre (
    id_diffusion serial PRIMARY KEY,
    id_offre integer NOT NULL REFERENCES offre_emploi (id_offre) ON DELETE CASCADE,
    type varchar(30) NOT NULL,
    canal varchar(150),
    statut varchar(30) NOT NULL DEFAULT 'PUBLIEE',
    date_diffusion timestamp NOT NULL DEFAULT now(),
    reference varchar(300),
    commentaire varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_diffusion_offre ON diffusion_offre (id_offre);
CREATE INDEX IF NOT EXISTS idx_diffusion_statut ON diffusion_offre (statut);

-- --------------------------------------------------------------------------
-- Inscription des salaries a la paie
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inscription_paie (
    id_inscription_paie serial PRIMARY KEY,
    id_emp integer NOT NULL UNIQUE REFERENCES emp (id_emp) ON DELETE CASCADE,
    numero_paie varchar(50) UNIQUE,
    organisme varchar(150),
    statut varchar(30) NOT NULL DEFAULT 'INSCRIT',
    date_inscription timestamp NOT NULL DEFAULT now(),
    date_debut date,
    commentaire varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_inscription_paie_statut ON inscription_paie (statut);

-- --------------------------------------------------------------------------
-- GED : documents du salarie et alertes d'expiration
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_employe (
    id_document_employe serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp) ON DELETE CASCADE,
    type_document varchar(100) NOT NULL,
    nom_original varchar(255) NOT NULL,
    chemin varchar(500) NOT NULL,
    date_emission date,
    date_validite date,
    alerte_expiration_envoyee boolean NOT NULL DEFAULT false,
    commentaire varchar(500),
    date_upload timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_document_employe_emp ON document_employe (id_emp);
CREATE INDEX IF NOT EXISTS idx_document_employe_validite ON document_employe (date_validite);

-- --------------------------------------------------------------------------
-- Sorties de salaries
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sortie_employe (
    id_sortie serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp) ON DELETE CASCADE,
    type_sortie varchar(30) NOT NULL,
    date_sortie date NOT NULL,
    date_notification date,
    motif varchar(500),
    preavis_jours integer,
    solde_tout_compte integer,
    statut varchar(30) NOT NULL DEFAULT 'EN_COURS',
    commentaire varchar(1000),
    id_traite_par integer REFERENCES utilisateur (id_user),
    date_creation timestamp NOT NULL DEFAULT now(),
    date_validation timestamp
);
CREATE INDEX IF NOT EXISTS idx_sortie_emp ON sortie_employe (id_emp);
CREATE INDEX IF NOT EXISTS idx_sortie_statut ON sortie_employe (statut);

-- --------------------------------------------------------------------------
-- Journal d'audit applicatif
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS journal_audit (
    id_audit serial PRIMARY KEY,
    username varchar(100),
    methode varchar(10),
    chemin varchar(500),
    entite varchar(100),
    id_entite varchar(50),
    details varchar(1000),
    adresse_ip varchar(60),
    date_action timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_username ON journal_audit (username);
CREATE INDEX IF NOT EXISTS idx_audit_date ON journal_audit (date_action);
CREATE INDEX IF NOT EXISTS idx_audit_entite ON journal_audit (entite, id_entite);

-- --------------------------------------------------------------------------
-- Nouveaux roles : DIRECTION, IT, PAIE
-- --------------------------------------------------------------------------
INSERT INTO role (nom_role)
SELECT v.libelle FROM (VALUES ('DIRECTION'), ('IT'), ('PAIE')) AS v(libelle)
WHERE NOT EXISTS (
    SELECT 1 FROM role r WHERE r.nom_role = v.libelle
);

-- --------------------------------------------------------------------------
-- Bloc 5 : dossier administratif
-- --------------------------------------------------------------------------
ALTER TABLE emp ADD COLUMN IF NOT EXISTS date_naissance date;
ALTER TABLE emp ADD COLUMN IF NOT EXISTS sexe varchar(20);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS nationalite varchar(100);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS etat_civil varchar(50);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS piece_identite varchar(100);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS adresse varchar(255);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS telephone varchar(50);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS contact_urgence varchar(255);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS rib varchar(100);
ALTER TABLE emp ADD COLUMN IF NOT EXISTS minutes_recuperation integer DEFAULT 0;

CREATE TABLE IF NOT EXISTS historique_salaire (
    id_historique_salaire serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_auteur integer REFERENCES utilisateur (id_user),
    montant integer NOT NULL,
    date_effet date NOT NULL,
    motif varchar(255),
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_histo_salaire_emp ON historique_salaire (id_emp, date_effet DESC);

CREATE TABLE IF NOT EXISTS historique_carriere (
    id_historique_carriere serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_poste integer REFERENCES poste (id_poste),
    id_dept integer REFERENCES departement (id_dept),
    id_manager integer REFERENCES emp (id_emp),
    id_auteur integer REFERENCES utilisateur (id_user),
    date_effet date NOT NULL,
    motif varchar(30) NOT NULL,
    commentaire varchar(500),
    date_creation timestamp NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_histo_carriere_emp ON historique_carriere (id_emp, date_effet DESC);

CREATE TABLE IF NOT EXISTS changement_sensible (
    id_changement serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_demandeur integer REFERENCES utilisateur (id_user),
    id_validateur integer REFERENCES utilisateur (id_user),
    champ varchar(40) NOT NULL,
    ancienne_valeur varchar(500),
    nouvelle_valeur varchar(500),
    statut varchar(30) NOT NULL DEFAULT 'SOUMIS',
    motif varchar(500),
    date_soumission timestamp NOT NULL DEFAULT now(),
    date_validation timestamp
);
CREATE INDEX IF NOT EXISTS idx_changement_statut ON changement_sensible (statut);
CREATE INDEX IF NOT EXISTS idx_changement_emp ON changement_sensible (id_emp);

-- --------------------------------------------------------------------------
-- Bloc 5 : checklist de depart (sortie)
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS checklist_sortie (
    id_checklist serial PRIMARY KEY,
    id_sortie integer NOT NULL REFERENCES sortie_employe (id_sortie),
    libelle varchar(200) NOT NULL,
    responsable varchar(40),
    fait boolean NOT NULL DEFAULT false,
    date_fait timestamp,
    commentaire varchar(500)
);
CREATE INDEX IF NOT EXISTS idx_checklist_sortie ON checklist_sortie (id_sortie);

COMMIT;
