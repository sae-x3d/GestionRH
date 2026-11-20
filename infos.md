# Jeu de donnees SIRH — version consolidee

## Import

1. Partir d’une base PostgreSQL vide.
2. Importer `datas/db/tables-finales.sql`.
3. Importer `datas/db/data-finales.sql`.

Les rubriques de paie, le plafond hebdomadaire et les valeurs initiales sont inclus dans ces deux fichiers; aucun script SQL de migration séparé n’est nécessaire pour une nouvelle base.

--
# MOT DE PASSE : `DemoManager!2026#`.
--

## Comptes de service

| Login | E-mail | Role | Remarque |
|---|---|---|---|
| `demo_rh` | `rh@example.test` | `rh` | Validation et suivi RH |
| `demo_gestionnaire` | `gestionnaire@example.test` | `gestionnaire` | Comptes et validations de profils |
| `demo_administration` | `administration@example.test` | `administration` | Parametrage des effectifs et creation des offres |

## Managers

| Login | E-mail | Departement |
|---|---|---|
| `mgr_it_01` | `mgr.it01@example.test` | Informatique |
| `mgr_it_02` | `mgr.it02@example.test` | Informatique |
| `mgr_log_01` | `mgr.log01@example.test` | Logistique |
| `mgr_log_02` | `mgr.log02@example.test` | Logistique |
| `mgr_fin_01` | `mgr.fin01@example.test` | Finance |
| `mgr_fin_02` | `mgr.fin02@example.test` | Finance |
| `mgr_com_01` | `mgr.com01@example.test` | Commercial |
| `mgr_com_02` | `mgr.com02@example.test` | Commercial |

## Trois comptes non salaries

| Login | E-mail | Statut du compte |
|---|---|---|
| `candidat_valide` | `candidat.valide@example.test` | `APPROVED` |
| `candidat_attente` | `candidat.attente@example.test` | `PENDING` |
| `candidat_non_verifie` | `candidat.nonverifie@example.test` | `NON_VERIFIE` |

## Employes de demonstration

Tous les comptes ci-dessous sont rattaches a leur departement et ont un dossier avec coordonnees, contact d'urgence, formation, contrat PDF et CV PDF. Le mot de passe commun est celui indique plus haut.

| Login | Departement | Poste |
|---|---|---|
| `mgr_it_01` | Informatique | Manager |
| `mgr_it_02` | Informatique | Manager |
| `emp_it_dev_01` | Informatique | Developpeur junior |
| `emp_it_dev_02` | Informatique | Developpeur junior |
| `emp_it_ast_01` | Informatique | Assistant |
| `emp_it_ast_02` | Informatique | Assistant |
| `mgr_log_01` | Logistique | Manager |
| `mgr_log_02` | Logistique | Manager |
| `emp_log_agent_01` | Logistique | Agent logistique |
| `emp_log_agent_02` | Logistique | Agent logistique |
| `emp_log_ast_01` | Logistique | Assistant |
| `emp_log_ast_02` | Logistique | Assistant |
| `mgr_fin_01` | Finance | Manager |
| `mgr_fin_02` | Finance | Manager |
| `emp_fin_cpt_01` | Finance | Comptable |
| `emp_fin_cpt_02` | Finance | Comptable |https://github.com/sae-x3d/GestionRH.git
| `emp_fin_ast_01` | Finance | Assistant |
| `emp_fin_ast_02` | Finance | Assistant |
| `mgr_com_01` | Commercial | Manager |
| `mgr_com_02` | Commercial | Manager |
| `emp_com_sales_01` | Commercial | Commercial |
| `emp_com_sales_02` | Commercial | Commercial |
| `emp_com_ast_01` | Commercial | Assistant |
| `emp_com_ast_02` | Commercial | Assistant |

## Contenu inclus

- 4 departements et 6 familles de postes, avec effectifs autorises, minima et fourchettes de salaire de base.
- 24 salaries: 8 managers et 16 collaborateurs, avec contrats CDI et CDD.
- Matricules generes par PostgreSQL a la creation des fiches employe.
- 20 demandes de recrutement: 3 refusees, 3 en attente Finance, 12 transformees en offres cloturees et 2 offres supplementaires publiees. Les 14 demandes au statut `OFFRE_CREEE` comprennent donc 14 offres.
- Candidatures, historiques, entretiens, questionnaires, parcours et contrats PDF pour les 24 salaries.
- 6 demandes de conge: 2 approuvees, 2 a valider par le manager et 2 a valider par RH. Les conges approuves sont inscrits dans les plannings.
- Plannings d'equipe archives, de la semaine courante et de S+1 pour chaque departement.

## Fichiers generes

Les CV sont des copies de `datas/cv/cv_test.pdf`; les photos sont des copies de `datas/pdp/pdp_test.png`. Les contrats sont generes en PDF a partir des donnees du seed.

- CV de candidature: `datas/cv/cv_<login>.pdf`.
- Photo: `datas/pdp/pdp_<login>.png`.
- CV versionne dans le dossier administratif: `datas/dossier-employe/<id>/<uuid>.pdf`.
- Contrat PDF: `datas/contrats/contrat_<login>.pdf`.

Les contrats et donnees sont pedagogiques. Remplacer l'identite juridique de l'employeur et faire verifier les clauses avant tout usage reel.
