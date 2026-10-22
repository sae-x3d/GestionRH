BEGIN;

-- Migration des statuts existants vers le workflow de validation RH.
-- À exécuter une seule fois sur une base déjà alimentée par le seed du 22/10/2026.

UPDATE demande_status
SET libelle_status = 'EN_ATTENTE_VALIDATION_RH'
WHERE id_demande_status = 1;

UPDATE demande_status
SET libelle_status = 'APPROUVE'
WHERE id_demande_status = 2;

UPDATE demande_status
SET libelle_status = 'REFUSE'
WHERE id_demande_status = 3;

UPDATE demande_status
SET libelle_status = 'EN_ATTENTE_VALIDATION_BUDGET'
WHERE id_demande_status = 4;

-- Les demandes terminées deviennent des demandes approuvées.
UPDATE demande
SET id_demande_status = 2
WHERE id_demande_status = 5;

-- Le statut 5 est réutilisé pour la validation Direction.
INSERT INTO demande_status (id_demande_status, libelle_status)
VALUES (5, 'EN_ATTENTE_VALIDATION_DIRECTION')
ON CONFLICT (id_demande_status)
DO UPDATE SET libelle_status = EXCLUDED.libelle_status;

COMMIT;
