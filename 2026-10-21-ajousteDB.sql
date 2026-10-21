ALTER TABLE role
    ALTER COLUMN nom_role SET NOT NULL,
    ADD CONSTRAINT uq_role_nom UNIQUE (nom_role),
    ADD CONSTRAINT ck_role_nom_non_vide CHECK (btrim(nom_role) <> '');

ALTER TABLE departement
    ALTER COLUMN nom_dept SET NOT NULL,
    ADD CONSTRAINT uq_departement_nom UNIQUE (nom_dept),
    ADD CONSTRAINT ck_departement_nom_non_vide CHECK (btrim(nom_dept) <> '');

ALTER TABLE poste
    ALTER COLUMN nom_poste SET NOT NULL,
    ALTER COLUMN id_role SET NOT NULL,
    ALTER COLUMN salaire_max SET NOT NULL,
    ALTER COLUMN salaire_min SET NOT NULL,
    ADD CONSTRAINT ck_poste_nom_non_vide CHECK (btrim(nom_poste) <> ''),
    ADD CONSTRAINT ck_poste_salaires_positifs CHECK (salaire_min >= 0 AND salaire_max >= 0),
    ADD CONSTRAINT ck_poste_salaire_coherent CHECK (salaire_min <= salaire_max),
    ADD CONSTRAINT uq_poste_nom_role UNIQUE (nom_poste, id_role);

ALTER TABLE motif
    ALTER COLUMN libelle SET NOT NULL,
    ADD CONSTRAINT uq_motif_libelle UNIQUE (libelle),
    ADD CONSTRAINT ck_motif_libelle_non_vide CHECK (btrim(libelle) <> '');

ALTER TABLE demande_status
    ALTER COLUMN libelle_status SET NOT NULL,
    ADD CONSTRAINT uq_demande_status_libelle UNIQUE (libelle_status),
    ADD CONSTRAINT ck_demande_status_libelle_non_vide CHECK (btrim(libelle_status) <> '');

ALTER TABLE demande
    ALTER COLUMN id_poste SET NOT NULL,
    ALTER COLUMN id_dept SET NOT NULL,
    ALTER COLUMN date_creation SET NOT NULL,
    ALTER COLUMN id_motif SET NOT NULL,
    ALTER COLUMN budget SET NOT NULL,
    ALTER COLUMN date_souhaite SET NOT NULL,
    ALTER COLUMN id_demande_status SET NOT NULL,
    ADD CONSTRAINT ck_demande_budget_positif CHECK (budget >= 0),
    ADD CONSTRAINT ck_demande_dates_coherentes
        CHECK (date_souhaite >= date_creation AND (date_reponse IS NULL OR date_reponse >= date_creation));

ALTER TABLE provenance
    ALTER COLUMN libelle SET NOT NULL,
    ADD CONSTRAINT uq_provenance_libelle UNIQUE (libelle),
    ADD CONSTRAINT ck_provenance_libelle_non_vide CHECK (btrim(libelle) <> '');

ALTER TABLE emp
    ALTER COLUMN nom_emp SET NOT NULL,
    ALTER COLUMN date_embauche SET NOT NULL,
    ALTER COLUMN id_poste SET NOT NULL,
    ALTER COLUMN id_dept SET NOT NULL,
    ALTER COLUMN salaire_emp SET NOT NULL,
    ALTER COLUMN id_provenance SET NOT NULL,
    ADD CONSTRAINT ck_emp_nom_non_vide CHECK (btrim(nom_emp) <> ''),
    ADD CONSTRAINT ck_emp_salaire_positif CHECK (salaire_emp >= 0),
    ADD CONSTRAINT ck_emp_dates_coherentes
        CHECK (date_depart IS NULL OR date_depart >= date_embauche);

ALTER TABLE utilisateur
    ALTER COLUMN id_emp SET NOT NULL,
    ALTER COLUMN username SET NOT NULL,
    ALTER COLUMN password_hash SET NOT NULL,
    ADD CONSTRAINT uq_utilisateur_username UNIQUE (username),
    ADD CONSTRAINT uq_utilisateur_employe UNIQUE (id_emp),
    ADD CONSTRAINT ck_utilisateur_username_non_vide CHECK (btrim(username) <> ''),
    ADD CONSTRAINT ck_utilisateur_password_hash_non_vide CHECK (btrim(password_hash) <> '');

ALTER TABLE poste_par_dept
    ALTER COLUMN id_dept SET NOT NULL,
    ALTER COLUMN id_poste SET NOT NULL,
    ALTER COLUMN nb_poste SET NOT NULL,
    ADD CONSTRAINT uq_poste_par_dept UNIQUE (id_dept, id_poste),
    ADD CONSTRAINT ck_poste_par_dept_nb_positif CHECK (nb_poste > 0);