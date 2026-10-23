BEGIN;

CREATE TABLE IF NOT EXISTS proposition_embauche (
    id_proposition SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL UNIQUE REFERENCES candidature(id_candidature) ON DELETE CASCADE,
    id_offre INT NOT NULL REFERENCES offre_emploi(id_offre),
    id_candidat_user INT NOT NULL REFERENCES utilisateur(id_user),
    id_rh_user INT REFERENCES utilisateur(id_user),
    salaire_propose INT,
    type_contrat VARCHAR(50),
    date_embauche_souhaitee DATE,
    date_proposition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP,
    message TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    date_reponse TIMESTAMP,
    motif_refus TEXT,
    CONSTRAINT chk_proposition_statut CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'EXPIREE'))
);

CREATE INDEX IF NOT EXISTS idx_proposition_candidature ON proposition_embauche(id_candidature);
CREATE INDEX IF NOT EXISTS idx_proposition_candidat ON proposition_embauche(id_candidat_user);

COMMIT;
