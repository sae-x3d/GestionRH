-- ============================================================
-- GestionRH - jeu de donnees de demonstration global
-- ============================================================
-- A executer APRES DB/script-global.sql.
-- Cible : base de demonstration uniquement.
-- Date de reference du scenario : 13/11/2026.
--
--   createdb -U postgres gestionrh_demo_test
--   psql -U postgres -d gestionrh_demo_test -f DB/script-global.sql
--   psql -U postgres -d gestionrh_demo_test -f DB/script-demo-global.sql
--
-- Comptes : mot de passe commun MotDePasse2026
--   admin / rh.sophie / rh.nina / manager.it / manager.finance
--   manager.marketing / paul.rabe / marie.randria / finance.claire
--   user.simple / direction.demo / andry.nouveau
-- ============================================================

BEGIN;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- ============================================================
-- 1. Correctifs de schema necessaires au scenario
-- ============================================================

ALTER TABLE public.demande_conge
    DROP CONSTRAINT IF EXISTS demande_conge_statut_check;

ALTER TABLE public.demande_conge
    ADD CONSTRAINT demande_conge_statut_check
    CHECK (statut IN (
        'BROUILLON',
        'SOUMISE',
        'VALIDEE_MANAGER',
        'ATTENTE_DIRECTION',
        'VALIDEE_RH',
        'REFUSEE',
        'ANNULEE'
    ));

-- ============================================================
-- 2. Referentiels et parametres
-- ============================================================

UPDATE public.type_conge
SET duree_max_jours = 30,
    preavis_jours = 7,
    validation_direction = false,
    direction_a_partir_jours = 10
WHERE id_type_conge = 1;

UPDATE public.type_conge
SET duree_max_jours = 10,
    preavis_jours = 3,
    validation_direction = false,
    direction_a_partir_jours = NULL
WHERE id_type_conge = 2;

UPDATE public.type_conge
SET duree_max_jours = 15,
    preavis_jours = 15,
    validation_direction = true,
    direction_a_partir_jours = NULL
WHERE id_type_conge = 5;

INSERT INTO public.droit_conge (id_droit_conge, anciennete_min_mois, jours_par_an, id_type_conge)
VALUES (2, 0, 10, 2)
ON CONFLICT (id_droit_conge) DO UPDATE
SET anciennete_min_mois = EXCLUDED.anciennete_min_mois,
    jours_par_an = EXCLUDED.jours_par_an,
    id_type_conge = EXCLUDED.id_type_conge;

INSERT INTO public.jour_ferie (id_jour_ferie, date_jour_ferie, libelle)
VALUES
    (4, DATE '2026-06-26', 'Fete de l independance'),
    (5, DATE '2026-08-15', 'Assomption'),
    (6, DATE '2026-11-01', 'Toussaint'),
    (7, DATE '2027-01-01', 'Jour de l an')
ON CONFLICT (id_jour_ferie) DO UPDATE
SET date_jour_ferie = EXCLUDED.date_jour_ferie,
    libelle = EXCLUDED.libelle;

UPDATE public.parametre_temps_travail
SET heures_hebdomadaires = 35,
    heure_debut = TIME '08:00',
    heure_fin = TIME '17:00',
    pause_minutes = 60,
    seuil_hebdo_heures_sup = 35,
    tolerance_retard_minutes = 10,
    seuil_tranche_25_minutes = 480,
    majoration_dimanche = 50,
    majoration_ferie = 100,
    majoration_nuit = 25,
    actif = true
WHERE id_parametre = 1;

UPDATE public.departement
SET effectif = 8, enveloppe_budgetaire = 30000000, budget_consomme = 12000000
WHERE id_dept = 1;

UPDATE public.departement
SET effectif = 4, enveloppe_budgetaire = 15000000, budget_consomme = 6000000
WHERE id_dept = 2;

UPDATE public.departement
SET effectif = 5, enveloppe_budgetaire = 20000000, budget_consomme = 8000000
WHERE id_dept = 3;

UPDATE public.departement
SET effectif = 5, enveloppe_budgetaire = 18000000, budget_consomme = 7000000
WHERE id_dept = 4;

UPDATE public.departement
SET effectif = 3, enveloppe_budgetaire = 12000000, budget_consomme = 5000000
WHERE id_dept = 5;

UPDATE public.poste SET effectif = 2, statut = 'OCCUPE' WHERE id_poste = 1;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 2;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 3;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 4;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 5;
UPDATE public.poste SET effectif = 2, statut = 'OCCUPE' WHERE id_poste = 6;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 7;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 8;
UPDATE public.poste SET effectif = 3, statut = 'OCCUPE' WHERE id_poste = 9;
UPDATE public.poste SET effectif = 1, statut = 'OCCUPE' WHERE id_poste = 10;

-- ============================================================
-- 3. Remise a zero des donnees operationnelles
-- ============================================================

TRUNCATE TABLE
    public.absence,
    public.accreditation,
    public.affectation,
    public.anomalie_pointage,
    public.candidature,
    public.changement_sensible,
    public.checklist_sortie,
    public.consommation_conge,
    public.contrat,
    public.demande,
    public.demande_conge,
    public.diffusion_offre,
    public.document,
    public.document_employe,
    public.emp,
    public.entretien,
    public.evaluation_candidat,
    public.heure_sup,
    public.historique_carriere,
    public.historique_salaire,
    public.inscription_paie,
    public.jour_travaille,
    public.journal_audit,
    public.notification,
    public.offre_emploi,
    public.onboarding,
    public.onboarding_tache,
    public.periode_paie,
    public.planning,
    public.planning_jour,
    public.pointage,
    public.question,
    public.reponse,
    public.solde_conge,
    public.sortie_employe,
    public.utilisateur,
    public.validation_dossier,
    public.validation_etape,
    public.variable_paie
RESTART IDENTITY CASCADE;

-- ============================================================
-- 4. Utilisateurs, employes et affectations
-- ============================================================

INSERT INTO public.utilisateur
    (id_user, id_emp, id_role, username, password_hash, email, actif, security_version)
VALUES
    (1, NULL, 1, 'admin', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'admin@gestionrh.mg', true, 0),
    (2, NULL, 2, 'rh.sophie', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'sophie.rakotomalala@gestionrh.mg', true, 0),
    (3, NULL, 3, 'manager.it', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'jean.rakoto@gestionrh.mg', true, 0),
    (4, NULL, 3, 'manager.finance', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'michel.rajoelina@gestionrh.mg', true, 0),
    (5, NULL, 3, 'manager.marketing', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'laura.randriam@gestionrh.mg', true, 0),
    (6, NULL, 4, 'paul.rabe', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'paul.rabe@gestionrh.mg', true, 0),
    (7, NULL, 4, 'marie.randria', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'marie.randria@gestionrh.mg', true, 0),
    (8, NULL, 6, 'finance.claire', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'claire.ravel@gestionrh.mg', true, 0),
    (9, NULL, 5, 'user.simple', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'candidat.simple@example.mg', true, 0),
    (10, NULL, 2, 'rh.nina', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'nina.rasoa@gestionrh.mg', true, 0),
    (11, NULL, 7, 'direction.demo', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'direction@gestionrh.mg', true, 0),
    (12, NULL, 4, 'andry.nouveau', '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO', 'andry.nouveau@gestionrh.mg', true, 0);

INSERT INTO public.emp
    (id_emp, nom_emp, date_embauche, date_depart, id_poste, id_dept, salaire_emp,
     id_provenance, email, matricule, statut, id_candidature, adresse, contact_urgence,
     date_naissance, etat_civil, minutes_recuperation, nationalite, piece_identite, rib,
     sexe, telephone, id_regime)
VALUES
    (1, 'Jean Rakoto', TIMESTAMP '2022-01-10 00:00:00', NULL, 4, 1, 6500000, 1,
     'jean.rakoto@gestionrh.mg', 'EMP-001', 'ACTIF', NULL, 'Lot II M 45, Antananarivo',
     'Helene Rakoto - 034 11 111 11', DATE '1985-04-12', 'Marie', 0, 'Malagasy',
     'CNI-001', 'RIB-001', 'M', '034 11 111 01', 1),
    (2, 'Paul Rabe', TIMESTAMP '2023-03-01 00:00:00', NULL, 9, 4, 3200000, 1,
     'paul.rabe@gestionrh.mg', 'EMP-002', 'ACTIF', NULL, 'Lot III A 12, Antananarivo',
     'Sonia Rabe - 034 11 111 12', DATE '1992-08-21', 'Marie', 0, 'Malagasy',
     'CNI-002', 'RIB-002', 'M', '034 11 111 02', 1),
    (3, 'Marie Randria', TIMESTAMP '2021-06-15 00:00:00', NULL, 9, 4, 3400000, 1,
     'marie.randria@gestionrh.mg', 'EMP-003', 'ACTIF', NULL, 'Lot II B 34, Antananarivo',
     'Rado Randria - 034 11 111 13', DATE '1990-11-09', 'Mariee', 0, 'Malagasy',
     'CNI-003', 'RIB-003', 'F', '034 11 111 03', 1),
    (4, 'Luc Andrianina', TIMESTAMP '2024-02-01 00:00:00', NULL, 9, 4, 3000000, 1,
     'luc.andrianina@gestionrh.mg', 'EMP-004', 'ACTIF', NULL, 'Lot IV C 07, Antananarivo',
     'Lova Andrianina - 034 11 111 14', DATE '1996-03-17', 'Celibataire', 0, 'Malagasy',
     'CNI-004', 'RIB-004', 'M', '034 11 111 04', 1),
    (5, 'Sophie Rakotomalala', TIMESTAMP '2020-01-15 00:00:00', NULL, 5, 2, 5800000, 1,
     'sophie.rakotomalala@gestionrh.mg', 'EMP-005', 'ACTIF', NULL, 'Lot I D 21, Antananarivo',
     'Andry Rakotomalala - 034 11 111 15', DATE '1984-07-02', 'Mariee', 0, 'Malagasy',
     'CNI-005', 'RIB-005', 'F', '034 11 111 05', 1),
    (6, 'Nina Rasoa', TIMESTAMP '2022-09-01 00:00:00', NULL, 6, 2, 2600000, 1,
     'nina.rasoa@gestionrh.mg', 'EMP-006', 'ACTIF', NULL, 'Lot II E 09, Antananarivo',
     'Fanja Rasoa - 034 11 111 16', DATE '1997-01-25', 'Celibataire', 0, 'Malagasy',
     'CNI-006', 'RIB-006', 'F', '034 11 111 06', 1),
    (7, 'Michel Rajoelina', TIMESTAMP '2019-05-20 00:00:00', NULL, 8, 3, 6200000, 1,
     'michel.rajoelina@gestionrh.mg', 'EMP-007', 'ACTIF', NULL, 'Lot III F 55, Antananarivo',
     'Vola Rajoelina - 034 11 111 17', DATE '1980-12-03', 'Marie', 0, 'Malagasy',
     'CNI-007', 'RIB-007', 'M', '034 11 111 07', 1),
    (8, 'Claire Ravel', TIMESTAMP '2023-07-10 00:00:00', NULL, 7, 3, 3100000, 1,
     'claire.ravel@gestionrh.mg', 'EMP-008', 'ACTIF', NULL, 'Lot II G 18, Antananarivo',
     'Hery Ravel - 034 11 111 18', DATE '1994-05-14', 'Mariee', 0, 'Malagasy',
     'CNI-008', 'RIB-008', 'F', '034 11 111 08', 1),
    (9, 'Patrick Randrianasolo', TIMESTAMP '2024-04-01 00:00:00', NULL, 2, 1, 3300000, 1,
     'patrick.randrianasolo@gestionrh.mg', 'EMP-009', 'ACTIF', NULL, 'Lot I H 31, Antananarivo',
     'Miora Randrianasolo - 034 11 111 19', DATE '1993-09-28', 'Marie', 0, 'Malagasy',
     'CNI-009', 'RIB-009', 'M', '034 11 111 09', 1),
    (10, 'Eric Rakotomalala', TIMESTAMP '2023-11-15 00:00:00', NULL, 3, 1, 3800000, 1,
     'eric.rakotomalala@gestionrh.mg', 'EMP-010', 'ACTIF', NULL, 'Lot IV J 44, Antananarivo',
     'Noro Rakotomalala - 034 11 111 20', DATE '1991-06-06', 'Marie', 0, 'Malagasy',
     'CNI-010', 'RIB-010', 'M', '034 11 111 10', 1),
    (11, 'Julie Rasoanaivo', TIMESTAMP '2024-08-01 00:00:00', NULL, 6, 2, 2900000, 1,
     'julie.rasoanaivo@gestionrh.mg', 'EMP-011', 'ACTIF', NULL, 'Lot II K 26, Antananarivo',
     'Tiana Rasoanaivo - 034 11 111 21', DATE '1995-10-19', 'Mariee', 0, 'Malagasy',
     'CNI-011', 'RIB-011', 'F', '034 11 111 11', 1),
    (12, 'Thomas Rakoto', TIMESTAMP '2022-02-01 00:00:00', NULL, 1, 1, 3600000, 1,
     'thomas.rakoto@gestionrh.mg', 'EMP-012', 'ACTIF', NULL, 'Lot III L 63, Antananarivo',
     'Aina Rakoto - 034 11 111 22', DATE '1990-02-11', 'Marie', 0, 'Malagasy',
     'CNI-012', 'RIB-012', 'M', '034 11 111 12', 1),
    (13, 'Laura Randriam', TIMESTAMP '2021-09-01 00:00:00', NULL, 10, 5, 5500000, 2,
     'laura.randriam@gestionrh.mg', 'EMP-013', 'ACTIF', NULL, 'Lot I M 38, Antananarivo',
     'Manda Randriam - 034 11 111 23', DATE '1987-03-30', 'Mariee', 0, 'Malagasy',
     'CNI-013', 'RIB-013', 'F', '034 11 111 13', 1),
    (14, 'David Rakoto', TIMESTAMP '2020-03-01 00:00:00', TIMESTAMP '2026-06-30 00:00:00', 9, 4, 3000000, 1,
     'david.rakoto@gestionrh.mg', 'EMP-014', 'SORTI', NULL, 'Lot II N 74, Antananarivo',
     'Voahangy Rakoto - 034 11 111 24', DATE '1988-12-08', 'Marie', 0, 'Malagasy',
     'CNI-014', 'RIB-014', 'M', '034 11 111 14', 1),
    (15, 'Anna Ravel', TIMESTAMP '2021-01-10 00:00:00', TIMESTAMP '2026-08-31 00:00:00', 6, 2, 2500000, 1,
     'anna.ravel@gestionrh.mg', 'EMP-015', 'SORTI', NULL, 'Lot III O 82, Antananarivo',
     'Rivo Ravel - 034 11 111 25', DATE '1998-04-22', 'Celibataire', 0, 'Malagasy',
     'CNI-015', 'RIB-015', 'F', '034 11 111 15', 1),
    (16, 'Andry Nouveau', TIMESTAMP '2026-05-02 00:00:00', NULL, 1, 1, 3500000, 1,
     'andry.nouveau@gestionrh.mg', 'EMP-016', 'ACTIF', NULL, 'Lot IV P 91, Antananarivo',
     'Soa Nouveau - 034 11 111 26', DATE '1997-07-13', 'Celibataire', 0, 'Malagasy',
     'CNI-016', 'RIB-016', 'M', '034 11 111 16', 1);

UPDATE public.utilisateur SET id_emp = 5  WHERE id_user = 2;
UPDATE public.utilisateur SET id_emp = 1  WHERE id_user = 3;
UPDATE public.utilisateur SET id_emp = 7  WHERE id_user = 4;
UPDATE public.utilisateur SET id_emp = 13 WHERE id_user = 5;
UPDATE public.utilisateur SET id_emp = 2  WHERE id_user = 6;
UPDATE public.utilisateur SET id_emp = 3  WHERE id_user = 7;
UPDATE public.utilisateur SET id_emp = 8  WHERE id_user = 8;
UPDATE public.utilisateur SET id_emp = 6  WHERE id_user = 10;
UPDATE public.utilisateur SET id_emp = 16 WHERE id_user = 12;

INSERT INTO public.affectation
    (id_affectation, id_emp, id_poste, id_dept, id_manager, date_debut, date_fin, statut, date_creation)
VALUES
    (1, 1, 4, 1, 3, DATE '2022-01-10', NULL, 'ACTIVE', TIMESTAMP '2022-01-10 09:00:00'),
    (2, 2, 9, 4, NULL, DATE '2023-03-01', NULL, 'ACTIVE', TIMESTAMP '2023-03-01 09:00:00'),
    (3, 3, 9, 4, NULL, DATE '2021-06-15', NULL, 'ACTIVE', TIMESTAMP '2021-06-15 09:00:00'),
    (4, 4, 9, 4, NULL, DATE '2024-02-01', NULL, 'ACTIVE', TIMESTAMP '2024-02-01 09:00:00'),
    (5, 5, 5, 2, 2, DATE '2020-01-15', NULL, 'ACTIVE', TIMESTAMP '2020-01-15 09:00:00'),
    (6, 6, 6, 2, 2, DATE '2022-09-01', NULL, 'ACTIVE', TIMESTAMP '2022-09-01 09:00:00'),
    (7, 7, 8, 3, 4, DATE '2019-05-20', NULL, 'ACTIVE', TIMESTAMP '2019-05-20 09:00:00'),
    (8, 8, 7, 3, 4, DATE '2023-07-10', NULL, 'ACTIVE', TIMESTAMP '2023-07-10 09:00:00'),
    (9, 9, 2, 1, 3, DATE '2024-04-01', NULL, 'ACTIVE', TIMESTAMP '2024-04-01 09:00:00'),
    (10, 10, 3, 1, 3, DATE '2023-11-15', NULL, 'ACTIVE', TIMESTAMP '2023-11-15 09:00:00'),
    (11, 11, 6, 2, 2, DATE '2024-08-01', NULL, 'ACTIVE', TIMESTAMP '2024-08-01 09:00:00'),
    (12, 12, 1, 1, 3, DATE '2022-02-01', NULL, 'ACTIVE', TIMESTAMP '2022-02-01 09:00:00'),
    (13, 13, 10, 5, 5, DATE '2021-09-01', NULL, 'ACTIVE', TIMESTAMP '2021-09-01 09:00:00'),
    (14, 16, 1, 1, 3, DATE '2026-05-02', NULL, 'ACTIVE', TIMESTAMP '2026-05-02 09:00:00');

-- ============================================================
-- 5. Recrutement : demandes, offres, candidatures
-- ============================================================

INSERT INTO public.demande
    (id_demande, id_poste, id_dept, id_demandeur, id_emp_remplace, date_creation,
     id_motif, budget, date_souhaite, id_demande_status, date_reponse)
VALUES
    (1, 1, 1, 3, NULL, TIMESTAMP '2026-09-01 09:00:00', 3, 8000000,
     TIMESTAMP '2026-10-01 09:00:00', 2, TIMESTAMP '2026-09-03 11:00:00'),
    (2, 6, 2, 2, NULL, TIMESTAMP '2026-09-02 10:00:00', 1, 4500000,
     TIMESTAMP '2026-10-01 09:00:00', 2, TIMESTAMP '2026-09-04 15:00:00'),
    (3, 3, 1, 3, NULL, TIMESTAMP '2026-10-05 08:30:00', 2, 6000000,
     TIMESTAMP '2026-11-15 09:00:00', 1, NULL),
    (4, 7, 3, 4, NULL, TIMESTAMP '2026-10-08 14:00:00', 3, 5000000,
     TIMESTAMP '2026-12-01 09:00:00', 4, NULL),
    (5, 10, 5, 5, NULL, TIMESTAMP '2026-10-12 09:15:00', 1, 7000000,
     TIMESTAMP '2027-01-05 09:00:00', 5, NULL),
    (6, 2, 1, 3, NULL, TIMESTAMP '2026-08-20 10:30:00', 2, 5500000,
     TIMESTAMP '2026-09-30 09:00:00', 3, TIMESTAMP '2026-08-28 16:00:00'),
    (7, 6, 2, 2, NULL, TIMESTAMP '2026-09-15 11:00:00', 2, 4000000,
     TIMESTAMP '2026-11-01 09:00:00', 2, TIMESTAMP '2026-09-18 09:30:00'),
    (8, 4, 1, 3, NULL, TIMESTAMP '2026-04-02 08:00:00', 2, 9000000,
     TIMESTAMP '2026-05-01 09:00:00', 6, TIMESTAMP '2026-04-20 10:00:00');

INSERT INTO public.offre_emploi
    (id_offre, id_demande, titre, description, lieu, type_contrat, salaire,
     date_creation, date_publication, date_cloture, statut, id_auteur)
VALUES
    (1, 1, 'Developpeur Backend Java',
     'Developpement de services Java/Spring Boot et participation aux revues de code.',
     'Antananarivo', 'CDI', 3500000, TIMESTAMP '2026-09-05 09:00:00',
     TIMESTAMP '2026-09-10 09:00:00', NULL, 'PUBLIEE', 3),
    (2, 2, 'Assistant RH',
     'Gestion administrative du personnel, suivi des dossiers et appui au recrutement.',
     'Antananarivo', 'CDI', 2300000, TIMESTAMP '2026-09-06 10:00:00',
     TIMESTAMP '2026-09-15 09:00:00', NULL, 'PUBLIEE', 2);

INSERT INTO public.diffusion_offre
    (id_diffusion, canal, commentaire, date_diffusion, reference, statut, type, id_offre)
VALUES
    (1, 'Site carriere GestionRH', 'Publication principale', TIMESTAMP '2026-09-10 10:00:00',
     'OFFRE-001-SITE', 'PUBLIEE', 'SITE', 1),
    (2, 'LinkedIn', 'Campagne de diffusion IT', TIMESTAMP '2026-09-11 09:00:00',
     'OFFRE-001-LINKEDIN', 'PUBLIEE', 'RESEAUX_SOCIAUX', 1),
    (3, 'Cabinet Talents IT', 'Diffusion specialisee', TIMESTAMP '2026-09-20 09:00:00',
     'OFFRE-001-CABINET', 'PLANIFIEE', 'CABINET', 1),
    (4, 'Site carriere GestionRH', 'Publication principale', TIMESTAMP '2026-09-15 10:00:00',
     'OFFRE-002-SITE', 'PUBLIEE', 'SITE', 2),
    (5, 'Facebook', 'Relais communautaire', TIMESTAMP '2026-09-16 09:00:00',
     'OFFRE-002-FACEBOOK', 'PUBLIEE', 'RESEAUX_SOCIAUX', 2);

INSERT INTO public.question (id_question, id_offre, libelle, ordre)
VALUES
    (1, 1, 'Combien d annees d experience avez-vous en Java ?', 1),
    (2, 1, 'Quel est votre niveau en Spring Boot ?', 2),
    (3, 2, 'Avez-vous deja travaille dans un service RH ?', 1),
    (4, 2, 'Quelle est votre disponibilite ?', 2);

INSERT INTO public.candidature
    (id_candidature, id_user, id_offre, cv_num, date_candidature, date_decision,
     id_candidature_status, salaire_convenu)
VALUES
    (1, 9, 1, 'cv-candidat-simple.pdf', TIMESTAMP '2026-10-01 09:30:00',
     NULL, 1, NULL),
    (2, 12, 1, 'cv-andry-nouveau.pdf', TIMESTAMP '2026-09-20 14:00:00',
     TIMESTAMP '2026-04-25 10:00:00', 5, 3500000),
    (3, 9, 2, 'cv-candidat-simple-rh.pdf', TIMESTAMP '2026-10-05 10:15:00',
     NULL, 2, NULL);

INSERT INTO public.info
    (id_info, id_candidature, id_parcours, id_niveau, domaine, commentaire)
VALUES
    (1, 1, 1, 5, 'Informatique', 'Master en genie logiciel.'),
    (2, 1, 3, 13, 'Developpement web', 'Stage de 6 mois en developpement Java.'),
    (3, 2, 1, 3, 'Informatique', 'Licence en informatique.'),
    (4, 2, 4, 23, 'Developpement backend', '4 ans d experience en Java/Spring.'),
    (5, 3, 2, 13, 'Ressources humaines', 'Formation courte en gestion des ressources humaines.');

INSERT INTO public.reponse (id_reponse, id_candidature, id_question, valeur)
VALUES
    (1, 1, 1, '3 ans'),
    (2, 1, 2, 'Intermediaire'),
    (3, 2, 1, '4 ans'),
    (4, 2, 2, 'Avance'),
    (5, 3, 3, 'Oui, 2 ans en assistance RH'),
    (6, 3, 4, 'Immediate');

INSERT INTO public.entretien
    (id_entretien, id_candidature, type, date_heure, lieu, participants, resultat,
     evaluation, disponibilites)
VALUES
    (1, 1, 'TEST', TIMESTAMP '2026-10-10 09:00:00', 'Visioconference',
     'Jean Rakoto, Sophie Rakotomalala', 'Test technique reussi', 'Bon niveau Java', 'Des que possible'),
    (2, 1, 'ENTRETIEN', TIMESTAMP '2026-10-14 10:00:00', 'Siege GestionRH',
     'Jean Rakoto, Sophie Rakotomalala', 'A revoir sur Spring Boot', 'Motivation correcte', 'Des que possible'),
    (3, 2, 'TEST', TIMESTAMP '2026-09-25 09:00:00', 'Visioconference',
     'Jean Rakoto', 'Test technique reussi', 'Tres bon niveau', 'Sous preavis de 1 mois'),
    (4, 3, 'ENTRETIEN', TIMESTAMP '2026-10-12 14:00:00', 'Siege GestionRH',
     'Sophie Rakotomalala, Nina Rasoa', 'Entretien concluant', 'Profil a confirmer', 'Immediate');

INSERT INTO public.evaluation_candidat
    (id_evaluation, commentaire, date_evaluation, grille, note, id_entretien, id_evaluateur)
VALUES
    (1, 'Bonne maitrise des fondamentaux Java.', TIMESTAMP '2026-10-10 11:00:00',
     'Java:4; SQL:3; Communication:3', 15, 1, 3),
    (2, 'Motivation et capacite d apprentissage correctes.', TIMESTAMP '2026-10-14 11:30:00',
     'Java:3; Spring:2; Communication:4', 12, 2, 3),
    (3, 'Profil experimente et autonome.', TIMESTAMP '2026-09-25 11:00:00',
     'Java:5; Spring:4; Communication:4', 17, 3, 3);

UPDATE public.emp SET id_candidature = 2 WHERE id_emp = 16;

-- ============================================================
-- 6. Contrats et onboarding
-- ============================================================

INSERT INTO public.contrat
    (id_contrat, adresse_employeur, adresse_salarie, categorie_professionnelle,
     date_acceptation, date_effet, date_fin, date_generation, duree_travail,
     fonction, indice_classification, lieu_travail, motif_cdd, nom_employeur,
     nom_salarie, pdf, salaire, statut, type_contrat, id_candidature, id_emp,
     date_signature, lieu_signature, numero_contrat, duree_essai_mois,
     date_fin_essai, id_contrat_precedent, alerte_essai_envoyee,
     alerte_fin_envoyee, nature, numero_avenant, commentaire)
VALUES
    (1, 'Lot II M 45, Antananarivo', 'Lot IV P 91, Antananarivo', 'Employe qualifie',
     TIMESTAMP '2026-04-25 10:00:00', DATE '2026-05-02', DATE '2026-11-01',
     TIMESTAMP '2026-04-20 09:00:00', '35 heures par semaine', 'Developpeur Backend',
     'I-2', 'Antananarivo', 'Renforcement de l equipe informatique', 'GestionRH SARL',
     'Andry Nouveau', NULL, 3500000, 'RENOUVELE', 'CDD', 2, 16,
     DATE '2026-04-25', 'Antananarivo', 'CT-2026-001', 0, NULL, NULL, false,
     false, 'INITIAL', NULL, 'Contrat initial de 6 mois.'),
    (2, 'Lot II M 45, Antananarivo', 'Lot IV P 91, Antananarivo', 'Employe qualifie',
     TIMESTAMP '2026-10-20 10:00:00', DATE '2026-11-02', NULL,
     TIMESTAMP '2026-10-18 09:00:00', '35 heures par semaine', 'Developpeur Backend',
     'I-2', 'Antananarivo', NULL, 'GestionRH SARL', 'Andry Nouveau', NULL, 3500000,
     'ACCEPTE', 'CDI', 2, 16, DATE '2026-10-20', 'Antananarivo', 'CT-2026-002',
     3, NULL, 1, false, false, 'CONVERSION', NULL, 'Conversion du CDD en CDI.');

INSERT INTO public.onboarding
    (id_onboarding, id_contrat, id_emp, id_affectation, statut, date_debut,
     date_cloture, decision_essai, commentaire, date_creation)
VALUES
    (1, 1, 16, 14, 'EN_COURS', DATE '2026-05-02', NULL, NULL,
     'Parcours d integration en cours.', TIMESTAMP '2026-05-02 09:00:00');

INSERT INTO public.onboarding_tache
    (id_tache, id_onboarding, libelle, ordre, obligatoire, fait, date_realisation,
     commentaire, date_creation, date_echeance, responsable)
VALUES
    (1, 1, 'Signature du contrat de travail', 1, true, true, DATE '2026-05-02',
     'Contrat signe.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-02', 'RH'),
    (2, 1, 'Remise du livret d accueil', 2, true, true, DATE '2026-05-03',
     'Livret remis.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-03', 'RH'),
    (3, 1, 'Presentation a l equipe et au manager', 3, true, true, DATE '2026-05-02',
     'Presentation faite.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-02', 'MANAGER'),
    (4, 1, 'Installation du poste de travail', 4, true, false, NULL,
     'En attente de l equipe IT.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-04', 'IT'),
    (5, 1, 'Formation securite et reglement interieur', 5, true, true, DATE '2026-05-05',
     'Formation suivie.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-05', 'SERVICES_GENERAUX'),
    (6, 1, 'Point de suivi de fin de premiere semaine', 6, false, false, NULL,
     'A planifier avec le manager.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-09', 'MANAGER'),
    (7, 1, 'Transmission du RIB et de la piece d identite', 7, true, false, NULL,
     'Documents justificatifs a deposer.', TIMESTAMP '2026-05-02 09:00:00', DATE '2026-05-10', 'EMPLOYE');

INSERT INTO public.document
    (id_document, id_onboarding, type_document, nom_original, chemin, date_upload, valide, commentaire)
VALUES
    (1, 1, 'CONTRAT_SIGNE', 'contrat-andry-2026.pdf', 'contrat-andry-2026.pdf',
     TIMESTAMP '2026-05-02 10:00:00', true, 'Contrat valide par la RH.'),
    (2, 1, 'PIECE_IDENTITE', 'cni-andry.pdf', 'cni-andry.pdf',
     TIMESTAMP '2026-05-02 10:05:00', true, 'Piece d identite verifiee.'),
    (3, 1, 'DIPLOME', 'diplome-andry.pdf', 'diplome-andry.pdf',
     TIMESTAMP '2026-05-03 09:30:00', false, 'Diplome en attente de verification.');

-- ============================================================
-- 7. Conges, absences et dossier administratif
-- ============================================================

INSERT INTO public.solde_conge
    (id_solde_conge, date_acquisition, date_creation, date_expiration,
     quantite_initiale, quantite_restante, id_emp, id_type_conge)
VALUES
    (1, DATE '2025-06-01', TIMESTAMP '2025-06-01 09:00:00', DATE '2026-12-31', 10, 1, 2, 1),
    (2, DATE '2026-06-01', TIMESTAMP '2026-06-01 09:00:00', DATE '2027-12-31', 20, 20, 2, 1),
    (3, DATE '2025-06-01', TIMESTAMP '2025-06-01 09:00:00', DATE '2026-12-31', 30, 27, 3, 1),
    (4, DATE '2026-05-02', TIMESTAMP '2026-05-02 09:00:00', DATE '2027-05-01', 15, 10, 16, 1),
    (5, DATE '2026-01-01', TIMESTAMP '2026-01-01 09:00:00', DATE '2026-12-31', 10, 10, 2, 2),
    (6, DATE '2025-01-01', TIMESTAMP '2025-01-01 09:00:00', DATE '2026-12-31', 30, 25, 4, 1),
    (7, DATE '2025-01-01', TIMESTAMP '2025-01-01 09:00:00', DATE '2026-12-31', 30, 30, 5, 1),
    (8, DATE '2026-01-01', TIMESTAMP '2026-01-01 09:00:00', DATE '2026-12-31', 10, 10, 3, 2);

INSERT INTO public.demande_conge
    (id_demande_conge, commentaire_manager, commentaire_rh, date_creation,
     date_debut, date_fin, date_soumission, date_validation_manager,
     date_validation_rh, demi_jour_debut, demi_jour_fin, motif, nombre_jours,
     statut, id_emp, id_manager_validateur, id_rh_validateur, id_type_conge,
     justificatif_chemin, justificatif_nom, commentaire_direction,
     date_validation_direction, id_direction_validateur)
VALUES
    (1, 'Accord pour la periode de fin d annee.', 'Solde verifie.', TIMESTAMP '2026-10-20 09:00:00',
     DATE '2026-12-21', DATE '2026-12-23', TIMESTAMP '2026-10-20 09:30:00',
     TIMESTAMP '2026-10-22 10:00:00', TIMESTAMP '2026-10-25 11:00:00', false, false,
     'Conges de fin d annee', 3, 'VALIDEE_RH', 3, 2, 2, 1, NULL, NULL, NULL, NULL, NULL),
    (2, 'En attente de validation manager.', NULL, TIMESTAMP '2026-11-10 08:45:00',
     DATE '2026-12-14', DATE '2026-12-18', TIMESTAMP '2026-11-10 09:00:00',
     NULL, NULL, false, false, 'Conges personnels', 5, 'SOUMISE', 16, NULL, NULL, 1,
     NULL, NULL, NULL, NULL, NULL),
    (3, 'Accord.', 'Solde suffisant.', TIMESTAMP '2026-07-15 09:00:00',
     DATE '2026-08-10', DATE '2026-08-14', TIMESTAMP '2026-07-15 09:10:00',
     TIMESTAMP '2026-07-18 09:00:00', TIMESTAMP '2026-07-20 10:00:00', false, false,
     'Conges d ete', 5, 'VALIDEE_RH', 2, 2, 2, 1, NULL, NULL, NULL, NULL, NULL),
    (4, 'Accord manager.', NULL, TIMESTAMP '2026-11-12 10:00:00',
     DATE '2027-01-04', DATE '2027-01-08', TIMESTAMP '2026-11-12 10:15:00',
     TIMESTAMP '2026-11-13 09:00:00', NULL, false, false, 'Conges sans solde',
     5, 'ATTENTE_DIRECTION', 3, 2, NULL, 5, NULL, NULL,
     'En attente de l accord de la direction.', NULL, NULL),
    (5, 'Accord.', 'Solde verifie.', TIMESTAMP '2026-10-10 09:00:00',
     DATE '2026-11-02', DATE '2026-11-06', TIMESTAMP '2026-10-10 09:20:00',
     TIMESTAMP '2026-10-12 09:00:00', TIMESTAMP '2026-10-14 10:00:00', false, false,
     'Conges personnels', 5, 'VALIDEE_RH', 4, 2, 2, 1, NULL, NULL, NULL, NULL, NULL),
    (6, 'Accord manager.', NULL, TIMESTAMP '2026-11-12 14:00:00',
     DATE '2026-12-28', DATE '2026-12-31', TIMESTAMP '2026-11-12 14:30:00',
     TIMESTAMP '2026-11-13 09:30:00', NULL, false, false, 'Conges de fin d annee',
     4, 'VALIDEE_MANAGER', 2, 2, NULL, 1, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.consommation_conge
    (id_consommation_conge, date_consommation, quantite, id_demande_conge, id_solde_conge)
VALUES
    (1, TIMESTAMP '2026-08-10 00:00:00', 5, 3, 1),
    (2, TIMESTAMP '2026-12-21 00:00:00', 3, 1, 3),
    (3, TIMESTAMP '2026-12-14 00:00:00', 5, 2, 4),
    (4, TIMESTAMP '2026-11-02 00:00:00', 5, 5, 6),
    (5, TIMESTAMP '2026-12-28 00:00:00', 4, 6, 1);

INSERT INTO public.absence
    (id_absence, date_creation, date_debut, date_fin, date_validation,
     justificatif_chemin, justificatif_nom, statut, type_absence, id_emp, id_valide_par)
VALUES
    (1, TIMESTAMP '2026-10-12 08:00:00', DATE '2026-10-12', DATE '2026-10-13',
     TIMESTAMP '2026-10-14 09:00:00', 'absence-nina.pdf', 'certificat-medical-nina.pdf',
     'VALIDEE_RH', 'MALADIE', 6, 2),
    (2, TIMESTAMP '2026-09-01 09:00:00', DATE '2026-09-01', DATE '2026-09-01',
     TIMESTAMP '2026-09-02 10:00:00', 'absence-paul.pdf', 'justificatif-paul.pdf',
     'REFUSEE', 'AUTRE', 2, 2),
    (3, TIMESTAMP '2026-11-09 08:30:00', DATE '2026-11-09', DATE '2026-11-10',
     NULL, 'absence-thomas.pdf', 'certificat-medical-thomas.pdf',
     'SOUMISE', 'MALADIE', 12, NULL),
    (4, TIMESTAMP '2026-06-01 09:00:00', DATE '2026-06-01', DATE '2026-08-31',
     TIMESTAMP '2026-06-02 09:00:00', 'absence-julie.pdf', 'certificat-maternite-julie.pdf',
     'VALIDEE_RH', 'MATERNITE', 11, 2);

INSERT INTO public.document_employe
    (id_document_employe, alerte_expiration_envoyee, chemin, commentaire, date_emission,
     date_upload, date_validite, nom_original, type_document, id_emp)
VALUES
    (1, false, 'cni-jean.pdf', 'Piece d identite verifiee.', DATE '2015-02-10',
     TIMESTAMP '2022-01-10 10:00:00', DATE '2030-02-10', 'cni-jean.pdf', 'PIECE_IDENTITE', 1),
    (2, false, 'rib-paul.pdf', 'RIB valide.', DATE '2023-02-20',
     TIMESTAMP '2023-03-01 10:00:00', NULL, 'rib-paul.pdf', 'RIB', 2),
    (3, false, 'contrat-marie.pdf', 'Contrat archive.', DATE '2021-06-15',
     TIMESTAMP '2021-06-15 10:00:00', NULL, 'contrat-marie.pdf', 'CONTRAT', 3),
    (4, false, 'cni-sophie.pdf', 'Piece d identite verifiee.', DATE '2014-05-05',
     TIMESTAMP '2020-01-15 10:00:00', DATE '2029-05-05', 'cni-sophie.pdf', 'PIECE_IDENTITE', 5),
    (5, false, 'diplome-michel.pdf', 'Diplome verifie.', DATE '2005-09-01',
     TIMESTAMP '2019-05-20 10:00:00', NULL, 'diplome-michel.pdf', 'DIPLOME', 7),
    (6, false, 'rib-claire.pdf', 'RIB valide.', DATE '2023-07-01',
     TIMESTAMP '2023-07-10 10:00:00', NULL, 'rib-claire.pdf', 'RIB', 8),
    (7, true, 'permis-thomas.pdf', 'Permis a renouveler prochainement.', DATE '2018-11-20',
     TIMESTAMP '2022-02-01 10:00:00', DATE '2026-12-01', 'permis-thomas.pdf', 'PERMIS', 12),
    (8, false, 'diplome-andry.pdf', 'Diplome en attente de verification.', DATE '2020-07-01',
     TIMESTAMP '2026-05-03 09:30:00', NULL, 'diplome-andry.pdf', 'DIPLOME', 16);

INSERT INTO public.historique_salaire
    (id_historique_salaire, date_creation, date_effet, montant, motif, id_auteur, id_emp)
VALUES
    (1, TIMESTAMP '2022-01-10 09:00:00', DATE '2022-01-10', 6000000, 'Salaire d embauche', 2, 1),
    (2, TIMESTAMP '2023-03-01 09:00:00', DATE '2023-03-01', 3200000, 'Salaire d embauche', 2, 2),
    (3, TIMESTAMP '2021-06-15 09:00:00', DATE '2021-06-15', 3400000, 'Salaire d embauche', 2, 3),
    (4, TIMESTAMP '2024-02-01 09:00:00', DATE '2024-02-01', 3000000, 'Salaire d embauche', 2, 4),
    (5, TIMESTAMP '2020-01-15 09:00:00', DATE '2020-01-15', 5800000, 'Salaire d embauche', 2, 5),
    (6, TIMESTAMP '2022-09-01 09:00:00', DATE '2022-09-01', 2600000, 'Salaire d embauche', 2, 6),
    (7, TIMESTAMP '2019-05-20 09:00:00', DATE '2019-05-20', 6200000, 'Salaire d embauche', 2, 7),
    (8, TIMESTAMP '2023-07-10 09:00:00', DATE '2023-07-10', 3100000, 'Salaire d embauche', 2, 8);

INSERT INTO public.historique_carriere
    (id_historique_carriere, commentaire, date_creation, date_effet, motif, id_auteur,
     id_dept, id_emp, id_manager, id_poste)
VALUES
    (1, 'Embauche comme Responsable Informatique.', TIMESTAMP '2022-01-10 09:00:00',
     DATE '2022-01-10', 'EMBAUCHE', 2, 1, 1, NULL, 4),
    (2, 'Embauche comme Commercial.', TIMESTAMP '2023-03-01 09:00:00',
     DATE '2023-03-01', 'EMBAUCHE', 2, 4, 2, NULL, 9),
    (3, 'Embauche comme Commerciale.', TIMESTAMP '2021-06-15 09:00:00',
     DATE '2021-06-15', 'EMBAUCHE', 2, 4, 3, NULL, 9),
    (4, 'Embauche comme Responsable RH.', TIMESTAMP '2020-01-15 09:00:00',
     DATE '2020-01-15', 'EMBAUCHE', 2, 2, 5, NULL, 5),
    (5, 'Embauche comme Assistante RH.', TIMESTAMP '2022-09-01 09:00:00',
     DATE '2022-09-01', 'EMBAUCHE', 2, 2, 6, 2, 6),
    (6, 'Embauche comme Responsable Finance.', TIMESTAMP '2019-05-20 09:00:00',
     DATE '2019-05-20', 'EMBAUCHE', 2, 3, 7, NULL, 8),
    (7, 'Embauche comme Comptable.', TIMESTAMP '2023-07-10 09:00:00',
     DATE '2023-07-10', 'EMBAUCHE', 2, 3, 8, 4, 7),
    (8, 'Embauche comme Developpeur Backend.', TIMESTAMP '2026-05-02 09:00:00',
     DATE '2026-05-02', 'EMBAUCHE', 2, 1, 16, 3, 1),
    (9, 'Fin de contrat.', TIMESTAMP '2026-06-30 09:00:00',
     DATE '2026-06-30', 'SORTIE', 2, 4, 14, NULL, 9);

INSERT INTO public.changement_sensible
    (id_changement, ancienne_valeur, champ, date_soumission, date_validation, motif,
     nouvelle_valeur, statut, id_demandeur, id_emp, id_validateur)
VALUES
    (1, 'RIB-OLD-002', 'RIB', TIMESTAMP '2026-11-10 09:00:00', NULL,
     'Mise a jour du compte bancaire.', 'RIB-002', 'SOUMIS', 6, 2, NULL),
    (2, 'Lot III A 10, Antananarivo', 'ADRESSE', TIMESTAMP '2026-10-01 09:00:00',
     TIMESTAMP '2026-10-02 10:00:00', 'Demande de l employe.', 'Lot II B 34, Antananarivo',
     'VALIDE', 7, 3, 2),
    (3, '034 11 111 16', 'TELEPHONE', TIMESTAMP '2026-10-20 11:00:00',
     TIMESTAMP '2026-10-21 09:00:00', 'Numero non joignable.', '034 11 111 99',
     'REFUSE', 12, 16, 2),
    (4, 'CNI-OLD-005', 'PIECE_IDENTITE', TIMESTAMP '2026-11-12 10:00:00', NULL,
     'Renouvellement de la piece d identite.', 'CNI-005', 'SOUMIS', 2, 5, NULL);

-- ============================================================
-- 8. Accreditations, validations et sorties
-- ============================================================

INSERT INTO public.accreditation
    (id_accreditation, id_user, id_candidature, type_accreditation, organisme,
     numero, date_obtention, date_expiration, statut, document_nom_original,
     document_chemin, commentaire, id_verificateur, date_verification, date_creation)
VALUES
    (1, 12, 2, 'Certification AWS', 'Amazon Web Services', 'AWS-2025-001',
     DATE '2025-11-20', DATE '2028-11-20', 'VALIDEE', 'certification-aws-andry.pdf',
     'certification-aws-andry.pdf', 'Certification verifiee.', 2, TIMESTAMP '2026-05-02 11:00:00',
     TIMESTAMP '2026-05-02 10:30:00'),
    (2, 9, 1, 'TOEIC', 'ETS', 'TOEIC-2026-001', DATE '2026-03-10', DATE '2028-03-10',
     'VALIDEE', 'toeic-candidat.pdf', 'toeic-candidat.pdf', 'Score 850.', 2,
     TIMESTAMP '2026-10-01 11:00:00', TIMESTAMP '2026-10-01 10:30:00'),
    (3, 3, NULL, 'Habilitation electrique', 'EDF', 'HAB-2026-001',
     DATE '2026-02-01', DATE '2027-02-01', 'EN_VALIDATION_MANAGER',
     'habilitation-jean.pdf', 'habilitation-jean.pdf', 'En attente de validation manager.',
     NULL, NULL, TIMESTAMP '2026-11-01 09:00:00'),
    (4, 6, NULL, 'CACES', 'CNAM', 'CACES-2024-001', DATE '2024-05-01', DATE '2026-05-01',
     'A_CORRIGER', 'caces-nina.pdf', 'caces-nina.pdf', 'Document expire, a renouveler.',
     2, TIMESTAMP '2026-06-01 09:00:00', TIMESTAMP '2026-05-01 09:00:00'),
    (5, 12, 2, 'Permis de conduire', 'Ministere des Transports', 'PERMIS-2020-001',
     DATE '2020-01-01', DATE '2030-01-01', 'EN_VALIDATION_RH', 'permis-andry.pdf',
     'permis-andry.pdf', 'Validation manager effectuee.', NULL, NULL,
     TIMESTAMP '2026-11-04 09:00:00');

INSERT INTO public.validation_dossier
    (id_dossier, type_objet, id_objet, numero_tentative, departement_id, statut,
     etape_courante, id_createur, lien, date_creation, date_cloture)
VALUES
    (1, 'ACCREDITATION', 3, 1, 1, 'EN_COURS', 1, 3, '/accreditations/3',
     TIMESTAMP '2026-11-01 09:00:00', NULL),
    (2, 'ACCREDITATION', 5, 1, 1, 'EN_COURS', 2, 12, '/accreditations/5',
     TIMESTAMP '2026-11-04 09:00:00', NULL);

INSERT INTO public.validation_etape
    (id_etape, id_dossier, ordre, role_requis, statut, id_decideur, date_decision, commentaire)
VALUES
    (1, 1, 1, 'MANAGER', 'EN_ATTENTE', NULL, NULL, NULL),
    (2, 1, 2, 'RH', 'EN_ATTENTE', NULL, NULL, NULL),
    (3, 1, 3, 'DIRECTION', 'EN_ATTENTE', NULL, NULL, NULL),
    (4, 2, 1, 'MANAGER', 'APPROUVE', 3, TIMESTAMP '2026-11-05 09:00:00',
     'Habilitation verifiee par le manager IT.'),
    (5, 2, 2, 'RH', 'EN_ATTENTE', NULL, NULL, NULL),
    (6, 2, 3, 'DIRECTION', 'EN_ATTENTE', NULL, NULL, NULL);

INSERT INTO public.sortie_employe
    (id_sortie, commentaire, date_creation, date_notification, date_sortie,
     date_validation, motif, preavis_jours, solde_tout_compte, statut, type_sortie,
     id_emp, id_traite_par)
VALUES
    (1, 'Sortie a preparer avec le manager IT.', TIMESTAMP '2026-11-05 09:00:00',
     DATE '2026-11-05', DATE '2026-12-31', NULL,
     'Nouvelle opportunite professionnelle', 30, NULL, 'EN_COURS', 'DEMISSION', 12, 2),
    (2, 'Sortie traitee et soldee.', TIMESTAMP '2026-06-01 09:00:00',
     DATE '2026-06-01', DATE '2026-06-30', TIMESTAMP '2026-06-30 10:00:00',
     'Fin de contrat', 0, 0, 'VALIDEE', 'FIN_CONTRAT', 14, 2);

INSERT INTO public.checklist_sortie
    (id_checklist, commentaire, date_fait, fait, libelle, responsable, id_sortie)
VALUES
    (1, 'A faire avant le depart.', NULL, false, 'Restitution du materiel informatique', 'IT', 1),
    (2, 'A faire le dernier jour.', NULL, false, 'Desactivation des acces informatiques', 'IT', 1),
    (3, 'Badge et cles a recuperer.', NULL, false, 'Restitution du badge et des cles', 'RH', 1),
    (4, 'Solde a calculer.', NULL, false, 'Solde de tout compte et documents de fin de contrat', 'PAIE', 1),
    (5, 'Dossier a archiver.', NULL, false, 'Archivage du dossier administratif', 'RH', 1),
    (6, 'Materiel restitue.', TIMESTAMP '2026-06-28 10:00:00', true, 'Restitution du materiel informatique', 'IT', 2),
    (7, 'Acces desactives.', TIMESTAMP '2026-06-30 09:00:00', true, 'Desactivation des acces informatiques', 'IT', 2),
    (8, 'Badge et cles recuperes.', TIMESTAMP '2026-06-28 11:00:00', true, 'Restitution du badge et des cles', 'RH', 2),
    (9, 'Solde verse.', TIMESTAMP '2026-06-30 10:00:00', true, 'Solde de tout compte et documents de fin de contrat', 'PAIE', 2),
    (10, 'Dossier archive.', TIMESTAMP '2026-06-30 11:00:00', true, 'Archivage du dossier administratif', 'RH', 2);

-- ============================================================
-- 9. Paie, temps de travail et planning
-- ============================================================

INSERT INTO public.periode_paie
    (id_periode_paie, annee, date_cloture, date_debut, date_fin, date_ouverture,
     mois, statut, id_cloture_par)
VALUES
    (1, 2026, TIMESTAMP '2026-10-01 09:00:00', DATE '2026-09-01', DATE '2026-09-30',
     TIMESTAMP '2026-09-01 08:00:00', 9, 'CLOTUREE', 8),
    (2, 2026, NULL, DATE '2026-10-01', DATE '2026-10-31',
     TIMESTAMP '2026-10-01 08:00:00', 10, 'OUVERTE', NULL),
    (3, 2026, NULL, DATE '2026-11-01', DATE '2026-11-30',
     TIMESTAMP '2026-11-01 08:00:00', 11, 'OUVERTE', NULL);

INSERT INTO public.heure_sup
    (id_heure_sup, date_creation, date_heure_sup, date_validation, minutes, motif,
     statut, taux_majoration, id_emp, id_periode_paie, id_valide_par)
VALUES
    (1, TIMESTAMP '2026-09-24 18:00:00', DATE '2026-09-24', TIMESTAMP '2026-09-25 09:00:00',
     120, 'Inventaire de fin de mois', 'PAYEE', 25, 2, 1, 3),
    (2, TIMESTAMP '2026-10-28 18:00:00', DATE '2026-10-28', TIMESTAMP '2026-10-29 09:00:00',
     90, 'Relance clients', 'VALIDEE', 25, 2, NULL, 3),
    (3, TIMESTAMP '2026-11-12 18:00:00', DATE '2026-11-12', NULL,
     60, 'Mise en production', 'EN_ATTENTE', 25, 16, NULL, NULL),
    (4, TIMESTAMP '2026-11-10 18:00:00', DATE '2026-11-10', TIMESTAMP '2026-11-11 09:00:00',
     30, 'Saisie des commandes', 'VALIDEE', 25, 3, NULL, 3),
    (5, TIMESTAMP '2026-11-11 18:00:00', DATE '2026-11-11', TIMESTAMP '2026-11-12 09:00:00',
     90, 'Preparation du reporting', 'VALIDEE', 25, 2, NULL, 3);

INSERT INTO public.variable_paie
    (id_variable_paie, date_creation, libelle, montant, quantite, reference,
     type_variable, id_emp, id_periode_paie)
VALUES
    (1, TIMESTAMP '2026-10-01 09:00:00', 'Heures supplementaires septembre',
     NULL, 2.00, 'HS-2026-09', 'HEURE_SUP', 2, 1),
    (2, TIMESTAMP '2026-10-01 09:00:00', 'Prime exceptionnelle septembre',
     200000, NULL, 'PRIME-2026-09', 'PRIME', 2, 1);

INSERT INTO public.inscription_paie
    (id_inscription_paie, commentaire, date_debut, date_inscription, numero_paie,
     organisme, statut, id_emp)
VALUES
    (1, 'Inscription CNaPS.', DATE '2023-03-01', TIMESTAMP '2023-03-01 09:00:00',
     'PAIE-002', 'CNaPS', 'INSCRIT', 2),
    (2, 'Inscription CNaPS.', DATE '2021-06-15', TIMESTAMP '2021-06-15 09:00:00',
     'PAIE-003', 'CNaPS', 'INSCRIT', 3),
    (3, 'Inscription CNaPS.', DATE '2024-02-01', TIMESTAMP '2024-02-01 09:00:00',
     'PAIE-004', 'CNaPS', 'INSCRIT', 4),
    (4, 'Inscription CNaPS.', DATE '2020-01-15', TIMESTAMP '2020-01-15 09:00:00',
     'PAIE-005', 'CNaPS', 'INSCRIT', 5),
    (5, 'Inscription CNaPS.', DATE '2022-09-01', TIMESTAMP '2022-09-01 09:00:00',
     'PAIE-006', 'CNaPS', 'INSCRIT', 6),
    (6, 'Inscription CNaPS.', DATE '2019-05-20', TIMESTAMP '2019-05-20 09:00:00',
     'PAIE-007', 'CNaPS', 'INSCRIT', 7),
    (7, 'Inscription CNaPS.', DATE '2023-07-10', TIMESTAMP '2023-07-10 09:00:00',
     'PAIE-008', 'CNaPS', 'INSCRIT', 8),
    (8, 'Dossier en cours de constitution.', DATE '2026-05-02', TIMESTAMP '2026-05-02 09:00:00',
     NULL, 'CNaPS', 'EN_ATTENTE', 16);

INSERT INTO public.planning
    (id_planning, commentaire, date_creation, date_debut, date_fin, date_publication,
     statut, id_cree_par, id_emp)
VALUES
    (1, 'Planning hebdomadaire de Jean.', TIMESTAMP '2026-11-01 09:00:00',
     DATE '2026-11-09', DATE '2026-11-15', TIMESTAMP '2026-11-01 10:00:00', 'PUBLIE', 3, 1),
    (2, 'Planning hebdomadaire de Paul.', TIMESTAMP '2026-11-01 09:00:00',
     DATE '2026-11-09', DATE '2026-11-15', TIMESTAMP '2026-11-01 10:00:00', 'PUBLIE', 3, 2),
    (3, 'Planning hebdomadaire de Marie.', TIMESTAMP '2026-11-01 09:00:00',
     DATE '2026-11-09', DATE '2026-11-15', TIMESTAMP '2026-11-01 10:00:00', 'PUBLIE', 2, 3),
    (4, 'Planning hebdomadaire de Luc.', TIMESTAMP '2026-11-01 09:00:00',
     DATE '2026-11-09', DATE '2026-11-15', TIMESTAMP '2026-11-01 10:00:00', 'PUBLIE', 3, 4),
    (5, 'Planning hebdomadaire d Andry.', TIMESTAMP '2026-11-01 09:00:00',
     DATE '2026-11-09', DATE '2026-11-15', TIMESTAMP '2026-11-01 10:00:00', 'PUBLIE', 3, 16),
    (6, 'Planning de la semaine suivante.', TIMESTAMP '2026-11-12 09:00:00',
     DATE '2026-11-16', DATE '2026-11-22', NULL, 'BROUILLON', 3, 16),
    (7, 'Planning des conges de fin d annee.', TIMESTAMP '2026-12-01 09:00:00',
     DATE '2026-12-21', DATE '2026-12-27', TIMESTAMP '2026-12-01 10:00:00', 'PUBLIE', 2, 3);

INSERT INTO public.planning_jour
    (date_jour, heure_debut, heure_fin, pause_minutes, type_jour, id_planning)
SELECT
    d::date,
    CASE WHEN EXTRACT(ISODOW FROM d) BETWEEN 1 AND 5 THEN TIME '08:00' END,
    CASE WHEN EXTRACT(ISODOW FROM d) BETWEEN 1 AND 5 THEN TIME '16:00' END,
    CASE WHEN EXTRACT(ISODOW FROM d) BETWEEN 1 AND 5 THEN 60 END,
    CASE WHEN EXTRACT(ISODOW FROM d) BETWEEN 1 AND 5 THEN 'TRAVAIL' ELSE 'REPOS' END,
    p.id_planning
FROM public.planning p
CROSS JOIN generate_series(p.date_debut, p.date_fin, INTERVAL '1 day') AS d
WHERE p.id_planning BETWEEN 1 AND 6;

INSERT INTO public.planning_jour
    (date_jour, heure_debut, heure_fin, pause_minutes, type_jour, id_planning)
VALUES
    (DATE '2026-12-21', NULL, NULL, NULL, 'CONGE', 7),
    (DATE '2026-12-22', NULL, NULL, NULL, 'CONGE', 7),
    (DATE '2026-12-23', NULL, NULL, NULL, 'CONGE', 7),
    (DATE '2026-12-24', TIME '08:00', TIME '16:00', 60, 'TRAVAIL', 7),
    (DATE '2026-12-25', NULL, NULL, NULL, 'FERIE', 7),
    (DATE '2026-12-26', NULL, NULL, NULL, 'REPOS', 7),
    (DATE '2026-12-27', NULL, NULL, NULL, 'REPOS', 7);

INSERT INTO public.pointage
    (id_pointage, commentaire, date_creation, date_modification, date_pointage,
     heure_arrivee, heure_depart, pause_minutes, statut, id_emp)
VALUES
    (1, NULL, TIMESTAMP '2026-11-09 16:05:00', NULL, DATE '2026-11-09', TIME '08:00', TIME '16:00', 60, 'PRESENT', 1),
    (2, NULL, TIMESTAMP '2026-11-10 16:05:00', NULL, DATE '2026-11-10', TIME '08:00', TIME '16:00', 60, 'PRESENT', 1),
    (3, NULL, TIMESTAMP '2026-11-11 16:05:00', NULL, DATE '2026-11-11', TIME '08:00', TIME '16:00', 60, 'PRESENT', 1),
    (4, NULL, TIMESTAMP '2026-11-12 16:05:00', NULL, DATE '2026-11-12', TIME '08:00', TIME '16:00', 60, 'PRESENT', 1),
    (5, NULL, TIMESTAMP '2026-11-13 16:05:00', NULL, DATE '2026-11-13', TIME '08:00', TIME '16:00', 60, 'PRESENT', 1),
    (6, NULL, TIMESTAMP '2026-11-09 16:05:00', NULL, DATE '2026-11-09', TIME '08:00', TIME '16:00', 60, 'PRESENT', 2),
    (7, 'Retard de 15 minutes.', TIMESTAMP '2026-11-10 16:05:00', NULL, DATE '2026-11-10', TIME '08:15', TIME '16:00', 60, 'RETARD', 2),
    (8, 'Depassement pour preparation du reporting.', TIMESTAMP '2026-11-11 17:35:00', NULL, DATE '2026-11-11', TIME '08:00', TIME '17:30', 60, 'PRESENT', 2),
    (9, NULL, TIMESTAMP '2026-11-12 16:05:00', NULL, DATE '2026-11-12', TIME '08:00', TIME '16:00', 60, 'PRESENT', 2),
    (10, NULL, TIMESTAMP '2026-11-13 16:05:00', NULL, DATE '2026-11-13', TIME '08:00', TIME '16:00', 60, 'PRESENT', 2),
    (11, NULL, TIMESTAMP '2026-11-09 16:05:00', NULL, DATE '2026-11-09', TIME '08:00', TIME '16:00', 60, 'PRESENT', 3),
    (12, NULL, TIMESTAMP '2026-11-10 16:05:00', NULL, DATE '2026-11-10', TIME '08:00', TIME '16:00', 60, 'PRESENT', 3),
    (13, NULL, TIMESTAMP '2026-11-11 16:05:00', NULL, DATE '2026-11-11', TIME '08:00', TIME '16:00', 60, 'PRESENT', 3),
    (14, NULL, TIMESTAMP '2026-11-12 16:05:00', NULL, DATE '2026-11-12', TIME '08:00', TIME '16:00', 60, 'PRESENT', 3),
    (15, NULL, TIMESTAMP '2026-11-13 16:05:00', NULL, DATE '2026-11-13', TIME '08:00', TIME '16:00', 60, 'PRESENT', 3),
    (16, 'Retard de 20 minutes.', TIMESTAMP '2026-11-09 16:05:00', NULL, DATE '2026-11-09', TIME '08:20', TIME '16:00', 60, 'RETARD', 4),
    (17, NULL, TIMESTAMP '2026-11-10 16:05:00', NULL, DATE '2026-11-10', TIME '08:00', TIME '16:00', 60, 'PRESENT', 4),
    (18, NULL, TIMESTAMP '2026-11-11 16:05:00', NULL, DATE '2026-11-11', TIME '08:00', TIME '16:00', 60, 'PRESENT', 4),
    (19, 'Depart anticipe de 60 minutes.', TIMESTAMP '2026-11-12 15:05:00', NULL, DATE '2026-11-12', TIME '08:00', TIME '15:00', 60, 'DEPART_ANTICIPE', 4),
    (20, NULL, TIMESTAMP '2026-11-13 16:05:00', NULL, DATE '2026-11-13', TIME '08:00', TIME '16:00', 60, 'PRESENT', 4),
    (21, NULL, TIMESTAMP '2026-11-09 16:05:00', NULL, DATE '2026-11-09', TIME '08:00', TIME '16:00', 60, 'PRESENT', 16),
    (22, NULL, TIMESTAMP '2026-11-10 16:05:00', NULL, DATE '2026-11-10', TIME '08:00', TIME '16:00', 60, 'PRESENT', 16),
    (23, NULL, TIMESTAMP '2026-11-12 16:05:00', NULL, DATE '2026-11-12', TIME '08:00', TIME '16:00', 60, 'PRESENT', 16),
    (24, NULL, TIMESTAMP '2026-11-13 16:05:00', NULL, DATE '2026-11-13', TIME '08:00', TIME '16:00', 60, 'PRESENT', 16);

INSERT INTO public.anomalie_pointage
    (id_anomalie, commentaire, date_anomalie, date_traitement, minutes, statut,
     type_anomalie, id_emp, id_pointage, id_traite_par)
VALUES
    (1, 'Retard constate.', DATE '2026-11-10', NULL, 15, 'OUVERTE', 'RETARD', 2, 7, NULL),
    (2, 'Retard constate.', DATE '2026-11-09', NULL, 20, 'OUVERTE', 'RETARD', 4, 16, NULL),
    (3, 'Depart anticipe constate.', DATE '2026-11-12', NULL, 60, 'OUVERTE', 'DEPART_ANTICIPE', 4, 19, NULL),
    (4, 'Absence non justifiee a ce jour.', DATE '2026-11-09', NULL, 480, 'OUVERTE', 'ABSENCE', 12, NULL, NULL),
    (5, 'Absence non justifiee a ce jour.', DATE '2026-11-11', NULL, 480, 'OUVERTE', 'ABSENCE', 16, NULL, NULL);

INSERT INTO public.notification
    (id_notification, date_creation, date_lecture, lien, lu, message, type, id_user)
VALUES
    (1, TIMESTAMP '2026-11-12 09:00:00', NULL, '/conges/2', false,
     'Votre demande de conge du 14 au 18 decembre est en attente de validation.', 'CONGE', 12),
    (2, TIMESTAMP '2026-11-13 09:30:00', NULL, '/conges/6', false,
     'Votre demande de conge du 28 au 31 decembre a ete validee par votre manager.', 'CONGE', 6),
    (3, TIMESTAMP '2026-11-12 10:00:00', NULL, '/validations', false,
     'Une demande de conge sans solde attend la validation de la direction.', 'VALIDATION', 11),
    (4, TIMESTAMP '2026-11-05 09:00:00', TIMESTAMP '2026-11-05 10:00:00', '/sorties/1', true,
     'La sortie de Thomas Rakoto est en cours de preparation.', 'SORTIE', 2),
    (5, TIMESTAMP '2026-11-04 09:00:00', NULL, '/accreditations/5', false,
     'Votre accréditation Permis de conduire attend la validation RH.', 'ACCREDITATION', 12),
    (6, TIMESTAMP '2026-11-01 09:00:00', NULL, '/accreditations/3', false,
     'Votre habilitation electrique attend la validation de votre manager.', 'ACCREDITATION', 3),
    (7, TIMESTAMP '2026-11-12 14:00:00', TIMESTAMP '2026-11-12 15:00:00', '/pointages', true,
     'Une anomalie de pointage a ete detectee pour Andry Nouveau.', 'POINTAGE', 3),
    (8, TIMESTAMP '2026-11-10 10:00:00', NULL, '/onboarding/1', false,
     'Il reste des taches d integration a finaliser pour Andry Nouveau.', 'ONBOARDING', 2),
    (9, TIMESTAMP '2026-10-01 09:00:00', TIMESTAMP '2026-10-01 09:30:00', '/paie', true,
     'La periode de paie de septembre 2026 a ete cloturee.', 'PAIE', 8),
    (10, TIMESTAMP '2026-09-10 09:00:00', TIMESTAMP '2026-09-10 12:00:00', '/offres/1', true,
     'Deux nouvelles offres d emploi sont publiees.', 'RECRUTEMENT', 3);

INSERT INTO public.journal_audit
    (id_audit, adresse_ip, chemin, date_action, details, entite, id_entite,
     methode, username)
VALUES
    (1, '127.0.0.1', '/utilisateurs', TIMESTAMP '2026-11-13 08:00:00',
     'Connexion administrateur.', 'Utilisateur', '1', 'POST', 'admin'),
    (2, '127.0.0.1', '/conges/2', TIMESTAMP '2026-11-10 09:00:00',
     'Soumission demande de conge.', 'DemandeConge', '2', 'POST', 'andry.nouveau'),
    (3, '127.0.0.1', '/conges/6/valider-manager', TIMESTAMP '2026-11-13 09:30:00',
     'Validation manager demande de conge.', 'DemandeConge', '6', 'POST', 'manager.it'),
    (4, '127.0.0.1', '/absences/3', TIMESTAMP '2026-11-09 08:30:00',
     'Soumission absence maladie.', 'Absence', '3', 'POST', 'thomas.rakoto'),
    (5, '127.0.0.1', '/pointages', TIMESTAMP '2026-11-12 16:05:00',
     'Pointage du jour enregistre.', 'Pointage', '23', 'POST', 'andry.nouveau'),
    (6, '127.0.0.1', '/sorties/1', TIMESTAMP '2026-11-05 09:00:00',
     'Creation d une sortie.', 'SortieEmploye', '1', 'POST', 'rh.sophie'),
    (7, '127.0.0.1', '/accreditations/5/valider', TIMESTAMP '2026-11-05 09:00:00',
     'Validation manager accreditation.', 'Accreditation', '5', 'POST', 'manager.it'),
    (8, '127.0.0.1', '/paie/periodes/1/cloturer', TIMESTAMP '2026-10-01 09:00:00',
     'Cloture de la periode de paie.', 'PeriodePaie', '1', 'POST', 'finance.claire'),
    (9, '127.0.0.1', '/onboarding/1/taches/4', TIMESTAMP '2026-05-03 10:00:00',
     'Mise a jour tache onboarding.', 'OnboardingTache', '4', 'POST', 'rh.sophie'),
    (10, '127.0.0.1', '/changements/1', TIMESTAMP '2026-11-10 09:00:00',
     'Soumission changement RIB.', 'ChangementSensible', '1', 'POST', 'paul.rabe');

-- ============================================================
-- 10. Realignement des sequences
-- ============================================================

DO $$
DECLARE
    r RECORD;
    seq_name TEXT;
    max_value BIGINT;
BEGIN
    FOR r IN
        SELECT c.table_schema, c.table_name, c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND (c.is_identity = 'YES' OR c.column_default LIKE 'nextval%')
        ORDER BY c.table_name, c.column_name
    LOOP
        seq_name := pg_get_serial_sequence(
            format('%I.%I', r.table_schema, r.table_name), r.column_name);
        IF seq_name IS NOT NULL THEN
            EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I.%I',
                r.column_name, r.table_schema, r.table_name) INTO max_value;
            IF max_value > 0 THEN
                PERFORM setval(seq_name, max_value, true);
            ELSE
                PERFORM setval(seq_name, 1, false);
            END IF;
        END IF;
    END LOOP;
END $$;

COMMIT;

