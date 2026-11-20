INSERT INTO types_initiative_sortie(code,libelle) VALUES
 ('SALARIE','À l’initiative du salarié'),('EMPLOYEUR','À l’initiative de l’employeur'),
 ('ECHEANCE','Échéance du contrat'),('COMMUN','Accord des parties'),('EVENEMENT','Événement indépendant')
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle;

INSERT INTO types_notifications(code,libelle) VALUES
 ('DOSSIER_SORTIE','Dossier de sortie'),('STC_A_CONTROLER','Solde de tout compte à contrôler'),
 ('SORTIE_FINALISEE','Sortie de salarié finalisée')
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle;

INSERT INTO types_ligne_solde_sortie(code,libelle,nature_id,ordre)
SELECT v.code,v.libelle,c.id,v.ordre FROM (VALUES
 ('SALAIRE_DU','Salaire brut restant dû','GAIN',10),
 ('CONGES_NON_PRIS','Indemnité compensatrice de congés payés','GAIN',20),
 ('PREAVIS_DU','Indemnité compensatrice de préavis (si due)','GAIN',30),
 ('RUPTURE_DUE','Indemnité de rupture légalement due (si applicable)','GAIN',40),
 ('AUTRE_GAIN','Autre élément brut dû','GAIN',50),
 ('AVANCE','Avance salariale restant à récupérer','RETENUE_SALARIALE',60),
 ('RETENUE','Retenue légale ou autorisée','RETENUE_SALARIALE',70),
 ('COTISATION','Cotisation salariale','RETENUE_SALARIALE',80)
) AS v(code,libelle,nature,ordre) JOIN categories_rubrique_paie c ON c.code=v.nature
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,nature_id=EXCLUDED.nature_id,ordre=EXCLUDED.ordre;

INSERT INTO motifs_sortie(code,libelle,initiative_id,ordre) VALUES
 ('DEMISSION','Démission',(SELECT id FROM types_initiative_sortie WHERE code='SALARIE'),10),
 ('LICENCIEMENT','Licenciement',(SELECT id FROM types_initiative_sortie WHERE code='EMPLOYEUR'),20),
 ('FIN_CDD','Fin de contrat à durée déterminée',(SELECT id FROM types_initiative_sortie WHERE code='ECHEANCE'),30),
 ('ACCORD_MUTUEL','Rupture d’un commun accord',(SELECT id FROM types_initiative_sortie WHERE code='COMMUN'),40),
 ('RETRAITE','Départ à la retraite',(SELECT id FROM types_initiative_sortie WHERE code='SALARIE'),50),
 ('DECES','Décès du salarié',(SELECT id FROM types_initiative_sortie WHERE code='EVENEMENT'),60),
 ('INCAPACITE','Inaptitude ou incapacité reconnue',(SELECT id FROM types_initiative_sortie WHERE code='EMPLOYEUR'),70)
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,initiative_id=EXCLUDED.initiative_id,ordre=EXCLUDED.ordre,actif=TRUE;

INSERT INTO statuts_sortie(code,libelle,ordre,terminal) VALUES
 ('SOUMISE_RH','Soumise aux ressources humaines',10,FALSE),
 ('EN_PREPARATION','Préparation du dossier de sortie',20,FALSE),
 ('STC_A_VERIFIER','Décompte à vérifier par les ressources humaines',30,FALSE),
 ('STC_VALIDEE','Décompte contrôlé et validé',40,FALSE),
 ('STC_PAYE','Solde de tout compte payé',50,FALSE),
 ('DOCUMENTS_REMIS','Documents de sortie remis',60,FALSE),
 ('FINALISEE','Sortie finalisée',70,TRUE),
 ('ANNULEE','Demande annulée',80,TRUE)
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,ordre=EXCLUDED.ordre,terminal=EXCLUDED.terminal;

INSERT INTO types_materiel(code,libelle) VALUES
 ('ORDINATEUR','Ordinateur'),('TELEPHONE','Téléphone'),('BADGE','Badge et clé'),
 ('ACCESSOIRE','Accessoires informatiques'),('DOCUMENT','Documents ou supports confiés')
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,actif=TRUE;

INSERT INTO statuts_restitution_materiel(code,libelle) VALUES
 ('A_RESTITUER','À restituer'),('RESTITUE','Restitué'),('NON_CONCERNE','Non concerné'),('LITIGE','À vérifier')
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle;

INSERT INTO types_documents_sortie(code,libelle,obligatoire) VALUES
 ('CERTIFICAT_TRAVAIL','Certificat de travail',TRUE),
 ('DECOMPTE_STC','Décompte détaillé du solde de tout compte',TRUE),
 ('NOTIFICATION_RUPTURE','Notification écrite de rupture',FALSE),
 ('RECU_REMISE','Reçu de remise des documents',FALSE)
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,obligatoire=EXCLUDED.obligatoire;

INSERT INTO types_documents_employe(code,libelle,obligatoire,visible_manager) VALUES
 ('CERTIFICAT_TRAVAIL','Certificat de travail',TRUE,FALSE),
 ('DECOMPTE_STC','Décompte de sortie / solde de tout compte',TRUE,FALSE)
ON CONFLICT(code) DO UPDATE SET libelle=EXCLUDED.libelle,obligatoire=EXCLUDED.obligatoire,visible_manager=EXCLUDED.visible_manager;
