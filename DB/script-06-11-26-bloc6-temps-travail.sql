-- Bloc 6 : temps de travail et cloture de paie
-- Parametres, planning hebdomadaire, pointage, anomalies, heures supplementaires,
-- periodes de paie et variables. Script idempotent : peut etre rejoue sans perte.

BEGIN;

CREATE TABLE IF NOT EXISTS parametre_temps_travail (
    id_parametre serial PRIMARY KEY,
    heures_hebdomadaires numeric(5,1) NOT NULL DEFAULT 35.0,
    heure_debut time NOT NULL DEFAULT '08:00',
    heure_fin time NOT NULL DEFAULT '17:00',
    pause_minutes integer NOT NULL DEFAULT 60,
    tolerance_retard_minutes integer NOT NULL DEFAULT 10,
    seuil_hebdo_heures_sup numeric(5,1) NOT NULL DEFAULT 35.0,
    actif boolean NOT NULL DEFAULT true
);

CREATE TABLE IF NOT EXISTS planning (
    id_planning serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    date_debut date NOT NULL,
    date_fin date NOT NULL,
    statut varchar(20) NOT NULL,
    commentaire varchar(500),
    id_cree_par integer REFERENCES utilisateur (id_user),
    date_creation timestamp NOT NULL DEFAULT now(),
    date_publication timestamp,
    CONSTRAINT uk_planning_emp_semaine UNIQUE (id_emp, date_debut)
);

CREATE TABLE IF NOT EXISTS planning_jour (
    id_planning_jour serial PRIMARY KEY,
    id_planning integer NOT NULL REFERENCES planning (id_planning) ON DELETE CASCADE,
    date_jour date NOT NULL,
    type_jour varchar(20) NOT NULL,
    heure_debut time,
    heure_fin time,
    pause_minutes integer,
    CONSTRAINT uk_planning_jour_date UNIQUE (id_planning, date_jour)
);

CREATE TABLE IF NOT EXISTS pointage (
    id_pointage serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    date_pointage date NOT NULL,
    heure_arrivee time,
    heure_depart time,
    pause_minutes integer,
    statut varchar(30) NOT NULL,
    commentaire varchar(500),
    date_creation timestamp NOT NULL DEFAULT now(),
    date_modification timestamp,
    CONSTRAINT uk_pointage_emp_date UNIQUE (id_emp, date_pointage)
);

CREATE TABLE IF NOT EXISTS anomalie_pointage (
    id_anomalie serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    id_pointage integer REFERENCES pointage (id_pointage),
    date_anomalie date NOT NULL,
    type_anomalie varchar(40) NOT NULL,
    minutes integer,
    statut varchar(20) NOT NULL,
    commentaire varchar(500),
    id_traite_par integer REFERENCES utilisateur (id_user),
    date_traitement timestamp
);

CREATE TABLE IF NOT EXISTS periode_paie (
    id_periode_paie serial PRIMARY KEY,
    annee integer NOT NULL,
    mois integer NOT NULL,
    date_debut date NOT NULL,
    date_fin date NOT NULL,
    statut varchar(20) NOT NULL,
    date_ouverture timestamp NOT NULL DEFAULT now(),
    date_cloture timestamp,
    id_cloture_par integer REFERENCES utilisateur (id_user),
    CONSTRAINT uk_periode_paie_mois UNIQUE (annee, mois)
);

CREATE TABLE IF NOT EXISTS heure_sup (
    id_heure_sup serial PRIMARY KEY,
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    date_heure_sup date NOT NULL,
    minutes integer NOT NULL,
    taux_majoration integer NOT NULL,
    statut varchar(20) NOT NULL,
    motif varchar(500),
    id_periode_paie integer REFERENCES periode_paie (id_periode_paie),
    id_valide_par integer REFERENCES utilisateur (id_user),
    date_validation timestamp,
    date_creation timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS variable_paie (
    id_variable_paie serial PRIMARY KEY,
    id_periode_paie integer NOT NULL REFERENCES periode_paie (id_periode_paie),
    id_emp integer NOT NULL REFERENCES emp (id_emp),
    type_variable varchar(30) NOT NULL,
    libelle varchar(120) NOT NULL,
    quantite numeric(8,2),
    montant numeric(12,2),
    reference varchar(80),
    date_creation timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_planning_emp ON planning (id_emp);
CREATE INDEX IF NOT EXISTS idx_planning_semaine ON planning (date_debut);
CREATE INDEX IF NOT EXISTS idx_pointage_emp_date ON pointage (id_emp, date_pointage);
CREATE INDEX IF NOT EXISTS idx_anomalie_emp_date ON anomalie_pointage (id_emp, date_anomalie);
CREATE INDEX IF NOT EXISTS idx_anomalie_statut ON anomalie_pointage (statut);
CREATE INDEX IF NOT EXISTS idx_heure_sup_emp_date ON heure_sup (id_emp, date_heure_sup);
CREATE INDEX IF NOT EXISTS idx_heure_sup_statut ON heure_sup (statut);
CREATE INDEX IF NOT EXISTS idx_variable_paie_periode ON variable_paie (id_periode_paie);
CREATE INDEX IF NOT EXISTS idx_variable_paie_emp ON variable_paie (id_emp);

INSERT INTO parametre_temps_travail (heures_hebdomadaires, heure_debut, heure_fin,
                                     pause_minutes, tolerance_retard_minutes,
                                     seuil_hebdo_heures_sup, actif)
SELECT 35.0, '08:00', '17:00', 60, 10, 35.0, true
WHERE NOT EXISTS (SELECT 1 FROM parametre_temps_travail WHERE actif = true);

COMMIT;
