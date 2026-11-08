-- Bloc 3 : contrat et embauche. A executer une seule fois sur la base gestionrh.
-- Hibernate cree aussi cette table en ddl-auto=update, ce script est fourni pour deployment controle.
BEGIN;
CREATE TABLE IF NOT EXISTS contrat (
    id_contrat SERIAL PRIMARY KEY,
    id_candidature INT NOT NULL UNIQUE REFERENCES candidature(id_candidature),
    id_emp INT REFERENCES emp(id_emp),
    type_contrat VARCHAR(20) NOT NULL CHECK (type_contrat IN ('CDI','CDD')),
    nom_salarie VARCHAR(200) NOT NULL,
    adresse_salarie VARCHAR(500) NOT NULL,
    nom_employeur VARCHAR(200) NOT NULL,
    adresse_employeur VARCHAR(500) NOT NULL,
    fonction VARCHAR(200) NOT NULL,
    categorie_professionnelle VARCHAR(150) NOT NULL,
    indice_classification VARCHAR(100) NOT NULL,
    salaire INT NOT NULL CHECK (salaire > 0),
    duree_travail VARCHAR(150) NOT NULL,
    lieu_travail VARCHAR(200) NOT NULL,
    date_effet DATE NOT NULL,
    date_fin DATE,
    motif_cdd VARCHAR(500),
    statut VARCHAR(30) NOT NULL,
    date_generation TIMESTAMP,
    date_acceptation TIMESTAMP,
    pdf BYTEA,
    CONSTRAINT ck_contrat_cdd_dates CHECK (type_contrat <> 'CDD' OR date_fin IS NOT NULL),
    CONSTRAINT ck_contrat_cdi_sans_fin CHECK (type_contrat <> 'CDI' OR date_fin IS NULL)
);
CREATE INDEX IF NOT EXISTS idx_contrat_statut ON contrat(statut);
COMMIT;
