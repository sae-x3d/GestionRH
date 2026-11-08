BEGIN;

-- ==========================================
-- BLOC 1 : ROLE FINANCE ET COMPTE DE TEST
-- ==========================================

-- Le role Finance est distinct du role Manager : il autorise la validation
-- du budget. Il est ajoute apres les roles du seed du 22/10/2026.
INSERT INTO role (nom_role)
VALUES ('Finance')
ON CONFLICT (nom_role) DO NOTHING;

-- Compte de test rattache a Claire Ravel (employe 8, departement Finance).
-- Mot de passe commun aux comptes de demonstration : MotDePasse2026
INSERT INTO utilisateur (id_emp, id_role, username, password_hash)
SELECT 8, r.id_role, 'finance.claire',
       '$2a$10$1nm2BgnvH3qKcaVrDRx3XOPJrg0.ANPQwOFlQNVjn8zusD2pD/HcO'
FROM role r
WHERE r.nom_role = 'Finance'
ON CONFLICT (username) DO NOTHING;

SELECT setval(
    pg_get_serial_sequence('role', 'id_role'),
    (SELECT MAX(id_role) FROM role)
);

SELECT setval(
    pg_get_serial_sequence('utilisateur', 'id_user'),
    (SELECT MAX(id_user) FROM utilisateur)
);

COMMIT;
