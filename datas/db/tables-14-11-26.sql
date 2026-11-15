-- Schema complet SIRH au 14-11-26. A importer avec data-14-11-26.sql sur une base vide.

CREATE TABLE IF NOT EXISTS statuts_validation_compte (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS motifs_recrutement (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_evenement_effectif (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    justificatif_requis BOOLEAN NOT NULL DEFAULT FALSE,
    acquisition_active BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS jours_semaine (
    id SERIAL PRIMARY KEY,
    numero_iso SMALLINT NOT NULL UNIQUE CHECK (numero_iso BETWEEN 1 AND 7),
    code VARCHAR(12) NOT NULL UNIQUE,
    libelle VARCHAR(24) NOT NULL UNIQUE,
    ouvrable_par_defaut BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS jours_feries (
    id SERIAL PRIMARY KEY,
    date_ferie DATE NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS statuts_demande_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS decisions_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_mouvement_solde_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_absence (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    justificatif_requis BOOLEAN NOT NULL DEFAULT FALSE,
    compte_effectif BOOLEAN NOT NULL DEFAULT FALSE,
    impacte_planning BOOLEAN NOT NULL DEFAULT TRUE,
    retenue_paie BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS statuts_demande_recrutement (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS decisions_validation_demande (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS statuts_offre_emploi (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_notifications (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS statuts_candidature (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS statuts_entretien (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_entretien (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_contrat (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS statuts_contrat (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_parcours (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS niveaux_etude (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL,
    ordre SMALLINT NOT NULL CHECK (ordre > 0)
);

CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(254) NOT NULL UNIQUE,
    full_name VARCHAR(150),
    phone_number VARCHAR(30),
    region VARCHAR(120),
    photo_profil_path VARCHAR(500),
    password_hash VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    administrative_validation_status_id INTEGER NOT NULL
        REFERENCES statuts_validation_compte(id),
    verification_requested_at TIMESTAMPTZ,
    is_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_users_username_safe CHECK (username ~ '^[A-Za-z0-9._-]+$'),
    CONSTRAINT ck_users_photo_path CHECK (
        photo_profil_path IS NULL OR photo_profil_path ~ '^pdp/pdp_[A-Za-z0-9._-]+[.]png$'
    )
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower ON users (LOWER(email));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_username_lower ON users (LOWER(username));

CREATE TABLE IF NOT EXISTS departements (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    code_matricule VARCHAR(5) NOT NULL UNIQUE,
    nom VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS circuits_validation_demande (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS etapes_circuit_validation (
    id SERIAL PRIMARY KEY,
    circuit_id INTEGER NOT NULL REFERENCES circuits_validation_demande(id) ON DELETE RESTRICT,
    ordre_etape INTEGER NOT NULL,
    code VARCHAR(40) NOT NULL,
    libelle VARCHAR(120) NOT NULL,
    UNIQUE (circuit_id, ordre_etape),
    UNIQUE (circuit_id, code)
);

CREATE TABLE IF NOT EXISTS etapes_validateurs_roles (
    etape_circuit_id INTEGER PRIMARY KEY REFERENCES etapes_circuit_validation(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS etapes_validateurs_departements (
    etape_circuit_id INTEGER PRIMARY KEY REFERENCES etapes_circuit_validation(id) ON DELETE CASCADE,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_circuit_validation_actif
    ON circuits_validation_demande (actif)
    WHERE actif = TRUE;

CREATE TABLE IF NOT EXISTS postes (
    id SERIAL PRIMARY KEY,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    code_matricule VARCHAR(5) NOT NULL,
    intitule VARCHAR(150) NOT NULL,
    description TEXT,
    effectif_autorise INTEGER NOT NULL DEFAULT 0,
    salaire_base_min NUMERIC(14, 2) NOT NULL CHECK (salaire_base_min > 0),
    salaire_base_max NUMERIC(14, 2) NOT NULL CHECK (salaire_base_max > 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (departement_id, intitule),
    UNIQUE (departement_id, code_matricule),
    CONSTRAINT ck_postes_fourchette_salaire_base CHECK (salaire_base_min <= salaire_base_max)
);

CREATE TABLE IF NOT EXISTS regles_effectif_minimal (
    id SERIAL PRIMARY KEY,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    minimum_presence INTEGER NOT NULL DEFAULT 0 CHECK (minimum_presence >= 0),
    date_debut DATE NOT NULL,
    date_fin DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    modifie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motif VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_regle_effectif_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_regle_effectif_minimal_active
    ON regles_effectif_minimal(poste_id) WHERE actif = TRUE AND date_fin IS NULL;

CREATE TABLE IF NOT EXISTS provenances_employe (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS employes (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    matricule VARCHAR(30) NOT NULL UNIQUE,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    provenance_id INTEGER NOT NULL REFERENCES provenances_employe(id) ON DELETE RESTRICT,
    date_embauche DATE NOT NULL,
    date_naissance DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION generer_matricule_employe()
RETURNS TRIGGER AS $$
DECLARE
    code_departement VARCHAR(5);
    code_poste VARCHAR(5);
    ancien_departement_id INTEGER;
    nouveau_departement_id INTEGER;
    code_provenance VARCHAR(30);
BEGIN
    SELECT d.code_matricule, p.code_matricule
      INTO code_departement, code_poste
      FROM postes p
      JOIN departements d ON d.id = p.departement_id
     WHERE p.id = NEW.poste_id;

    IF code_departement IS NULL OR code_poste IS NULL THEN
        RAISE EXCEPTION 'Le département et le code du poste sont requis pour générer le matricule.';
    END IF;

    IF TG_OP = 'INSERT' THEN
        code_provenance := 'RECRUTEMENT';
    ELSIF NEW.poste_id IS DISTINCT FROM OLD.poste_id THEN
        SELECT departement_id INTO ancien_departement_id FROM postes WHERE id = OLD.poste_id;
        SELECT departement_id INTO nouveau_departement_id FROM postes WHERE id = NEW.poste_id;
        code_provenance := CASE WHEN ancien_departement_id = nouveau_departement_id THEN 'PROMOTION' ELSE 'TRANSFERT' END;
    END IF;

    IF code_provenance IS NOT NULL THEN
        SELECT id INTO NEW.provenance_id FROM provenances_employe WHERE code = code_provenance;
        IF NEW.provenance_id IS NULL THEN
            RAISE EXCEPTION 'Employee provenance % is missing from its reference table.', code_provenance;
        END IF;
    END IF;

    NEW.matricule := code_departement || code_poste || LPAD(NEW.id::TEXT, 3, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_generer_matricule_employe ON employes;
CREATE TRIGGER trg_generer_matricule_employe
BEFORE INSERT OR UPDATE ON employes
FOR EACH ROW EXECUTE FUNCTION generer_matricule_employe();

CREATE TABLE IF NOT EXISTS demandes_recrutement (
    id SERIAL PRIMARY KEY,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    demandeur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    motif_id INTEGER NOT NULL REFERENCES motifs_recrutement(id),
    effectif INTEGER NOT NULL CHECK (effectif > 0),
    effectif_recrute INTEGER NOT NULL DEFAULT 0 CHECK (effectif_recrute >= 0),
    budget_salaire_base NUMERIC(14, 2) NOT NULL CHECK (budget_salaire_base > 0),
    justification TEXT NOT NULL,
    date_souhaitee DATE NOT NULL,
    statut_id INTEGER NOT NULL REFERENCES statuts_demande_recrutement(id),
    circuit_id INTEGER NOT NULL REFERENCES circuits_validation_demande(id) ON DELETE RESTRICT,
    soumise_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historique_effectifs_poste (
    id SERIAL PRIMARY KEY,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    ancien_effectif INTEGER NOT NULL CHECK (ancien_effectif >= 0),
    nouvel_effectif INTEGER NOT NULL CHECK (nouvel_effectif >= 0),
    type_evenement_id INTEGER NOT NULL REFERENCES types_evenement_effectif(id) ON DELETE RESTRICT,
    demande_id INTEGER REFERENCES demandes_recrutement(id) ON DELETE RESTRICT,
    acteur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motif TEXT NOT NULL,
    date_effet TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (demande_id, type_evenement_id)
);

-- Journal des décisions du circuit. Les rôles et départements validateurs sont liés
-- aux étapes depuis deux tables dédiées.
CREATE TABLE IF NOT EXISTS validations_demande (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL REFERENCES demandes_recrutement(id) ON DELETE RESTRICT,
    etape_circuit_id INTEGER NOT NULL REFERENCES etapes_circuit_validation(id) ON DELETE RESTRICT,
    validateur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    decision_id INTEGER NOT NULL REFERENCES decisions_validation_demande(id),
    commentaire TEXT,
    decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS offres_emploi (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL UNIQUE REFERENCES demandes_recrutement(id) ON DELETE RESTRICT,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    titre VARCHAR(150) NOT NULL,
    description TEXT,
    competences TEXT,
    statut_id INTEGER NOT NULL REFERENCES statuts_offre_emploi(id),
    date_publication DATE,
    date_cloture DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_offres_dates CHECK (date_publication IS NULL OR date_cloture > date_publication)
);

CREATE TABLE IF NOT EXISTS candidatures (
    id SERIAL PRIMARY KEY,
    offre_id INTEGER NOT NULL REFERENCES offres_emploi(id) ON DELETE RESTRICT,
    candidat_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_candidature(id) ON DELETE RESTRICT,
    cv_path VARCHAR(500) NOT NULL CHECK (cv_path ~ '^cv/cv_[A-Za-z0-9._-]+[.]pdf$'),
    envoyee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (offre_id, candidat_id)
);

CREATE TABLE IF NOT EXISTS historique_candidatures (
    id SERIAL PRIMARY KEY,
    candidature_id INTEGER NOT NULL REFERENCES candidatures(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_candidature(id) ON DELETE RESTRICT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    commentaire TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS entretiens_candidature (
    id SERIAL PRIMARY KEY,
    candidature_id INTEGER NOT NULL REFERENCES candidatures(id) ON DELETE RESTRICT,
    type_id INTEGER NOT NULL REFERENCES types_entretien(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_entretien(id) ON DELETE RESTRICT,
    planifie_at TIMESTAMP NOT NULL,
    note TEXT,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS questionnaires_embauche (
    id SERIAL PRIMARY KEY,
    candidature_id INTEGER NOT NULL UNIQUE REFERENCES candidatures(id) ON DELETE RESTRICT,
    regles_acceptees BOOLEAN NOT NULL,
    date_naissance DATE NOT NULL,
    date_debut_souhaitee DATE NOT NULL,
    salaire_base_souhaite NUMERIC(14, 2) NOT NULL CHECK (salaire_base_souhaite > 0),
    type_contrat_souhaite_id INTEGER NOT NULL REFERENCES types_contrat(id) ON DELETE RESTRICT,
    salaire_base_final NUMERIC(14, 2) CHECK (salaire_base_final IS NULL OR salaire_base_final > 0),
    date_debut_finale DATE,
    type_contrat_final_id INTEGER REFERENCES types_contrat(id) ON DELETE RESTRICT,
    salaire_base_propose_candidat NUMERIC(14, 2) CHECK (salaire_base_propose_candidat IS NULL OR salaire_base_propose_candidat > 0),
    date_debut_proposee_candidat DATE,
    type_contrat_propose_candidat_id INTEGER REFERENCES types_contrat(id) ON DELETE RESTRICT,
    commentaire_candidat TEXT,
    repondu_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS contrats (
    id SERIAL PRIMARY KEY,
    candidature_id INTEGER NOT NULL UNIQUE REFERENCES candidatures(id) ON DELETE RESTRICT,
    numero VARCHAR(40) NOT NULL UNIQUE,
    type_contrat_id INTEGER NOT NULL REFERENCES types_contrat(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_contrat(id) ON DELETE RESTRICT,
    salaire_base_mensuel NUMERIC(14, 2) NOT NULL CHECK (salaire_base_mensuel > 0),
    date_debut DATE NOT NULL,
    date_fin DATE,
    document_path VARCHAR(500) NOT NULL,
    genere_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_contrats_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE TABLE IF NOT EXISTS parcours (
    id SERIAL PRIMARY KEY,
    candidature_id INTEGER NOT NULL REFERENCES candidatures(id) ON DELETE CASCADE,
    type_parcours_id INTEGER NOT NULL REFERENCES types_parcours(id) ON DELETE RESTRICT,
    niveau_etude_id INTEGER REFERENCES niveaux_etude(id) ON DELETE RESTRICT,
    intitule VARCHAR(200) NOT NULL,
    organisme VARCHAR(200) NOT NULL,
    description TEXT,
    ordre INTEGER NOT NULL CHECK (ordre > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS durees (
    id SERIAL PRIMARY KEY,
    parcours_id INTEGER NOT NULL UNIQUE REFERENCES parcours(id) ON DELETE CASCADE,
    date_debut DATE NOT NULL,
    date_fin DATE,
    en_cours BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_durees_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_durees_en_cours CHECK (
        (en_cours = TRUE AND date_fin IS NULL) OR
        (en_cours = FALSE AND date_fin IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    destinataire_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type_id INTEGER NOT NULL REFERENCES types_notifications(id),
    titre VARCHAR(150) NOT NULL,
    resume VARCHAR(500) NOT NULL,
    lien VARCHAR(300) NOT NULL,
    lue_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_administrative_status ON users(administrative_validation_status_id);
CREATE INDEX IF NOT EXISTS idx_postes_departement ON postes(departement_id);
CREATE INDEX IF NOT EXISTS idx_employes_poste ON employes(poste_id);
CREATE INDEX IF NOT EXISTS idx_demandes_departement_statut ON demandes_recrutement(departement_id, statut_id);
CREATE INDEX IF NOT EXISTS idx_demandes_demandeur ON demandes_recrutement(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_validations_demande_historique ON validations_demande(demande_id, decidee_at, id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_validations_demande_etape ON validations_demande(demande_id, etape_circuit_id);
CREATE INDEX IF NOT EXISTS idx_notifications_destinataire_date ON notifications(destinataire_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_non_lues ON notifications(destinataire_id, id) WHERE lue_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_candidatures_rh ON candidatures(statut_id, envoyee_at DESC);
CREATE INDEX IF NOT EXISTS idx_candidatures_candidat ON candidatures(candidat_id, envoyee_at DESC);
CREATE INDEX IF NOT EXISTS idx_parcours_candidature ON parcours(candidature_id, ordre);
CREATE INDEX IF NOT EXISTS idx_historique_candidatures ON historique_candidatures(candidature_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_entretiens_candidature ON entretiens_candidature(candidature_id, id DESC);

-- Bloc 5 : dossier administratif, documents versionnés, historique et demandes de correction.
CREATE TABLE IF NOT EXISTS statuts_administratifs_employe (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS types_documents_employe (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    visible_manager BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS statuts_demande_dossier (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS champs_dossier_modifiables (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS types_evenements_dossier (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS situation_administrative_employe (
    employe_id INTEGER PRIMARY KEY REFERENCES employes(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_administratifs_employe(id) ON DELETE RESTRICT,
    date_effet DATE NOT NULL,
    motif TEXT,
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historique_statut_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_administratifs_employe(id) ON DELETE RESTRICT,
    date_effet DATE NOT NULL,
    motif TEXT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS historique_affectation_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_poste_id INTEGER REFERENCES postes(id) ON DELETE RESTRICT,
    nouveau_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    provenance_id INTEGER NOT NULL REFERENCES provenances_employe(id) ON DELETE RESTRICT,
    date_effet DATE NOT NULL,
    motif TEXT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS informations_personnelles_employe (
    employe_id INTEGER PRIMARY KEY REFERENCES employes(id) ON DELETE RESTRICT,
    adresse_ligne VARCHAR(250),
    ville VARCHAR(120),
    code_postal VARCHAR(20),
    pays VARCHAR(100),
    contact_urgence_nom VARCHAR(150),
    contact_urgence_lien VARCHAR(80),
    contact_urgence_telephone VARCHAR(30),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS documents_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    type_document_id INTEGER NOT NULL REFERENCES types_documents_employe(id) ON DELETE RESTRICT,
    ajoute_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    archive_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS versions_documents_employe (
    id SERIAL PRIMARY KEY,
    document_id INTEGER NOT NULL REFERENCES documents_employe(id) ON DELETE RESTRICT,
    numero_version INTEGER NOT NULL,
    cle_stockage VARCHAR(100) NOT NULL UNIQUE,
    nom_fichier VARCHAR(180) NOT NULL,
    type_mime VARCHAR(80) NOT NULL,
    taille_octets BIGINT NOT NULL,
    date_emission DATE,
    date_expiration DATE,
    ajoute_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (document_id, numero_version)
);

CREATE TABLE IF NOT EXISTS demandes_modification_dossier (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    champ_id INTEGER NOT NULL REFERENCES champs_dossier_modifiables(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_demande_dossier(id) ON DELETE RESTRICT,
    ancienne_valeur TEXT,
    nouvelle_valeur TEXT NOT NULL,
    motif TEXT NOT NULL,
    commentaire_decision TEXT,
    en_cours BOOLEAN NOT NULL DEFAULT TRUE,
    soumise_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decidee_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    decidee_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pieces_justificatives_demande_dossier (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL UNIQUE REFERENCES demandes_modification_dossier(id) ON DELETE RESTRICT,
    cle_stockage VARCHAR(100) NOT NULL UNIQUE,
    nom_fichier VARCHAR(180) NOT NULL,
    type_mime VARCHAR(80) NOT NULL,
    taille_octets BIGINT NOT NULL,
    ajoute_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evenements_dossier_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    type_evenement_id INTEGER NOT NULL REFERENCES types_evenements_dossier(id) ON DELETE RESTRICT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    objet_type VARCHAR(50),
    objet_id INTEGER,
    date_evenement TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_effet DATE,
    motif TEXT,
    resume VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_demande_dossier_champ_ouverte
    ON demandes_modification_dossier(employe_id, champ_id) WHERE en_cours = TRUE;
CREATE INDEX IF NOT EXISTS idx_documents_employe_actifs ON documents_employe(employe_id, type_document_id) WHERE archive_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_versions_documents_latest ON versions_documents_employe(document_id, numero_version DESC);
CREATE INDEX IF NOT EXISTS idx_demandes_dossier_statut_date ON demandes_modification_dossier(statut_id, soumise_at DESC);
CREATE INDEX IF NOT EXISTS idx_demandes_dossier_employe ON demandes_modification_dossier(employe_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evenements_dossier_chrono ON evenements_dossier_employe(employe_id, date_evenement DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_historique_statut_employe_chrono ON historique_statut_employe(employe_id, date_effet DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_historique_affectation_employe_chrono ON historique_affectation_employe(employe_id, date_effet DESC, id DESC);

CREATE OR REPLACE FUNCTION initialiser_situation_administrative_employe()
RETURNS TRIGGER AS $$
DECLARE statut_actif_id INTEGER;
BEGIN
    SELECT id INTO statut_actif_id FROM statuts_administratifs_employe WHERE code = 'ACTIF';
    IF statut_actif_id IS NOT NULL THEN
        INSERT INTO situation_administrative_employe (employe_id, statut_id, date_effet, motif)
        VALUES (NEW.id, statut_actif_id, NEW.date_embauche, 'Création automatique du dossier salarié')
        ON CONFLICT (employe_id) DO NOTHING;
        INSERT INTO historique_statut_employe (employe_id, statut_id, date_effet, motif)
        VALUES (NEW.id, statut_actif_id, NEW.date_embauche, 'Création automatique du dossier salarié');
        INSERT INTO historique_affectation_employe (employe_id, nouveau_poste_id, provenance_id, date_effet, motif)
        VALUES (NEW.id, NEW.poste_id, NEW.provenance_id, NEW.date_embauche, 'Affectation initiale');
        INSERT INTO informations_personnelles_employe (employe_id) VALUES (NEW.id) ON CONFLICT (employe_id) DO NOTHING;
        INSERT INTO evenements_dossier_employe (employe_id, type_evenement_id, objet_type, objet_id, date_effet, resume)
        SELECT NEW.id, type.id, 'EMPLOYE', NEW.id, NEW.date_embauche, 'Création du dossier administratif salarié'
        FROM types_evenements_dossier type WHERE type.code = 'DOSSIER_CREE';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_initialiser_situation_administrative ON employes;
CREATE TRIGGER trg_initialiser_situation_administrative
AFTER INSERT ON employes
FOR EACH ROW EXECUTE FUNCTION initialiser_situation_administrative_employe();

-- Bloc 6 : organisation hebdomadaire du travail et pointage réel.
CREATE TABLE IF NOT EXISTS types_jour_planning (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE,
    compte_effectif BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS statuts_planning_equipe (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_evenement_pointage (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE,
    ordre SMALLINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS statuts_presence_pointage (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE,
    ordre SMALLINT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS niveaux_anomalie_pointage (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_anomalie_pointage (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    niveau_id INTEGER NOT NULL REFERENCES niveaux_anomalie_pointage(id) ON DELETE RESTRICT,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS regles_temps_travail (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    valeur_minutes INTEGER NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    cree_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parametres_horaires_planning (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    pause_debut TIME NOT NULL,
    pause_fin TIME NOT NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_parametre_horaire_coherent CHECK (
        heure_fin > heure_debut AND pause_debut >= heure_debut
        AND pause_fin > pause_debut AND pause_fin <= heure_fin
    )
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_parametres_horaires_planning_actif
    ON parametres_horaires_planning(actif) WHERE actif = TRUE;

CREATE TABLE IF NOT EXISTS plannings_equipe (
    id SERIAL PRIMARY KEY,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    semaine_debut DATE NOT NULL,
    statut_id INTEGER NOT NULL REFERENCES statuts_planning_equipe(id) ON DELETE RESTRICT,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    publie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    publie_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (departement_id, semaine_debut)
);

CREATE TABLE IF NOT EXISTS jours_planning_employe (
    id SERIAL PRIMARY KEY,
    planning_id INTEGER NOT NULL REFERENCES plannings_equipe(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    date_travail DATE NOT NULL,
    type_jour_id INTEGER NOT NULL REFERENCES types_jour_planning(id) ON DELETE RESTRICT,
    heure_debut TIME,
    heure_fin TIME,
    pause_debut TIME,
    pause_fin TIME,
    observation VARCHAR(500),
    modifie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employe_id, date_travail),
    CONSTRAINT ck_jour_planning_horaires_coherents CHECK (
        (heure_debut IS NULL AND heure_fin IS NULL AND pause_debut IS NULL AND pause_fin IS NULL)
        OR (heure_debut IS NOT NULL AND heure_fin IS NOT NULL AND heure_fin > heure_debut
            AND ((pause_debut IS NULL AND pause_fin IS NULL)
                 OR (pause_debut IS NOT NULL AND pause_fin IS NOT NULL
                     AND pause_debut >= heure_debut AND pause_fin > pause_debut AND pause_fin <= heure_fin)))
    )
);

CREATE OR REPLACE FUNCTION valider_jour_planning_employe()
RETURNS TRIGGER AS $$
DECLARE semaine DATE; departement_planning INTEGER; departement_employe INTEGER;
BEGIN
    SELECT semaine_debut, departement_id INTO semaine, departement_planning
    FROM plannings_equipe WHERE id = NEW.planning_id;
    SELECT p.departement_id INTO departement_employe
    FROM employes e JOIN postes p ON p.id = e.poste_id WHERE e.id = NEW.employe_id;
    IF semaine IS NULL OR NEW.date_travail < semaine OR NEW.date_travail >= semaine + 7 THEN
        RAISE EXCEPTION 'La date affectée doit appartenir à la semaine du planning.';
    END IF;
    IF departement_employe IS DISTINCT FROM departement_planning THEN
        RAISE EXCEPTION 'Un planning d’équipe ne peut contenir que des employés de son département.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_valider_jour_planning_employe ON jours_planning_employe;
CREATE TRIGGER trg_valider_jour_planning_employe
BEFORE INSERT OR UPDATE OF planning_id, employe_id, date_travail ON jours_planning_employe
FOR EACH ROW EXECUTE FUNCTION valider_jour_planning_employe();

CREATE TABLE IF NOT EXISTS historiques_jour_planning (
    id SERIAL PRIMARY KEY,
    jour_planning_id INTEGER NOT NULL REFERENCES jours_planning_employe(id) ON DELETE RESTRICT,
    planning_id INTEGER NOT NULL REFERENCES plannings_equipe(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    date_travail DATE NOT NULL,
    ancien_type_jour_id INTEGER REFERENCES types_jour_planning(id) ON DELETE RESTRICT,
    nouveau_type_jour_id INTEGER NOT NULL REFERENCES types_jour_planning(id) ON DELETE RESTRICT,
    ancienne_heure_debut TIME,
    ancienne_heure_fin TIME,
    ancienne_pause_debut TIME,
    ancienne_pause_fin TIME,
    nouvelle_heure_debut TIME,
    nouvelle_heure_fin TIME,
    nouvelle_pause_debut TIME,
    nouvelle_pause_fin TIME,
    observation TEXT,
    modifie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    modifie_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions_pointage (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    jour_planning_id INTEGER REFERENCES jours_planning_employe(id) ON DELETE RESTRICT,
    date_service DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (employe_id, date_service)
);

CREATE TABLE IF NOT EXISTS evenements_pointage (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES sessions_pointage(id) ON DELETE RESTRICT,
    type_id INTEGER NOT NULL REFERENCES types_evenement_pointage(id) ON DELETE RESTRICT,
    horodatage TIMESTAMPTZ NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'ESPACE_EMPLOYE',
    adresse_ip INET,
    empreinte_requete VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (session_id, type_id)
);

CREATE TABLE IF NOT EXISTS anomalies_pointage (
    id SERIAL PRIMARY KEY,
    session_id INTEGER NOT NULL REFERENCES sessions_pointage(id) ON DELETE RESTRICT,
    type_id INTEGER NOT NULL REFERENCES types_anomalie_pointage(id) ON DELETE RESTRICT,
    detectee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details VARCHAR(300) NOT NULL,
    resolue_at TIMESTAMPTZ,
    resolue_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    resolution TEXT,
    UNIQUE (session_id, type_id)
);

CREATE TABLE IF NOT EXISTS historique_statut_planning (
    id SERIAL PRIMARY KEY,
    planning_id INTEGER NOT NULL REFERENCES plannings_equipe(id) ON DELETE RESTRICT,
    ancien_statut_id INTEGER REFERENCES statuts_planning_equipe(id) ON DELETE RESTRICT,
    nouveau_statut_id INTEGER NOT NULL REFERENCES statuts_planning_equipe(id) ON DELETE RESTRICT,
    acteur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motif VARCHAR(300),
    change_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_plannings_equipe_departement_semaine ON plannings_equipe(departement_id, semaine_debut);
CREATE INDEX IF NOT EXISTS idx_jours_planning_date_employe ON jours_planning_employe(date_travail, employe_id);
CREATE INDEX IF NOT EXISTS idx_historique_planning_chrono ON historiques_jour_planning(jour_planning_id, modifie_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_pointage_employe_date ON sessions_pointage(employe_id, date_service DESC);
CREATE INDEX IF NOT EXISTS idx_evenements_pointage_session ON evenements_pointage(session_id, horodatage, id);
CREATE INDEX IF NOT EXISTS idx_anomalies_pointage_session ON anomalies_pointage(session_id, resolue_at);
CREATE INDEX IF NOT EXISTS idx_historique_statut_planning_chrono ON historique_statut_planning(planning_id, change_at DESC, id DESC);

-- Bloc 7 : congés, absences, acquisition des droits et seuils d'effectif.
CREATE TABLE IF NOT EXISTS etapes_validation_conge (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL,
    ordre SMALLINT NOT NULL UNIQUE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS regles_acquisition_conge (
    id SERIAL PRIMARY KEY,
    type_conge_id INTEGER NOT NULL REFERENCES types_conge(id) ON DELETE RESTRICT,
    jours_par_mois NUMERIC(6, 3) NOT NULL CHECK (jours_par_mois >= 0),
    date_debut DATE NOT NULL,
    date_fin DATE,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    modifie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_regle_acquisition_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_regle_acquisition_conge_active
    ON regles_acquisition_conge(type_conge_id) WHERE actif = TRUE AND date_fin IS NULL;

CREATE TABLE IF NOT EXISTS demandes_conge (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    type_conge_id INTEGER NOT NULL REFERENCES types_conge(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_demande_conge(id) ON DELETE RESTRICT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    jours_demandes NUMERIC(7, 2) NOT NULL CHECK (jours_demandes > 0),
    motif TEXT,
    soumise_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decidee_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_demande_conge_dates CHECK (date_fin >= date_debut)
);

CREATE TABLE IF NOT EXISTS justificatifs_demande_conge (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL REFERENCES demandes_conge(id) ON DELETE RESTRICT,
    cle_stockage VARCHAR(120) NOT NULL UNIQUE,
    nom_fichier VARCHAR(180) NOT NULL,
    type_mime VARCHAR(80) NOT NULL,
    taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
    ajoute_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS validations_conge (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL REFERENCES demandes_conge(id) ON DELETE RESTRICT,
    etape_id INTEGER NOT NULL REFERENCES etapes_validation_conge(id) ON DELETE RESTRICT,
    decision_id INTEGER NOT NULL REFERENCES decisions_conge(id) ON DELETE RESTRICT,
    validateur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    commentaire TEXT,
    decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (demande_id, etape_id)
);

CREATE TABLE IF NOT EXISTS mouvements_solde_conge (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    type_conge_id INTEGER NOT NULL REFERENCES types_conge(id) ON DELETE RESTRICT,
    type_mouvement_id INTEGER NOT NULL REFERENCES types_mouvement_solde_conge(id) ON DELETE RESTRICT,
    demande_id INTEGER REFERENCES demandes_conge(id) ON DELETE RESTRICT,
    jours NUMERIC(7, 2) NOT NULL CHECK (jours <> 0),
    periode_debut DATE,
    periode_fin DATE,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    commentaire VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mouvement_conge_periode CHECK (periode_fin IS NULL OR periode_debut IS NULL OR periode_fin >= periode_debut)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_conge_demande_type
    ON mouvements_solde_conge(demande_id, type_mouvement_id) WHERE demande_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mouvement_conge_acquisition_periode
    ON mouvements_solde_conge(employe_id, type_conge_id, periode_fin)
    WHERE demande_id IS NULL AND periode_fin IS NOT NULL;

CREATE TABLE IF NOT EXISTS absences (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    type_absence_id INTEGER NOT NULL REFERENCES types_absence(id) ON DELETE RESTRICT,
    demande_conge_id INTEGER UNIQUE REFERENCES demandes_conge(id) ON DELETE RESTRICT,
    date_debut TIMESTAMPTZ NOT NULL,
    date_fin TIMESTAMPTZ NOT NULL,
    enregistree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motif TEXT,
    justifiee BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_absence_dates CHECK (date_fin > date_debut)
);

CREATE TABLE IF NOT EXISTS justificatifs_absence (
    id SERIAL PRIMARY KEY,
    absence_id INTEGER NOT NULL REFERENCES absences(id) ON DELETE RESTRICT,
    cle_stockage VARCHAR(120) NOT NULL UNIQUE,
    nom_fichier VARCHAR(180) NOT NULL,
    type_mime VARCHAR(80) NOT NULL,
    taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
    ajoute_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_regles_effectif_minimal_date ON regles_effectif_minimal(poste_id, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_historique_effectif_poste_chrono ON historique_effectifs_poste(poste_id, date_effet DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_demandes_conge_employe_date ON demandes_conge(employe_id, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_demandes_conge_statut_date ON demandes_conge(statut_id, soumise_at DESC);
CREATE INDEX IF NOT EXISTS idx_justificatifs_demande_conge ON justificatifs_demande_conge(demande_id);
CREATE INDEX IF NOT EXISTS idx_validations_conge_historique ON validations_conge(demande_id, decidee_at, id);
CREATE INDEX IF NOT EXISTS idx_mouvements_solde_conge_employe ON mouvements_solde_conge(employe_id, type_conge_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_absences_employe_date ON absences(employe_id, date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_justificatifs_absence ON justificatifs_absence(absence_id);

-- Bloc 8 : paie, parametres datés, variables justifiées et bulletins historisés.
-- Les taux/règles ne sont pas codés en dur. L'Administration les paramètre en base.
CREATE TABLE IF NOT EXISTS regimes_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS categories_professionnelles (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS affectations_regime_paie (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    regime_id INTEGER NOT NULL REFERENCES regimes_paie(id) ON DELETE RESTRICT,
    categorie_id INTEGER REFERENCES categories_professionnelles(id) ON DELETE RESTRICT,
    date_debut DATE NOT NULL,
    date_fin DATE,
    modifie_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    motif VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_affectation_regime_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_affectation_regime_active
    ON affectations_regime_paie(employe_id) WHERE date_fin IS NULL;

CREATE TABLE IF NOT EXISTS categories_rubrique_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS modes_calcul_rubrique_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS rubriques_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    categorie_id INTEGER NOT NULL REFERENCES categories_rubrique_paie(id) ON DELETE RESTRICT,
    mode_calcul_id INTEGER NOT NULL REFERENCES modes_calcul_rubrique_paie(id) ON DELETE RESTRICT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_debut DATE NOT NULL,
    date_fin DATE,
    assujettie_irsa BOOLEAN NOT NULL DEFAULT FALSE,
    assujettie_cnaps BOOLEAN NOT NULL DEFAULT FALSE,
    assujettie_organisme_sante BOOLEAN NOT NULL DEFAULT FALSE,
    saisie_manuelle_autorisee BOOLEAN NOT NULL DEFAULT FALSE,
    source_regle VARCHAR(300) NOT NULL,
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_rubrique_paie_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);

CREATE TABLE IF NOT EXISTS parametres_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL,
    libelle VARCHAR(180) NOT NULL,
    valeur NUMERIC(16, 6) NOT NULL,
    unite VARCHAR(40) NOT NULL,
    regime_id INTEGER REFERENCES regimes_paie(id) ON DELETE RESTRICT,
    date_debut DATE NOT NULL,
    date_fin DATE,
    source_regle VARCHAR(300) NOT NULL,
    a_verifier BOOLEAN NOT NULL DEFAULT FALSE,
    motif_modification VARCHAR(500),
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_parametre_paie_dates CHECK (date_fin IS NULL OR date_fin >= date_debut)
);
CREATE INDEX IF NOT EXISTS idx_parametres_paie_code_dates
    ON parametres_paie(code, regime_id, date_debut, date_fin);

CREATE TABLE IF NOT EXISTS tranches_irsa (
    id SERIAL PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE,
    borne_inferieure NUMERIC(14, 2) NOT NULL,
    borne_superieure NUMERIC(14, 2),
    taux NUMERIC(9, 6) NOT NULL,
    source_regle VARCHAR(300) NOT NULL,
    motif_modification VARCHAR(500),
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tranche_irsa_dates CHECK (date_fin IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_tranche_irsa_bornes CHECK (borne_superieure IS NULL OR borne_superieure > borne_inferieure),
    CONSTRAINT ck_tranche_irsa_taux CHECK (taux >= 0 AND taux <= 1)
);
CREATE INDEX IF NOT EXISTS idx_tranches_irsa_dates ON tranches_irsa(date_debut, date_fin, borne_inferieure);

CREATE TABLE IF NOT EXISTS statuts_periode_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS periodes_paie (
    id SERIAL PRIMARY KEY,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut_id INTEGER NOT NULL REFERENCES statuts_periode_paie(id) ON DELETE RESTRICT,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    valide_at TIMESTAMPTZ,
    cloture_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date_debut, date_fin),
    CONSTRAINT ck_periode_paie_dates CHECK (date_fin >= date_debut)
);

CREATE TABLE IF NOT EXISTS statuts_variable_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS statuts_bulletin_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS statuts_avance_paie (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS avances_salariales (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    date_avance DATE NOT NULL,
    montant NUMERIC(14, 2) NOT NULL CHECK (montant > 0),
    statut_id INTEGER NOT NULL REFERENCES statuts_avance_paie(id) ON DELETE RESTRICT,
    periode_deduction_id INTEGER REFERENCES periodes_paie(id) ON DELETE RESTRICT,
    saisi_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    valide_at TIMESTAMPTZ,
    motif VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS variables_paie (
    id SERIAL PRIMARY KEY,
    periode_id INTEGER NOT NULL REFERENCES periodes_paie(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    rubrique_id INTEGER NOT NULL REFERENCES rubriques_paie(id) ON DELETE RESTRICT,
    quantite NUMERIC(12, 3) NOT NULL DEFAULT 1,
    montant_unitaire NUMERIC(14, 2),
    montant NUMERIC(14, 2) NOT NULL,
    session_pointage_id INTEGER REFERENCES sessions_pointage(id) ON DELETE RESTRICT,
    absence_id INTEGER REFERENCES absences(id) ON DELETE RESTRICT,
    avance_id INTEGER REFERENCES avances_salariales(id) ON DELETE RESTRICT,
    source_detail TEXT NOT NULL,
    statut_id INTEGER NOT NULL REFERENCES statuts_variable_paie(id) ON DELETE RESTRICT,
    saisi_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    valide_at TIMESTAMPTZ,
    motif_decision VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bulletins_paie (
    id SERIAL PRIMARY KEY,
    periode_id INTEGER NOT NULL REFERENCES periodes_paie(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    contrat_id INTEGER REFERENCES contrats(id) ON DELETE RESTRICT,
    matricule_snapshot VARCHAR(30) NOT NULL,
    nom_snapshot VARCHAR(150) NOT NULL,
    categorie_snapshot VARCHAR(120),
    fonction_snapshot VARCHAR(150) NOT NULL,
    salaire_base_contractuel NUMERIC(14, 2) NOT NULL,
    salaire_base_proratise NUMERIC(14, 2) NOT NULL,
    salaire_brut NUMERIC(14, 2) NOT NULL,
    revenu_imposable NUMERIC(14, 2) NOT NULL,
    total_retenues_salariales NUMERIC(14, 2) NOT NULL,
    salaire_net NUMERIC(14, 2) NOT NULL,
    total_charges_patronales NUMERIC(14, 2) NOT NULL,
    net_a_payer NUMERIC(14, 2) NOT NULL,
    ordre_paiement VARCHAR(80),
    statut_id INTEGER NOT NULL REFERENCES statuts_bulletin_paie(id) ON DELETE RESTRICT,
    calcule_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    valide_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (periode_id, employe_id)
);

CREATE TABLE IF NOT EXISTS lignes_bulletin_paie (
    id SERIAL PRIMARY KEY,
    bulletin_id INTEGER NOT NULL REFERENCES bulletins_paie(id) ON DELETE RESTRICT,
    rubrique_id INTEGER REFERENCES rubriques_paie(id) ON DELETE RESTRICT,
    code_snapshot VARCHAR(40) NOT NULL,
    libelle_snapshot VARCHAR(150) NOT NULL,
    categorie_snapshot VARCHAR(100) NOT NULL,
    quantite NUMERIC(12, 3) NOT NULL DEFAULT 1,
    base_calcul NUMERIC(14, 2),
    taux_applique NUMERIC(10, 6),
    montant NUMERIC(14, 2) NOT NULL,
    variable_id INTEGER REFERENCES variables_paie(id) ON DELETE RESTRICT,
    session_pointage_id INTEGER REFERENCES sessions_pointage(id) ON DELETE RESTRICT,
    absence_id INTEGER REFERENCES absences(id) ON DELETE RESTRICT,
    avance_id INTEGER REFERENCES avances_salariales(id) ON DELETE RESTRICT,
    source_snapshot TEXT NOT NULL,
    ordre SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bulletin_id, ordre)
);

CREATE INDEX IF NOT EXISTS idx_bulletins_paie_periode ON bulletins_paie(periode_id, matricule_snapshot);
CREATE INDEX IF NOT EXISTS idx_lignes_bulletin_paie_bulletin ON lignes_bulletin_paie(bulletin_id, ordre);
CREATE INDEX IF NOT EXISTS idx_variables_paie_periode_employe ON variables_paie(periode_id, employe_id);
CREATE INDEX IF NOT EXISTS idx_avances_paie_employe_date ON avances_salariales(employe_id, date_avance DESC);

