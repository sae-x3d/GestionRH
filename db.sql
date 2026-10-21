CREATE TABLE role (
    id_role SERIAL PRIMARY KEY,
    nom_role VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE departement (
    id_dept SERIAL PRIMARY KEY,
    nom_dept VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE poste (
    id_poste SERIAL PRIMARY KEY,
    nom_poste VARCHAR(100) NOT NULL,
    id_role INT NOT NULL REFERENCES role(id_role),
    salaire_max INT,
    salaire_min INT,
    CONSTRAINT chk_salaire_poste
        CHECK (
            salaire_min IS NULL
            OR salaire_max IS NULL
            OR (
                salaire_min >= 0
                AND salaire_max >= salaire_min
            )
        )
);

CREATE TABLE motif (
    id_motif SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE demande_status (
    id_demande_status SERIAL PRIMARY KEY,
    libelle_status VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE provenance (
    id_provenance SERIAL PRIMARY KEY,
    libelle VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE emp (
    id_emp SERIAL PRIMARY KEY,
    nom_emp VARCHAR(255) NOT NULL,
    date_embauche TIMESTAMP,
    date_depart TIMESTAMP,
    id_poste INT REFERENCES poste(id_poste),
    id_dept INT REFERENCES departement(id_dept),
    salaire_emp INT,
    id_provenance INT REFERENCES provenance(id_provenance),
    CONSTRAINT chk_dates_emp
        CHECK (
            date_depart IS NULL
            OR date_embauche IS NULL
            OR date_depart >= date_embauche
        ),
    CONSTRAINT chk_salaire_emp
        CHECK (salaire_emp IS NULL OR salaire_emp >= 0)
);

CREATE TABLE utilisateur (
    id_user SERIAL PRIMARY KEY,
    id_emp INT REFERENCES emp(id_emp),
    id_role INT NOT NULL REFERENCES role(id_role),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE poste_par_dept (
    id SERIAL PRIMARY KEY,
    id_dept INT NOT NULL REFERENCES departement(id_dept),
    id_poste INT NOT NULL REFERENCES poste(id_poste),
    nb_poste INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_poste_dept UNIQUE (id_dept, id_poste),
    CONSTRAINT chk_nb_poste CHECK (nb_poste >= 0)
);

CREATE TABLE demande (
    id_demande SERIAL PRIMARY KEY,
    id_poste INT NOT NULL REFERENCES poste(id_poste),
    id_dept INT NOT NULL REFERENCES departement(id_dept),
    id_demandeur INT NOT NULL REFERENCES utilisateur(id_user),
    id_emp_remplace INT REFERENCES emp(id_emp),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_motif INT NOT NULL REFERENCES motif(id_motif),
    budget INT,
    date_souhaite TIMESTAMP,
    id_demande_status INT NOT NULL REFERENCES demande_status(id_demande_status),
    date_reponse TIMESTAMP,
    CONSTRAINT chk_budget_demande
        CHECK (budget IS NULL OR budget >= 0)
);

CREATE INDEX idx_demande_demandeur
    ON demande(id_demandeur);

CREATE INDEX idx_demande_status
    ON demande(id_demande_status);

CREATE INDEX idx_demande_departement
    ON demande(id_dept);

CREATE INDEX idx_emp_poste_dept
    ON emp(id_poste, id_dept);
