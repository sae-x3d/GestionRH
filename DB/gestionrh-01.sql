-- ============================================================
-- GESTIONRH - SCRIPT UNIQUE DE CREATION ET D ALIMENTATION
-- ============================================================
-- Fichier canonique a importer pour obtenir une base complete.
-- Il remplace l import manuel successif des scripts :
--   1. script-19-10-26-sae-tables.sql
--   2. script-22-10-26-L-donnes.sql
--   3. script-22-10-2026-Jo.sql
--   4. script-22-10-26-L-views.sql
--
-- Ne pas appliquer script-23-10-26-migration-validation-rh.sql
-- sur une base creee avec ce fichier : le statut 5 est deja present.
--
-- Usage :
--   psql -h localhost -U postgres -d gestionrh -f DB/gestionrh.sql
--
-- ATTENTION : ce script supprime et recree le schema public.
-- ============================================================

DROP SCHEMA IF EXISTS public CASCADE;
CREATE SCHEMA public;

-- ============================================================
-- PARTIE 1/4 : STRUCTURE
-- ============================================================

CREATE TABLE role (
    id_role SERIAL PRIMARY KEY,
    nom_role VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE departement (
    id_dept SERIAL PRIMARY KEY,
    nom_dept VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE poste (
    id_poste SERIAL PRIMARY KEY,
    nom_poste VARCHAR(100) NOT NULL,
    id_role INT NOT NULL REFERENCES role(id_role),
    salaire_max INT,
    salaire_min INT,
    CONSTRAINT chk_salaire_poste
        CHECK (
            salaire_min IS NULL
            OR salaire_max IS NULL
            OR (
                salaire_min >= 0
                AND salaire_max >= salaire_min
            )
        )
);

CREATE TABLE motif (
    id_motif SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE demande_status (
    id_demande_status SERIAL PRIMARY KEY,
    libelle_status VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE provenance (
    id_provenance SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE emp (
    id_emp SERIAL PRIMARY KEY,
    nom_emp VARCHAR(255) NOT NULL,
    date_embauche TIMESTAMP,
    date_depart TIMESTAMP,
    id_poste INT REFERENCES poste(id_poste),
    id_dept INT REFERENCES departement(id_dept),
    salaire_emp INT,
    id_provenance INT REFERENCES provenance(id_provenance),
    CONSTRAINT chk_dates_emp
        CHECK (
            date_depart IS NULL
            OR date_embauche IS NULL
            OR date_depart >= date_embauche
        ),
    CONSTRAINT chk_salaire_emp
        CHECK (salaire_emp IS NULL OR salaire_emp >= 0)
);

CREATE TABLE utilisateur (
    id_user SERIAL PRIMARY KEY,
    id_emp INT REFERENCES emp(id_emp),
    id_role INT NOT NULL REFERENCES role(id_role),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE poste_par_dept (
    id SERIAL PRIMARY KEY,
    id_dept INT NOT NULL REFERENCES departement(id_dept),
    id_poste INT NOT NULL REFERENCES poste(id_poste),
    nb_poste INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_poste_dept UNIQUE (id_dept, id_poste),
    CONSTRAINT chk_nb_poste CHECK (nb_poste >= 0)
);

CREATE TABLE demande (
    id_demande SERIAL PRIMARY KEY,
    id_poste INT NOT NULL REFERENCES poste(id_poste),
    id_dept INT NOT NULL REFERENCES departement(id_dept),
    id_demandeur INT NOT NULL REFERENCES utilisateur(id_user),
    id_emp_remplace INT REFERENCES emp(id_emp),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_motif INT NOT NULL REFERENCES motif(id_motif),
    budget INT,
    date_souhaite TIMESTAMP,
    id_demande_status INT NOT NULL REFERENCES demande_status(id_demande_status),
    date_reponse TIMESTAMP,
    CONSTRAINT chk_budget_demande
        CHECK (budget IS NULL OR budget >= 0)
);

CREATE INDEX idx_demande_demandeur
    ON demande(id_demandeur);

CREATE INDEX idx_demande_status
    ON demande(id_demande_status);

CREATE INDEX idx_demande_departement
    ON demande(id_dept);

CREATE INDEX idx_emp_poste_dept
    ON emp(id_poste, id_dept);

-- ============================================================

-- PARTIE 2/4 : DONNEES DE REFERENCE ET DEMANDES

-- ============================================================


BEGIN;

-- ==========================================
-- 1. ROLES
-- ==========================================

INSERT INTO role (id_role, nom_role) VALUES
(1, 'Administrateur'),
(2, 'RH'),
(3, 'Manager'),
(4, 'Employe');


-- ==========================================
-- 2. DEPARTEMENTS
-- ==========================================

INSERT INTO departement (id_dept, nom_dept) VALUES
(1, 'Informatique'),
(2, 'Ressources Humaines'),
(3, 'Finance'),
(4, 'Commercial'),
(5, 'Marketing');


-- ==========================================
-- 3. POSTES
-- ==========================================

INSERT INTO poste
(id_poste, nom_poste, id_role, salaire_min, salaire_max)
VALUES
(1, 'Developpeur Backend', 4, 2000000, 4500000),
(2, 'Developpeur Frontend', 4, 1800000, 4000000),
(3, 'Administrateur Systeme', 4, 2500000, 5000000),
(4, 'Responsable Informatique', 3, 4000000, 7000000),
(5, 'Responsable RH', 2, 3500000, 6000000),
(6, 'Assistant RH', 2, 1500000, 3000000),
(7, 'Comptable', 4, 1800000, 3500000),
(8, 'Responsable Finance', 3, 4000000, 6500000),
(9, 'Commercial', 4, 1500000, 4000000),
(10, 'Responsable Marketing', 3, 3500000, 6000000);


-- ==========================================
-- 4. MOTIFS DE RECRUTEMENT
-- ==========================================

INSERT INTO motif (id_motif, libelle) VALUES
(1, 'Creation de poste'),
(2, 'Remplacement'),
(3, 'Augmentation des effectifs'),
(4, 'Depart a la retraite'),
(5, 'Demission');


-- ==========================================
-- 5. STATUTS DES DEMANDES
-- ==========================================

INSERT INTO demande_status
(id_demande_status, libelle_status)
VALUES

(1, 'EN_ATTENTE_VALIDATION_RH'),
(2, 'APPROUVE'),
(3, 'REFUSE'),
(4, 'EN_ATTENTE_VALIDATION_BUDGET'),
(5, 'EN_ATTENTE_VALIDATION_DIRECTION');



-- ==========================================
-- 6. PROVENANCES
-- ==========================================

INSERT INTO provenance (id_provenance, libelle) VALUES
(1, 'Recrutement externe'),
(2, 'Promotion interne'),
(3, 'Mutation interne'),
(4, 'Recommandation');


-- ==========================================
-- 7. EMPLOYES
-- ==========================================

INSERT INTO emp
(
    id_emp,
    nom_emp,
    date_embauche,
    date_depart,
    id_poste,
    id_dept,
    salaire_emp,
    id_provenance
)
VALUES

-- Informatique
(1, 'Jean Rakoto', '2021-01-10', NULL, 4, 1, 5500000, 1),
(2, 'Paul Rabe', '2022-03-15', NULL, 1, 1, 3200000, 1),
(3, 'Marie Randria', '2023-06-01', NULL, 2, 1, 2800000, 4),
(4, 'Luc Andry', '2020-09-10', NULL, 3, 1, 4000000, 2),

-- Ressources Humaines
(5, 'Sophie Rakotomalala', '2020-02-01', NULL, 5, 2, 4500000, 1),
(6, 'Nina Rasoa', '2023-01-12', NULL, 6, 2, 2200000, 1),

-- Finance
(7, 'Michel Rajoelina', '2019-05-20', NULL, 8, 3, 5500000, 2),
(8, 'Claire Ravel', '2022-11-01', NULL, 7, 3, 2700000, 1),
(9, 'Patrick Ranaivo', '2023-04-15', NULL, 7, 3, 2500000, 1),

-- Commercial
(10, 'Eric Rakoto', '2021-07-10', NULL, 9, 4, 3000000, 1),
(11, 'Julie Andrianina', '2022-08-01', NULL, 9, 4, 2600000, 4),
(12, 'Thomas Rabe', '2024-01-15', NULL, 9, 4, 2200000, 1),

-- Marketing
(13, 'Laura Randriam', '2020-06-10', NULL, 10, 5, 5000000, 1),

-- Employes partis
(14, 'David Rakoto', '2021-01-01', '2025-06-30', 1, 1, 3000000, 1),
(15, 'Anna Rasolo', '2022-02-01', '2025-08-15', 6, 2, 2000000, 1);


-- ==========================================
-- 8. UTILISATEURS
-- ==========================================

-- IMPORTANT :
-- Mot de passe commun a tous les comptes de demonstration : MotDePasse2026
-- Le hash ci-dessous est un bcrypt (10 tours) genere par Spring Security
-- (BCryptPasswordEncoder). Le remplacer par de vrais hash bcrypt ou Argon2
-- generes par l'application avant toute mise en production.

INSERT INTO utilisateur
(id_user, id_emp, id_role, username, password_hash)
VALUES

(1, NULL, 1, 'admin', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(2, 5, 2, 'rh.sophie', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(3, 1, 3, 'manager.it', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(4, 7, 3, 'manager.finance', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(5, 13, 3, 'manager.marketing', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(6, 2, 4, 'paul.rabe', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(7, 3, 4, 'marie.randria', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'),

(8, 6, 2, 'rh.nina', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO');


-- ==========================================
-- 9. POSTES PAR DEPARTEMENT
-- ==========================================

INSERT INTO poste_par_dept
(id, id_dept, id_poste, nb_poste)
VALUES

-- Informatique
(1, 1, 1, 5),
(2, 1, 2, 4),
(3, 1, 3, 2),
(4, 1, 4, 1),

-- RH
(5, 2, 5, 1),
(6, 2, 6, 3),

-- Finance
(7, 3, 7, 4),
(8, 3, 8, 1),

-- Commercial
(9, 4, 9, 8),

-- Marketing
(10, 5, 10, 2);


-- ==========================================
-- 10. DEMANDES DE RECRUTEMENT
-- ==========================================

INSERT INTO demande
(
    id_demande,
    id_poste,
    id_dept,
    id_demandeur,
    id_emp_remplace,
    date_creation,
    id_motif,
    budget,
    date_souhaite,
    id_demande_status,
    date_reponse
)
VALUES

-- ==========================================
-- DEMANDES EN ATTENTE DE VALIDATION RH
-- ==========================================

(
    1,
    1,
    1,
    3,
    NULL,
    '2026-09-01',
    3,
    3500000,
    '2026-11-01',
    1,
    NULL
),

(
    2,
    2,
    1,
    3,
    NULL,
    '2026-09-05',
    1,
    3000000,
    '2026-12-01',
    1,
    NULL
),

(
    3,
    7,
    3,
    4,
    NULL,
    '2026-09-10',
    3,
    2800000,
    '2026-11-15',
    1,
    NULL
),

-- ==========================================
-- DEMANDES APPROUVEES
-- ==========================================

(
    4,
    1,
    1,
    3,
    14,
    '2026-07-01',
    5,
    3200000,
    '2026-09-01',
    2,
    '2026-07-05'
),

(
    5,
    6,
    2,
    2,
    15,
    '2026-07-10',
    2,
    2200000,
    '2026-09-15',
    2,
    '2026-07-15'
),

(
    6,
    9,
    4,
    2,
    NULL,
    '2026-08-01',
    3,
    2500000,
    '2026-10-01',
    2,
    '2026-08-05'
),

-- ==========================================
-- DEMANDES REFUSEES
-- ==========================================

(
    7,
    3,
    1,
    3,
    NULL,
    '2026-06-01',
    1,
    5000000,
    '2026-08-01',
    3,
    '2026-06-10'
),

(
    8,
    10,
    5,
    5,
    NULL,
    '2026-06-15',
    3,
    6000000,
    '2026-09-01',
    3,
    '2026-06-20'
),

-- ==========================================
-- DEMANDES EN ATTENTE DE VALIDATION BUDGET
-- ==========================================

(
    9,
    1,
    1,
    3,
    NULL,
    '2026-08-10',
    3,
    3500000,
    '2026-10-15',
    4,
    '2026-08-12'
),

(
    10,
    7,
    3,
    4,
    NULL,
    '2026-08-15',
    1,
    3000000,
    '2026-11-01',
    4,
    '2026-08-18'
),

-- ==========================================
-- DEMANDES APPROUVEES (HISTORIQUE)
-- ==========================================

(
    11,
    2,
    1,
    3,
    NULL,
    '2026-03-01',
    3,
    2800000,
    '2026-05-01',
    2,
    '2026-03-05'
),

(
    12,
    9,
    4,
    2,
    NULL,
    '2026-02-01',
    1,
    2500000,
    '2026-04-01',
    2,
    '2026-02-05'
);


-- ==========================================
-- 11. REINITIALISATION DES SEQUENCES
-- ==========================================

-- Important car les IDs ont ete inseres manuellement.

SELECT setval(
    pg_get_serial_sequence('role', 'id_role'),
    (SELECT MAX(id_role) FROM role)
);

SELECT setval(
    pg_get_serial_sequence('departement', 'id_dept'),
    (SELECT MAX(id_dept) FROM departement)
);

SELECT setval(
    pg_get_serial_sequence('poste', 'id_poste'),
    (SELECT MAX(id_poste) FROM poste)
);

SELECT setval(
    pg_get_serial_sequence('motif', 'id_motif'),
    (SELECT MAX(id_motif) FROM motif)
);

SELECT setval(
    pg_get_serial_sequence('demande_status', 'id_demande_status'),
    (SELECT MAX(id_demande_status) FROM demande_status)
);

SELECT setval(
    pg_get_serial_sequence('provenance', 'id_provenance'),
    (SELECT MAX(id_provenance) FROM provenance)
);

SELECT setval(
    pg_get_serial_sequence('emp', 'id_emp'),
    (SELECT MAX(id_emp) FROM emp)
);

SELECT setval(
    pg_get_serial_sequence('utilisateur', 'id_user'),
    (SELECT MAX(id_user) FROM utilisateur)
);

SELECT setval(
    pg_get_serial_sequence('poste_par_dept', 'id'),
    (SELECT MAX(id) FROM poste_par_dept)
);

SELECT setval(
    pg_get_serial_sequence('demande', 'id_demande'),
    (SELECT MAX(id_demande) FROM demande)
);

COMMIT;




-- ============================================================

-- PARTIE 3/4 : OFFRES, SIRH ET NORMALISATION

-- ============================================================


BEGIN;

-- ==========================================
-- 1. TABLE DES OFFRES D'EMPLOI
-- ==========================================

CREATE TABLE IF NOT EXISTS offre_emploi (
    id_offre SERIAL PRIMARY KEY,
    id_demande INT NOT NULL UNIQUE REFERENCES demande(id_demande),
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    lieu VARCHAR(150),
    salaire INT,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_publication TIMESTAMP,
    date_cloture TIMESTAMP,
    statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON',
    id_auteur INT REFERENCES utilisateur(id_user),
    CONSTRAINT chk_offre_salaire
        CHECK (salaire IS NULL OR salaire >= 0),
    CONSTRAINT chk_offre_statut
        CHECK (statut IN ('BROUILLON', 'PUBLIEE', 'CLOTUREE'))
);

CREATE INDEX IF NOT EXISTS idx_offre_statut
    ON offre_emploi(statut);

CREATE INDEX IF NOT EXISTS idx_offre_demande
    ON offre_emploi(id_demande);

CREATE INDEX IF NOT EXISTS idx_offre_publication
    ON offre_emploi(date_publication DESC);


-- ==========================================
-- 2. ROLE UTILISATEUR SIMPLE
-- ==========================================

INSERT INTO role (nom_role)
VALUES ('Utilisateur')
ON CONFLICT (nom_role) DO NOTHING;


-- ==========================================
-- 3. COMPTE DE DEMONSTRATION UTILISATEUR SIMPLE
-- ==========================================

-- Mot de passe commun aux comptes de demonstration : MotDePasse2026
INSERT INTO utilisateur (id_emp, id_role, username, password_hash)
SELECT NULL, r.id_role, 'user.simple',
       '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'
FROM role r
WHERE r.nom_role = 'Utilisateur'
ON CONFLICT (username) DO NOTHING;


-- ==========================================
-- 4. OFFRES DE DEMONSTRATION
-- ==========================================

-- Une offre est creee a partir d'une demande validee par la direction
-- (statut "Approuvee", id_demande_status = 2).

INSERT INTO offre_emploi
(
    id_demande,
    titre,
    description,
    lieu,
    salaire,
    date_creation,
    date_publication,
    date_cloture,
    statut,
    id_auteur
)
SELECT
    4,
    'Developpeur Backend - Informatique',
    'Nous recherchons un developpeur backend pour renforcer l''equipe Informatique. '
        || 'Vous participerez a la conception et au developpement des applications metier.',
    'Informatique',
    3200000,
    TIMESTAMP '2026-08-01 09:00:00',
    TIMESTAMP '2026-08-02 10:00:00',
    TIMESTAMP '2026-12-31 23:59:59',
    'PUBLIEE',
    (SELECT id_user FROM utilisateur WHERE username = 'rh.sophie')
WHERE NOT EXISTS (
    SELECT 1 FROM offre_emploi WHERE id_demande = 4
);

INSERT INTO offre_emploi
(
    id_demande,
    titre,
    description,
    lieu,
    salaire,
    date_creation,
    date_publication,
    date_cloture,
    statut,
    id_auteur
)
SELECT
    5,
    'Assistant RH - Ressources Humaines',
    'Dans le cadre d''un remplacement, nous recherchons un assistant RH '
        || 'pour accompagner les equipes sur le recrutement et l''administration du personnel.',
    'Ressources Humaines',
    2200000,
    TIMESTAMP '2026-08-12 09:00:00',
    TIMESTAMP '2026-08-13 09:30:00',
    TIMESTAMP '2026-11-30 18:00:00',
    'PUBLIEE',
    (SELECT id_user FROM utilisateur WHERE username = 'rh.sophie')
WHERE NOT EXISTS (
    SELECT 1 FROM offre_emploi WHERE id_demande = 5
);

INSERT INTO offre_emploi
(
    id_demande,
    titre,
    description,
    lieu,
    salaire,
    date_creation,
    date_publication,
    date_cloture,
    statut,
    id_auteur
)
SELECT
    6,
    'Commercial - Commercial',
    'Nous recherchons un commercial pour developper le portefeuille clients. '
        || 'Cette offre est encore en cours de redaction.',
    'Commercial',
    2500000,
    TIMESTAMP '2026-08-20 14:00:00',
    NULL,
    TIMESTAMP '2026-12-15 18:00:00',
    'BROUILLON',
    (SELECT id_user FROM utilisateur WHERE username = 'rh.sophie')
WHERE NOT EXISTS (
    SELECT 1 FROM offre_emploi WHERE id_demande = 6
);


-- ==========================================
-- 5. SIRH : CANDIDATS, PARCOURS, NIVEAUX, INFO
-- ==========================================

-- Recree les tables SIRH pour recueillir les donnees des postulants.
-- Les tables sont supprimees puis recreees car aucune donnee reelle
-- n'existe encore (les anciennes versions avaient une structure incomplete).
DROP TABLE IF EXISTS reponse CASCADE;
DROP TABLE IF EXISTS entretien CASCADE;
DROP TABLE IF EXISTS question CASCADE;
DROP TABLE IF EXISTS info CASCADE;
DROP TABLE IF EXISTS candidature CASCADE;
DROP TABLE IF EXISTS candidature_status CASCADE;
DROP TABLE IF EXISTS candidats CASCADE;
DROP TABLE IF EXISTS parcours CASCADE;
DROP TABLE IF EXISTS niveau CASCADE;

-- Type de parcours du candidat.
CREATE TABLE parcours (
    id_parcours SERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    CONSTRAINT chk_parcours_type
        CHECK (type IN ('Etude', 'Formation', 'Stage', 'Emploi'))
);

-- Niveau du parcours : diplomes (Bacc+1 a Bacc+7) ou durees
-- (1 mois ... 12 mois, 1 an ... 50 ans).
-- La categorie n'autorise les diplomes que pour le parcours "Etude".
CREATE TABLE niveau (
    id_niveau SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL UNIQUE,
    categorie VARCHAR(20) NOT NULL DEFAULT 'DIPLOME',
    CONSTRAINT chk_niveau_categorie
        CHECK (categorie IN ('DIPLOME', 'DUREE'))
);

-- Statut d'une candidature. Statut initial : RECUE.
CREATE TABLE candidature_status (
    id_candidature_status SERIAL PRIMARY KEY,
    libelle VARCHAR(50) NOT NULL UNIQUE
);

-- Candidature d'un utilisateur connecte a une offre d'emploi.
CREATE TABLE candidature (
    id_candidature SERIAL PRIMARY KEY,
    id_user INT NOT NULL REFERENCES utilisateur(id_user),
    id_offre INT NOT NULL REFERENCES offre_emploi(id_offre),
    cv_num VARCHAR(255),
    date_candidature TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_decision TIMESTAMP,
    id_candidature_status INT NOT NULL REFERENCES candidature_status(id_candidature_status),
    CONSTRAINT uk_candidature_user_offre UNIQUE (id_user, id_offre)
);

CREATE INDEX idx_candidature_user
    ON candidature(id_user);

CREATE INDEX idx_candidature_offre
    ON candidature(id_offre);

-- Parcours saisi pour une candidature : domaine + commentaire.
CREATE TABLE info (
    id_info SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL REFERENCES candidature(id_candidature) ON DELETE CASCADE,
    id_parcours INT NOT NULL REFERENCES parcours(id_parcours),
    id_niveau INT NOT NULL REFERENCES niveau(id_niveau),
    domaine VARCHAR(255),
    commentaire TEXT
);

CREATE INDEX idx_info_candidature
    ON info(id_candidature);

CREATE INDEX idx_info_parcours
    ON info(id_parcours);

-- Questionnaire rattache a une offre d'emploi.
CREATE TABLE question (
    id_question SERIAL PRIMARY KEY,
    id_offre INT NOT NULL REFERENCES offre_emploi(id_offre) ON DELETE CASCADE,
    libelle VARCHAR(255) NOT NULL,
    ordre INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_question_offre
    ON question(id_offre);

-- Reponses du candidat au questionnaire de l'offre visee.
CREATE TABLE reponse (
    id_reponse SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL REFERENCES candidature(id_candidature) ON DELETE CASCADE,
    id_question INT NOT NULL REFERENCES question(id_question) ON DELETE CASCADE,
    valeur TEXT NOT NULL,
    CONSTRAINT uk_reponse_candidature_question
        UNIQUE (id_candidature, id_question)
);

CREATE INDEX idx_reponse_candidature
    ON reponse(id_candidature);

-- Entretiens et tests planifies pour une candidature.
CREATE TABLE entretien (
    id_entretien SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL REFERENCES candidature(id_candidature) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    date_heure TIMESTAMP NOT NULL,
    lieu VARCHAR(255),
    participants VARCHAR(255),
    resultat TEXT,
    evaluation VARCHAR(255),
    CONSTRAINT chk_entretien_type
        CHECK (type IN ('ENTRETIEN', 'TEST'))
);

CREATE INDEX idx_entretien_candidature
    ON entretien(id_candidature);


-- Niveaux diplomes : Bacc+1 a Bacc+7 (reserves au parcours "Etude")
INSERT INTO niveau (libelle, categorie)
SELECT 'Bacc+' || n, 'DIPLOME'
FROM generate_series(1, 7) AS n
ON CONFLICT (libelle) DO NOTHING;

-- Niveaux duree : 1 mois a 12 mois
INSERT INTO niveau (libelle, categorie)
SELECT n || ' mois', 'DUREE'
FROM generate_series(1, 12) AS n
ON CONFLICT (libelle) DO NOTHING;

-- Niveaux duree : 1 an a 50 ans
INSERT INTO niveau (libelle, categorie)
SELECT n || ' ans', 'DUREE'
FROM generate_series(1, 50) AS n
ON CONFLICT (libelle) DO NOTHING;


INSERT INTO parcours (type)
SELECT t.type
FROM (VALUES ('Etude'), ('Formation'), ('Stage'), ('Emploi')) AS t(type)
WHERE NOT EXISTS (
    SELECT 1 FROM parcours p WHERE p.type = t.type
);

INSERT INTO candidature_status (libelle)
SELECT t.libelle
FROM (VALUES ('RECUE'), ('EN_EXAMEN'), ('RETENUE'), ('REFUSEE'), ('ACCEPTEE')) AS t(libelle)
WHERE NOT EXISTS (
    SELECT 1 FROM candidature_status c WHERE c.libelle = t.libelle
);


-- ==========================================
-- 6. CANDIDATURES DE DEMONSTRATION
-- ==========================================

-- Une candidature de l'utilisateur "user.simple" a l'offre Developpeur Backend (id_offre = 1).
INSERT INTO candidature (id_candidature, id_user, id_offre, cv_num, date_candidature, id_candidature_status)
SELECT 1,
       (SELECT id_user FROM utilisateur WHERE username = 'user.simple'),
       1,
       'CV_user.simple_1.pdf',
       TIMESTAMP '2026-08-05 10:00:00',
       (SELECT id_candidature_status FROM candidature_status WHERE libelle = 'RECUE')
WHERE NOT EXISTS (SELECT 1 FROM candidature WHERE id_candidature = 1);

-- Parcours declares pour la candidature : (id_info, id_candidature, id_parcours, id_niveau, domaine, commentaire)
-- id_niveau 3 = Bacc+3 ; id_parcours 1 = Etude, 4 = Emploi.
INSERT INTO info (id_info, id_candidature, id_parcours, id_niveau, domaine, commentaire) VALUES
(1, 1, 1, 3, 'Informatique', 'Licence en informatique, mention bien.'),
(2, 1, 4, 23, 'Informatique', '4 ans d''experience en developpement backend.');

-- Questionnaire de demonstration pour l'offre Developpeur Backend (id_offre = 1).
INSERT INTO question (id_question, id_offre, libelle, ordre) VALUES
(1, 1, 'Combien d''annees d''experience avez-vous en developpement backend ?', 1),
(2, 1, 'Quels langages et frameworks maitrisez-vous ?', 2);

INSERT INTO reponse (id_candidature, id_question, valeur) VALUES
(1, 1, '4 ans'),
(1, 2, 'Java, Spring Boot, PostgreSQL');

-- Entretien de demonstration planifie pour la candidature 1.
INSERT INTO entretien (id_candidature, type, date_heure, lieu, participants) VALUES
(1, 'ENTRETIEN', TIMESTAMP '2026-08-20 10:00:00', 'Salle RH', 'Sophie Rakotomalala, Jean Rakoto');

SELECT setval(pg_get_serial_sequence('candidature', 'id_candidature'),
              (SELECT MAX(id_candidature) FROM candidature));
SELECT setval(pg_get_serial_sequence('info', 'id_info'),
              (SELECT MAX(id_info) FROM info));
SELECT setval(pg_get_serial_sequence('question', 'id_question'),
              (SELECT MAX(id_question) FROM question));


-- ==========================================
-- 7. NORMALISATION DES LIBELLES (SANS ACCENT)
-- ==========================================

-- Les libelles historiques ont ete saisis avec des accents (ou des
-- caracteres corrompus). On les aligne sur les scripts sans accent.

UPDATE role SET nom_role = 'Employe'
WHERE id_role = 4 AND nom_role <> 'Employe';

UPDATE poste SET nom_poste = 'Developpeur Backend'
WHERE id_poste = 1 AND nom_poste <> 'Developpeur Backend';

UPDATE poste SET nom_poste = 'Developpeur Frontend'
WHERE id_poste = 2 AND nom_poste <> 'Developpeur Frontend';

UPDATE poste SET nom_poste = 'Administrateur Systeme'
WHERE id_poste = 3 AND nom_poste <> 'Administrateur Systeme';

UPDATE motif SET libelle = 'Creation de poste'
WHERE id_motif = 1 AND libelle <> 'Creation de poste';

UPDATE motif SET libelle = 'Depart a la retraite'
WHERE id_motif = 4 AND libelle <> 'Depart a la retraite';

-- Les offres de demonstration initiales portaient des titres et
-- des descriptions accentues.
UPDATE offre_emploi SET titre = 'Developpeur Backend - Informatique'
WHERE id_demande = 4 AND titre <> 'Developpeur Backend - Informatique';

UPDATE offre_emploi SET
    description = 'Nous recherchons un developpeur backend pour renforcer l''equipe Informatique. '
        || 'Vous participerez a la conception et au developpement des applications metier.'
WHERE id_demande = 4;

UPDATE offre_emploi SET
    description = 'Dans le cadre d''un remplacement, nous recherchons un assistant RH '
        || 'pour accompagner les equipes sur le recrutement et l''administration du personnel.'
WHERE id_demande = 5;

UPDATE offre_emploi SET
    description = 'Nous recherchons un commercial pour developper le portefeuille clients. '
        || 'Cette offre est encore en cours de redaction.'
WHERE id_demande = 6;

UPDATE motif SET libelle = 'Demission'
WHERE id_motif = 5 AND libelle <> 'Demission';

-- Workflow de validation des demandes (aligne sur le CSV du projet).
UPDATE demande_status SET libelle_status = 'EN_ATTENTE_VALIDATION_RH'
WHERE id_demande_status = 1 AND libelle_status <> 'EN_ATTENTE_VALIDATION_RH';

UPDATE demande_status SET libelle_status = 'APPROUVE'
WHERE id_demande_status = 2 AND libelle_status <> 'APPROUVE';

UPDATE demande_status SET libelle_status = 'REFUSE'
WHERE id_demande_status = 3 AND libelle_status <> 'REFUSE';

UPDATE demande_status SET libelle_status = 'EN_ATTENTE_VALIDATION_BUDGET'
WHERE id_demande_status = 4 AND libelle_status <> 'EN_ATTENTE_VALIDATION_BUDGET';

UPDATE demande_status SET libelle_status = 'EN_ATTENTE_VALIDATION_DIRECTION'
WHERE id_demande_status = 5 AND libelle_status <> 'EN_ATTENTE_VALIDATION_DIRECTION';

COMMIT;

-- ============================================================

-- PARTIE 4/4 : VUES

-- ============================================================


CREATE VIEW v_Employe_Post_Depart AS
SELECT
    e.id_emp,
    e.nom_emp,
    p.nom_poste,
    d.nom_dept,
    e.salaire_emp,
    e.date_embauche
FROM emp e
JOIN poste p ON e.id_poste = p.id_poste
JOIN departement d ON e.id_dept = d.id_dept
WHERE e.date_depart IS NULL
ORDER BY e.id_emp;

CREATE VIEW v_Demandes_Recrutement AS
SELECT
    dem.id_demande,
    p.nom_poste,
    d.nom_dept,
    u.username AS demandeur,
    m.libelle AS motif,
    dem.budget,
    s.libelle_status AS statut,
    dem.date_creation
FROM demande dem
JOIN poste p
    ON dem.id_poste = p.id_poste
JOIN departement d
    ON dem.id_dept = d.id_dept
JOIN utilisateur u
    ON dem.id_demandeur = u.id_user
JOIN motif m
    ON dem.id_motif = m.id_motif
JOIN demande_status s
    ON dem.id_demande_status = s.id_demande_status
ORDER BY dem.date_creation DESC;

CREATE VIEW v_Nombre_Demande_Par_Statut AS
SELECT
    s.libelle_status,
    COUNT(d.id_demande) AS nombre_demandes
FROM demande_status s
LEFT JOIN demande d
    ON s.id_demande_status = d.id_demande_status
GROUP BY s.id_demande_status, s.libelle_status
ORDER BY s.id_demande_status;
-- ============================================================

-- FIN DU SCRIPT UNIQUE

-- ============================================================
