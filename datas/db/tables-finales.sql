-- Schéma consolidé SIRH (base vide), blocs 1 à 12.

-- Référentiels en tables dédiées, clés SERIAL et FK explicites.

BEGIN;

CREATE TABLE IF NOT EXISTS types_mouvement_carriere (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(120) NOT NULL UNIQUE,
    categorie VARCHAR(20) NOT NULL CHECK (categorie IN ('AFFECTATION', 'SALAIRE', 'MIXTE')),
    ordre SMALLINT NOT NULL DEFAULT 0,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

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
    niveau_min SMALLINT NOT NULL DEFAULT 0,
    niveau_max SMALLINT NOT NULL DEFAULT 5,
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
    type_mouvement_id INTEGER REFERENCES types_mouvement_carriere(id) ON DELETE RESTRICT,
    ancien_departement_id INTEGER REFERENCES departements(id) ON DELETE RESTRICT,
    nouveau_departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
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



CREATE TABLE IF NOT EXISTS statuts_procedure_disciplinaire (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS gravites_incident (
    id SERIAL PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    libelle VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS types_sanction (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(100) NOT NULL UNIQUE,
    retenue_salaire BOOLEAN NOT NULL DEFAULT FALSE,
    necessite_periode BOOLEAN NOT NULL DEFAULT FALSE,
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS parametres_discipline (
    id SERIAL PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    libelle VARCHAR(180) NOT NULL,
    valeur VARCHAR(60) NOT NULL,
    unite VARCHAR(30) NOT NULL,
    modifie_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_parametres_discipline_valeur CHECK (CASE code WHEN 'MODE_COMPTAGE_RETENUE' THEN valeur IN ('CALENDAIRE', 'OUVRE') WHEN 'DELAI_REPONSE_JOURS' THEN valeur ~ '^[1-9][0-9]{0,2}$' WHEN 'DIVISEUR_TAUX_JOURNALIER' THEN valeur ~ '^[1-9][0-9]{0,2}$' ELSE TRUE END)
);

CREATE TABLE IF NOT EXISTS incidents (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    declarant_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    gravite_id INTEGER NOT NULL REFERENCES gravites_incident(id) ON DELETE RESTRICT,
    titre VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    date_faits DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_incidents_titre CHECK (LENGTH(BTRIM(titre)) > 0),
    CONSTRAINT ck_incidents_description CHECK (LENGTH(BTRIM(description)) > 0)
);

CREATE TABLE IF NOT EXISTS procedures_disciplinaires (
    id SERIAL PRIMARY KEY,
    incident_id INTEGER NOT NULL UNIQUE REFERENCES incidents(id) ON DELETE RESTRICT,
    statut_id INTEGER NOT NULL REFERENCES statuts_procedure_disciplinaire(id) ON DELETE RESTRICT,
    ouverte_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ouverte_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cloturee_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS demandes_explication (
    id SERIAL PRIMARY KEY,
    procedure_id INTEGER NOT NULL UNIQUE REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,
    message TEXT NOT NULL,
    date_limite DATE NOT NULL,
    demandee_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    demandee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_demande_explication_message CHECK (LENGTH(BTRIM(message)) > 0)
);

CREATE TABLE IF NOT EXISTS reponses_explication (
    id SERIAL PRIMARY KEY,
    demande_id INTEGER NOT NULL UNIQUE REFERENCES demandes_explication(id) ON DELETE RESTRICT,
    texte TEXT NOT NULL,
    repondue_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    repondue_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_reponse_explication_texte CHECK (LENGTH(BTRIM(texte)) > 0)
);

CREATE TABLE IF NOT EXISTS sanctions (
    id SERIAL PRIMARY KEY,
    procedure_id INTEGER NOT NULL UNIQUE REFERENCES procedures_disciplinaires(id) ON DELETE RESTRICT,
    type_id INTEGER NOT NULL REFERENCES types_sanction(id) ON DELETE RESTRICT,
    motif TEXT NOT NULL,
    date_debut DATE,
    date_fin DATE,
    nombre_jours INTEGER,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    decidee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sanctions_motif CHECK (LENGTH(BTRIM(motif)) > 0),
    CONSTRAINT ck_sanctions_periode CHECK ((date_debut IS NULL AND date_fin IS NULL AND nombre_jours IS NULL) OR (date_debut IS NOT NULL AND date_fin IS NOT NULL AND date_fin >= date_debut AND nombre_jours IS NOT NULL AND nombre_jours > 0 AND nombre_jours <= (date_fin - date_debut + 1)))
);

CREATE TABLE IF NOT EXISTS pieces_disciplinaires (
    id SERIAL PRIMARY KEY,
    incident_id INTEGER NOT NULL REFERENCES incidents(id) ON DELETE RESTRICT,
    reponse_id INTEGER REFERENCES reponses_explication(id) ON DELETE RESTRICT,
    cle_stockage VARCHAR(500) NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    type_mime VARCHAR(100) NOT NULL,
    taille_octets BIGINT NOT NULL CHECK (taille_octets > 0),
    ajoutee_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    ajoutee_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pieces_disciplinaires_fichiers CHECK (
        LENGTH(BTRIM(cle_stockage)) > 0
        AND LENGTH(BTRIM(nom_fichier)) > 0
        AND LENGTH(BTRIM(type_mime)) > 0
    )
);

CREATE TABLE IF NOT EXISTS journal_acces_discipline (
    id SERIAL PRIMARY KEY,
    utilisateur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    origine VARCHAR(12) NOT NULL DEFAULT 'UTILISATEUR',
    action VARCHAR(20) NOT NULL,
    objet_type VARCHAR(40) NOT NULL,
    objet_id INTEGER,
    employe_id INTEGER REFERENCES employes(id) ON DELETE RESTRICT,
    detail VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_journal_discipline_action CHECK (
        action IN ('LECTURE', 'CREATION', 'MODIFICATION', 'DECISION', 'TELECHARGEMENT')
    ),
    CONSTRAINT ck_journal_discipline_informations CHECK (LENGTH(BTRIM(objet_type)) > 0 AND (detail IS NULL OR LENGTH(BTRIM(detail)) > 0)),
    CONSTRAINT ck_journal_discipline_origine CHECK (origine IN ('UTILISATEUR','SYSTEME') AND (origine='SYSTEME' OR utilisateur_id IS NOT NULL))
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
    sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT,
    source_detail TEXT NOT NULL,
    statut_id INTEGER NOT NULL REFERENCES statuts_variable_paie(id) ON DELETE RESTRICT,
    saisi_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    valide_at TIMESTAMPTZ,
    motif_decision VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_variables_paie_id_sanction UNIQUE(id, sanction_id)
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
    sanction_id INTEGER REFERENCES sanctions(id) ON DELETE RESTRICT,
    source_snapshot TEXT NOT NULL,
    ordre SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bulletin_id, ordre)
);

CREATE TABLE IF NOT EXISTS retenues_sanction (
    id SERIAL PRIMARY KEY,
    sanction_id INTEGER NOT NULL REFERENCES sanctions(id) ON DELETE RESTRICT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    nombre_jours INTEGER NOT NULL CHECK (nombre_jours > 0),
    taux_journalier NUMERIC(14, 2) NOT NULL CHECK (taux_journalier >= 0),
    montant NUMERIC(14, 2) NOT NULL CHECK (montant >= 0),
    variable_paie_id INTEGER UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_retenues_sanction_dates CHECK (date_fin >= date_debut),
    CONSTRAINT ck_retenues_sanction_jours CHECK (
        nombre_jours <= (date_fin - date_debut + 1)
    ),
    CONSTRAINT ck_retenues_sanction_montant CHECK (montant = ROUND(taux_journalier * nombre_jours, 2)),
    CONSTRAINT fk_retenues_sanction_variable_sanction FOREIGN KEY(variable_paie_id,sanction_id) REFERENCES variables_paie(id,sanction_id) ON DELETE RESTRICT
);



CREATE INDEX IF NOT EXISTS idx_bulletins_paie_periode ON bulletins_paie(periode_id, matricule_snapshot);

CREATE INDEX IF NOT EXISTS idx_lignes_bulletin_paie_bulletin ON lignes_bulletin_paie(bulletin_id, ordre);

CREATE INDEX IF NOT EXISTS idx_variables_paie_periode_employe ON variables_paie(periode_id, employe_id);

CREATE INDEX IF NOT EXISTS idx_avances_paie_employe_date ON avances_salariales(employe_id, date_avance DESC);

CREATE TABLE IF NOT EXISTS competences (
    id SERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    libelle VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_competences_code CHECK (code = UPPER(code) AND code ~ '^[A-Z0-9_]{2,40}$')
);

CREATE TABLE IF NOT EXISTS criteres_poste (
    id SERIAL PRIMARY KEY,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL DEFAULT 1,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire VARCHAR(1000),
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (poste_id, competence_id),
    CONSTRAINT ck_criteres_poste_niveaux CHECK (niveau_attendu >= 0),
    CONSTRAINT ck_criteres_poste_seuil CHECK (seuil_ecart >= 0)
);

CREATE TABLE IF NOT EXISTS evaluations_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    evaluateur_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    valide_par_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    date_evaluation DATE NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'SOUMISE',
    commentaire VARCHAR(1500),
    bareme_min SMALLINT NOT NULL,
    bareme_max SMALLINT NOT NULL,
    soumise_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validee_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_evaluations_statut CHECK (statut IN ('SOUMISE', 'VALIDEE')),
    CONSTRAINT ck_evaluations_bareme CHECK (bareme_min >= 0 AND bareme_max > bareme_min),
    CONSTRAINT ck_evaluations_validation CHECK (
        (statut = 'SOUMISE' AND valide_par_id IS NULL AND validee_at IS NULL)
        OR (statut = 'VALIDEE' AND valide_par_id IS NOT NULL AND validee_at IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS evaluation_lignes (
    id SERIAL PRIMARY KEY,
    evaluation_id INTEGER NOT NULL REFERENCES evaluations_employe(id) ON DELETE RESTRICT,
    critere_id INTEGER NOT NULL REFERENCES criteres_poste(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    niveau_acquis SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL,
    ecart SMALLINT NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT TRUE,
    commentaire VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (evaluation_id, critere_id),
    CONSTRAINT ck_evaluation_lignes_niveaux CHECK (niveau_attendu >= 0 AND niveau_acquis >= 0),
    CONSTRAINT ck_evaluation_lignes_seuil CHECK (seuil_ecart >= 0),
    CONSTRAINT ck_evaluation_lignes_ecart CHECK (ecart = GREATEST(0, niveau_attendu - niveau_acquis))
);

CREATE TABLE IF NOT EXISTS besoins_formation (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    evaluation_ligne_id INTEGER NOT NULL UNIQUE REFERENCES evaluation_lignes(id) ON DELETE RESTRICT,
    niveau_attendu SMALLINT NOT NULL,
    niveau_acquis_initial SMALLINT NOT NULL,
    seuil_ecart SMALLINT NOT NULL,
    ecart_initial SMALLINT NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'OUVERT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cloture_at TIMESTAMPTZ,
    CONSTRAINT ck_besoins_formation_statut CHECK (statut IN ('OUVERT', 'PLANIFIE', 'CLOTURE', 'ANNULE')),
    CONSTRAINT ck_besoins_formation_ecart CHECK (ecart_initial >= seuil_ecart),
    CONSTRAINT ck_besoins_formation_niveaux CHECK (niveau_attendu >= 0 AND niveau_acquis_initial >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_besoin_formation_actif
    ON besoins_formation (employe_id, competence_id)
    WHERE statut IN ('OUVERT', 'PLANIFIE');

CREATE TABLE IF NOT EXISTS formations (
    id SERIAL PRIMARY KEY,
    competence_id INTEGER NOT NULL REFERENCES competences(id) ON DELETE RESTRICT,
    titre VARCHAR(200) NOT NULL,
    description VARCHAR(1500),
    organisme VARCHAR(150),
    date_debut DATE,
    date_fin DATE,
    niveau_cible SMALLINT,
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIEE',
    cree_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_formations_statut CHECK (statut IN ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE')),
    CONSTRAINT ck_formations_dates CHECK (date_fin IS NULL OR date_debut IS NULL OR date_fin >= date_debut),
    CONSTRAINT ck_formations_niveau_cible CHECK (niveau_cible IS NULL OR niveau_cible >= 0)
);

CREATE TABLE IF NOT EXISTS participations_formation (
    id SERIAL PRIMARY KEY,
    formation_id INTEGER NOT NULL REFERENCES formations(id) ON DELETE RESTRICT,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    besoin_id INTEGER NOT NULL REFERENCES besoins_formation(id) ON DELETE RESTRICT,
    statut VARCHAR(20) NOT NULL DEFAULT 'INSCRIT',
    inscrit_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    termine_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (formation_id, employe_id),
    CONSTRAINT ck_participations_statut CHECK (statut IN ('INSCRIT', 'TERMINE', 'ABANDONNE'))
);

CREATE TABLE IF NOT EXISTS mesures_formation (
    id SERIAL PRIMARY KEY,
    participation_id INTEGER NOT NULL UNIQUE REFERENCES participations_formation(id) ON DELETE RESTRICT,
    niveau_avant SMALLINT NOT NULL,
    niveau_apres SMALLINT NOT NULL,
    manque SMALLINT NOT NULL,
    acquis SMALLINT NOT NULL,
    pourcentage NUMERIC(5, 2) NOT NULL,
    mesure_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    mesure_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    commentaire VARCHAR(1000),
    CONSTRAINT ck_mesures_niveaux CHECK (niveau_avant >= 0 AND niveau_apres >= 0),
    CONSTRAINT ck_mesures_manque CHECK (manque >= 0),
    CONSTRAINT ck_mesures_acquis CHECK (acquis >= 0 AND acquis <= manque),
    CONSTRAINT ck_mesures_pourcentage CHECK (pourcentage >= 0 AND pourcentage <= 100)
);

CREATE INDEX IF NOT EXISTS idx_competences_actif ON competences(actif, libelle);

CREATE INDEX IF NOT EXISTS idx_criteres_poste_poste ON criteres_poste(poste_id, competence_id);

CREATE INDEX IF NOT EXISTS idx_evaluations_employe_employe_date ON evaluations_employe(employe_id, date_evaluation DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_evaluations_employe_statut ON evaluations_employe(statut, date_evaluation DESC);

CREATE INDEX IF NOT EXISTS idx_evaluation_lignes_evaluation ON evaluation_lignes(evaluation_id);

CREATE INDEX IF NOT EXISTS idx_besoins_formation_statut ON besoins_formation(statut, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_besoins_formation_employe ON besoins_formation(employe_id, competence_id);

CREATE INDEX IF NOT EXISTS idx_formations_competence_statut ON formations(competence_id, statut, date_debut DESC);

CREATE INDEX IF NOT EXISTS idx_participations_formation_employe ON participations_formation(employe_id, statut);

CREATE INDEX IF NOT EXISTS idx_participations_formation_formation ON participations_formation(formation_id, statut);

CREATE TABLE IF NOT EXISTS historique_salaire_employe (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) CHECK (ancien_salaire_base IS NULL OR ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    type_mouvement_id INTEGER NOT NULL REFERENCES types_mouvement_carriere(id) ON DELETE RESTRICT,
    contrat_id INTEGER REFERENCES contrats(id) ON DELETE RESTRICT,
    date_effet DATE NOT NULL,
    motif TEXT,
    acteur_id INTEGER REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS promotions (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    nouveau_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) NOT NULL CHECK (ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    date_effet DATE NOT NULL,
    motif TEXT NOT NULL,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    historique_affectation_id INTEGER NOT NULL UNIQUE REFERENCES historique_affectation_employe(id) ON DELETE RESTRICT,
    historique_salaire_id INTEGER NOT NULL UNIQUE REFERENCES historique_salaire_employe(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_promotions_postes CHECK (ancien_poste_id <> nouveau_poste_id),
    CONSTRAINT ck_promotions_salaire CHECK (nouveau_salaire_base > ancien_salaire_base)
);

CREATE TABLE IF NOT EXISTS mutations (
    id SERIAL PRIMARY KEY,
    employe_id INTEGER NOT NULL REFERENCES employes(id) ON DELETE RESTRICT,
    ancien_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    nouveau_poste_id INTEGER NOT NULL REFERENCES postes(id) ON DELETE RESTRICT,
    ancien_departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    nouveau_departement_id INTEGER NOT NULL REFERENCES departements(id) ON DELETE RESTRICT,
    ancien_salaire_base NUMERIC(14, 2) NOT NULL CHECK (ancien_salaire_base > 0),
    nouveau_salaire_base NUMERIC(14, 2) NOT NULL CHECK (nouveau_salaire_base > 0),
    date_effet DATE NOT NULL,
    motif TEXT NOT NULL,
    decide_par_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    historique_affectation_id INTEGER NOT NULL UNIQUE REFERENCES historique_affectation_employe(id) ON DELETE RESTRICT,
    historique_salaire_id INTEGER UNIQUE REFERENCES historique_salaire_employe(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_mutations_postes CHECK (ancien_poste_id <> nouveau_poste_id),
    CONSTRAINT ck_mutations_departements CHECK (ancien_departement_id <> nouveau_departement_id)
);

CREATE INDEX IF NOT EXISTS idx_historique_salaire_employe_chrono
    ON historique_salaire_employe(employe_id, date_effet DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_promotions_employe_chrono
    ON promotions(employe_id, date_effet DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_mutations_employe_chrono
    ON mutations(employe_id, date_effet DESC, id DESC);

CREATE OR REPLACE FUNCTION initialiser_historique_salaire_employe()
RETURNS TRIGGER AS $$
DECLARE
    employe_cible_id INTEGER;
    date_embauche_employe DATE;
BEGIN
    SELECT e.id, e.date_embauche
      INTO employe_cible_id, date_embauche_employe
      FROM candidatures c
      JOIN employes e ON e.user_id = c.candidat_id
     WHERE c.id = NEW.candidature_id;

    IF employe_cible_id IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM historique_salaire_employe WHERE employe_id = employe_cible_id) THEN
        INSERT INTO historique_salaire_employe
            (employe_id, nouveau_salaire_base, type_mouvement_id, contrat_id, date_effet, motif)
        SELECT employe_cible_id, NEW.salaire_base_mensuel, t.id, NEW.id,
               GREATEST(NEW.date_debut, date_embauche_employe),
               'Salaire initial repris du contrat'
        FROM types_mouvement_carriere t
        WHERE t.code = 'SALAIRE_INITIAL';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_initialiser_historique_salaire ON contrats;

CREATE TRIGGER trg_initialiser_historique_salaire
AFTER INSERT ON contrats
FOR EACH ROW EXECUTE FUNCTION initialiser_historique_salaire_employe();

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
        INSERT INTO historique_affectation_employe
            (employe_id, nouveau_poste_id, nouveau_departement_id, provenance_id,
             type_mouvement_id, date_effet, motif)
        SELECT NEW.id, NEW.poste_id, p.departement_id, NEW.provenance_id,
               t.id, NEW.date_embauche, 'Affectation initiale'
        FROM postes p
        JOIN types_mouvement_carriere t ON t.code = 'AFFECTATION_INITIALE'
        WHERE p.id = NEW.poste_id;
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

CREATE OR REPLACE FUNCTION valider_chronologie_affectation_employe()
RETURNS TRIGGER AS $$
DECLARE
    date_embauche_employe DATE;
    derniere_date DATE;
BEGIN
    SELECT date_embauche INTO date_embauche_employe FROM employes WHERE id = NEW.employe_id;
    IF date_embauche_employe IS NULL THEN
        RAISE EXCEPTION 'Salarie % introuvable pour l''historique d''affectation.', NEW.employe_id;
    END IF;
    IF NEW.date_effet < date_embauche_employe THEN
        RAISE EXCEPTION 'La date d''effet % precede la date d''embauche %.', NEW.date_effet, date_embauche_employe;
    END IF;
    SELECT MAX(date_effet) INTO derniere_date
      FROM historique_affectation_employe
     WHERE employe_id = NEW.employe_id;
    IF derniere_date IS NOT NULL AND NEW.date_effet <= derniere_date THEN
        RAISE EXCEPTION 'La date d''effet % doit etre strictement posterieure au dernier mouvement d''affectation %.',
            NEW.date_effet, derniere_date;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION valider_chronologie_historique_salaire()
RETURNS TRIGGER AS $$
DECLARE
    date_embauche_employe DATE;
    derniere_date DATE;
BEGIN
    SELECT date_embauche INTO date_embauche_employe FROM employes WHERE id = NEW.employe_id;
    IF date_embauche_employe IS NULL THEN
        RAISE EXCEPTION 'Salarie % introuvable pour l''historique de salaire.', NEW.employe_id;
    END IF;
    IF NEW.date_effet < date_embauche_employe THEN
        RAISE EXCEPTION 'La date d''effet % precede la date d''embauche %.', NEW.date_effet, date_embauche_employe;
    END IF;
    SELECT MAX(date_effet) INTO derniere_date
      FROM historique_salaire_employe
     WHERE employe_id = NEW.employe_id;
    IF derniere_date IS NOT NULL AND NEW.date_effet <= derniere_date THEN
        RAISE EXCEPTION 'La date d''effet % doit etre strictement posterieure au dernier salaire historise %.',
            NEW.date_effet, derniere_date;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_valider_chronologie_affectation ON historique_affectation_employe;

CREATE TRIGGER trg_valider_chronologie_affectation
BEFORE INSERT ON historique_affectation_employe
FOR EACH ROW EXECUTE FUNCTION valider_chronologie_affectation_employe();

DROP TRIGGER IF EXISTS trg_valider_chronologie_salaire ON historique_salaire_employe;

CREATE TRIGGER trg_valider_chronologie_salaire
BEFORE INSERT ON historique_salaire_employe
FOR EACH ROW EXECUTE FUNCTION valider_chronologie_historique_salaire();

CREATE OR REPLACE FUNCTION empecher_modification_historique_carriere()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Les historiques de carriere sont en lecture seule. Creez un nouveau mouvement au lieu de modifier ou supprimer une ligne.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_historique_affectation_immuable ON historique_affectation_employe;

CREATE TRIGGER trg_historique_affectation_immuable
BEFORE UPDATE OR DELETE ON historique_affectation_employe
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_historique_salaire_immuable ON historique_salaire_employe;

CREATE TRIGGER trg_historique_salaire_immuable
BEFORE UPDATE OR DELETE ON historique_salaire_employe
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_promotion_immuable ON promotions;

CREATE TRIGGER trg_promotion_immuable
BEFORE UPDATE OR DELETE ON promotions
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

DROP TRIGGER IF EXISTS trg_mutation_immuable ON mutations;

CREATE TRIGGER trg_mutation_immuable
BEFORE UPDATE OR DELETE ON mutations
FOR EACH ROW EXECUTE FUNCTION empecher_modification_historique_carriere();

CREATE OR REPLACE FUNCTION fn_valider_sanction() RETURNS trigger AS $$
DECLARE
    v_necessite BOOLEAN;
    v_code VARCHAR(40);
BEGIN
    SELECT necessite_periode, code INTO v_necessite, v_code
    FROM types_sanction WHERE id = NEW.type_id;

    IF v_necessite AND (NEW.date_debut IS NULL OR NEW.date_fin IS NULL OR NEW.nombre_jours IS NULL) THEN
        RAISE EXCEPTION 'La sanction % exige une date de debut, une date de fin et un nombre de jours.', v_code
            USING ERRCODE = 'check_violation';
    END IF;
    IF NOT v_necessite AND (NEW.date_debut IS NOT NULL OR NEW.date_fin IS NOT NULL OR NEW.nombre_jours IS NOT NULL) THEN
        RAISE EXCEPTION 'La sanction % ne comporte pas de periode.', v_code
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sanctions_valider ON sanctions;

CREATE TRIGGER trg_sanctions_valider
    BEFORE INSERT OR UPDATE ON sanctions
    FOR EACH ROW EXECUTE FUNCTION fn_valider_sanction();

CREATE OR REPLACE FUNCTION fn_valider_piece_disciplinaire() RETURNS trigger AS $$
DECLARE
    v_incident INTEGER;
BEGIN
    IF NEW.reponse_id IS NULL THEN
        RETURN NEW;
    END IF;
    SELECT pr.incident_id INTO v_incident
    FROM reponses_explication r
    JOIN demandes_explication d ON d.id = r.demande_id
    JOIN procedures_disciplinaires pr ON pr.id = d.procedure_id
    WHERE r.id = NEW.reponse_id;

    IF v_incident IS DISTINCT FROM NEW.incident_id THEN
        RAISE EXCEPTION 'La reponse % n''appartient pas a l''incident %.', NEW.reponse_id, NEW.incident_id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_pieces_disciplinaires_valider ON pieces_disciplinaires;

CREATE TRIGGER trg_pieces_disciplinaires_valider
    BEFORE INSERT OR UPDATE OF incident_id, reponse_id ON pieces_disciplinaires
    FOR EACH ROW EXECUTE FUNCTION fn_valider_piece_disciplinaire();

CREATE OR REPLACE FUNCTION fn_valider_retenue_sanction() RETURNS trigger AS $$
DECLARE
    s RECORD;
    v RECORD;
    v_mode VARCHAR(60);
    v_duree INTEGER;
BEGIN
    PERFORM pg_advisory_xact_lock(NEW.sanction_id);
    SELECT sa.date_debut, sa.date_fin, ts.retenue_salaire, ts.code, i.employe_id
    INTO s
    FROM sanctions sa
    JOIN types_sanction ts ON ts.id = sa.type_id
    JOIN procedures_disciplinaires pr ON pr.id = sa.procedure_id
    JOIN incidents i ON i.id = pr.incident_id
    WHERE sa.id = NEW.sanction_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Sanction % introuvable.', NEW.sanction_id USING ERRCODE = 'foreign_key_violation';
    END IF;
    IF NOT s.retenue_salaire OR s.date_debut IS NULL THEN
        RAISE EXCEPTION 'La sanction % n''entraine aucune retenue de salaire.', s.code
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.date_debut < s.date_debut OR NEW.date_fin > s.date_fin THEN
        RAISE EXCEPTION 'La tranche % - % depasse la periode de la sanction (% - %).',
            NEW.date_debut, NEW.date_fin, s.date_debut, s.date_fin
            USING ERRCODE = 'check_violation';
    END IF;

    v_duree := NEW.date_fin - NEW.date_debut + 1;
    SELECT valeur INTO v_mode FROM parametres_discipline WHERE code = 'MODE_COMPTAGE_RETENUE';
    IF COALESCE(v_mode, 'CALENDAIRE') = 'CALENDAIRE' AND NEW.nombre_jours <> v_duree THEN
        RAISE EXCEPTION 'En comptage calendaire, la tranche % - % compte % jour(s), pas %.',
            NEW.date_debut, NEW.date_fin, v_duree, NEW.nombre_jours
            USING ERRCODE = 'check_violation';
    END IF;

    IF EXISTS (
        SELECT 1 FROM retenues_sanction r
        WHERE r.sanction_id = NEW.sanction_id
          AND r.id IS DISTINCT FROM NEW.id
          AND r.date_debut <= NEW.date_fin
          AND r.date_fin >= NEW.date_debut
    ) THEN
        RAISE EXCEPTION 'La tranche % - % chevauche une autre retenue de la sanction %.',
            NEW.date_debut, NEW.date_fin, NEW.sanction_id
            USING ERRCODE = 'exclusion_violation';
    END IF;

    IF NEW.variable_paie_id IS NOT NULL THEN
        SELECT vp.employe_id, vp.montant, r.code AS rubrique
        INTO v
        FROM variables_paie vp
        JOIN rubriques_paie r ON r.id = vp.rubrique_id
        WHERE vp.id = NEW.variable_paie_id;

        IF v.rubrique IS DISTINCT FROM 'RETENUE_MISE_A_PIED' THEN
            RAISE EXCEPTION 'La variable de paie % n''est pas une retenue de mise a pied.', NEW.variable_paie_id
                USING ERRCODE = 'check_violation';
        END IF;
        IF v.employe_id IS DISTINCT FROM s.employe_id THEN
            RAISE EXCEPTION 'La variable de paie % concerne un autre employe que la sanction.', NEW.variable_paie_id
                USING ERRCODE = 'check_violation';
        END IF;
        IF v.montant IS DISTINCT FROM NEW.montant THEN
            RAISE EXCEPTION 'Le montant de la variable de paie (%) differe de celui de la retenue (%).', v.montant, NEW.montant
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_retenues_sanction_valider ON retenues_sanction;

CREATE TRIGGER trg_retenues_sanction_valider
    BEFORE INSERT OR UPDATE ON retenues_sanction
    FOR EACH ROW EXECUTE FUNCTION fn_valider_retenue_sanction();

CREATE OR REPLACE FUNCTION fn_journal_discipline_immuable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Le journal disciplinaire est en ajout seul : % interdit.', TG_OP
        USING ERRCODE = 'restrict_violation';
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_journal_discipline_immuable ON journal_acces_discipline;

CREATE TRIGGER trg_journal_discipline_immuable
    BEFORE UPDATE OR DELETE ON journal_acces_discipline
    FOR EACH ROW EXECUTE FUNCTION fn_journal_discipline_immuable();

DROP TRIGGER IF EXISTS trg_journal_discipline_truncate ON journal_acces_discipline;

CREATE TRIGGER trg_journal_discipline_truncate
    BEFORE TRUNCATE ON journal_acces_discipline
    FOR EACH STATEMENT EXECUTE FUNCTION fn_journal_discipline_immuable();

REVOKE UPDATE, DELETE, TRUNCATE ON journal_acces_discipline FROM PUBLIC;

CREATE INDEX IF NOT EXISTS idx_incidents_employe ON incidents(employe_id, date_faits DESC);

CREATE INDEX IF NOT EXISTS idx_incidents_declarant ON incidents(declarant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_procedures_statut ON procedures_disciplinaires(statut_id, ouverte_at DESC);

CREATE INDEX IF NOT EXISTS idx_demandes_explication_limite ON demandes_explication(date_limite);

CREATE INDEX IF NOT EXISTS idx_sanctions_periode ON sanctions(date_debut, date_fin) WHERE date_debut IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_incident ON pieces_disciplinaires(incident_id);

CREATE INDEX IF NOT EXISTS idx_pieces_disciplinaires_reponse ON pieces_disciplinaires(reponse_id) WHERE reponse_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_journal_discipline_employe ON journal_acces_discipline(employe_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_journal_discipline_utilisateur ON journal_acces_discipline(utilisateur_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_journal_discipline_objet ON journal_acces_discipline(objet_type, objet_id);

CREATE INDEX IF NOT EXISTS idx_variables_paie_sanction ON variables_paie(sanction_id) WHERE sanction_id IS NOT NULL;

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

COMMIT;