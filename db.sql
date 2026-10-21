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
    id_role INT REFERENCES role(id_role),
    salaire_max INT,
    salaire_min INT
);

CREATE TABLE motif (
    id_motif SERIAL PRIMARY KEY,
    libelle VARCHAR(100)
);

CREATE TABLE demande_status (
    id_demande_status SERIAL PRIMARY KEY,
    libelle_status VARCHAR(50)
);

CREATE TABLE demande (
    id_demande SERIAL PRIMARY KEY,
    id_poste INT REFERENCES poste(id_poste),
    id_dept INT REFERENCES departement(id_dept),
    date_creation TIMESTAMP,
    id_motif INT REFERENCES motif(id_motif),
    budget INT,
    date_souhaite TIMESTAMP,
    id_demande_status INT REFERENCES demande_status(id_demande_status),
    date_reponse TIMESTAMP
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
    id_poste INT REFERENCES poste(id_poste),
    id_dept INT REFERENCES departement(id_dept),
    salaire_emp INT,
    id_provenance INT REFERENCES provenance(id_provenance)
);

CREATE TABLE utilisateur (
    id_user SERIAL PRIMARY KEY,
    id_emp INT REFERENCES emp(id_emp),
    username VARCHAR(100),
    password_hash VARCHAR(255)
);

CREATE TABLE poste_par_dept (
    id SERIAL PRIMARY KEY,
    id_dept INT REFERENCES departement(id_dept),
    id_poste INT REFERENCES poste(id_poste),
    nb_poste INT
);
