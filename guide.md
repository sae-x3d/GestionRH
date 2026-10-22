# Guide du projet GestionRH

Projet Spring Boot (Java 21) de gestion des ressources humaines, avec PostgreSQL.

## Prerequis

- **Java 21** (obligatoire, le projet ne compile pas en Java 8/17)
- **PostgreSQL** installe et demarre
- Aucune installation Maven necessaire : le projet inclut le wrapper `mvnw`

### Configuration de Java 21

Le `java` par defaut du systeme peut etre une vieille version. Forcer Java 21 avec :

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # doit afficher openjdk 21
```

Pour que ce soit permanent, ajouter ces deux lignes dans `~/.bashrc`.

## 1. Base de donnees

La base `gestionrh` doit exister (Hibernate cree le schema automatiquement, mais pas la base elle-meme).

```bash
sudo -u postgres psql -c "CREATE DATABASE gestionrh;"
```

Connexion utilisee par l'application (configuree dans `gestion/src/main/resources/application.properties`) :

| Parametre | Valeur       |
|-----------|--------------|
| Hote      | localhost    |
| Port      | 5432         |
| Base      | gestionrh    |
| Utilisateur | gestionrh  |
| Mot de passe | gestionrh  |

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

Le fichier executable est genere dans `target/gestion-0.0.1-SNAPSHOT.jar`.

## 3. Demarrage

### Option A - avec le wrapper Maven (developpement)

```bash
cd gestion
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./mvnw spring-boot:run
```

### Option B - avec le jar compile

```bash
cd gestion
java -jar target/gestion-0.0.1-SNAPSHOT.jar
```

L'application demarre sur : **http://localhost:8080**

## 4. Acces et securite

Spring Security est actif : toutes les pages sont protegees par une page de connexion.

Les comptes proviennent de la table `utilisateur` (role `role` inclus dans le menu).
Les comptes de demonstration crees par le script de seed partagent le mot de passe
**`MotDePasse2026`** (hash bcrypt dans la colonne `password_hash`) :

| Identifiant | Role | Employe lie |
|-------------|------|-------------|
| `admin` | Administrateur | - |
| `rh.sophie` | RH | Sophie Rakotomalala |
| `rh.nina` | RH | Nina Rasoa |
| `manager.it` | Manager | Jean Rakoto |
| `manager.finance` | Manager | Michel Rajoelina |
| `manager.marketing` | Manager | Laura Randriam |
| `paul.rabe` | Employe | Paul Rabe |
| `marie.randria` | Employe | Marie Randria |
| `user.simple` | Utilisateur | - |

> En production, remplacer ces hash par des mots de passe reels et supprimer
> les comptes de demonstration.

## 5. Structure du code et conventions

Le code suit les conventions du projet de reference **EggLand** (`com.app.eggland`).

### Arborescence des paquets (`gestion/src/main/java/com/gestion`)

| Paquet    | Role |
|-----------|------|
| `model`   | Entites JPA (ex-`entity`) : `Demande`, `Employe`, `Poste`, `Utilisateur`... |
| `controller` | Controleurs MVC (`@Controller`), un dossier par ecran |
| `service` | Services metier (`@Service`), un service par entite |
| `repository` | Interfaces `JpaRepository<T, Integer>` |
| `dto`     | Objets de transport du formulaire (POJO a accesseurs manuels) |
| `config`  | Configuration Spring (`SecurityConfig`) |
| `exception` | Erreurs API (`ApiError`, `GlobalExceptionHandler`, `RessourceIntrouvableException`) |

### Conventions de code

- **Entites** : `@Entity` + `@Table` + Lombok `@Data @NoArgsConstructor @AllArgsConstructor @Builder`,
  imports jokers (`import jakarta.persistence.*;` / `import lombok.*;`),
  cle primaire `Integer` en `GenerationType.IDENTITY`, colonnes en `snake_case` via `@Column(name = "...")`.
- **Services** : classes concretes `@Service`, injection par constructeur
  (`@RequiredArgsConstructor` + `private final`), `@Transactional` **methode par methode**,
  noms de methodes en francais : `listerTous()`, `listerActifs()`, `trouverParId()`,
  `creer()`, `supprimer()`, `verifier*()` pour les regles de gestion.
- **Controleurs** : `@Controller` + `@RequestMapping` de classe, la vue est renvoyee
  sous forme de `String` (`"demande/formulaire"`), messages via
  `RedirectAttributes.addFlashAttribute("succes" | "erreur", message)`.
- **Repositories** : requetes derivees par defaut (`findAllByOrderByNomPosteAsc`),
  `@Query` en bloc de texte (`"""`) pour les jointures et agregats.
- **Front** : Thymeleaf + Layout Dialect : `fragments/layout.html` est le gabarit commun,
  les pages utilisent `layout:decorate="~{fragments/layout}"` et `layout:fragment="content"`.
  CSS en CSS simple avec variables dans `static/css/variables.css`, un fichier par ecran
  (`demande.css`, `connexion.css`) et un fichier par squelette
  (`layout.css`, `sidebar.css`, `header.css`, `components.css`).

### Ecarts assumes par rapport a EggLand

Ces points ont ete conserves car ils repondent a des exigences propres a GestionRH :

- **Validation Bean Validation** (`jakarta.validation`) sur les DTO + `@Valid` dans le controleur.
- **Gestionnaire d'erreurs global** (`GlobalExceptionHandler`) et `ApiError` pour les reponses JSON.
- **Mots de passe haches en BCrypt** et **protection CSRF activee** (EggLand utilise un
  `PasswordEncoder` en clair et desactive le CSRF : a ne pas reproduire).
- Les relations `ManyToOne` restent en chargement `EAGER` (EggLand utilise `LAZY`),
  afin que les entites restent exploitables hors transaction (tests, exports).

## 6. Formulaire de demande de recrutement

La page **http://localhost:8080/demandes/nouvelle** permet de saisir une demande.

- Les listes (postes, departements, motifs, employes actifs) sont chargees
  depuis la base a chaque affichage du formulaire.
- Le **demandeur** est determine automatiquement : il s'agit de l'utilisateur
  connecte, il n'y a pas de champ pour le saisir.
- Le **statut initial** est determine automatiquement a partir de la propriete
  `app.demande.statut-initial` (valeur par defaut : `En attente`), il n'y a pas
  de champ pour le saisir non plus.
- Les erreurs de saisie sont affichees champ par champ sous chaque input.

## 7. Offres d'emploi

Une offre d'emploi est creee a partir d'une **demande validee par la direction**
(statut `Approuvee`, id configure par `app.offre.id-statut-demande-validee`).

| Page | URL | Acces |
|------|-----|-------|
| Fil d'actualite (offres publiees) | `/offres` | tous les utilisateurs connectes |
| Detail d'une offre | `/offres/{id}` | tous (brouillons reserves aux gestionnaires) |
| Gestion des offres | `/offres/gestion` | `Administrateur`, `RH` |
| Creation / edition | `/offres/nouvelle`, `/offres/{id}/modifier` | `Administrateur`, `RH` |
| Publication | `POST /offres/{id}/publier` | `Administrateur`, `RH` |

- Le **salaire** de l'offre correspond au **budget valide par la finance** dans la
  demande : il est pre-rempli a la creation de l'offre.
- Une offre nouvellement creee est en **brouillon** (`BROUILLON`) ; la publication
  la fait apparaitre dans le fil d'actualite (`PUBLIEE`).
- Le **role `Utilisateur`** (compte de demonstration `user.simple`) accede
  uniquement au fil d'actualite et aux offres publiees.

Les nouvelles tables SIRH (`candidats`, `parcours`, `niveau`, `info`) et la table
`offre_emploi` sont creees par `DB/script-22-10-2026-Jo.sql`.

## 8. Depannage

| Probleme | Solution |
|----------|----------|
| `Unsupported class file major version` ou erreur de version Java | Verifier `java -version` (doit etre 21) et reprendre l'export `JAVA_HOME` |
| `Connection refused` sur le port 5432 | PostgreSQL n'est pas demarre : `sudo systemctl start postgresql` |
| `database "gestionrh" does not exist` | Creer la base : voir section 1 |
| `Port 8080 was already in use` | Lancer avec un autre port : `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| `Password authentication failed` | Verifier le mot de passe dans `application.properties` (section 1) |
