BEGIN;

-- Ajout du role Finance s'il n'existe pas
INSERT INTO role (nom_role)
VALUES ('Finance')
ON CONFLICT (nom_role) DO NOTHING;

-- Attribution du role Finance a l'utilisateur manager.finance
UPDATE utilisateur
SET id_role = (SELECT id_role FROM role WHERE nom_role = 'Finance')
WHERE username = 'manager.finance';

-- Attribution du role Finance a tous les utilisateurs lies a un employe du departement Finance
-- (au cas ou d'autres comptes existent)
UPDATE utilisateur u
SET id_role = (SELECT id_role FROM role WHERE nom_role = 'Finance')
FROM employe e
JOIN departement d ON d.id_dept = e.id_dept
WHERE u.id_emp = e.id_emp
  AND LOWER(d.nom_dept) = 'finance'
  AND u.username <> 'manager.finance';

COMMIT;
