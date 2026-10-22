CREATE VIEW v_Employe_Post_Depart AS
SELECT
    e.id_emp,
    e.nom_emp,
    p.nom_poste,
    d.nom_dept,
    e.salaire_emp,
    e.date_embauche
FROM emp e
JOIN poste p ON e.id_poste = p.id_poste
JOIN departement d ON e.id_dept = d.id_dept
WHERE e.date_depart IS NULL
ORDER BY e.id_emp;

CREATE VIEW v_Demandes_Recrutement AS
SELECT
    dem.id_demande,
    p.nom_poste,
    d.nom_dept,
    u.username AS demandeur,
    m.libelle AS motif,
    dem.budget,
    s.libelle_status AS statut,
    dem.date_creation
FROM demande dem
JOIN poste p
    ON dem.id_poste = p.id_poste
JOIN departement d
    ON dem.id_dept = d.id_dept
JOIN utilisateur u
    ON dem.id_demandeur = u.id_user
JOIN motif m
    ON dem.id_motif = m.id_motif
JOIN demande_status s
    ON dem.id_demande_status = s.id_demande_status
ORDER BY dem.date_creation DESC;

CREATE VIEW v_Nombre_Demande_Par_Statut AS
SELECT
    s.libelle_status,
    COUNT(d.id_demande) AS nombre_demandes
FROM demande_status s
LEFT JOIN demande d
    ON s.id_demande_status = d.id_demande_status
GROUP BY s.id_demande_status, s.libelle_status
ORDER BY s.id_demande_status;