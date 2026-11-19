-- Tables du Bloc 12 : rupture de contrat, décompte de sortie et traçabilité.
-- À intégrer dans tables-finales.sql après les tables de contrats, paie et congés.
CREATE TABLE types_initiative_sortie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE types_ligne_solde_sortie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(180) NOT NULL UNIQUE,
    nature_id INTEGER NOT NULL REFERENCES categories_rubrique_paie(id) ON DELETE RESTRICT,
    ordre SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE motifs_sortie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(140) NOT NULL UNIQUE,
    initiative_id INTEGER NOT NULL REFERENCES types_initiative_sortie(id) ON DELETE RESTRICT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    ordre SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE statuts_sortie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    ordre SMALLINT NOT NULL DEFAULT 0,
    terminal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE dossiers_sortie (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    contrat_id INTEGER NOT NULL REFERENCES contrats(id) ON DELETE RESTRICT,
    motif_id INTEGER NOT NULL REFERENCES motifs_sortie(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_sortie(id) ON DELETE RESTRICT,
    initie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    date_notification DATE NOT NULL,
    dernier_jour_travaille DATE NOT NULL,
    commentaire VARCHAR(2000),
    motif_annulation VARCHAR(1000),
    stc_prepare_at TIMESTAMPTZ,
    validee_at TIMESTAMPTZ,
    paiement_reference VARCHAR(120),
    paiement_date DATE,
    paiement_confirme_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    montant_net_valide NUMERIC(14,2),
    net_verifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    net_verifie_at TIMESTAMPTZ,
    controle_net_detail VARCHAR(2000),
    stc_document_employe_id INTEGER REFERENCES documents_employe(id) ON DELETE RESTRICT,
    certificat_document_employe_id INTEGER REFERENCES documents_employe(id) ON DELETE RESTRICT,
    notification_document_employe_id INTEGER REFERENCES documents_employe(id) ON DELETE RESTRICT,
    sortie_finalisee_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_dossiers_sortie_dates CHECK (dernier_jour_travaille >= date_notification)
);

CREATE TABLE lignes_solde_sortie (
    id SERIAL PRIMARY KEY,
    dossier_sortie_id INTEGER NOT NULL REFERENCES dossiers_sortie(id) ON DELETE RESTRICT,
    rubrique_id INTEGER REFERENCES rubriques_paie(id) ON DELETE RESTRICT,
    type_ligne_id INTEGER NOT NULL REFERENCES types_ligne_solde_sortie(id) ON DELETE RESTRICT,
    libelle VARCHAR(180) NOT NULL,
    quantite NUMERIC(12,3) NOT NULL DEFAULT 1,
    montant_unitaire NUMERIC(14,2) NOT NULL,
    montant NUMERIC(14,2) NOT NULL,
    detail VARCHAR(1500) NOT NULL,
    source_table VARCHAR(60),
    source_id INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_ligne_solde_sortie_amount CHECK (quantite >= 0 AND montant_unitaire >= 0 AND montant >= 0)
);

CREATE TABLE historique_sortie (
    id SERIAL PRIMARY KEY,
    dossier_sortie_id INTEGER NOT NULL REFERENCES dossiers_sortie(id) ON DELETE RESTRICT,
    ancien_statut_id INTEGER REFERENCES statuts_sortie(id) ON DELETE RESTRICT,
    nouveau_statut_id INTEGER NOT NULL REFERENCES statuts_sortie(id) ON DELETE RESTRICT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    commentaire VARCHAR(1500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE types_materiel (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE statuts_restitution_materiel (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE types_documents_sortie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE restitutions_materiel (
    id SERIAL PRIMARY KEY,
    dossier_sortie_id INTEGER NOT NULL REFERENCES dossiers_sortie(id) ON DELETE RESTRICT,
    type_materiel_id INTEGER NOT NULL REFERENCES types_materiel(id) ON DELETE RESTRICT,
    libelle_materiel VARCHAR(180) NOT NULL,
    numero_inventaire VARCHAR(100),
    statut_restitution_id INTEGER NOT NULL REFERENCES statuts_restitution_materiel(id) ON DELETE RESTRICT,
    recu_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    restitue_at TIMESTAMPTZ,
    observation VARCHAR(1000)
);

CREATE TABLE documents_sortie (
    id SERIAL PRIMARY KEY,
    dossier_sortie_id INTEGER NOT NULL REFERENCES dossiers_sortie(id) ON DELETE RESTRICT,
    type_document_id INTEGER NOT NULL REFERENCES types_documents_sortie(id) ON DELETE RESTRICT,
    document_employe_id INTEGER REFERENCES documents_employe(id) ON DELETE RESTRICT,
    genere_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    genere_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remis_at TIMESTAMPTZ,
    CONSTRAINT uq_document_sortie_dossier_type UNIQUE(dossier_sortie_id,type_document_id)
);

CREATE INDEX idx_dossiers_sortie_employe ON dossiers_sortie(employe_id,created_at DESC);
CREATE INDEX idx_dossiers_sortie_statut ON dossiers_sortie(statut_id,created_at DESC);
CREATE INDEX idx_lignes_stc_dossier ON lignes_solde_sortie(dossier_sortie_id,id);
CREATE INDEX idx_historique_sortie_dossier ON historique_sortie(dossier_sortie_id,id);
