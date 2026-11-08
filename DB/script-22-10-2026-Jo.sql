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
