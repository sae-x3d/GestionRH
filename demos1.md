# Démos GestionRH : Blocs 1 à 4

Scénario de démonstration pour le professeur, écrit d'après le code (routes, rôles, statuts).
L'application n'a pas été lancée pour rédiger ce guide : répéter une fois avant le passage pour confirmer les libellés exacts des boutons.

**Conseil : raconter une seule histoire.** Suivre un même poste (par exemple « Développeur » au département Informatique) de la demande jusqu'à l'onboarding. Le salarié créé au bloc 4 sert ensuite de fil rouge pour les démos des blocs 5 à 7 (voir `demos2.md`). Le prof voit un cycle de vie complet, pas des écrans isolés.

---

## Préparation

- **Comptes** (mot de passe `MotDePasse2026`, chargés par `gestionrh-01.sql`) :
  - `manager.it` : manager du département Informatique
  - `rh.sophie` : RH (seul rôle accepté pour la validation RH d'une demande, l'admin n'y a pas accès)
  - `finance.claire` : Finance
  - `manager.finance` : manager du département Finance
  - `admin` : joue la Direction (aucun compte `DIRECTION` n'est fourni)
  - `user.simple` : le candidat
- **Budget** : l'enveloppe des départements est vide dans les données seed, donc le contrôle de budget ne se déclenche pas. Fixer une enveloppe par SQL sur le département Informatique (vérifier les noms de colonnes de la table `departement`).
- **Flux à sens unique** : une fois le contrat accepté, `user.simple` devient salarié. Faire une sauvegarde de la base avant, ou prévoir un second compte candidat pour recommencer.
- **Dates** : une date d'entretien ou d'effet de contrat ne peut pas être dans le passé.

---

## Bloc 1 : Expression du besoin en personnel

### Démo A : la demande et ses contrôles

- **Compte** : `manager.it`
- **Étapes** :
  1. Aller sur `/demandes/nouvelle` et saisir poste, département, motif, budget, date souhaitée.
  2. Refaire une demande avec un budget supérieur à l'enveloppe restante.
- **Ce qu'on voit** :
  - La première demande est créée au statut « en attente validation RH ».
  - La seconde est refusée avec « Budget insuffisant pour le département… ».
  - Un poste gelé ou un effectif maximal atteint est refusé aussi.
- **Pourquoi** : le PDF demande « poste, département, effectif, motif, budget ». Le système refuse avant toute validation humaine ce qui est impossible : on ne fait pas valider une demande qui dépasse l'enveloppe.

### Démo B : la chaîne de validation

- **Comptes** : `rh.sophie`, puis `finance.claire`, puis `admin`
- **Étapes** :
  1. `/demandes/validation-rh` : accepter.
  2. `/demandes/validation/finance` : accepter.
  3. `/demandes/validation/direction` : accepter.
- **Ce qu'on voit** : le statut passe de `EN_ATTENTE_VALIDATION_RH` à `EN_ATTENTE_VALIDATION_BUDGET`, puis `EN_ATTENTE_VALIDATION_DIRECTION`, puis `APPROUVE`. Chaque acteur ne voit que sa file.
- **Pourquoi** : c'est le workflow « Manager → RH → Direction » du PDF, avec la Finance au milieu pour le budget. Chaque rôle a un pouvoir limité (séparation des responsabilités).

### Démo C : une règle de gouvernance

- **Compte** : `manager.finance`
- **Étapes** : créer une demande pour le département Finance, puis essayer de valider son budget.
- **Ce qu'on voit** : refus « un département ne peut pas valider le budget de sa propre demande ».
- **Pourquoi** : un contrôle de gestion ne s'approuve pas lui-même. À dire explicitement au prof.

### Démo D : un refus

- **Compte** : `rh.sophie`
- **Étapes** : refuser une autre demande.
- **Ce qu'on voit** : statut `REFUSE`, la demande ne peut plus avancer.
- **Pourquoi** : le workflow a deux issues, et la décision est tracée.

---

## Bloc 2 : Recrutement

### Démo E : de la demande à l'offre

- **Compte** : `rh.sophie`
- **Étapes** :
  1. `/offres/nouvelle` : créer une offre depuis la demande approuvée.
  2. `/offres/{id}/questions` : ajouter des questions de présélection.
  3. `/offres/{id}/diffusions` : diffuser sur un canal.
  4. Publier l'offre.
- **Ce qu'on voit** : l'offre passe de `BROUILLON` à `PUBLIEE`, avec une date de clôture et plusieurs canaux de diffusion.
- **Pourquoi** : l'offre est un objet distinct de la demande (une demande peut mener à plusieurs diffusions). La date de clôture empêche de postuler après.

### Démo F : la candidature

- **Compte** : `user.simple`
- **Étapes** :
  1. `/candidatures/postuler` : postuler avec un CV.
  2. Essayer de postuler une deuxième fois à la même offre.
- **Ce qu'on voit** : candidature au statut `RECUE`, puis refus « vous avez déjà postulé ».
- **Pourquoi** : la candidature est une entité à part (personne + offre), pas un champ sur la personne. Une personne peut avoir plusieurs candidatures.

### Démo G : présélection, entretien, évaluation

- **Compte** : `rh.sophie`
- **Étapes** sur `/candidatures/rh/{id}` :
  1. Passer en examen.
  2. Planifier un entretien (date future).
  3. Enregistrer l'évaluation de l'entretien avec note et grille.
  4. Enregistrer le résultat.
- **Ce qu'on voit** : statuts `RECUE` → `EN_EXAMEN` → `CONVOQUE`, avec l'historique des évaluations conservé.
- **Pourquoi** : « conserver les évaluations et la décision finale ». L'évaluation est notée et structurée pour comparer plusieurs candidats.

---

## Bloc 3 : Sélection et embauche

### Démo H : la négociation

- **Comptes** : `rh.sophie`, puis `user.simple`
- **Étapes** :
  1. La RH retient le candidat et envoie le questionnaire de contrat (`/candidatures/rh/{id}/questionnaire-contrat/envoyer`).
  2. Le candidat répond sur `/candidatures/{id}/questionnaire-contrat` avec un salaire demandé.
  3. La RH contre-propose, le candidat accepte.
- **Ce qu'on voit** : les tours de négociation sont historisés, jusqu'au statut `ACCEPTEE`.
- **Pourquoi** : le PDF parle d'une « proposition d'embauche » avec acceptation. Le système garde chaque tour (proposition, contre-proposition) au lieu d'un seul champ salaire.

### Démo I : la proposition de contrat et sa validation

- **Comptes** : `rh.sophie`, `manager.it`, `rh.sophie`, `admin`
- **Étapes** :
  1. La RH génère le contrat (`/candidatures/rh/{id}/contrat/nouveau`) avec type, dates, salaire.
  2. Le contrat apparaît dans `/validations/bloc4`, où il est approuvé par le manager, puis la RH, puis la Direction.
- **Ce qu'on voit** : statuts du contrat `EN_VALIDATION_RH` puis `EN_VALIDATION_DIRECTION`, puis en attente de réponse du candidat. Un refus renvoie le contrat en « à corriger ».
- **Pourquoi** : le contrat engage l'entreprise, donc trois niveaux de validation avant d'être soumis au candidat. Les règles de cohérence sont contrôlées : CDD avec motif et durée entre 1 mois et 2 ans, CDI sans date de fin.

### Démo J : l'acceptation crée le salarié

- **Compte** : `user.simple`
- **Étapes** : sur `/contrats/{id}`, accepter le contrat (PDF téléchargeable via `/contrats/{id}/pdf`).
- **Ce qu'on voit** :
  - Un matricule est généré.
  - La fiche employé apparaît avec la même personne (le candidat est relié à son employé).
  - Le poste devient occupé.
  - L'onboarding démarre automatiquement.
- **Pourquoi** : c'est le point de conception « Candidat = Employé ». Aucune ressaisie. Si le candidat refuse, la candidature passe à `CONTRAT_REFUSE` et la RH est notifiée.

---

## Bloc 4 : Contrat et onboarding

### Démo K : historique des contrats

- **Compte** : `rh.sophie`
- **Étapes** sur `/contrats/{id}` :
  1. Renouveler un CDD (`/renouveler`).
  2. Convertir en CDI (`/convertir-cdi`).
  3. Faire un avenant sur le salaire (`/avenant`).
- **Ce qu'on voit** : chaque opération crée un nouveau contrat. L'ancien passe à `RENOUVELE`, `CONVERTI` ou `AVENANT`, et la nature (`INITIAL`, `RENOUVELLEMENT`, `AVENANT`, `CONVERSION`) est conservée.
- **Pourquoi** : « ne pas écraser l'historique en stockant seulement `employe.type_contrat` ». Un seul contrat actif à la fois, et tout l'historique reste consultable. On peut aussi montrer `contratsAEcheance`, qui sert aux alertes de fin de contrat.

### Démo L : la checklist d'onboarding

- **Comptes** : `paul.rabe` (ou le nouveau salarié) sur `/mon-onboarding`, puis `rh.sophie` sur `/onboarding`
- **Étapes** :
  1. Voir les tâches assignées à RH, MANAGER, IT et SERVICES_GENERAUX, avec échéances.
  2. Le salarié dépose ses documents (`/onboarding/{id}/documents`).
  3. La RH les valide.
- **Ce qu'on voit** : chaque tâche a un responsable, une échéance et un statut.
- **Pourquoi** : le PDF demande « checklist et workflow d'intégration ». Chaque service sait ce qu'il doit faire, et personne ne coche à la place d'un autre (accès refusé sinon).

### Démo M : clôture de l'onboarding

- **Comptes** : le salarié, puis `manager.it`, `rh.sophie`, `admin`
- **Étapes** : le salarié soumet la clôture avec la décision de fin d'essai (`/onboarding/{id}/soumettre`). Elle suit la même chaîne manager → RH → Direction dans `/validations/bloc4`.
- **Ce qu'on voit** : statut `TERMINE`, et l'affectation passe à `ACTIVE`. Un refus renvoie en « à corriger ».
- **Pourquoi** : l'intégration n'est terminée que quand toutes les tâches obligatoires sont faites et validées. L'affectation active marque le début réel du poste.

### Démo N : accréditations

- **Compte** : `rh.sophie`
- **Étapes** : sur `/accreditations/nouveau`, créer une demande d'accréditation (badge, accès) pour le salarié.
- **Pourquoi** : c'est le volet « badge, compte, matériel » de l'onboarding, avec sa propre validation.

---

## À annoncer au prof

- **Limites assumées** : l'admin joue la Direction (aucun compte `DIRECTION`, `IT` ni `PAIE` n'est fourni), les emails sont simulés, et le flux candidat → salarié est irréversible.
- **Phrase de conclusion** : « Chaque étape est une entité, un statut et une validation tracée, pas un simple écran de saisie. »