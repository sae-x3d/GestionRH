-- Bloc 7 : conges et absences
-- Referentiels (types, jours feries, regimes, droits), soldes FIFO, demandes
-- et consommations. Script idempotent : peut etre rejoue sans perte de donnees.

BEGIN;

CREATE TABLE IF NOT EXISTS type_conge (
    id_type_conge serial PRIMARY KEY,
    code varchar(40) NOT NULL UNIQUE,
    libelle varchar(100) NOT NULL,
    decompte_solde boolean NOT NULL DEFAULT true,
    justificatif_requis boolean NOT NULL DEFAULT false,
    actif boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS jour_ferie (
    id_jour_ferie serial PRIMARY KEY,
    date_jour_ferie date NOT NULL UNIQUE,
    libelle varchar(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS regime_travail (
    id_regime serial PRIMARY KEY,
    libelle varchar(100) NOT NULL,
    heures_hebdomadaires numeric(4,1),
    jours_ouvres varchar(40) DEFAULT 'LUN,MAR,MER,JEU,VEN',
    actif boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS droit_conge (
    id_droit_conge serial PRIMARY KEY,
    id_type_conge integer NOT NULL REFERENCES type_conge (id_type_conge),
    anciennete_min_mois integer NOT NULL DEFAULT 0,
    jours_par_an numeric(6,1) NOT NULL
);

CREATE TABLE IF NOT EXISTS solde_conge (
    id_solde_conge serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_type_conge integer NOT NULL REFERENCES type_conge (id_type_conge),
    date_acquisition date NOT NULL,
    date_expiration date,
    quantite_initiale numeric(6,1) NOT NULL,
    quantite_restante numeric(6,1) NOT NULL,
    date_creation timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS demande_conge (
    id_demande_conge serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_type_conge integer NOT NULL REFERENCES type_conge (id_type_conge),
    date_debut date NOT NULL,
    date_fin date NOT NULL,
    demi_jour_debut boolean NOT NULL DEFAULT false,
    demi_jour_fin boolean NOT NULL DEFAULT false,
    nombre_jours numeric(6,1) NOT NULL,
    motif varchar(500),
    statut varchar(30) NOT NULL,
    id_manager_validateur integer REFERENCES utilisateur (id_user),
    date_validation_manager timestamp,
    commentaire_manager varchar(500),
    id_rh_validateur integer REFERENCES utilisateur (id_user),
    date_validation_rh timestamp,
    commentaire_rh varchar(500),
    date_soumission timestamp,
    date_creation timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS consommation_conge (
    id_consommation_conge serial PRIMARY KEY,
    id_solde_conge integer NOT NULL REFERENCES solde_conge (id_solde_conge),
    id_demande_conge integer NOT NULL REFERENCES demande_conge (id_demande_conge) ON DELETE CASCADE,
    quantite numeric(6,1) NOT NULL,
    date_consommation timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_demande_conge_emp ON demande_conge (id_emp, statut);
CREATE INDEX IF NOT EXISTS idx_demande_conge_dates ON demande_conge (date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_solde_conge_emp_type ON solde_conge (id_emp, id_type_conge);
CREATE INDEX IF NOT EXISTS idx_consommation_conge_demande ON consommation_conge (id_demande_conge);

COMMIT;
