-- Dataflow candidature : convocation, entretien, questionnaire contrat et notifications.
-- A executer une fois sur une base existante. Hibernate complete le schema en ddl-auto=update.

BEGIN;

INSERT INTO candidature_status (libelle)
SELECT t.libelle
FROM (VALUES ('CONVOQUE'), ('RETENU'), ('PROPOSITION'), ('CONTRAT_REFUSE')) AS t(libelle)
WHERE NOT EXISTS (
    SELECT 1 FROM candidature_status c WHERE c.libelle = t.libelle
);

ALTER TABLE candidature ADD COLUMN IF NOT EXISTS salaire_convenu INT;

CREATE TABLE IF NOT EXISTS notification (
    id_notification SERIAL PRIMARY KEY,
    id_user INT NOT NULL REFERENCES utilisateur(id_user) ON DELETE CASCADE,
    type VARCHAR(50),
    message VARCHAR(500) NOT NULL,
    lien VARCHAR(500),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_lecture TIMESTAMP,
    lu BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_notification_user ON notification(id_user);
CREATE INDEX IF NOT EXISTS idx_notification_lu ON notification(id_user, lu);

CREATE TABLE IF NOT EXISTS question_contrat (
    id_question_contrat SERIAL PRIMARY KEY,
    libelle VARCHAR(255) NOT NULL,
    ordre INT NOT NULL DEFAULT 1,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS reponse_contrat (
    id_reponse_contrat SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL REFERENCES candidature(id_candidature) ON DELETE CASCADE,
    id_question_contrat INT REFERENCES question_contrat(id_question_contrat) ON DELETE SET NULL,
    code VARCHAR(30),
    accepte BOOLEAN NOT NULL DEFAULT FALSE,
    proposition TEXT,
    numero_tour INT NOT NULL,
    auteur VARCHAR(20) NOT NULL,
    date_reponse TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_reponse_contrat_candidature
    ON reponse_contrat(id_candidature, numero_tour);

INSERT INTO question_contrat (libelle, ordre, actif)
SELECT t.libelle, t.ordre, TRUE
FROM (VALUES
    ('J accepte le type de contrat propose.', 1),
    ('J accepte la date d effet proposee.', 2),
    ('J accepte le lieu de travail propose.', 3),
    ('J accepte la duree du travail proposee.', 4)
) AS t(libelle, ordre)
WHERE NOT EXISTS (SELECT 1 FROM question_contrat);

COMMIT;
