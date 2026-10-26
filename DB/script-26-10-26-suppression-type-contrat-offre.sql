BEGIN;

-- Suppression du type de contrat sur les offres d'emploi.
-- Le type de contrat reste porte par la table contrat.

ALTER TABLE offre_emploi DROP COLUMN IF EXISTS type_contrat;

COMMIT;
