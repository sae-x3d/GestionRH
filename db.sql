-- BLOC 1
CREATE TABLE role (
    id_role SERIAL PRIMARY KEY,
    nom_role VARCHAR(100)
);

CREATE TABLE departement (
    id_dept SERIAL PRIMARY KEY,
    nom_dept VARCHAR(100)
);

CREATE TABLE poste (
    id_poste SERIAL PRIMARY KEY,
    nom_poste VARCHAR(100),
    id_role INT,
    salaire_max INT,
    salaire_min INT,
    CONSTRAINT fk_id_role
        FOREIGN KEY (id_role) REFERENCES role(id_role)
);

CREATE TABLE motif (
    id_motif SERIAL PRIMARY KEY,
    libelle VARCHAR(100)
);

CREATE TABLE demande (
    id_demande SERIAL PRIMARY KEY,
    id_poste INT,
    id_dept INT,
    date_creation TIMESTAMP,
    id_motif INT,
    budget INT,
    date_souhaite TIMESTAMP,

    CONSTRAINT fk_id_poste
        FOREIGN KEY (id_poste) REFERENCES poste(id_poste),

    CONSTRAINT fk_id_dept
        FOREIGN KEY (id_dept) REFERENCES departement(id_dept),

    CONSTRAINT fk_id_motif
        FOREIGN KEY (id_motif) REFERENCES motif(id_motif)
);

CREATE TABLE provenance (
    id_provenance SERIAL PRIMARY KEY,
    libelle VARCHAR(100)
);

CREATE TABLE emp (
    id_emp SERIAL PRIMARY KEY,
    nom_emp VARCHAR(255),
    date_embauche TIMESTAMP,
    date_depart TIMESTAMP,
    id_poste INT,
    id_dept INT,
    salaire_emp INT,
    id_provenance INT,

    CONSTRAINT fk_id_poste
        FOREIGN KEY (id_poste) REFERENCES poste(id_poste),

    CONSTRAINT fk_id_dept
        FOREIGN KEY (id_dept) REFERENCES departement(id_dept),

    CONSTRAINT fk_id_provenance
        FOREIGN KEY (id_provenance)
        REFERENCES provenance(id_provenance)
);

CREATE TABLE "user" (
    id_user SERIAL PRIMARY KEY,
    id_emp INT,
    username VARCHAR(100),
    password_hash VARCHAR(255),

    CONSTRAINT fk_id_emp
        FOREIGN KEY (id_emp) REFERENCES emp(id_emp)
);

CREATE TABLE poste_par_dept (
    id SERIAL PRIMARY KEY,
    id_dept INT,
    id_poste INT,
    nb_poste INT,

    CONSTRAINT fk_id_dept
        FOREIGN KEY (id_dept) REFERENCES departement(id_dept),

    CONSTRAINT fk_id_poste
        FOREIGN KEY (id_poste) REFERENCES poste(id_poste)
);