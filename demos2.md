# Démos GestionRH : Blocs 5 à 7

Scénario de démonstration pour le professeur, écrit d'après le code (routes, rôles, statuts).
L'application n'a pas été lancée pour rédiger ce guide : répéter une fois avant le passage pour confirmer les libellés exacts des boutons.

Les blocs 1 à 4 sont dans `demos1.md`. Le salarié créé à la fin du bloc 4 peut servir de fil rouge ici.

---

## Préparation

- **Données** : `script-global.sql` crée le schéma et `admin`. Charger ensuite `script-demo-global.sql`, qui contient les comptes de `auth.txt` (mot de passe `MotDePasse2026`). Ne pas charger `gestionrh-01.sql` après `script-global.sql`.
- **Comptes utilisés** :
  - `paul.rabe` : employé
  - `andry.nouveau` : employé (parcours contrat/onboarding)
  - `manager.it` : manager
  - `rh.sophie` : RH
  - `finance.claire` : Finance
  - `direction.demo` : Direction
- **Soldes de congés** : `script-demo-global.sql` crée déjà les lots, dont deux lots pour Paul (un ancien qui expire bientôt, un récent) pour montrer le FIFO. Ne pas relancer `/conges/batch/credits`, sauf pour tester le mécanisme de crédit annuel.
- **Dates** : utiliser le 24 au 29 décembre 2026. Le 25 est un jour férié seedé et les 26 et 27 sont un week-end, ce qui rend le calcul visible.
- **Pointage en direct** : l'heure de pointage est celle du moment. Pour provoquer un retard, baisser `heureDebut` sur `/temps-travail/parametres` (par exemple 06:00).

---

## Bloc 5 : Dossier administratif

### Démo 1 : droits par champ

- **Comptes** : `manager.it`, puis `rh.sophie`, puis `finance.claire`
- **Étapes** : ouvrir la même fiche sur `/employes/{id}` avec chaque compte.
- **Ce qu'on voit** : le manager n'a ni salaire ni RIB, la RH voit tout, la Finance voit le salaire.
- **Pourquoi** : le PDF demande « confidentialité et habilitations ». Le même écran s'adapte au rôle, ce qui prouve que ce n'est pas un simple CRUD.

### Démo 2 : changement sensible validé

- **Comptes** : `paul.rabe`, puis `rh.sophie`
- **Étapes** :
  1. Sur `/mon-dossier`, modifier l'adresse (effet immédiat).
  2. Soumettre un changement de RIB.
  3. `rh.sophie` ouvre `/employes/changements` et accepte.
- **Ce qu'on voit** : l'adresse change tout de suite. Le RIB reste inchangé jusqu'à la décision de la RH.
- **Pourquoi** : une donnée de paie ne doit pas être modifiable seule par l'employé (risque de fraude). Distinguer donnée libre et donnée sensible est une règle de gestion.

### Démo 3 : historique et audit

- **Comptes** : `rh.sophie`, puis `admin`
- **Étapes** : changer un salaire sur `/employes/{id}/salaire`, regarder l'onglet salaire de la fiche, puis ouvrir `/audit`.
- **Ce qu'on voit** : l'ancien salaire n'est pas écrasé (nouvelle ligne avec date d'effet), et le journal montre qui, quand, avant et après.
- **Pourquoi** : même logique que pour les contrats du bloc 4 (« ne pas écraser l'historique »), avec la traçabilité exigée par le PDF.

### Démo 4 : documents obligatoires et validité

- **Compte** : `rh.sophie`
- **Étapes** : sur `/employes/documents-requis`, activer la pièce d'identité et le RIB. Déposer un document avec une date d'expiration proche sur `/employes/{id}/documents`.
- **Ce qu'on voit** : une alerte pour le document manquant ou bientôt expiré.
- **Pourquoi** : « gérer les documents et leur validité ». Le système prévient avant que le problème ne survienne.

---

## Bloc 7 : Congés et absences

(Présenté avant le bloc 6, car il l'alimente.)

### Démo 5 : demande et contrôles à la soumission

- **Compte** : `paul.rabe`
- **Étapes** : sur `/conges`, demander des congés payés du 24 au 29 décembre.
- **Ce qu'on voit** : 3 jours ouvrés décomptés, et le solde disponible baisse tout de suite (réservation à la soumission).
- **Pourquoi** : le calcul exclut week-end et férié selon le régime. La réservation empêche de poser deux demandes sur le même solde.

### Démo 6 : refus automatiques

- **Compte** : `paul.rabe`
- **Étapes** : faire une demande qui chevauche la première, puis une avec un solde insuffisant.
- **Ce qu'on voit** : le système refuse avec un message précis.
- **Pourquoi** : « contrôler chevauchements et disponibilité du solde » est une règle du PDF, appliquée par le système avant toute validation humaine.

### Démo 7 : workflow et FIFO

- **Comptes** : `manager.it`, puis `rh.sophie`
- **Étapes** : valider la demande d'Andry du 14 au 18 décembre sur `/conges/validation/manager` avec `manager.it`, puis `/conges/validation/rh` avec `rh.sophie`. Pour Paul (Commercial), l'étape manager est validée par la RH car aucun manager commercial n'est seedé. Montrer la table de consommation en base.
- **Ce qu'on voit** : les jours sont pris d'abord sur le lot le plus ancien, une ligne par lot touché.
- **Pourquoi** : le FIFO évite de perdre des jours qui expirent. La table de liaison permet d'annuler en recréditant le bon lot.

### Démo 8 : annulation

- **Compte** : `paul.rabe`
- **Étapes** : annuler la demande validée avec `/conges/{id}/annuler`.
- **Ce qu'on voit** : le solde revient dans le lot d'origine.
- **Pourquoi** : preuve que le solde n'est pas un simple nombre.

### Démo 9 : règles par type et validation Direction

- **Comptes** : `rh.sophie`, puis `direction.demo`
- **Étapes** : sur `/conges/parametres`, fixer un préavis ou une durée max pour un type. Faire une demande trop longue, et la valider avec `direction.demo` sur `/conges/validation/direction`.
- **Pourquoi** : les règles sont des paramètres modifiables, pas du code. La Direction n'intervient que sur les cas exceptionnels, comme dans la répartition des rôles.

### Démo 10 : absences maladie

- **Comptes** : `paul.rabe`, puis `rh.sophie`
- **Étapes** : sur `/absences`, déclarer une maladie sans justificatif (refusé), puis avec justificatif, que la RH décide.
- **Pourquoi** : maladie et maternité ne consomment pas de solde mais ont un justificatif obligatoire et un impact paie. C'est la distinction « congé » et « absence » du bloc.

### Démo 11 : seuil d'absents de l'équipe

- **Comptes** : deux employés du même département
- **Étapes** : faire des demandes qui font dépasser 30 % de l'équipe absente (`app.conges.seuil-absents-equipe`).
- **Ce qu'on voit** : la première demande passe, la suivante est refusée.
- **Pourquoi** : c'est la règle « premier arrivé, premier servi », garantie par le verrou sur le département lors de la soumission.

---

## Bloc 6 : Temps de travail et pointage

### Démo 12 : planning avec contrôles

- **Compte** : `manager.it`
- **Étapes** :
  1. Sur `/planning`, créer un planning pour un employé.
  2. Essayer une journée de plus de 12 h, puis un repos de moins de 11 h, puis un jour de congé validé.
  3. Publier avec `/planning/{id}/publier`.
- **Ce qu'on voit** : refus avec message sur chaque cas, avertissement de sous-effectif, puis publication.
- **Pourquoi** : le planning est le prévu, et les contrôles à la saisie protègent contre l'illégal. Le congé validé bloque le planning, ce qui montre le lien entre blocs 6 et 7.

### Démo 13 : pointage et anomalies

- **Comptes** : `paul.rabe`, puis `manager.it`
- **Étapes** :
  1. Sur `/pointage`, pointer arrivée puis départ.
  2. Le manager va sur `/pointage/anomalies`.
  3. Lancer `/pointage/anomalies/detecter` pour l'absence de la veille.
- **Ce qu'on voit** : retard et absence sans pointage, ni congé, ni férié, sont signalés. Un jour de congé validé ne produit aucune anomalie.
- **Pourquoi** : on compare le prévu et le réalisé. Un congé validé ne doit pas déclencher de fausse alerte.

### Démo 14 : heures sup et majorations

- **Comptes** : `paul.rabe`, puis `manager.it`
- **Étapes** : déclarer des heures sup sur `/heures-sup`, que le manager traite avec `/heures-sup/{id}/traiter`.
- **Ce qu'on voit** : les tranches à 25 % et 50 % apparaissent. Seule la décision du manager fait entrer ces heures dans la suite du circuit.
- **Pourquoi** : « seules les heures validées alimentent la paie ». Les seuils et taux sont des paramètres (`/temps-travail/parametres`).

### Démo 15 : clôture et régularisation

- **Comptes** : `rh.sophie`, puis `finance.claire`
- **Étapes** :
  1. Sur `/paie/cloture`, générer les variables puis clôturer la période.
  2. `finance.claire` exporte avec `/paie/variables/{id}/export` (CSV).
  3. Tenter une correction, puis passer par `/heures-sup/regularisation`.
- **Ce qu'on voit** : la période est verrouillée, la correction passe en régularisation sur la période suivante, avec trace dans l'audit.
- **Pourquoi** : on ne modifie pas un mois déjà transmis à la paie. C'est le principe comptable de verrouillage de période.

---

## Pour conclure devant le prof

Terminer par `/audit` (admin) : on y retrouve le changement de salaire, la clôture et la régularisation avec les valeurs avant/après. Mentionner aussi que `/reporting` montre l'absentéisme et les heures sup par département.

## Deux limites à annoncer honnêtement

- Les emails sont simulés par défaut. En profil `dev`, le lien de réinitialisation du mot de passe s'affiche à l'écran (`app.security.dev-reset-link=true`).
- Le calcul automatique des heures sup tourne par batch le dimanche soir : pour la démo, passer par la déclaration manuelle.