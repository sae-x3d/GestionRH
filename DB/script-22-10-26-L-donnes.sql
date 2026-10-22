BEGIN;

-- ==========================================
-- 1. ROLES
-- ==========================================

INSERT INTO role (id_role, nom_role) VALUES
(1, 'Administrateur'),
(2, 'RH'),
(3, 'Manager'),
(4, 'Employé');


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
(1, 'Développeur Backend', 4, 2000000, 4500000),
(2, 'Développeur Frontend', 4, 1800000, 4000000),
(3, 'Administrateur Système', 4, 2500000, 5000000),
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
(1, 'Création de poste'),
(2, 'Remplacement'),
(3, 'Augmentation des effectifs'),
(4, 'Départ à la retraite'),
(5, 'Démission');


-- ==========================================
-- 5. STATUTS DES DEMANDES
-- ==========================================

INSERT INTO demande_status
(id_demande_status, libelle_status)
VALUES
(1, 'En attente'),
(2, 'Approuvée'),
(3, 'Refusée'),
(4, 'En cours'),
(5, 'Terminée');


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

-- Employés partis
(14, 'David Rakoto', '2021-01-01', '2025-06-30', 1, 1, 3000000, 1),
(15, 'Anna Rasolo', '2022-02-01', '2025-08-15', 6, 2, 2000000, 1);


-- ==========================================
-- 8. UTILISATEURS
-- ==========================================

-- IMPORTANT :
-- Mot de passe commun à tous les comptes de démonstration : MotDePasse2026
-- Le hash ci-dessous est un bcrypt (10 tours) généré par Spring Security
-- (BCryptPasswordEncoder). Le remplacer par de vrais hash bcrypt ou Argon2
-- générés par l'application avant toute mise en production.

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
-- DEMANDES EN ATTENTE
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
-- DEMANDES EN COURS
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
-- DEMANDES TERMINEES
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
    5,
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
    5,
    '2026-02-05'
);


-- ==========================================
-- 11. REINITIALISATION DES SEQUENCES
-- ==========================================

-- Important car les IDs ont été insérés manuellement.

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



