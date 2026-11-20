-- Jeu de donnees de demonstration SIRH - 14/11/2026.
-- Importer apres tables-14-11-26.sql dans une base vide.
-- Puis lancer: node datas/db/generate-14-11-26-documents.js
-- Les mots de passe de demonstration sont documentes dans infos.md.
BEGIN;

-- Referentiels de compte, recrutement, dossier, conges, planning et paie.
INSERT INTO provenances_employe (code, libelle) VALUES
 ('RECRUTEMENT','Recrutement'),('PROMOTION','Promotion'),('TRANSFERT','Transfert');
INSERT INTO statuts_validation_compte (code, libelle) VALUES
 ('NON_VERIFIE','Profil a completer'),('PENDING','Verification demandee'),('APPROVED','Valide'),('REJECTED','Refuse');
INSERT INTO statuts_administratifs_employe (code, libelle) VALUES
 ('EN_PREPARATION','En preparation'),('ACTIF','Actif'),('EN_PREAVIS','En preavis'),('SORTI','Sorti');
INSERT INTO types_documents_employe (code, libelle, obligatoire, visible_manager) VALUES
 ('PIECE_IDENTITE','Carte nationale d identite (CIN)',TRUE,FALSE),('DIPLOME','Diplome',FALSE,FALSE),
 ('ATTESTATION_EMPLOI','Attestation professionnelle',FALSE,FALSE),('JUSTIFICATIF_DOMICILE','Justificatif de domicile',FALSE,FALSE),
 ('CV','CV du salarie',FALSE,FALSE),('CONTRAT_TRAVAIL','Contrat de travail',TRUE,FALSE),
 ('AUTRE','Autre document administratif',FALSE,FALSE);
INSERT INTO statuts_demande_dossier (code, libelle) VALUES
 ('SOUMISE','Soumise'),('EN_VERIFICATION','En verification'),('APPROUVEE','Approuvee'),('REFUSEE','Refusee'),('ANNULEE','Annulee');
INSERT INTO champs_dossier_modifiables (code, libelle) VALUES
 ('FULL_NAME','Nom complet'),('PHONE_NUMBER','Telephone'),('REGION','Region'),('ADRESSE_LIGNE','Adresse'),
 ('VILLE','Ville'),('CODE_POSTAL','Code postal'),('PAYS','Pays'),('CONTACT_URGENCE_NOM','Contact urgence'),
 ('CONTACT_URGENCE_LIEN','Lien contact urgence'),('CONTACT_URGENCE_TELEPHONE','Telephone contact urgence');
INSERT INTO types_evenements_dossier (code, libelle) VALUES
 ('DOSSIER_CREE','Dossier cree'),('DOCUMENT_AJOUTE','Document ajoute'),('DOCUMENT_ARCHIVE','Document archive'),
 ('DEMANDE_SOUMISE','Demande de correction soumise'),('DEMANDE_DECIDEE','Demande de correction traitee'),('STATUT_CHANGE','Statut administratif modifie');
INSERT INTO motifs_recrutement (code, libelle) VALUES
 ('AUGMENT_EFFECTIF','Augmentation de l effectif'),('POSTE_VACANT','Pourvoir un poste vacant');
INSERT INTO types_evenement_effectif (code, libelle) VALUES
 ('INITIALISATION','Initialisation de la cible'),('AUGMENTATION_APPROUVEE','Augmentation approuvee'),
 ('AJUSTEMENT_RH','Ajustement RH'),('EMBAUCHE','Embauche realisee'),('DEPART','Depart du salarie');
INSERT INTO types_conge (code, libelle, justificatif_requis, acquisition_active) VALUES
 ('CONGES_PAYES','Conges payes',FALSE,TRUE),('SANS_SOLDE','Conge sans solde',FALSE,FALSE),
 ('MALADIE','Conge maladie',TRUE,FALSE),('EVENEMENT_FAMILIAL','Conge evenement familial',TRUE,FALSE),
 ('MATERNITE_PATERNITE','Conge maternite ou paternite',TRUE,FALSE);
INSERT INTO jours_semaine (numero_iso, code, libelle, ouvrable_par_defaut) VALUES
 (1,'LUNDI','Lundi',TRUE),(2,'MARDI','Mardi',TRUE),(3,'MERCREDI','Mercredi',TRUE),(4,'JEUDI','Jeudi',TRUE),
 (5,'VENDREDI','Vendredi',TRUE),(6,'SAMEDI','Samedi',FALSE),(7,'DIMANCHE','Dimanche',FALSE);
INSERT INTO statuts_demande_conge (code, libelle) VALUES
 ('A_VALIDER_MANAGER','A valider par le manager'),('A_VALIDER_RH','A valider par les RH'),
 ('APPROUVEE','Approuvee'),('REFUSEE','Refusee'),('ANNULEE','Annulee');
INSERT INTO decisions_conge (code, libelle) VALUES ('APPROUVEE','Approuvee'),('REFUSEE','Refusee');
INSERT INTO types_mouvement_solde_conge (code, libelle) VALUES
 ('ACQUISITION','Jours acquis'),('PRISE','Conge consomme'),('REGULARISATION','Regularisation RH');
INSERT INTO types_absence (code, libelle, justificatif_requis, compte_effectif, impacte_planning, retenue_paie) VALUES
 ('MALADIE','Absence pour maladie',TRUE,FALSE,TRUE,FALSE),('ABSENCE_AUTORISEE','Absence autorisee',FALSE,FALSE,TRUE,FALSE),
 ('ABSENCE_INJUSTIFIEE','Absence injustifiee',FALSE,FALSE,TRUE,TRUE),('RETARD','Retard',FALSE,FALSE,FALSE,FALSE),
 ('SORTIE_ANTICIPEE','Depart anticipe',FALSE,FALSE,FALSE,FALSE);
INSERT INTO statuts_demande_recrutement (code, libelle) VALUES
 ('BROUILLON','Brouillon'),('SOUMISE','Soumise'),('A_VALIDER_RH','A valider par les RH'),
 ('A_VALIDER_FINANCE','A valider par la Finance'),('A_CREER_OFFRE','A transformer en offre'),('OFFRE_CREEE','Offre creee'),
 ('EN_COURS','En cours'),('A_CORRIGER','A corriger'),('APPROUVEE','Approuvee'),('REFUSEE','Refusee'),('ANNULEE','Annulee');
INSERT INTO decisions_validation_demande (code, libelle) VALUES
 ('APPROUVEE','Approuvee'),('REFUSEE','Refusee'),('CORRECTION_DEMANDEE','Correction demandee');
INSERT INTO statuts_offre_emploi (code, libelle) VALUES
 ('BROUILLON','Brouillon'),('PUBLIEE','Publiee'),('SUSPENDUE','Suspendue'),('CLOTUREE','Cloturee');
INSERT INTO types_notifications (code, libelle) VALUES
 ('DEMANDE_A_VALIDER','Demande a valider'),('DEMANDE_STATUT','Changement de statut'),('OFFRE_A_CREER','Offre a creer'),
 ('CANDIDATURE_A_TRAITER','Candidature a traiter'),('CANDIDATURE_STATUT','Mise a jour de candidature'),
 ('COMPTE_STATUT','Mise a jour du compte'),('INSCRIPTION_A_TRAITER','Nouvelle inscription a traiter'),
 ('DOSSIER_A_VALIDER','Demande de dossier a traiter'),('DOSSIER_STATUT','Mise a jour du dossier salarie'),
 ('CONGE_A_VALIDER','Demande de conge a valider'),('CONGE_STATUT','Mise a jour de demande de conge'),
 ('EFFECTIF_SOUS_MINIMUM','Alerte de sous-effectif'),('BULLETIN_PAIE','Bulletin de paie disponible');
INSERT INTO statuts_candidature (code, libelle) VALUES
 ('ENVOYEE_RH','Transmise aux RH'),('PRESELECTIONNEE','Preselectionnee'),('ENTRETIEN_PROPOSE','Entretien propose'),
 ('ENTRETIEN_ACCEPTE','Entretien accepte'),('ENTRETIEN_REFUSE','Entretien refuse'),('ENTRETIEN_EFFECTUE','Entretien effectue'),
 ('RETENUE_ENTRETIEN','Retenue apres entretien'),('QUESTIONNAIRE_A_REMPLIR','Questionnaire a remplir'),
 ('QUESTIONNAIRE_REMPLI','Questionnaire rempli'),('CONTRAT_PROPOSE','Proposition de contrat envoyee'),
 ('CANDIDAT_PROPOSE_MODIFICATION','Modification proposee par le candidat'),('CONTRAT_REFUSE','Proposition de contrat refusee'),
 ('REFUSEE','Candidature refusee'),('EMBAUCHEE','Candidat embauche');
INSERT INTO statuts_entretien (code, libelle) VALUES
 ('PROPOSE','En attente de reponse'),('ACCEPTE','Accepte par le candidat'),('REFUSE','Refuse par le candidat'),('EFFECTUE','Entretien effectue');
INSERT INTO types_entretien (code, libelle) VALUES ('ENTRETIEN','Entretien'),('ENTRETIEN_TEST','Entretien avec test');
INSERT INTO types_contrat (code, libelle) VALUES
 ('CDI','CDI'),('CDI_PERIODE_ESSAI','CDI avec periode d essai'),('CDD','CDD'),('CDD_RENOUVELABLE','CDD renouvelable');
INSERT INTO statuts_contrat (code, libelle) VALUES ('ACTIF','Actif'),('TERMINE','Termine'),('RESILIE','Resilie');
INSERT INTO types_parcours (code, libelle) VALUES
 ('ETUDE','Etude'),('FORMATION_PRO','Formation professionnelle'),('EXPERIENCE_PRO','Experience professionnelle');
INSERT INTO niveaux_etude (code, libelle, ordre) VALUES
 ('BAC', 'Baccalaureat',1),('BAC_PLUS_1','Bac + 1',2),('BAC_PLUS_2','Bac + 2',3),('BAC_PLUS_3','Bac + 3',4),
 ('BAC_PLUS_4','Bac + 4',5),('BAC_PLUS_5','Bac + 5',6),('BAC_PLUS_6','Bac + 6',7),('BAC_PLUS_7','Bac + 7',8),
 ('BAC_PLUS_8','Bac + 8',9),('BAC_PLUS_9','Bac + 9',10),('NIVEAU_1','Niveau 1',11),('NIVEAU_2','Niveau 2',12),('NIVEAU_3','Niveau 3',13);
INSERT INTO roles (role_name) VALUES ('simple_user'),('employe'),('manager'),('rh'),('administration'),('gestionnaire');
INSERT INTO etapes_validation_conge (code, libelle, ordre, role_id)
SELECT 'MANAGER','Validation du manager',1,id FROM roles WHERE role_name='manager';
INSERT INTO etapes_validation_conge (code, libelle, ordre, role_id)
SELECT 'RH','Validation des ressources humaines',2,id FROM roles WHERE role_name='rh';

INSERT INTO departements (code, code_matricule, nom, description) VALUES
 ('INFORMATIQUE','IT','Informatique','Developpement, maintenance et support des services numeriques.'),
 ('LOGISTIQUE','LG','Logistique','Gestion des stocks, approvisionnements et flux logistiques.'),
 ('FINANCE','FI','Finance','Comptabilite, suivi budgetaire et controle financier.'),
 ('COMMERCIAL','CO','Commercial','Prospection, vente et relation client.');

-- Six familles de postes, instanciees seulement dans les departements pertinents.
-- Le role RH est un droit applicatif et ne cree pas un 5e departement.
INSERT INTO postes (departement_id, code_matricule, intitule, description, effectif_autorise, salaire_base_min, salaire_base_max)
SELECT d.id, p.code, p.intitule, p.description, p.effectif, p.salaire_min, p.salaire_max
FROM (VALUES
 ('INFORMATIQUE','MGR','Manager','Pilotage de l equipe et des priorites du departement.',2,1200000::numeric,2500000::numeric),
 ('INFORMATIQUE','DEV','Developpeur junior','Developpement, tests et maintenance des applications.',3,750000::numeric,1800000::numeric),
 ('INFORMATIQUE','AST','Assistant','Coordination et suivi administratif du departement.',2,500000::numeric,1100000::numeric),
 ('LOGISTIQUE','MGR','Manager','Pilotage des operations logistiques et de la securite.',2,1200000::numeric,2500000::numeric),
 ('LOGISTIQUE','AGL','Agent logistique','Reception, inventaire et preparation des mouvements de stock.',2,500000::numeric,1200000::numeric),
 ('LOGISTIQUE','AST','Assistant','Saisie des mouvements et suivi des dossiers logistiques.',2,500000::numeric,1100000::numeric),
 ('FINANCE','MGR','Manager','Pilotage budgetaire et controle des activites financieres.',2,1200000::numeric,2500000::numeric),
 ('FINANCE','CPT','Comptable','Tenue comptable, rapprochements et preparation des clotures.',2,800000::numeric,1800000::numeric),
 ('FINANCE','AST','Assistant','Appui administratif et preparation des pieces comptables.',2,500000::numeric,1100000::numeric),
 ('COMMERCIAL','MGR','Manager','Pilotage des ventes et accompagnement de l equipe commerciale.',2,1200000::numeric,2500000::numeric),
 ('COMMERCIAL','COM','Commercial','Prospection, vente et suivi du portefeuille client.',3,700000::numeric,2000000::numeric),
 ('COMMERCIAL','AST','Assistant','Preparation des offres et suivi administratif des ventes.',2,500000::numeric,1100000::numeric)
) AS p(departement, code, intitule, description, effectif, salaire_min, salaire_max)
JOIN departements d ON d.code=p.departement;

-- Comptes de personnel. Les donnees contact et les fichiers demo sont remplis par le script associe.
INSERT INTO users (username,email,full_name,phone_number,region,photo_profil_path,password_hash,email_verified_at,administrative_validation_status_id)
SELECT a.username,a.email,a.nom,a.telephone,a.region,'pdp/pdp_'||a.username||'.png',
       '$2a$12$C8Q12AOPdn8BE4XPcsZ.e.jmVY/P70DhH5rQkznchEqbpySJasiHq',CURRENT_TIMESTAMP,s.id
FROM (VALUES
 ('demo_rh','rh@example.test','Soa Ravelomanana','+261340000101','Analamanga','APPROVED'),
 ('demo_gestionnaire','gestionnaire@example.test','Haja Randrianarisoa','+261340000102','Analamanga','APPROVED'),
 ('demo_administration','administration@example.test','Lova Andriamampionona','+261340000103','Analamanga','APPROVED'),
 ('candidat_valide','candidat.valide@example.test','Miora Rakoto','+261340000201','Analamanga','APPROVED'),
 ('candidat_attente','candidat.attente@example.test','Fara Rasoanaivo','+261340000202','Boeny','PENDING'),
 ('candidat_non_verifie','candidat.nonverifie@example.test','Tiana Raveloson','+261340000203','Atsinanana','NON_VERIFIE')
) AS a(username,email,nom,telephone,region,status_code)
JOIN statuts_validation_compte s ON s.code=a.status_code;

INSERT INTO user_roles (user_id,role_id)
SELECT u.id,r.id FROM users u JOIN roles r ON r.role_name=CASE u.username
 WHEN 'demo_rh' THEN 'rh' WHEN 'demo_gestionnaire' THEN 'gestionnaire'
 WHEN 'demo_administration' THEN 'administration' ELSE 'simple_user' END
WHERE u.username IN ('demo_rh','demo_gestionnaire','demo_administration','candidat_valide','candidat_attente','candidat_non_verifie');
UPDATE users SET verification_requested_at=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE username='candidat_attente';

CREATE TEMP TABLE seed_employes (
 username VARCHAR(50), email VARCHAR(254), nom VARCHAR(150), telephone VARCHAR(30), region VARCHAR(120),
 adresse VARCHAR(250), ville VARCHAR(120), urgence_nom VARCHAR(150), urgence_tel VARCHAR(30),
 departement VARCHAR(30), poste VARCHAR(5), salaire NUMERIC(14,2), type_contrat VARCHAR(30),
 date_embauche DATE, date_naissance DATE, date_fin_contrat DATE, offre_code VARCHAR(40),
 formation VARCHAR(180), organisme VARCHAR(180), role_code VARCHAR(30)
) ON COMMIT DROP;
INSERT INTO seed_employes VALUES
 ('mgr_it_01','mgr.it01@example.test','Hery Randriamihaja','+261340100101','Analamanga','Lot IT 101','Antananarivo','Mialy Randriamihaja','+261340900101','INFORMATIQUE','MGR',1800000,'CDI','2022-02-15','1984-04-16',NULL,'OFF-IT-MGR','Licence en informatique','Universite d Antananarivo','manager'),
 ('mgr_it_02','mgr.it02@example.test','Mialy Rakotoarisoa','+261340100102','Analamanga','Lot IT 102','Antananarivo','Hery Randriamihaja','+261340900102','INFORMATIQUE','MGR',1700000,'CDI','2022-02-15','1988-12-06',NULL,'OFF-IT-MGR','Master en systemes informatiques','Universite d Antananarivo','manager'),
 ('mgr_log_01','mgr.log01@example.test','Mamy Randrianarisoa','+261340100103','Atsinanana','Lot LG 101','Toamasina','Noro Randrianarisoa','+261340900103','LOGISTIQUE','MGR',1750000,'CDI','2021-04-15','1982-08-21',NULL,'OFF-LOG-MGR','Licence en gestion logistique','Universite de Toamasina','manager'),
 ('mgr_log_02','mgr.log02@example.test','Noro Rasoanirina','+261340100104','Atsinanana','Lot LG 102','Toamasina','Mamy Randrianarisoa','+261340900104','LOGISTIQUE','MGR',1650000,'CDI','2021-04-15','1987-02-11',NULL,'OFF-LOG-MGR','Master en supply chain','Universite de Toamasina','manager'),
 ('mgr_fin_01','mgr.fin01@example.test','Lova Rakotomalala','+261340100105','Analamanga','Lot FI 101','Antananarivo','Fanja Rakotomalala','+261340900105','FINANCE','MGR',1900000,'CDI','2023-05-15','1980-05-09',NULL,'OFF-FIN-MGR','Master en finance et comptabilite','Universite d Antananarivo','manager'),
 ('mgr_fin_02','mgr.fin02@example.test','Fanja Andriamampionona','+261340100106','Analamanga','Lot FI 102','Antananarivo','Lova Rakotomalala','+261340900106','FINANCE','MGR',1750000,'CDI','2023-05-15','1986-10-19',NULL,'OFF-FIN-MGR','Licence en comptabilite','Universite d Antananarivo','manager'),
 ('mgr_com_01','mgr.com01@example.test','Tojo Ramaroson','+261340100107','Vakinankaratra','Lot CO 101','Antsirabe','Volana Ramaroson','+261340900107','COMMERCIAL','MGR',1800000,'CDI','2021-06-15','1983-01-24',NULL,'OFF-COM-MGR','Master en commerce','Universite d Antananarivo','manager'),
 ('mgr_com_02','mgr.com02@example.test','Volana Ravelomanantsoa','+261340100108','Vakinankaratra','Lot CO 102','Antsirabe','Tojo Ramaroson','+261340900108','COMMERCIAL','MGR',1700000,'CDI','2021-06-15','1988-07-30',NULL,'OFF-COM-MGR','Licence en marketing','Universite d Antananarivo','manager'),
 ('emp_it_dev_01','emp.it.dev01@example.test','Ando Razafindrakoto','+261340200101','Analamanga','Lot IT 201','Antananarivo','Miora Razafindrakoto','+261340900201','INFORMATIQUE','DEV',1200000,'CDI','2022-07-15','1998-03-12',NULL,'OFF-IT-DEV','Licence professionnelle en informatique','Ecole Nationale d Informatique','employe'),
 ('emp_it_dev_02','emp.it.dev02@example.test','Felana Rakotondrabe','+261340200102','Analamanga','Lot IT 202','Antananarivo','Tiana Rakotondrabe','+261340900202','INFORMATIQUE','DEV',1100000,'CDI','2022-07-15','1999-06-18',NULL,'OFF-IT-DEV','Licence en genie logiciel','Universite de Fianarantsoa','employe'),
 ('emp_it_ast_01','emp.it.ast01@example.test','Miora Randrianasolo','+261340200103','Analamanga','Lot IT 203','Antananarivo','Aina Randrianasolo','+261340900203','INFORMATIQUE','AST',750000,'CDI','2024-03-15','1997-11-02',NULL,'OFF-IT-AST','BTS en gestion administrative','Lycee Technique d Antananarivo','employe'),
 ('emp_it_ast_02','emp.it.ast02@example.test','Aina Rakotovao','+261340200104','Analamanga','Lot IT 204','Antananarivo','Lanto Rakotovao','+261340900204','INFORMATIQUE','AST',700000,'CDD','2026-08-15','2000-02-20','2027-08-14','OFF-IT-AST','BTS en assistanat de gestion','Institut Superieur de Technologie','employe'),
 ('emp_log_agent_01','emp.log.agent01@example.test','Niry Randrianasolo','+261340200105','Atsinanana','Lot LG 201','Toamasina','Aro Randrianasolo','+261340900205','LOGISTIQUE','AGL',850000,'CDI','2023-08-15','1995-09-10',NULL,'OFF-LOG-AGENT','Licence en gestion logistique','Universite de Toamasina','employe'),
 ('emp_log_agent_02','emp.log.agent02@example.test','Aro Rakotondrazaka','+261340200106','Atsinanana','Lot LG 202','Toamasina','Tiana Rakotondrazaka','+261340900206','LOGISTIQUE','AGL',780000,'CDI','2023-08-15','1996-12-03',NULL,'OFF-LOG-AGENT','BTS en gestion des stocks','Lycee Technique de Toamasina','employe'),
 ('emp_log_ast_01','emp.log.ast01@example.test','Tiana Rasoamanana','+261340200107','Atsinanana','Lot LG 203','Toamasina','Mamy Rasoamanana','+261340900207','LOGISTIQUE','AST',700000,'CDI','2024-05-15','1998-04-14',NULL,'OFF-LOG-AST','BTS en administration','Universite de Toamasina','employe'),
 ('emp_log_ast_02','emp.log.ast02@example.test','Fanja Rakotovao','+261340200108','Atsinanana','Lot LG 204','Toamasina','Solo Rakotovao','+261340900208','LOGISTIQUE','AST',680000,'CDI','2024-05-15','1999-10-27',NULL,'OFF-LOG-AST','Licence en gestion','Universite de Toamasina','employe'),
 ('emp_fin_cpt_01','emp.fin.cpt01@example.test','Nantenaina Rakotoarisoa','+261340200109','Analamanga','Lot FI 201','Antananarivo','Hasina Rakotoarisoa','+261340900209','FINANCE','CPT',1300000,'CDI','2023-10-15','1994-07-08',NULL,'OFF-FIN-CPT','Master en comptabilite et audit','Universite d Antananarivo','employe'),
 ('emp_fin_cpt_02','emp.fin.cpt02@example.test','Hasina Ramarolahy','+261340200110','Analamanga','Lot FI 202','Antananarivo','Toky Ramarolahy','+261340900210','FINANCE','CPT',1150000,'CDI','2023-10-15','1995-01-29',NULL,'OFF-FIN-CPT','Licence en comptabilite','Universite d Antananarivo','employe'),
 ('emp_fin_ast_01','emp.fin.ast01@example.test','Malala Rasoanaivo','+261340200111','Analamanga','Lot FI 203','Antananarivo','Solofo Rasoanaivo','+261340900211','FINANCE','AST',720000,'CDD','2026-09-15','1999-05-06','2027-09-14','OFF-FIN-AST','BTS en gestion comptable','Institut Superieur de Technologie','employe'),
 ('emp_fin_ast_02','emp.fin.ast02@example.test','Noro Andriamampianina','+261340200112','Analamanga','Lot FI 204','Antananarivo','Lova Andriamampianina','+261340900212','FINANCE','AST',760000,'CDI','2025-02-15','1997-03-25',NULL,'OFF-FIN-AST','Licence en gestion financiere','Universite d Antananarivo','employe'),
 ('emp_com_sales_01','emp.com.sales01@example.test','Fetra Ramaroson','+261340200113','Vakinankaratra','Lot CO 201','Antsirabe','Mialy Ramaroson','+261340900213','COMMERCIAL','COM',1250000,'CDI','2024-06-15','1996-06-12',NULL,'OFF-COM-SALES','Licence en commerce et vente','Universite d Antananarivo','employe'),
 ('emp_com_sales_02','emp.com.sales02@example.test','Tahiry Andriatsilavo','+261340200114','Vakinankaratra','Lot CO 202','Antsirabe','Zo Andriatsilavo','+261340900214','COMMERCIAL','COM',1100000,'CDI','2024-06-15','1997-08-22',NULL,'OFF-COM-SALES','BTS en action commerciale','Lycee Technique d Antsirabe','employe'),
 ('emp_com_ast_01','emp.com.ast01@example.test','Manda Rakotomalala','+261340200115','Vakinankaratra','Lot CO 203','Antsirabe','Haja Rakotomalala','+261340900215','COMMERCIAL','AST',720000,'CDD','2026-08-15','2000-01-15','2027-08-14','OFF-COM-AST','BTS en assistanat commercial','Institut Superieur de Technologie','employe'),
 ('emp_com_ast_02','emp.com.ast02@example.test','Haja Andrianina','+261340200116','Vakinankaratra','Lot CO 204','Antsirabe','Tovo Andrianina','+261340900216','COMMERCIAL','AST',700000,'CDI','2025-08-15','1998-09-04',NULL,'OFF-COM-AST','Licence en gestion commerciale','Universite d Antananarivo','employe');

INSERT INTO users (username,email,full_name,phone_number,region,photo_profil_path,password_hash,email_verified_at,administrative_validation_status_id)
SELECT e.username,e.email,e.nom,e.telephone,e.region,'pdp/pdp_'||e.username||'.png',
       '$2a$12$C8Q12AOPdn8BE4XPcsZ.e.jmVY/P70DhH5rQkznchEqbpySJasiHq',CURRENT_TIMESTAMP,s.id
FROM seed_employes e JOIN statuts_validation_compte s ON s.code='APPROVED' ORDER BY e.username;
INSERT INTO user_roles (user_id,role_id)
SELECT u.id,r.id FROM seed_employes e JOIN users u ON u.username=e.username JOIN roles r ON r.role_name=e.role_code;
INSERT INTO employes (user_id,poste_id,provenance_id,date_embauche,date_naissance)
SELECT u.id,p.id,pr.id,e.date_embauche,e.date_naissance
FROM seed_employes e JOIN users u ON u.username=e.username JOIN departements d ON d.code=e.departement
JOIN postes p ON p.departement_id=d.id AND p.code_matricule=e.poste
JOIN provenances_employe pr ON pr.code='RECRUTEMENT'
ORDER BY e.username COLLATE "C";

-- Effectifs minimaux parametres par l Administration; RH n est pas proprietaire de ces regles.
INSERT INTO regles_effectif_minimal (poste_id,minimum_presence,date_debut,modifie_par_id,motif)
SELECT p.id, CASE WHEN p.code_matricule='MGR' THEN 2 WHEN p.code_matricule='AST' THEN 1 ELSE 2 END,
       CURRENT_DATE,admin.id,'Cible de demonstration initiale; ajustable par Administration.'
FROM postes p CROSS JOIN users admin WHERE admin.username='demo_administration';

-- Circuit manager -> RH -> Finance (le validateur Finance est un manager rattache au departement Finance).
INSERT INTO circuits_validation_demande (code,libelle,actif) VALUES ('STANDARD','Validation manager, RH puis Finance',TRUE);
INSERT INTO etapes_circuit_validation (circuit_id,ordre_etape,code,libelle)
SELECT c.id,1,'VALIDATION_RH','Validation Ressources humaines' FROM circuits_validation_demande c WHERE c.code='STANDARD';
INSERT INTO etapes_circuit_validation (circuit_id,ordre_etape,code,libelle)
SELECT c.id,2,'VALIDATION_FINANCE','Validation du departement Finance' FROM circuits_validation_demande c WHERE c.code='STANDARD';
INSERT INTO etapes_validateurs_roles (etape_circuit_id,role_id)
SELECT e.id,r.id FROM etapes_circuit_validation e JOIN circuits_validation_demande c ON c.id=e.circuit_id
JOIN roles r ON r.role_name='rh' WHERE c.code='STANDARD' AND e.code='VALIDATION_RH';
INSERT INTO etapes_validateurs_departements (etape_circuit_id,departement_id)
SELECT e.id,d.id FROM etapes_circuit_validation e JOIN circuits_validation_demande c ON c.id=e.circuit_id
JOIN departements d ON d.code='FINANCE' WHERE c.code='STANDARD' AND e.code='VALIDATION_FINANCE';

CREATE TEMP TABLE seed_demandes (
 code VARCHAR(40), departement VARCHAR(30), manager_username VARCHAR(50), poste VARCHAR(5), motif VARCHAR(30),
 effectif INTEGER, budget NUMERIC(14,2), justification TEXT, date_souhaitee DATE, statut VARCHAR(30),
 soumise_at TIMESTAMPTZ, offre_titre VARCHAR(150), offre_description TEXT, competences TEXT,
 offre_statut VARCHAR(20), date_publication DATE, date_cloture DATE, recrute INTEGER
) ON COMMIT DROP;
INSERT INTO seed_demandes VALUES
 ('OFF-IT-MGR','INFORMATIQUE','mgr_it_01','MGR','AUGMENT_EFFECTIF',2,1800000,'Constituer le binome de pilotage informatique afin de separer le suivi des applications et celui du support.','2022-02-15','OFFRE_CREEE','2021-12-01','Manager informatique','Piloter la priorisation des demandes, accompagner les collaborateurs et coordonner le suivi des applications et des incidents.','Management, organisation, coordination, suivi operationnel','CLOTUREE','2022-01-01','2022-01-31',2),
 ('OFF-LOG-MGR','LOGISTIQUE','mgr_log_01','MGR','AUGMENT_EFFECTIF',2,1750000,'Renforcer le pilotage des flux et assurer une delegation de responsabilite sur les operations logistiques.','2021-04-15','OFFRE_CREEE','2021-02-01','Manager logistique','Organiser les flux, suivre les niveaux de stock, coordonner les equipes et traiter les alertes d approvisionnement.','Management, gestion des stocks, planification, securite','CLOTUREE','2021-03-01','2021-03-31',2),
 ('OFF-FIN-MGR','FINANCE','mgr_fin_01','MGR','AUGMENT_EFFECTIF',2,1900000,'Renforcer le controle des operations financieres et garantir la continuite des clotures periodiques.','2023-05-15','OFFRE_CREEE','2023-03-01','Manager finance','Coordonner les travaux comptables, suivre le budget et organiser les controles et clotures periodiques.','Management, budget, comptabilite, controle interne','CLOTUREE','2023-04-01','2023-04-30',2),
 ('OFF-COM-MGR','COMMERCIAL','mgr_com_01','MGR','AUGMENT_EFFECTIF',2,1800000,'Structurer le suivi commercial et partager la responsabilite des objectifs et de la relation client.','2021-06-15','OFFRE_CREEE','2021-04-01','Manager commercial','Organiser les activites commerciales, accompagner les equipes de vente et suivre les objectifs et la satisfaction client.','Management, vente, relation client, analyse d activite','CLOTUREE','2021-05-01','2021-05-31',2),
 ('OFF-IT-DEV','INFORMATIQUE','mgr_it_01','DEV','POSTE_VACANT',2,1200000,'Pourvoir deux postes deja autorises pour maintenir la capacite de developpement et de maintenance des applications internes.','2022-07-15','OFFRE_CREEE','2022-05-01','Developpeur junior','Contribuer a la conception, au developpement, aux tests et a la maintenance des applications internes sous la supervision de l equipe.','Java, API REST, SQL, tests, Git','CLOTUREE','2022-06-01','2022-06-30',2),
 ('OFF-IT-AST','INFORMATIQUE','mgr_it_02','AST','POSTE_VACANT',2,750000,'Pourvoir deux postes autorises d appui administratif et de coordination du departement.','2024-03-15','OFFRE_CREEE','2024-01-01','Assistant informatique','Assurer le suivi des demandes, organiser les interventions et tenir a jour les documents et inventaires du departement.','Organisation, bureautique, suivi des demandes, communication','CLOTUREE','2024-02-01','2024-02-29',2),
 ('OFF-LOG-AGENT','LOGISTIQUE','mgr_log_01','AGL','POSTE_VACANT',2,850000,'Pourvoir deux postes autorises afin de separer les receptions, les inventaires et la preparation des sorties.','2023-08-15','OFFRE_CREEE','2023-06-01','Agent logistique','Controler les receptions, mettre a jour les mouvements de stock et preparer les sorties selon les demandes validees.','Inventaire, reception, rangement, preparation de commandes','CLOTUREE','2023-07-01','2023-07-31',2),
 ('OFF-LOG-AST','LOGISTIQUE','mgr_log_02','AST','POSTE_VACANT',2,700000,'Pourvoir deux postes autorises pour fiabiliser la saisie des mouvements et le classement des documents logistiques.','2024-05-15','OFFRE_CREEE','2024-03-01','Assistant logistique','Saisir les mouvements, suivre les documents de livraison et preparer les tableaux de suivi des flux.','Bureautique, classement, suivi des stocks, rigueur','CLOTUREE','2024-04-01','2024-04-30',2),
 ('OFF-FIN-CPT','FINANCE','mgr_fin_01','CPT','POSTE_VACANT',2,1300000,'Pourvoir deux postes autorises pour repartir la saisie, les rapprochements et la preparation des pieces de cloture.','2023-10-15','OFFRE_CREEE','2023-08-01','Comptable','Assurer la tenue des pieces comptables, les rapprochements et la preparation des situations periodiques sous validation du manager.','Comptabilite, rapprochement, Excel, controle de pieces','CLOTUREE','2023-09-01','2023-09-30',2),
 ('OFF-FIN-AST','FINANCE','mgr_fin_02','AST','POSTE_VACANT',2,740000,'Pourvoir deux postes autorises d appui a la collecte, au controle formel et au classement des justificatifs.','2026-09-15','OFFRE_CREEE','2026-07-01','Assistant finance','Verifier la presence des justificatifs, classer les pieces et preparer les dossiers de suivi pour les comptables.','Bureautique, classement, controle formel, discretion','CLOTUREE','2026-08-01','2026-08-31',2),
 ('OFF-COM-SALES','COMMERCIAL','mgr_com_01','COM','POSTE_VACANT',2,1250000,'Pourvoir deux postes autorises de vente afin d assurer la couverture des clients existants et la prospection.','2024-06-15','OFFRE_CREEE','2024-04-01','Commercial','Prospecter de nouveaux clients, presenter les offres, formaliser les propositions et assurer un suivi regulier du portefeuille.','Prospection, negociation, relation client, reporting','CLOTUREE','2024-05-01','2024-05-31',2),
 ('OFF-COM-AST','COMMERCIAL','mgr_com_02','AST','POSTE_VACANT',2,720000,'Pourvoir deux postes autorises pour preparer les offres commerciales et assurer le suivi administratif des ventes.','2026-08-15','OFFRE_CREEE','2026-06-01','Assistant commercial','Preparer les propositions, mettre a jour les dossiers clients et coordonner les elements administratifs des ventes.','Bureautique, suivi commercial, relation client, organisation','CLOTUREE','2026-07-01','2026-07-31',2),
 ('OFF-IT-DEV-EXP','INFORMATIQUE','mgr_it_01','DEV','AUGMENT_EFFECTIF',1,1300000,'Ajouter une place pour absorber la feuille de route applicative et reduire le delai de traitement du backlog.','2027-01-15','OFFRE_CREEE',CURRENT_DATE-5,'Developpeur junior - renfort applicatif','Participer aux evolutions du portail interne, aux tests de non-regression et a la documentation technique.','Java, SQL, tests, documentation','PUBLIEE',CURRENT_DATE-3,CURRENT_DATE+27,0),
 ('OFF-COM-SALES-EXP','COMMERCIAL','mgr_com_02','COM','AUGMENT_EFFECTIF',1,1300000,'Ouvrir une place supplementaire pour developper la prospection sur le secteur et reduire les delais de suivi client.','2027-01-15','OFFRE_CREEE',CURRENT_DATE-5,'Commercial - developpement de secteur','Developper un portefeuille de prospects, assurer les rendez-vous et suivre les offres jusqu a leur conclusion.','Prospection, negociation, suivi de portefeuille','PUBLIEE',CURRENT_DATE-3,CURRENT_DATE+27,0),
 ('REQ-15','INFORMATIQUE','mgr_it_02','DEV','AUGMENT_EFFECTIF',1,1250000,'Demande soumise pour renforcer la maintenance applicative; le budget et le calendrier sont en attente de validation Finance.',CURRENT_DATE+60,'A_VALIDER_FINANCE',CURRENT_TIMESTAMP-INTERVAL '4 days',NULL,NULL,NULL,NULL,NULL,NULL,0),
 ('REQ-16','LOGISTIQUE','mgr_log_02','AGL','AUGMENT_EFFECTIF',1,900000,'Demande pour absorber la hausse saisonniere des receptions et fiabiliser les inventaires; validation budgetaire en attente.',CURRENT_DATE+60,'A_VALIDER_FINANCE',CURRENT_TIMESTAMP-INTERVAL '4 days',NULL,NULL,NULL,NULL,NULL,NULL,0),
 ('REQ-17','FINANCE','mgr_fin_02','CPT','AUGMENT_EFFECTIF',1,1400000,'Demande de renfort pour la separation des controles et le traitement des clotures mensuelles; validation Finance en attente.',CURRENT_DATE+60,'A_VALIDER_FINANCE',CURRENT_TIMESTAMP-INTERVAL '4 days',NULL,NULL,NULL,NULL,NULL,NULL,0),
 ('REQ-18','COMMERCIAL','mgr_com_01','COM','AUGMENT_EFFECTIF',1,1450000,'Demande de renfort rejetee: le plan de prospection n est pas encore valide pour le prochain exercice.',CURRENT_DATE+75,'REFUSEE',CURRENT_TIMESTAMP-INTERVAL '30 days',NULL,NULL,NULL,NULL,NULL,NULL,0),
 ('REQ-19','FINANCE','mgr_fin_01','AST','AUGMENT_EFFECTIF',1,800000,'Demande de renfort rejetee: la redistribution des taches doit etre mesuree avant toute hausse durable des effectifs.',CURRENT_DATE+75,'REFUSEE',CURRENT_TIMESTAMP-INTERVAL '29 days',NULL,NULL,NULL,NULL,NULL,NULL,0),
 ('REQ-20','INFORMATIQUE','mgr_it_02','AST','AUGMENT_EFFECTIF',1,850000,'Demande rejetee: le budget est reporte au prochain cycle apres reestimation de la charge de support.',CURRENT_DATE+75,'REFUSEE',CURRENT_TIMESTAMP-INTERVAL '28 days',NULL,NULL,NULL,NULL,NULL,NULL,0);

-- L effectif initial des postes a deja ete autorise. Les demandes d augmentation approuvees ont ajoute leur capacite.
INSERT INTO demandes_recrutement (departement_id,demandeur_id,poste_id,motif_id,effectif,effectif_recrute,budget_salaire_base,justification,date_souhaitee,statut_id,circuit_id,soumise_at,created_at)
SELECT d.id,u.id,p.id,m.id,s.effectif,s.recrute,s.budget,s.justification,s.date_souhaitee,st.id,c.id,s.soumise_at,s.soumise_at
FROM seed_demandes s JOIN departements d ON d.code=s.departement JOIN users u ON u.username=s.manager_username
JOIN postes p ON p.departement_id=d.id AND p.code_matricule=s.poste JOIN motifs_recrutement m ON m.code=s.motif
JOIN statuts_demande_recrutement st ON st.code=s.statut JOIN circuits_validation_demande c ON c.code='STANDARD';

-- Journal des changements de capacite: demandes approuvees, postes vacants et deux ouvertures en cours.
INSERT INTO historique_effectifs_poste (poste_id,ancien_effectif,nouvel_effectif,type_evenement_id,demande_id,acteur_id,motif,date_effet)
SELECT p.id, CASE WHEN s.code LIKE 'OFF-%-MGR' THEN 0 ELSE 2 END,
       CASE WHEN s.code IN ('OFF-IT-DEV-EXP','OFF-COM-SALES-EXP') THEN 3 ELSE 2 END,
       te.id,dr.id,u.id,s.justification,COALESCE(s.soumise_at,CURRENT_TIMESTAMP)
FROM seed_demandes s JOIN demandes_recrutement dr ON dr.justification=s.justification
JOIN departements d ON d.code=s.departement JOIN postes p ON p.departement_id=d.id AND p.code_matricule=s.poste
JOIN users u ON u.username=CASE WHEN s.statut='OFFRE_CREEE' AND s.code IN ('OFF-IT-DEV-EXP','OFF-COM-SALES-EXP') THEN 'demo_administration' ELSE s.manager_username END
JOIN types_evenement_effectif te ON te.code=CASE
 WHEN s.motif='AUGMENT_EFFECTIF' THEN 'AUGMENTATION_APPROUVEE' ELSE 'INITIALISATION' END
WHERE s.code LIKE 'OFF-%' AND s.statut='OFFRE_CREEE';

-- Historique de validation RH puis Finance. Les trois demandes en attente n ont pas encore de decision Finance.
INSERT INTO validations_demande (demande_id,etape_circuit_id,validateur_id,decision_id,commentaire,decidee_at)
SELECT dr.id,ec.id,rh.id,dec.id,
       CASE WHEN s.statut='REFUSEE' THEN s.justification ELSE 'Demande examinee par les RH; effectif, motif et salaire de base verifies.' END,
       CASE WHEN s.statut='REFUSEE' THEN s.soumise_at+INTERVAL '2 days' ELSE s.soumise_at+INTERVAL '1 day' END
FROM seed_demandes s JOIN demandes_recrutement dr ON dr.justification=s.justification
JOIN etapes_circuit_validation ec ON ec.code='VALIDATION_RH' JOIN circuits_validation_demande c ON c.id=ec.circuit_id AND c.code='STANDARD'
JOIN users rh ON rh.username='demo_rh'
JOIN decisions_validation_demande dec ON dec.code=CASE WHEN s.statut='REFUSEE' THEN 'REFUSEE' ELSE 'APPROUVEE' END
WHERE s.statut IN ('OFFRE_CREEE','A_VALIDER_FINANCE','REFUSEE');
INSERT INTO validations_demande (demande_id,etape_circuit_id,validateur_id,decision_id,commentaire,decidee_at)
SELECT dr.id,ec.id,finance_manager.id,dec.id,'Budget par personne verifie par un manager rattache au departement Finance.',s.soumise_at+INTERVAL '2 days'
FROM seed_demandes s JOIN demandes_recrutement dr ON dr.justification=s.justification
JOIN etapes_circuit_validation ec ON ec.code='VALIDATION_FINANCE' JOIN circuits_validation_demande c ON c.id=ec.circuit_id AND c.code='STANDARD'
JOIN users finance_manager ON finance_manager.username='mgr_fin_01'
JOIN decisions_validation_demande dec ON dec.code='APPROUVEE'
WHERE s.statut='OFFRE_CREEE';

INSERT INTO offres_emploi (demande_id,cree_par_id,titre,description,competences,statut_id,date_publication,date_cloture,created_at)
SELECT dr.id,admin.id,s.offre_titre,s.offre_description,s.competences,st.id,s.date_publication,s.date_cloture,
       COALESCE(s.soumise_at,CURRENT_TIMESTAMP)
FROM seed_demandes s JOIN demandes_recrutement dr ON dr.justification=s.justification
JOIN users admin ON admin.username='demo_administration'
JOIN statuts_offre_emploi st ON st.code=s.offre_statut WHERE s.offre_titre IS NOT NULL;

-- Historique et candidatures finalisees pour les 24 salaries. Ces embauches couvrent exactement les 12 offres cloturees.
INSERT INTO candidatures (offre_id,candidat_id,statut_id,cv_path,envoyee_at,updated_at)
SELECT o.id,u.id,sc.id,'cv/cv_'||u.username||'.pdf',e.date_embauche-INTERVAL '35 days',e.date_embauche
FROM seed_employes e JOIN users u ON u.username=e.username
JOIN seed_demandes sd ON sd.code=e.offre_code JOIN demandes_recrutement dr ON dr.justification=sd.justification
JOIN offres_emploi o ON o.demande_id=dr.id JOIN statuts_candidature sc ON sc.code='EMBAUCHEE';
INSERT INTO historique_candidatures (candidature_id,statut_id,acteur_id,commentaire,created_at)
SELECT c.id,st.id,acteur.id,stage.commentaire,stage.date_action
FROM candidatures c JOIN users candidat ON candidat.id=c.candidat_id
JOIN seed_employes e ON e.username=candidat.username
CROSS JOIN LATERAL (VALUES
 ('ENVOYEE_RH','Dossier de candidature transmis aux RH.',e.date_embauche-INTERVAL '35 days'),
 ('PRESELECTIONNEE','Profil retenu pour un premier entretien.',e.date_embauche-INTERVAL '28 days'),
 ('ENTRETIEN_EFFECTUE','Entretien de recrutement effectue.',e.date_embauche-INTERVAL '18 days'),
 ('RETENUE_ENTRETIEN','Candidat retenu apres evaluation.',e.date_embauche-INTERVAL '14 days'),
 ('QUESTIONNAIRE_REMPLI','Questionnaire d embauche complete.',e.date_embauche-INTERVAL '10 days'),
 ('CONTRAT_PROPOSE','Proposition de contrat acceptee.',e.date_embauche-INTERVAL '7 days'),
 ('EMBAUCHEE','Embauche finalisee et dossier salarie cree.',e.date_embauche)
) AS stage(statut_code,commentaire,date_action)
JOIN statuts_candidature st ON st.code=stage.statut_code
JOIN users acteur ON acteur.username=CASE WHEN stage.statut_code='ENVOYEE_RH' THEN candidat.username ELSE 'demo_rh' END;
INSERT INTO entretiens_candidature (candidature_id,type_id,statut_id,planifie_at,note,cree_par_id,updated_at)
SELECT c.id,t.id,s.id,e.date_embauche-INTERVAL '18 days',
       'Entretien de demonstration effectue; evaluation satisfaisante sur les competences et la communication.',rh.id,e.date_embauche-INTERVAL '18 days'
FROM candidatures c JOIN seed_employes e ON e.username=(SELECT username FROM users WHERE id=c.candidat_id)
JOIN types_entretien t ON t.code='ENTRETIEN_TEST' JOIN statuts_entretien s ON s.code='EFFECTUE'
JOIN users rh ON rh.username='demo_rh';
INSERT INTO questionnaires_embauche (candidature_id,regles_acceptees,date_naissance,date_debut_souhaitee,salaire_base_souhaite,type_contrat_souhaite_id,salaire_base_final,date_debut_finale,type_contrat_final_id,repondu_at)
SELECT c.id,TRUE,e.date_naissance,e.date_embauche,e.salaire,tc.id,e.salaire,e.date_embauche,tc.id,e.date_embauche-INTERVAL '10 days'
FROM candidatures c JOIN users u ON u.id=c.candidat_id JOIN seed_employes e ON e.username=u.username
JOIN types_contrat tc ON tc.code=e.type_contrat;
INSERT INTO parcours (candidature_id,type_parcours_id,niveau_etude_id,intitule,organisme,description,ordre)
SELECT c.id,tp.id,n.id,e.formation,e.organisme,'Formation utilisee pour le dossier de recrutement de demonstration.',1
FROM candidatures c JOIN users u ON u.id=c.candidat_id JOIN seed_employes e ON e.username=u.username
JOIN types_parcours tp ON tp.code='ETUDE' JOIN niveaux_etude n ON n.code=CASE WHEN e.poste='MGR' OR e.poste='CPT' THEN 'BAC_PLUS_5' ELSE 'BAC_PLUS_3' END;
INSERT INTO durees (parcours_id,date_debut,date_fin,en_cours)
SELECT p.id,e.date_naissance+INTERVAL '18 years',e.date_embauche-INTERVAL '3 years',FALSE
FROM parcours p JOIN candidatures c ON c.id=p.candidature_id JOIN users u ON u.id=c.candidat_id
JOIN seed_employes e ON e.username=u.username;
INSERT INTO contrats (candidature_id,numero,type_contrat_id,statut_id,salaire_base_mensuel,date_debut,date_fin,document_path,genere_at)
SELECT c.id,'CTR-'||e.username,tc.id,st.id,e.salaire,e.date_embauche,e.date_fin_contrat,
       'contrats/contrat_'||e.username||'.pdf',e.date_embauche
FROM candidatures c JOIN users u ON u.id=c.candidat_id JOIN seed_employes e ON e.username=u.username
JOIN types_contrat tc ON tc.code=e.type_contrat JOIN statuts_contrat st ON st.code='ACTIF';

-- Dossier administratif: coordonnees, historique auto par trigger et CV versionne pour chaque salarie.
UPDATE informations_personnelles_employe i SET adresse_ligne=e.adresse,ville=e.ville,code_postal='101',pays='Madagascar',
 contact_urgence_nom=e.urgence_nom,contact_urgence_lien='Famille',contact_urgence_telephone=e.urgence_tel,updated_at=CURRENT_TIMESTAMP
FROM employes emp JOIN users u ON u.id=emp.user_id JOIN seed_employes e ON e.username=u.username WHERE i.employe_id=emp.id;
INSERT INTO documents_employe (employe_id,type_document_id,ajoute_par_id)
SELECT emp.id,t.id,admin.id FROM employes emp JOIN users u ON u.id=emp.user_id
JOIN types_documents_employe t ON t.code='CV' JOIN users admin ON admin.username='demo_administration';
INSERT INTO versions_documents_employe (document_id,numero_version,cle_stockage,nom_fichier,type_mime,taille_octets,date_emission,ajoute_par_id)
SELECT doc.id,1,'dossier-employe/'||emp.id||'/00000000-0000-0000-0000-'||LPAD(emp.id::text,12,'0')||'.pdf',
       'cv_'||u.username||'.pdf','application/pdf',871663,emp.date_embauche,admin.id
FROM documents_employe doc JOIN employes emp ON emp.id=doc.employe_id JOIN users u ON u.id=emp.user_id
JOIN types_documents_employe t ON t.id=doc.type_document_id AND t.code='CV'
JOIN users admin ON admin.username='demo_administration';

-- Le contrat figure aussi dans le dossier salarié, en plus de son lien de recrutement.
INSERT INTO documents_employe (employe_id,type_document_id,ajoute_par_id)
SELECT emp.id,t.id,admin.id FROM employes emp JOIN users u ON u.id=emp.user_id
JOIN candidatures c ON c.candidat_id=u.id JOIN contrats ct ON ct.candidature_id=c.id
JOIN types_documents_employe t ON t.code='CONTRAT_TRAVAIL'
JOIN users admin ON admin.username='demo_administration';
INSERT INTO versions_documents_employe (document_id,numero_version,cle_stockage,nom_fichier,type_mime,taille_octets,date_emission,ajoute_par_id)
SELECT doc.id,1,'dossier-employe/'||emp.id||'/10000000-0000-0000-0000-'||LPAD(emp.id::text,12,'0')||'.pdf',
       'contrat_'||u.username||'.pdf','application/pdf',10000,ct.date_debut,admin.id
FROM documents_employe doc JOIN employes emp ON emp.id=doc.employe_id JOIN users u ON u.id=emp.user_id
JOIN types_documents_employe t ON t.id=doc.type_document_id AND t.code='CONTRAT_TRAVAIL'
JOIN candidatures c ON c.candidat_id=u.id JOIN contrats ct ON ct.candidature_id=c.id
JOIN users admin ON admin.username='demo_administration';

-- Notifications de demonstration: action attendue dans le circuit Finance et validation des comptes.
INSERT INTO notifications (destinataire_id,type_id,titre,resume,lien,created_at)
SELECT u.id,t.id,'Demande de recrutement a valider',n.resume,n.lien,CURRENT_TIMESTAMP-INTERVAL '1 hour'
FROM (VALUES
 ('REQ-15','Demande informatique en attente de validation Finance.','/espace/finance/recrutement/demandes'),
 ('REQ-16','Demande logistique en attente de validation Finance.','/espace/finance/recrutement/demandes'),
 ('REQ-17','Demande Finance en attente de validation Finance.','/espace/finance/recrutement/demandes')
) n(code,resume,lien)
JOIN seed_demandes sd ON sd.code=n.code JOIN users u ON u.username='mgr_fin_01'
JOIN types_notifications t ON t.code='DEMANDE_A_VALIDER';
INSERT INTO notifications (destinataire_id,type_id,titre,resume,lien)
SELECT u.id,t.id,'Verification de compte a traiter','Un compte candidat attend la validation administrative.','/espace/gestionnaire/comptes/inscriptions'
FROM users u JOIN types_notifications t ON t.code='INSCRIPTION_A_TRAITER' WHERE u.username='demo_gestionnaire';

-- Bloc 6: calendriers par departement, semaines archivee/courante/S+1 et service de jour.
INSERT INTO types_jour_planning (code,libelle,compte_effectif) VALUES
 ('TRAVAIL','Jour travaille',TRUE),('REPOS','Jour de repos',FALSE),('CONGE','Conge approuve',FALSE),('ABSENCE','Absence',FALSE);
INSERT INTO statuts_planning_equipe (code,libelle) VALUES ('BROUILLON','Brouillon'),('PUBLIE','Publie'),('ARCHIVE','Archive');
INSERT INTO types_evenement_pointage (code,libelle,ordre) VALUES
 ('ARRIVEE','Arrivee au travail',1),('DEBUT_PAUSE','Debut de pause',2),('FIN_PAUSE','Fin de pause',3),('FIN_SERVICE','Fin de service',4);
INSERT INTO statuts_presence_pointage (code,libelle,ordre) VALUES
 ('A_VENIR','A venir',1),('EN_ATTENTE','En attente d arrivee',2),('PRESENT','Present',3),('EN_PAUSE','En pause',4),
 ('TERMINE','Service termine',5),('ABSENT','Absent',6),('NON_PLANIFIE','Non planifie',7),('REPOS','Repos',8);
INSERT INTO niveaux_anomalie_pointage (code,libelle) VALUES ('ATTENTION','A verifier'),('A_VERIFIER','Hors planning'),('INFO','Information');
INSERT INTO types_anomalie_pointage (code,libelle,niveau_id) VALUES
 ('RETARD','Retard',(SELECT id FROM niveaux_anomalie_pointage WHERE code='ATTENTION')),
 ('PAUSE_DEPASSEE','Pause depassee',(SELECT id FROM niveaux_anomalie_pointage WHERE code='ATTENTION')),
 ('DEPART_ANTICIPE','Depart anticipe',(SELECT id FROM niveaux_anomalie_pointage WHERE code='ATTENTION')),
 ('POINTAGE_HORS_PLANNING','Pointage hors planning',(SELECT id FROM niveaux_anomalie_pointage WHERE code='A_VERIFIER')),
 ('RAS','Aucune anomalie',(SELECT id FROM niveaux_anomalie_pointage WHERE code='INFO'));
INSERT INTO regles_temps_travail (code,libelle,valeur_minutes,cree_par_id) VALUES
 ('TOLERANCE_RETARD','Tolerance de retard pour le signalement',5,(SELECT id FROM users WHERE username='demo_administration')),
 ('TOLERANCE_PAUSE','Tolerance de depassement de pause',5,(SELECT id FROM users WHERE username='demo_administration')),
 ('TOLERANCE_DEPART','Tolerance de depart anticipe',5,(SELECT id FROM users WHERE username='demo_administration'));

INSERT INTO parametres_horaires_planning (code,libelle,heure_debut,heure_fin,pause_debut,pause_fin) VALUES
 ('JOUR_STANDARD','Horaire administratif',TIME '08:30',TIME '17:30',TIME '12:30',TIME '13:30');

-- Solde CP demonstratif: regle parametree et mouvements explicites, pas de valeur calculee dans le code.
INSERT INTO regles_acquisition_conge (type_conge_id,jours_par_mois,date_debut,modifie_par_id)
SELECT t.id,2.5,DATE '2026-01-01',u.id FROM types_conge t JOIN users u ON u.username='demo_administration'
WHERE t.code='CONGES_PAYES';
INSERT INTO mouvements_solde_conge (employe_id,type_conge_id,type_mouvement_id,jours,periode_debut,periode_fin,acteur_id,commentaire,created_at)
SELECT e.id,tc.id,tm.id,25.0,DATE '2026-01-01',DATE '2026-10-31',admin.id,
       'Solde de demonstration: acquisition cumulee sur dix mois selon la regle active.',TIMESTAMPTZ '2026-11-01 08:00:00+03'
FROM employes e JOIN types_conge tc ON tc.code='CONGES_PAYES' JOIN types_mouvement_solde_conge tm ON tm.code='ACQUISITION'
JOIN users admin ON admin.username='demo_administration';

-- Six demandes de conge: deux validees, deux chez RH et deux encore chez les managers.
CREATE TEMP TABLE seed_conges (username VARCHAR(50), code VARCHAR(30), debut DATE, fin DATE, jours NUMERIC(7,2), statut VARCHAR(30), motif TEXT, manager VARCHAR(50)) ON COMMIT DROP;
INSERT INTO seed_conges VALUES
 ('emp_it_ast_01','CP','2026-11-17','2026-11-18',2,'APPROUVEE','Conges payes demandes pour un rendez-vous familial planifie.','mgr_it_01'),
 ('emp_fin_ast_01','CP','2026-11-17','2026-11-18',2,'APPROUVEE','Conges payes planifies; les echeances comptables sont couvertes par le binome.','mgr_fin_01'),
 ('emp_log_agent_01','CP','2026-12-01','2026-12-03',3,'A_VALIDER_MANAGER','Demande de conges en attente de la validation du manager.','mgr_log_01'),
 ('emp_com_sales_01','CP','2026-12-07','2026-12-09',3,'A_VALIDER_MANAGER','Demande de conges en attente; le relais portefeuille reste a confirmer.','mgr_com_01'),
 ('emp_it_dev_01','CP','2026-12-14','2026-12-16',3,'A_VALIDER_RH','Le manager a valide; controle du solde et du calendrier par les RH requis.','mgr_it_01'),
 ('emp_fin_cpt_01','CP','2026-12-21','2026-12-23',3,'A_VALIDER_RH','Le manager a valide; verifier le calendrier de cloture avant decision finale.','mgr_fin_01');
INSERT INTO demandes_conge (employe_id,type_conge_id,statut_id,date_debut,date_fin,jours_demandes,motif,soumise_at,decidee_at)
SELECT e.id,tc.id,st.id,s.debut,s.fin,s.jours,s.motif,
       CASE WHEN s.statut='APPROUVEE' THEN CURRENT_TIMESTAMP-INTERVAL '10 days' ELSE CURRENT_TIMESTAMP-INTERVAL '2 days' END,
       CASE WHEN s.statut='APPROUVEE' THEN CURRENT_TIMESTAMP-INTERVAL '5 days' ELSE NULL END
FROM seed_conges s JOIN users u ON u.username=s.username JOIN employes e ON e.user_id=u.id
JOIN types_conge tc ON tc.code='CONGES_PAYES' JOIN statuts_demande_conge st ON st.code=s.statut;
INSERT INTO validations_conge (demande_id,etape_id,decision_id,validateur_id,commentaire,decidee_at)
SELECT dc.id,ec.id,decision.id,manager.id,'Couverture de service verifiee par le manager.',dc.soumise_at+INTERVAL '1 day'
FROM seed_conges s JOIN users u ON u.username=s.username JOIN employes emp ON emp.user_id=u.id
JOIN demandes_conge dc ON dc.employe_id=emp.id AND dc.date_debut=s.debut
JOIN etapes_validation_conge ec ON ec.code='MANAGER' JOIN decisions_conge decision ON decision.code='APPROUVEE'
JOIN users manager ON manager.username=s.manager
WHERE s.statut IN ('APPROUVEE','A_VALIDER_RH');
INSERT INTO validations_conge (demande_id,etape_id,decision_id,validateur_id,commentaire,decidee_at)
SELECT dc.id,ec.id,decision.id,hr.id,'Solde suffisant et absence compatible avec le planning.',dc.soumise_at+INTERVAL '2 days'
FROM seed_conges s JOIN users u ON u.username=s.username JOIN employes emp ON emp.user_id=u.id
JOIN demandes_conge dc ON dc.employe_id=emp.id AND dc.date_debut=s.debut
JOIN etapes_validation_conge ec ON ec.code='RH' JOIN decisions_conge decision ON decision.code='APPROUVEE'
JOIN users hr ON hr.username='demo_rh' WHERE s.statut='APPROUVEE';
INSERT INTO mouvements_solde_conge (employe_id,type_conge_id,type_mouvement_id,demande_id,jours,periode_debut,periode_fin,acteur_id,commentaire)
SELECT emp.id,tc.id,tm.id,dc.id,-dc.jours_demandes,dc.date_debut,dc.date_fin,hr.id,'Deduction apres approbation finale de la demande.'
FROM seed_conges s JOIN users u ON u.username=s.username JOIN employes emp ON emp.user_id=u.id
JOIN demandes_conge dc ON dc.employe_id=emp.id AND dc.date_debut=s.debut
JOIN types_conge tc ON tc.code='CONGES_PAYES' JOIN types_mouvement_solde_conge tm ON tm.code='PRISE'
JOIN users hr ON hr.username='demo_rh' WHERE s.statut='APPROUVEE';
INSERT INTO absences (employe_id,type_absence_id,demande_conge_id,date_debut,date_fin,enregistree_par_id,motif,justifiee)
SELECT emp.id,ta.id,dc.id,dc.date_debut::timestamp+TIME '00:00',dc.date_fin::timestamp+TIME '23:59',hr.id,
       'Absence issue d une demande de conge validee.',TRUE
FROM seed_conges s JOIN users u ON u.username=s.username JOIN employes emp ON emp.user_id=u.id
JOIN demandes_conge dc ON dc.employe_id=emp.id AND dc.date_debut=s.debut
JOIN types_absence ta ON ta.code='ABSENCE_AUTORISEE' JOIN users hr ON hr.username='demo_rh'
WHERE s.statut='APPROUVEE';

-- Trois semaines visibles par departement: archivee, semaine courante et S+1 publiee par le manager.
WITH manager_department(department_code,username) AS (VALUES
 ('INFORMATIQUE','mgr_it_01'),('LOGISTIQUE','mgr_log_01'),('FINANCE','mgr_fin_01'),('COMMERCIAL','mgr_com_01')),
weeks AS (SELECT date_trunc('week',CURRENT_DATE)::date + offset_days AS week_start, offset_days FROM (VALUES (-7),(0),(7)) v(offset_days))
INSERT INTO plannings_equipe (departement_id,semaine_debut,statut_id,cree_par_id,publie_par_id,publie_at,created_at)
SELECT d.id,w.week_start,sp.id,u.id,CASE WHEN w.offset_days=-7 THEN NULL ELSE u.id END,
       CASE WHEN w.offset_days=-7 THEN NULL ELSE (w.week_start-INTERVAL '3 days')::timestamptz END,
       (w.week_start-INTERVAL '10 days')::timestamptz
FROM manager_department md JOIN departements d ON d.code=md.department_code
JOIN users u ON u.username=md.username CROSS JOIN weeks w
JOIN statuts_planning_equipe sp ON sp.code=CASE WHEN w.offset_days=-7 THEN 'ARCHIVE' ELSE 'PUBLIE' END;
INSERT INTO historique_statut_planning (planning_id,ancien_statut_id,nouveau_statut_id,acteur_id,motif,change_at)
SELECT p.id,NULL,s.id,p.cree_par_id,'Publication de la semaine et revue de capacite par le manager.',p.publie_at
FROM plannings_equipe p JOIN statuts_planning_equipe s ON s.id=p.statut_id WHERE s.code='PUBLIE';

INSERT INTO jours_planning_employe (planning_id,employe_id,date_travail,type_jour_id,heure_debut,heure_fin,pause_debut,pause_fin,observation,modifie_par_id)
SELECT plan.id,emp.id,day.service_date,typ.id,
 CASE WHEN typ.code='TRAVAIL' THEN CASE WHEN dept.code='LOGISTIQUE' THEN TIME '07:30' WHEN dept.code='COMMERCIAL' THEN TIME '09:00' ELSE TIME '08:30' END ELSE NULL END,
 CASE WHEN typ.code='TRAVAIL' THEN CASE WHEN dept.code='LOGISTIQUE' THEN TIME '16:30' WHEN dept.code='COMMERCIAL' THEN TIME '18:00' ELSE TIME '17:30' END ELSE NULL END,
 CASE WHEN typ.code='TRAVAIL' THEN CASE WHEN dept.code='COMMERCIAL' THEN TIME '13:00' WHEN dept.code='LOGISTIQUE' THEN TIME '11:30' ELSE TIME '12:30' END ELSE NULL END,
 CASE WHEN typ.code='TRAVAIL' THEN CASE WHEN dept.code='COMMERCIAL' THEN TIME '14:00' WHEN dept.code='LOGISTIQUE' THEN TIME '12:30' ELSE TIME '13:30' END ELSE NULL END,
 CASE WHEN typ.code='CONGE' THEN 'Conge approuve - absent du planning de service.'
      WHEN typ.code='REPOS' THEN 'Repos hebdomadaire.' ELSE 'Jour de travail planifie.' END,
 plan.cree_par_id
FROM plannings_equipe plan JOIN departements dept ON dept.id=plan.departement_id
JOIN employes emp ON emp.poste_id IN (SELECT p.id FROM postes p WHERE p.departement_id=dept.id)
CROSS JOIN LATERAL generate_series(plan.semaine_debut,plan.semaine_debut+6,INTERVAL '1 day') AS day(service_date)
LEFT JOIN demandes_conge dc ON dc.employe_id=emp.id AND dc.statut_id=(SELECT id FROM statuts_demande_conge WHERE code='APPROUVEE')
 AND day.service_date BETWEEN dc.date_debut AND dc.date_fin
JOIN types_jour_planning typ ON typ.code=CASE WHEN dc.id IS NOT NULL THEN 'CONGE'
 WHEN EXTRACT(ISODOW FROM day.service_date) IN (6,7) THEN 'REPOS' ELSE 'TRAVAIL' END;

-- Regles et parametres de paie restent modifiables par le role Administration.
INSERT INTO regimes_paie (code,libelle) VALUES ('GENERAL','Regime general non agricole'),('AGRICOLE','Regime agricole');
INSERT INTO categories_professionnelles (code,libelle) VALUES
 ('5B','Categorie 5B'),('5A','Categorie 5A'),('M1','Categorie M1'),('OS1','Categorie OS1');
INSERT INTO categories_rubrique_paie (code,libelle) VALUES
 ('GAIN','Gain'),('RETENUE_SALARIALE','Retenue salariale'),('CHARGE_PATRONALE','Charge patronale'),('INFORMATION','Information');
INSERT INTO modes_calcul_rubrique_paie (code,libelle) VALUES
 ('CONTRAT','Valeur du contrat'),('VARIABLE_VALIDEE','Variable saisie et validee'),('REGLE_BASEE','Regle datee'),('SAISIE_VALIDEE','Montant saisi et valide');
INSERT INTO rubriques_paie (code,libelle,categorie_id,mode_calcul_id,date_debut,assujettie_irsa,assujettie_cnaps,assujettie_organisme_sante,saisie_manuelle_autorisee,source_regle)
SELECT r.code,r.libelle,c.id,m.id,DATE '2026-01-01',r.irsa,r.cnaps,r.sante,r.manuel,r.source
FROM (VALUES
 ('SALAIRE_BASE','Salaire de base','GAIN','CONTRAT',TRUE,TRUE,TRUE,FALSE,'Contrat de travail'),
 ('RETENUE_ABSENCE','Retenue sur absence non remuneree','RETENUE_SALARIALE','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Absence validee et non remuneree'),
 ('PRIME_ASSIDUITE','Prime assiduite','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Presence et regle datee par Administration'),
 ('PRIME_PRODUCTION','Prime production','GAIN','VARIABLE_VALIDEE',TRUE,TRUE,TRUE,TRUE,'Saisie justifiee et validee; module de production a venir'),
 ('PRIME_QUALITE','Prime qualite','GAIN','VARIABLE_VALIDEE',TRUE,TRUE,TRUE,TRUE,'Saisie justifiee et validee; module qualite a venir'),
 ('PRIME_ANCIENNETE','Prime anciennete','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Anciennete et bareme configure par Administration'),
 ('PRIME_FONCTION','Prime fonction ou responsabilite','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Fonction et regle configuree par Administration'),
 ('RAPPEL','Rappel de salaire','GAIN','SAISIE_VALIDEE',TRUE,TRUE,TRUE,TRUE,'Correction motivee et validee'),
 ('HEURES_SUP','Heures supplementaires','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Pointages valides et regles de majoration du cadrage'),
 ('TRAVAIL_DIMANCHE','Travail du dimanche','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Pointage valide du dimanche; taux horaire multiplie par 2'),
 ('TRAVAIL_NUIT','Travail de nuit','GAIN','REGLE_BASEE',TRUE,TRUE,TRUE,FALSE,'Pointage valide entre 22 h et 4 h; taux horaire multiplie par 1,5'),
 ('CNAPS_SALARIAL','Cotisation CNaPS salariale','RETENUE_SALARIALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Bareme applicable a verifier'),
 ('SANTE_SALARIAL','Cotisation sante salariale','RETENUE_SALARIALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Affiliation et bareme a verifier'),
 ('IRSA','Impot sur les revenus salariaux','RETENUE_SALARIALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Bareme fiscal applicable a verifier'),
 ('AVANCE','Remboursement avance salariale','RETENUE_SALARIALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Avance versee et validee'),
 ('CNAPS_PATRONAL','Cotisation CNaPS patronale','CHARGE_PATRONALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Bareme applicable a verifier'),
 ('SANTE_PATRONAL','Cotisation sante patronale','CHARGE_PATRONALE','REGLE_BASEE',FALSE,FALSE,FALSE,FALSE,'Affiliation et bareme a verifier')
) AS r(code,libelle,categorie,mode,irsa,cnaps,sante,manuel,source)
JOIN categories_rubrique_paie c ON c.code=r.categorie JOIN modes_calcul_rubrique_paie m ON m.code=r.mode;

INSERT INTO parametres_paie (code,libelle,valeur,unite,regime_id,date_debut,source_regle,a_verifier)
SELECT x.code,x.libelle,x.valeur,x.unite,r.id,DATE '2026-01-01',x.source,TRUE
FROM (VALUES
 ('HEURES_MENSUELLES_REFERENCE','Reference heures mensuelles',173.33::numeric,'heure','GENERAL','Valeur de demonstration a confirmer selon le regime'),
 ('HEURES_MENSUELLES_REFERENCE','Reference heures mensuelles',200.00::numeric,'heure','AGRICOLE','Valeur de demonstration a confirmer selon le regime'),
 ('SME_MENSUEL','Salaire minimum mensuel indicatif',350000::numeric,'MGA/mois','GENERAL','Valeur de demonstration a confirmer avant paie'),
 ('SME_MENSUEL','Salaire minimum mensuel indicatif',350000::numeric,'MGA/mois','AGRICOLE','Valeur de demonstration a confirmer avant paie'),
 ('TAUX_CNAPS_SALARIAL','Taux CNaPS salarial indicatif',0.01::numeric,'ratio',NULL,'A verifier selon les textes applicables'),
 ('CNAPS_PLAFOND_MULTIPLICATEUR','Multiplicateur du plafond CNaPS SME',8::numeric,'multiplicateur',NULL,'Valeur indiquée par le cadrage; vérifier avant usage'),
 ('TAUX_CNAPS_PATRONAL','Taux CNaPS patronal indicatif',0.13::numeric,'ratio','GENERAL','A verifier selon les textes applicables'),
 ('TAUX_CNAPS_PATRONAL','Taux CNaPS patronal indicatif',0.08::numeric,'ratio','AGRICOLE','A verifier selon les textes applicables'),
 ('TAUX_SANTE_SALARIAL','Taux sante salarial indicatif',0.01::numeric,'ratio',NULL,'A verifier selon affiliation'),
 ('TAUX_SANTE_PATRONAL','Taux sante patronal indicatif',0.06::numeric,'ratio',NULL,'A verifier selon affiliation'),
 ('PRIME_ASSIDUITE_MENSUELLE','Prime assiduite mensuelle',0::numeric,'MGA/mois',NULL,'A zero par defaut; activer uniquement apres validation de la regle par Administration'),
 ('PRIME_PRODUCTION_PLAFOND','Plafond prime production',0::numeric,'MGA/mois',NULL,'0 sans plafond; aucune evaluation automatique sans module'),
 ('PRIME_QUALITE_PLAFOND','Plafond prime qualite',0::numeric,'MGA/mois',NULL,'0 sans plafond; aucune evaluation automatique sans module'),
 ('PRIME_ANCIENNETE_TAUX','Taux prime anciennete indicatif',0::numeric,'ratio',NULL,'A parametrer selon politique de remuneration'),
 ('PRIME_FONCTION_MONTANT','Prime responsabilite indicative',0::numeric,'MGA/mois',NULL,'A parametrer selon poste et politique interne'),
 ('HEURES_SUP_SEUIL_1','Seuil hebdomadaire premiere tranche heures sup',12::numeric,'heure/semaine',NULL,'Cadre fourni dans finalbloc.txt; verifier avant usage'),
 ('HEURES_SUP_SEUIL_2','Plafond hebdomadaire heures supplementaires',20::numeric,'heure/semaine',NULL,'Cadre fourni dans finalbloc.txt'),
 ('HEURES_MAX_SEMAINE','Plafond hebdomadaire total de travail',60::numeric,'heure/semaine',NULL,'Cadre fourni dans finalbloc.txt'),
 ('MAJORATION_HEURES_SUP_1','Majoration premiere tranche heures sup',0.30::numeric,'ratio',NULL,'Cadre fourni dans finalbloc.txt; verifier avant usage'),
 ('MAJORATION_HEURES_SUP_2','Majoration deuxieme tranche heures sup',0.50::numeric,'ratio',NULL,'Cadre fourni dans finalbloc.txt; verifier avant usage'),
 ('MAJORATION_DIMANCHE','Majoration travail du dimanche',1.00::numeric,'ratio',NULL,'Multiplicateur total 2x; cadre fourni dans finalbloc.txt, a verifier'),
 ('MAJORATION_NUIT','Majoration travail de nuit',0.50::numeric,'ratio',NULL,'Multiplicateur total 1.5x; cadre fourni dans finalbloc.txt, a verifier'),
 ('HEURE_NUIT_DEBUT','Heure de debut du travail de nuit',22::numeric,'heure',NULL,'Cadre fourni dans finalbloc.txt; verifier avant usage'),
 ('HEURE_NUIT_FIN','Heure de fin du travail de nuit',4::numeric,'heure',NULL,'Cadre fourni dans finalbloc.txt; verifier avant usage'),
 ('MINIMUM_IRSA','Minimum de perception IRSA indicatif',3000::numeric,'MGA/mois',NULL,'A confirmer avant calcul de paie')
) AS x(code,libelle,valeur,unite,regime_code,source)
LEFT JOIN regimes_paie r ON r.code=x.regime_code;
INSERT INTO tranches_irsa (date_debut,borne_inferieure,borne_superieure,taux,source_regle) VALUES
 (DATE '2026-01-01',0,350000,0,'Bareme indicatif de demonstration - verifier avant usage'),
 (DATE '2026-01-01',350000,400000,0.05,'Bareme indicatif de demonstration - verifier avant usage'),
 (DATE '2026-01-01',400000,500000,0.10,'Bareme indicatif de demonstration - verifier avant usage'),
 (DATE '2026-01-01',500000,600000,0.15,'Bareme indicatif de demonstration - verifier avant usage'),
 (DATE '2026-01-01',600000,4000000,0.20,'Bareme indicatif de demonstration - verifier avant usage'),
 (DATE '2026-01-01',4000000,NULL,0.25,'Bareme indicatif de demonstration - verifier avant usage');
INSERT INTO statuts_periode_paie (code,libelle) VALUES
 ('BROUILLON','Brouillon'),('CALCULEE','Calculee'),('VALIDEE','Validee'),('PAYEE','Payee'),('CLOTUREE','Cloturee');
INSERT INTO statuts_variable_paie (code,libelle) VALUES
 ('A_VALIDER','A valider'),('VALIDEE','Validee'),('REFUSEE','Refusee'),('INTEGREE','Integree au bulletin');
INSERT INTO statuts_bulletin_paie (code,libelle) VALUES ('CALCULE','Calcule'),('VALIDE','Valide'),('PAYE','Paye');
INSERT INTO statuts_avance_paie (code,libelle) VALUES
 ('A_VALIDER','A valider'),('VERSEE','Versee'),('DEDUITE','Deduite'),('ANNULEE','Annulee');
INSERT INTO affectations_regime_paie (employe_id,regime_id,categorie_id,date_debut,modifie_par_id,motif)
SELECT e.id,r.id,c.id,e.date_embauche,admin.id,'Affectation de demonstration; regime et categorie a confirmer selon le secteur.'
FROM employes e JOIN regimes_paie r ON r.code='GENERAL' JOIN users u ON u.id=e.user_id
JOIN seed_employes s ON s.username=u.username
JOIN categories_professionnelles c ON c.code=CASE WHEN s.poste='MGR' THEN 'M1' WHEN s.departement='LOGISTIQUE' AND s.poste='AGL' THEN 'OS1' WHEN s.poste='CPT' THEN '5A' ELSE '5B' END
JOIN users admin ON admin.username='demo_administration';

COMMIT;
