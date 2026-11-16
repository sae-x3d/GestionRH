-- Donnees de demonstration Bloc 9 - 16/11/2026.
-- A importer apres tables-16-11-26.sql, lui-meme importe apres les scripts du 14/11/2026.

BEGIN;

INSERT INTO types_notifications (code, libelle) VALUES
 ('EVALUATION_A_VALIDER', 'Evaluation a valider'),
 ('EVALUATION_VALIDEE', 'Evaluation validee'),
 ('FORMATION_INSCRIT', 'Inscription a une formation'),
 ('FORMATION_MESUREE', 'Mesure de formation disponible')
ON CONFLICT (code) DO UPDATE SET libelle = EXCLUDED.libelle;

INSERT INTO competences (code, libelle, description, cree_par_id)
SELECT v.code, v.libelle, v.description, u.id
FROM (VALUES
 ('TECH_JAVA', 'Developpement Java', 'Conception et developpement d applications Java maintenables.'),
 ('API_REST', 'Conception API REST', 'Conception et consommation d API REST documentees.'),
 ('SQL', 'SQL et bases de donnees', 'Interrogation, modelisation et optimisation des donnees.'),
 ('TESTS', 'Tests logiciels', 'Tests unitaires, integration et non-regression.'),
 ('GIT', 'Git et collaboration', 'Versionnement, branches et revues de code.'),
 ('DOCUMENTATION', 'Documentation technique', 'Documentation des choix, procedures et APIs.'),
 ('GESTION_STOCK', 'Gestion des stocks', 'Suivi des entrees, sorties et inventaires.'),
 ('LOGISTIQUE_FLUX', 'Flux logistiques', 'Organisation des approvisionnements et livraisons.'),
 ('SECURITE', 'Securite des operations', 'Application des regles de securite et de conformite.'),
 ('COMPTABILITE', 'Comptabilite generale', 'Tenue des comptes et preparation des clotures.'),
 ('FISCALITE', 'Fiscalite', 'Application des obligations fiscales et declaratives.'),
 ('CONTROLE_BUDGET', 'Controle budgetaire', 'Suivi des budgets, ecarts et justifications.'),
 ('VENTE', 'Techniques de vente', 'Prospection, proposition commerciale et conclusion.'),
 ('RELATION_CLIENT', 'Relation client', 'Ecoute, suivi et fidelisation des clients.'),
 ('NEGOCIATION', 'Negociation commerciale', 'Preparation et conduite des negociations.'),
 ('MANAGEMENT', 'Management d equipe', 'Animation, delegation et developpement de l equipe.'),
 ('COORDINATION', 'Coordination', 'Organisation des priorites et des interfaces.'),
 ('COMMUNICATION', 'Communication professionnelle', 'Communication claire avec les parties prenantes.'),
 ('ORGANISATION', 'Organisation administrative', 'Classement, priorisation et suivi administratif.'),
 ('OUTILS_BUREAUTIQUE', 'Outils bureautiques', 'Tableur, traitement de texte et presentations.'),
 ('SUPPORT_UTILISATEUR', 'Support utilisateur', 'Diagnostic et resolution des demandes de support.')
) AS v(code, libelle, description)
CROSS JOIN users u
WHERE u.username = 'demo_rh'
ON CONFLICT (code) DO UPDATE
SET libelle = EXCLUDED.libelle, description = EXCLUDED.description;

INSERT INTO criteres_poste (poste_id, competence_id, niveau_attendu, seuil_ecart, obligatoire, cree_par_id)
SELECT p.id, c.id, v.niveau_attendu, v.seuil_ecart, v.obligatoire, u.id
FROM (VALUES
 ('MGR', 'MANAGEMENT', 4, 1, TRUE),
 ('MGR', 'COORDINATION', 4, 1, TRUE),
 ('MGR', 'COMMUNICATION', 4, 1, TRUE),
 ('MGR', 'ORGANISATION', 4, 1, TRUE),
 ('DEV', 'TECH_JAVA', 3, 1, TRUE),
 ('DEV', 'API_REST', 3, 1, TRUE),
 ('DEV', 'SQL', 3, 1, TRUE),
 ('DEV', 'TESTS', 3, 1, TRUE),
 ('DEV', 'GIT', 2, 1, TRUE),
 ('DEV', 'DOCUMENTATION', 2, 1, FALSE),
 ('AST', 'ORGANISATION', 3, 1, TRUE),
 ('AST', 'OUTILS_BUREAUTIQUE', 3, 1, TRUE),
 ('AST', 'COMMUNICATION', 3, 1, TRUE),
 ('AST', 'COORDINATION', 3, 1, TRUE),
 ('AGL', 'GESTION_STOCK', 3, 1, TRUE),
 ('AGL', 'LOGISTIQUE_FLUX', 3, 1, TRUE),
 ('AGL', 'SECURITE', 2, 1, TRUE),
 ('AGL', 'ORGANISATION', 2, 1, FALSE),
 ('CPT', 'COMPTABILITE', 3, 1, TRUE),
 ('CPT', 'FISCALITE', 3, 1, TRUE),
 ('CPT', 'CONTROLE_BUDGET', 3, 1, TRUE),
 ('CPT', 'OUTILS_BUREAUTIQUE', 3, 1, FALSE),
 ('COM', 'VENTE', 3, 1, TRUE),
 ('COM', 'RELATION_CLIENT', 3, 1, TRUE),
 ('COM', 'NEGOCIATION', 3, 1, TRUE),
 ('COM', 'COMMUNICATION', 3, 1, TRUE)
) AS v(poste_code, competence_code, niveau_attendu, seuil_ecart, obligatoire)
JOIN postes p ON p.code_matricule = v.poste_code AND p.actif = TRUE
JOIN competences c ON c.code = v.competence_code AND c.actif = TRUE
CROSS JOIN users u
WHERE u.username = 'demo_rh'
ON CONFLICT (poste_id, competence_id) DO NOTHING;

-- Un exemple complet : evaluation validee, besoins ouverts et formation planifiee.
DO $$
DECLARE
    v_rh_id INTEGER;
    v_emp_id INTEGER;
    v_poste_id INTEGER;
    v_bareme_min INTEGER;
    v_bareme_max INTEGER;
    v_eval_id INTEGER;
    v_besoin_id INTEGER;
    v_besoin_competence_id INTEGER;
    v_besoin_niveau INTEGER;
    v_competence_libelle VARCHAR(150);
    v_formation_id INTEGER;
    v_titre VARCHAR(200);
BEGIN
    SELECT id INTO v_rh_id FROM users WHERE username = 'demo_rh';

    SELECT e.id, e.poste_id, p.niveau_min, p.niveau_max
      INTO v_emp_id, v_poste_id, v_bareme_min, v_bareme_max
      FROM employes e
      JOIN users u ON u.id = e.user_id
      JOIN postes p ON p.id = e.poste_id
     WHERE u.username = 'emp_it_dev_01';

    IF v_emp_id IS NULL OR v_rh_id IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM evaluations_employe
        WHERE employe_id = v_emp_id AND date_evaluation = CURRENT_DATE - 10
    ) THEN
        INSERT INTO evaluations_employe (
            employe_id, poste_id, evaluateur_id, valide_par_id, date_evaluation, statut,
            commentaire, bareme_min, bareme_max, soumise_at, validee_at
        )
        SELECT v_emp_id, v_poste_id, mgr.id, v_rh_id, CURRENT_DATE - 10, 'VALIDEE',
               'Evaluation de demonstration : les tests et la maitrise SQL restent a renforcer.',
               v_bareme_min, v_bareme_max,
               CURRENT_TIMESTAMP - INTERVAL '9 days',
               CURRENT_TIMESTAMP - INTERVAL '8 days'
          FROM users mgr
         WHERE mgr.username = 'mgr_it_01'
        RETURNING id INTO v_eval_id;

        IF v_eval_id IS NOT NULL THEN
            INSERT INTO evaluation_lignes (
                evaluation_id, critere_id, competence_id, niveau_attendu, niveau_acquis,
                seuil_ecart, ecart, obligatoire
            )
            SELECT v_eval_id, cp.id, cp.competence_id, cp.niveau_attendu,
                   GREATEST(v_bareme_min, cp.niveau_attendu - CASE
                       WHEN c.code = 'TESTS' THEN 2
                       WHEN c.code = 'SQL' THEN 1
                       ELSE 0
                   END),
                   cp.seuil_ecart,
                   GREATEST(0, cp.niveau_attendu - GREATEST(v_bareme_min, cp.niveau_attendu - CASE
                       WHEN c.code = 'TESTS' THEN 2
                       WHEN c.code = 'SQL' THEN 1
                       ELSE 0
                   END)),
                   cp.obligatoire
              FROM criteres_poste cp
              JOIN competences c ON c.id = cp.competence_id
             WHERE cp.poste_id = v_poste_id;

            INSERT INTO besoins_formation (
                employe_id, competence_id, poste_id, evaluation_ligne_id,
                niveau_attendu, niveau_acquis_initial, seuil_ecart, ecart_initial, statut
            )
            SELECT v_emp_id, l.competence_id, v_poste_id, l.id,
                   l.niveau_attendu, l.niveau_acquis, l.seuil_ecart, l.ecart, 'OUVERT'
              FROM evaluation_lignes l
             WHERE l.evaluation_id = v_eval_id
               AND l.ecart >= l.seuil_ecart
               AND NOT EXISTS (
                   SELECT 1 FROM besoins_formation b
                    WHERE b.employe_id = v_emp_id
                      AND b.competence_id = l.competence_id
                      AND b.statut IN ('OUVERT', 'PLANIFIE')
               );
        END IF;
    END IF;

    SELECT b.id, b.competence_id, b.niveau_attendu, c.libelle
      INTO v_besoin_id, v_besoin_competence_id, v_besoin_niveau, v_competence_libelle
      FROM besoins_formation b
      JOIN competences c ON c.id = b.competence_id
     WHERE b.employe_id = v_emp_id AND b.statut = 'OUVERT'
     ORDER BY b.id
     LIMIT 1;

    IF v_besoin_id IS NOT NULL THEN
        v_titre := 'Renforcement : ' || v_competence_libelle;

        SELECT id INTO v_formation_id
          FROM formations
         WHERE titre = v_titre AND competence_id = v_besoin_competence_id
         ORDER BY id
         LIMIT 1;

        IF v_formation_id IS NULL THEN
            INSERT INTO formations (
                competence_id, titre, description, organisme, date_debut, date_fin,
                niveau_cible, statut, cree_par_id
            ) VALUES (
                v_besoin_competence_id, v_titre,
                'Formation de demonstration ciblee sur le besoin repere lors de l evaluation.',
                'Centre de formation interne',
                CURRENT_DATE + 7, CURRENT_DATE + 9,
                v_besoin_niveau, 'PLANIFIEE', v_rh_id
            )
            RETURNING id INTO v_formation_id;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM participations_formation
            WHERE formation_id = v_formation_id AND employe_id = v_emp_id
        ) THEN
            INSERT INTO participations_formation (formation_id, employe_id, besoin_id, statut)
            VALUES (v_formation_id, v_emp_id, v_besoin_id, 'INSCRIT');

            UPDATE besoins_formation
               SET statut = 'PLANIFIE', updated_at = CURRENT_TIMESTAMP
             WHERE id = v_besoin_id;
        END IF;
    END IF;
END $$;

COMMIT;
