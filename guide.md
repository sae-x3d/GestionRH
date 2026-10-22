# Guide du projet GestionRH

Projet Spring Boot (Java 21) de gestion des ressources humaines, avec PostgreSQL.

## Prérequis

- **Java 21** (obligatoire, le projet ne compile pas en Java 8/17)
- **PostgreSQL** installé et démarré
- Aucune installation Maven nécessaire : le projet inclut le wrapper `mvnw`

### Configuration de Java 21

Le `java` par défaut du système peut être une vieille version. Forcer Java 21 avec :

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # doit afficher openjdk 21
```

Pour que ce soit permanent, ajouter ces deux lignes dans `~/.bashrc`.

## 1. Base de données

La base `gestionrh` doit exister (Hibernate crée le schéma automatiquement, mais pas la base elle-même).

```bash
sudo -u postgres psql -c "CREATE DATABASE gestionrh;"
```

Connexion utilisée par l'application (configurée dans `gestion/src/main/resources/application.properties`) :

| Paramètre | Valeur       |
|-----------|--------------|
| Hôte      | localhost    |
| Port      | 5432         |
| Base      | gestionrh    |
| Utilisateur | postgres   |
| Mot de passe | 3958     |

## 2. Compilation

Depuis le dossier `gestion/` :

```bash
cd gestion
./mvnw clean package
```

Pour ignorer les tests :

```bash
./mvnw clean package -DskipTests
```

Le fichier exécutable est généré dans `target/gestion-0.0.1-SNAPSHOT.jar`.

## 3. Démarrage

### Option A — avec le wrapper Maven (développement)

```bash
cd gestion
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run
```

### Option B — avec le jar compilé

```bash
cd gestion
java -jar target/gestion-0.0.1-SNAPSHOT.jar
```

L'application démarre sur : **http://localhost:8080**

## 4. Accès et sécurité

Spring Security est actif : toutes les pages sont protégées par une page de connexion.

Les comptes proviennent de la table `utilisateur` (rôle `role` inclus dans le menu).
Les comptes de démonstration créés par le script de seed partagent le mot de passe
**`MotDePasse2026`** (hash bcrypt dans la colonne `password_hash`) :

| Identifiant | Rôle | Employé lié |
|-------------|------|-------------|
| `admin` | Administrateur | - |
| `rh.sophie` | RH | Sophie Rakotomalala |
| `rh.nina` | RH | Nina Rasoa |
| `manager.it` | Manager | Jean Rakoto |
| `manager.finance` | Manager | Michel Rajoelina |
| `manager.marketing` | Manager | Laura Randriam |
| `paul.rabe` | Employé | Paul Rabe |
| `marie.randria` | Employé | Marie Randria |

> En production, remplacer ces hash par des mots de passe réels et supprimer
> les comptes de démonstration.

## 5. Structure du code et conventions

Le code suit les conventions du projet de référence **EggLand** (`com.app.eggland`).

### Arborescence des paquets (`gestion/src/main/java/com/gestion`)

| Paquet    | Rôle |
|-----------|------|
| `model`   | Entités JPA (ex-`entity`) : `Demande`, `Employe`, `Poste`, `Utilisateur`… |
| `controller` | Contrôleurs MVC (`@Controller`), un dossier par écran |
| `service` | Services métier (`@Service`), un service par entité |
| `repository` | Interfaces `JpaRepository<T, Integer>` |
| `dto`     | Objets de transport du formulaire (POJO à accesseurs manuels) |
| `config`  | Configuration Spring (`SecurityConfig`) |
| `exception` | Erreurs API (`ApiError`, `GlobalExceptionHandler`, `RessourceIntrouvableException`) |

### Conventions de code

- **Entités** : `@Entity` + `@Table` + Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`,
  imports jokers (`import jakarta.persistence.*;` / `import lombok.*;`),
  clé primaire `Integer` en `GenerationType.IDENTITY`, colonnes en `snake_case` via `@Column(name = "...")`.
- **Services** : classes concrètes `@Service`, injection par constructeur
  (`@RequiredArgsConstructor` + `private final`), `@Transactional` **méthode par méthode**,
  noms de méthodes en français : `listerTous()`, `listerActifs()`, `trouverParId()`,
  `creer()`, `supprimer()`, `verifier*()` pour les règles de gestion.
- **Contrôleurs** : `@Controller` + `@RequestMapping` de classe, la vue est renvoyée
  sous forme de `String` (`"demande/formulaire"`), messages via
  `RedirectAttributes.addFlashAttribute("succes" | "erreur", message)`.
- **Repositories** : requêtes dérivées par défaut (`findAllByOrderByNomPosteAsc`),
  `@Query` en bloc de texte (`"""`) pour les jointures et agrégats.
- **Front** : Thymeleaf + Layout Dialect : `fragments/layout.html` est le gabarit commun,
  les pages utilisent `layout:decorate="~{fragments/layout}"` et `layout:fragment="content"`.
  CSS en CSS simple avec variables dans `static/css/variables.css`, un fichier par écran
  (`demande.css`, `connexion.css`) et un fichier par squelette
  (`layout.css`, `sidebar.css`, `header.css`, `components.css`).

### Écarts assumés par rapport à EggLand

Ces points ont été conservés car ils répondent à des exigences propres à GestionRH :

- **Validation Bean Validation** (`jakarta.validation`) sur les DTO + `@Valid` dans le contrôleur.
- **Gestionnaire d'erreurs global** (`GlobalExceptionHandler`) et `ApiError` pour les réponses JSON.
- **Mots de passe hachés en BCrypt** et **protection CSRF activée** (EggLand utilise un
  `PasswordEncoder` en clair et désactive le CSRF : à ne pas reproduire).
- Les relations `ManyToOne` restent en chargement `EAGER` (EggLand utilise `LAZY`),
  afin que les entités restent exploitables hors transaction (tests, exports).

## 6. Formulaire de demande de recrutement

La page **http://localhost:8080/demandes/nouvelle** permet de saisir une demande.

- Les listes (postes, départements, motifs, employés actifs) sont chargées
  depuis la base à chaque affichage du formulaire.
- Le **demandeur** est déterminé automatiquement : il s'agit de l'utilisateur
  connecté, il n'y a pas de champ pour le saisir.
- Le **statut initial** est déterminé automatiquement à partir de la propriété
  `app.demande.statut-initial` (valeur par défaut : `En attente`), il n'y a pas
  de champ pour le saisir non plus.
- Les erreurs de saisie sont affichées champ par champ sous chaque input.

## 7. Dépannage

| Problème | Solution |
|----------|----------|
| `Unsupported class file major version` ou erreur de version Java | Vérifier `java -version` (doit être 21) et reprendre l'export `JAVA_HOME` |
| `Connection refused` sur le port 5432 | PostgreSQL n'est pas démarré : `sudo systemctl start postgresql` |
| `database "gestionrh" does not exist` | Créer la base : voir section 1 |
| `Port 8080 was already in use` | Lancer avec un autre port : `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| `Password authentication failed` | Vérifier le mot de passe dans `application.properties` (section 1) |
