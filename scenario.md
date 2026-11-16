# Scénario de test — Bloc 9 : gestion de carrière

Date de référence : **15/11/2026** (fuseau `Indian/Antananarivo`).

Ce scénario valide les promotions, mutations, changements de poste, changements de salaire,
la consultation de la carrière à une date donnée, l’historisation append-only et les règles d’accès.
Les identifiants ci-dessous correspondent au jeu de données `data-14-11-26.sql`.

## 1. Préparation

1. Partir d’une base PostgreSQL vide.
2. Importer dans cet ordre :
   - `datas/db/tables-14-11-26.sql`
   - `datas/db/data-14-11-26.sql`
   - `datas/db/modification-carriere-15-11-26.sql`
3. Vérifier que le backfill a créé une affectation et un salaire initial par salarié :

```sql
SELECT
    (SELECT count(*) FROM employes) AS employes,
    (SELECT count(*) FROM historique_affectation_employe) AS affectations,
    (SELECT count(*) FROM historique_affectation_employe WHERE type_mouvement_id IS NULL) AS affectations_sans_type,
    (SELECT count(*) FROM historique_affectation_employe WHERE nouveau_departement_id IS NULL) AS affectations_sans_departement,
    (SELECT count(*) FROM historique_salaire_employe) AS salaires;
```

Résultat attendu : `24 | 24 | 0 | 0 | 24`.

4. Mémoriser les identifiants utiles :

```sql
SELECT e.id, u.username, u.full_name, e.matricule, p.intitule AS poste, d.nom AS departement,
       ct.salaire_base_mensuel AS salaire, p.salaire_base_min, p.salaire_base_max
FROM employes e
JOIN users u ON u.id = e.user_id
JOIN postes p ON p.id = e.poste_id
JOIN departements d ON d.id = p.departement_id
LEFT JOIN candidatures ca ON ca.candidat_id = e.user_id
LEFT JOIN contrats ct ON ct.candidature_id = ca.id
WHERE e.id BETWEEN 9 AND 12
ORDER BY e.id;
```

| Id | Login | Matricule | Poste de départ | Département | Salaire |
|---:|---|---|---|---|---:|
| 9 | `emp_it_ast_01` | ITAST009 | Assistant | Informatique | 750 000 Ar |
| 10 | `emp_it_ast_02` | ITAST010 | Assistant | Informatique | 700 000 Ar |
| 11 | `emp_it_dev_01` | ITDEV011 | Développeur junior | Informatique | 1 200 000 Ar |
| 12 | `emp_it_dev_02` | ITDEV012 | Développeur junior | Informatique | 1 100 000 Ar |

Comptes utilisés (mot de passe commun : `DemoManager!2026#`) :

| Login | Rôle | Usage |
|---|---|---|
| `demo_rh` | `rh` | Création et consultation de tous les mouvements |
| `mgr_it_01` | `manager` | Lecture seule de l’équipe Informatique |
| `mgr_log_01` | `manager` | Lecture seule de l’équipe Logistique après mutation |
| `emp_it_ast_01` | `employe` | Consultation de sa propre carrière, salaire inclus |
| `emp_it_dev_02` | `employe` | Salarié muté vers la Logistique |
| `demo_administration` | `administration` | Doit être refusé sur la gestion de carrière |
| `demo_gestionnaire` | `gestionnaire` | Doit être refusé sur la gestion de carrière |

## 2. Promotion (RH)

**Interface** : se connecter avec `demo_rh`, ouvrir **Carrière > Gestion de carrière**,
rechercher `ITAST009`, ouvrir le dossier puis cliquer sur **Promotion**.

**API** :

```http
POST /api/rh/carriere/promotions
Content-Type: application/json

{
  "employeId": 9,
  "nouveauPosteId": 2,
  "nouveauSalaireBase": 1000000,
  "dateEffet": "2026-11-14",
  "motif": "Promotion validée par la commission de carrière"
}
```

Le poste cible `2` est **Développeur junior** (Informatique, fourchette 750 000 à 1 800 000 Ar).

Résultat attendu :

- réponse `200` avec `situationActuelle.poste = "Développeur junior"` et `situationActuelle.salaireBase = 1000000` ;
- `historiqueAffectations[0].type = "PROMOTION"` et `nouveauDepartement = "Informatique"` ;
- `historiqueSalaires` contient deux lignes : `SALAIRE_INITIAL` à 750 000 Ar puis `PROMOTION` à 1 000 000 Ar ;
- `promotions[0]` contient l’ancien poste, le nouveau poste et les deux salaires ;
- le contrat actif est mis à jour à 1 000 000 Ar ;
- la fiche salarié pointe désormais sur le poste `2` ;
- un événement `CARRIERE_EVENEMENT` et une notification `CARRIERE_STATUT` sont créés.

**Vérification SQL** :

```sql
SELECT h.date_effet, t.code, h.ancien_poste_id, h.nouveau_poste_id,
       h.ancien_departement_id, h.nouveau_departement_id
FROM historique_affectation_employe h
JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
WHERE h.employe_id = 9 ORDER BY h.date_effet DESC, h.id DESC;

SELECT h.date_effet, t.code, h.ancien_salaire_base, h.nouveau_salaire_base
FROM historique_salaire_employe h
JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
WHERE h.employe_id = 9 ORDER BY h.date_effet DESC, h.id DESC;

SELECT e.poste_id, ct.salaire_base_mensuel
FROM employes e
JOIN candidatures ca ON ca.candidat_id = e.user_id
JOIN contrats ct ON ct.candidature_id = ca.id
JOIN statuts_contrat sc ON sc.id = ct.statut_id
WHERE e.id = 9 AND sc.code = 'ACTIF';
```

Attendu : contrat à `1000000.00`, poste `2`, deux lignes de salaire.

## 3. Mutation (RH)

**Interface** : dans le même dossier ou celui de `ITDEV012`, cliquer sur **Mutation**.

**API** :

```http
POST /api/rh/carriere/mutations
Content-Type: application/json

{
  "employeId": 12,
  "nouveauPosteId": 5,
  "dateEffet": "2026-11-14",
  "motif": "Réorganisation du service logistique"
}
```

Le poste cible `5` est **Agent logistique** (Logistique, fourchette 500 000 à 1 200 000 Ar).
Le salaire actuel de 1 100 000 Ar reste dans la fourchette, il n’est donc pas nécessaire de le modifier.

Résultat attendu :

- changement de département Informatique → Logistique et de poste Développeur junior → Agent logistique ;
- `mutations[0].ancienSalaireBase = 1100000` et `nouveauSalaireBase = 1100000` ;
- `historiqueSalaires` reste à une seule ligne ;
- le contrat et le poste de la fiche salarié sont mis à jour ;
- un événement et une notification sont créés.

**Vérification SQL** :

```sql
SELECT m.date_effet, a.nom AS ancien_departement, n.nom AS nouveau_departement,
       ao.intitule AS ancien_poste, no.intitule AS nouveau_poste,
       m.ancien_salaire_base, m.nouveau_salaire_base
FROM mutations m
JOIN departements a ON a.id = m.ancien_departement_id
JOIN departements n ON n.id = m.nouveau_departement_id
JOIN postes ao ON ao.id = m.ancien_poste_id
JOIN postes no ON no.id = m.nouveau_poste_id
WHERE m.employe_id = 12;
```

## 4. Changement de poste (RH)

**API** :

```http
POST /api/rh/carriere/changements-poste
Content-Type: application/json

{
  "employeId": 11,
  "nouveauPosteId": 3,
  "dateEffet": "2026-11-14",
  "motif": "Intérim managérial dans le département Informatique"
}
```

Le poste cible `3` est **Manager** (Informatique, fourchette 1 200 000 à 2 500 000 Ar).
Le salaire de 1 200 000 Ar reste inchangé et reste dans la fourchette.

Résultat attendu :

- `historiqueAffectations[0].type = "CHANGEMENT_POSTE"` ;
- aucune nouvelle ligne dans `historique_salaire_employe` ;
- le salaire du contrat reste 1 200 000 Ar.

## 5. Changement de salaire (RH)

**API** :

```http
POST /api/rh/carriere/changements-salaire
Content-Type: application/json

{
  "employeId": 10,
  "nouveauSalaireBase": 820000,
  "dateEffet": "2026-11-15",
  "motif": "Ajustement annuel de la rémunération"
}
```

Le poste reste **Assistant** (Informatique, fourchette 500 000 à 1 100 000 Ar).

Résultat attendu :

- `historiqueSalaires[0].type = "CHANGEMENT_SALAIRE"` avec `ancienSalaireBase = 700000` et `nouveauSalaireBase = 820000` ;
- aucune nouvelle ligne d’affectation ;
- le contrat actif passe à 820 000 Ar.

## 6. Consultation de la carrière à une date donnée

### 6.1 Avant la promotion

```http
GET /api/rh/carriere/employes/9/situation?date=2026-11-13
```

Attendu : poste **Assistant**, département **Informatique**, salaire **750 000 Ar**,
mouvement **Affectation initiale** du 15/03/2024.

### 6.2 Le jour de la promotion

```http
GET /api/rh/carriere/employes/9/situation?date=2026-11-14
```

Attendu : poste **Développeur junior**, salaire **1 000 000 Ar**, mouvement **Promotion**.

### 6.3 Après la mutation

```http
GET /api/rh/carriere/employes/12/situation?date=2026-11-13
GET /api/rh/carriere/employes/12/situation?date=2026-11-14
```

Attendu :

- au 13/11/2026 : Développeur junior, Informatique, 1 100 000 Ar ;
- au 14/11/2026 : Agent logistique, Logistique, 1 100 000 Ar.

La même consultation est disponible :

- pour le manager : `GET /api/manager/carriere/equipe/{employeeId}/situation?date=...` ;
- pour le salarié : `GET /api/carriere/me/situation?date=...`.

### 6.4 Vérification SQL de la situation à une date

```sql
WITH affectation AS (
    SELECT h.nouveau_poste_id, h.nouveau_departement_id, h.date_effet, t.code
    FROM historique_affectation_employe h
    JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
    WHERE h.employe_id = 9 AND h.date_effet <= DATE '2026-11-13'
    ORDER BY h.date_effet DESC, h.id DESC LIMIT 1
), salaire AS (
    SELECT h.nouveau_salaire_base, h.date_effet
    FROM historique_salaire_employe h
    WHERE h.employe_id = 9 AND h.date_effet <= DATE '2026-11-13'
    ORDER BY h.date_effet DESC, h.id DESC LIMIT 1
)
SELECT p.intitule, d.nom, s.nouveau_salaire_base, a.code, a.date_effet
FROM affectation a
JOIN postes p ON p.id = a.nouveau_poste_id
JOIN departements d ON d.id = a.nouveau_departement_id
CROSS JOIN salaire s;
```

Attendu : `Assistant | Informatique | 750000.00 | AFFECTATION_INITIALE | 2024-03-15`.

## 7. Cas invalides

Chaque appel doit être refusé sans modifier l’historique.

| Cas | Appel | Résultat attendu |
|---|---|---|
| Date future | promotion avec `dateEffet = 2026-11-16` | `400` — la date d’effet ne peut pas être dans le futur |
| Date déjà utilisée | deuxième mouvement pour l’employé 9 avec `dateEffet = 2026-11-14` | `400` — la date doit être strictement postérieure au dernier mouvement |
| Promotion vers le même poste | `nouveauPosteId = 1` pour l’employé 9 | `400` |
| Promotion vers un autre département | `nouveauPosteId = 5` pour l’employé 9 | `400` — une promotion conserve le même département |
| Promotion sans augmentation | salaire `900000` pour l’employé 9 après promotion à 1 000 000 | `400` — le salaire doit augmenter |
| Salaire hors fourchette | promotion à `2000000` pour le poste Développeur junior | `400` |
| Mutation dans le même département | `nouveauPosteId = 1` pour l’employé 12 avant mutation | `400` |
| Mutation avec salaire hors fourchette | `nouveauSalaireBase = 1300000` pour Agent logistique | `400` |
| Changement de poste vers un autre département | `nouveauPosteId = 5` pour l’employé 11 | `400` |
| Changement de poste incompatible avec le salaire | `nouveauPosteId = 1` pour l’employé 11 (salaire 1 200 000 > maximum Assistant 1 100 000) | `400` |
| Changement de salaire identique | `nouveauSalaireBase = 820000` pour l’employé 10 après l’ajustement | `400` |
| Changement de salaire hors fourchette | `nouveauSalaireBase = 1200000` pour un Assistant | `400` |
| Consultation avant l’embauche | `GET .../situation?date=2024-01-01` pour l’employé 9 | `400` |
| Consultation future | `GET .../situation?date=2027-01-01` | `400` |
| Salarié sorti | créer un mouvement pour un salarié au statut `SORTI` | `409` |

## 8. Règles d’accès et confidentialité

### 8.1 RH

- `GET /api/rh/carriere/employes` : `200`, tous les salariés, salaire visible.
- `GET /api/rh/carriere/employes/9` : `200`, historique complet et salaires.
- Les quatre `POST` de mouvement : `200` pour `demo_rh`.

### 8.2 Manager

- `GET /api/manager/carriere/equipe` : `200`, uniquement les salariés de son département, **sans champ `salaire_base`**.
- `GET /api/manager/carriere/equipe/9` : `200`, `salaireVisible = false`, pas de `historiqueSalaires`,
  et pas de champs de salaire dans les promotions et mutations.
- `GET /api/manager/carriere/equipe/12` avec `mgr_it_01` après la mutation : `403`
  (le salarié n’est plus dans son département).
- `GET /api/manager/carriere/equipe/9` avec `mgr_log_01` : `403`.
- `POST /api/rh/carriere/promotions` avec `mgr_it_01` : `403`.
- `GET /api/rh/carriere/employes` avec `mgr_it_01` : `403`.

### 8.3 Salarié

- `GET /api/carriere/me` avec `emp_it_ast_01` : `200`, uniquement sa propre carrière, salaire inclus.
- `GET /api/carriere/me/situation?date=2026-11-13` : `200`, situation personnelle à cette date.
- `GET /api/carriere/me` avec `emp_it_dev_02` : `200`, uniquement la carrière de `emp_it_dev_02`.
- `POST /api/rh/carriere/promotions` avec `emp_it_ast_01` : `403`.
- `GET /api/carriere/me` avec `demo_administration` ou `demo_gestionnaire` : `403`
  (aucun rôle salarié actif).
- `GET /api/rh/carriere/employes` avec `demo_administration` ou `demo_gestionnaire` : `403`.

### 8.4 Administration et gestionnaire

Aucun accès aux écrans ni aux endpoints de gestion de carrière.
Le menu latéral ne doit pas afficher la section **Carrière** pour ces rôles.

## 9. Historique append-only

Les triggers doivent empêcher toute réécriture. Exécuter chaque instruction dans une transaction
puis annuler :

```sql
BEGIN;
UPDATE historique_affectation_employe SET motif = 'modification interdite' WHERE employe_id = 9;
ROLLBACK;
-- ERROR: Les historiques de carriere sont en lecture seule.

BEGIN;
DELETE FROM historique_salaire_employe WHERE employe_id = 9;
ROLLBACK;
-- ERROR: Les historiques de carriere sont en lecture seule.

BEGIN;
UPDATE promotions SET motif = 'modification interdite' WHERE employe_id = 9;
ROLLBACK;
-- ERROR: Les historiques de carriere sont en lecture seule.

BEGIN;
DELETE FROM mutations WHERE employe_id = 12;
ROLLBACK;
-- ERROR: Les historiques de carriere sont en lecture seule.
```

La correction d’une erreur doit se faire par un nouveau mouvement, jamais par une mise à jour
ou une suppression.

## 10. Contrôles d’intégrité finaux

```sql
-- Une seule affectation initiale par salarié
SELECT employe_id, count(*) FROM historique_affectation_employe h
JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
WHERE t.code = 'AFFECTATION_INITIALE'
GROUP BY employe_id HAVING count(*) > 1;

-- Une seule ligne de salaire initial par salarié
SELECT employe_id, count(*) FROM historique_salaire_employe h
JOIN types_mouvement_carriere t ON t.id = h.type_mouvement_id
WHERE t.code = 'SALAIRE_INITIAL'
GROUP BY employe_id HAVING count(*) > 1;

-- Les mouvements sont liés à des lignes d’historique existantes
SELECT p.id, p.historique_affectation_id, p.historique_salaire_id
FROM promotions p
LEFT JOIN historique_affectation_employe a ON a.id = p.historique_affectation_id
LEFT JOIN historique_salaire_employe s ON s.id = p.historique_salaire_id
WHERE a.id IS NULL OR s.id IS NULL;

-- Les salaires restent dans la fourchette du poste occupé au moment du mouvement
SELECT h.id, h.employe_id, h.nouveau_salaire_base, p.intitule, p.salaire_base_min, p.salaire_base_max
FROM historique_salaire_employe h
JOIN historique_affectation_employe a
  ON a.employe_id = h.employe_id
 AND a.date_effet = (SELECT max(a2.date_effet) FROM historique_affectation_employe a2
                     WHERE a2.employe_id = h.employe_id AND a2.date_effet <= h.date_effet)
JOIN postes p ON p.id = a.nouveau_poste_id
WHERE h.nouveau_salaire_base < p.salaire_base_min
   OR h.nouveau_salaire_base > p.salaire_base_max;
```

Résultat attendu : aucune ligne pour les trois premières requêtes. La dernière requête peut
retourner des lignes historiques antérieures au bloc 9 si un poste a changé de fourchette ;
les mouvements créés dans ce scénario doivent toutefois respecter les fourchettes.

## 11. Événements et notifications

```sql
SELECT e.date_effet, t.code AS type_evenement, e.objet_type, e.objet_id, e.resume
FROM evenements_dossier_employe e
JOIN types_evenements_dossier t ON t.id = e.type_evenement_id
WHERE e.employe_id IN (9, 10, 11, 12)
  AND t.code = 'CARRIERE_EVENEMENT'
ORDER BY e.date_evenement DESC, e.id DESC;

SELECT u.username, n.titre, n.resume, n.lien, n.lue
FROM notifications n
JOIN users u ON u.id = n.destinataire_id
JOIN types_notifications t ON t.id = n.type_notification_id
WHERE t.code = 'CARRIERE_STATUT'
ORDER BY n.created_at DESC;
```

Attendu : un événement et une notification par mouvement, avec un lien vers
`/espace/employe/carriere`.

## 12. Parcours interface

1. **RH** : `demo_rh` → **Carrière > Gestion de carrière**.
   - Rechercher `ITAST009`, sélectionner le dossier, vérifier la situation actuelle.
   - Enregistrer une promotion, puis vérifier les tableaux **Historique des affectations**,
     **Historique des salaires**, **Promotions** et **Mutations**.
   - Consulter la situation au 13/11/2026 puis au 14/11/2026 et vérifier le changement de poste et de salaire.
2. **Manager** : `mgr_it_01` → **Mon équipe > Carrière**.
   - Vérifier que seuls les salariés Informatique apparaissent.
   - Vérifier qu’aucun montant de salaire n’est affiché et qu’aucun bouton de mouvement n’est proposé.
3. **Salarié** : `emp_it_ast_01` → **Contrat > Ma carrière**.
   - Vérifier l’affichage de sa propre carrière et de ses salaires.
   - Vérifier l’absence des boutons de création de mouvement.
4. **Administration / gestionnaire** : vérifier l’absence de la section **Carrière** et le refus
   des appels API avec un code `403`.
