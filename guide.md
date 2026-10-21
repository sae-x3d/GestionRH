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

Spring Security est actif : toutes les pages sont protégées.

- L'utilisateur par défaut s'affiche dans les logs au démarrage :
  ```
  Using generated security password: ********
  ```
- Utilisateur : `user`
- Mot de passe : celui affiché ci-dessus (regénéré à chaque redémarrage tant qu'aucun utilisateur n'est configuré en base)

## 5. Dépannage

| Problème | Solution |
|----------|----------|
| `Unsupported class file major version` ou erreur de version Java | Vérifier `java -version` (doit être 21) et reprendre l'export `JAVA_HOME` |
| `Connection refused` sur le port 5432 | PostgreSQL n'est pas démarré : `sudo systemctl start postgresql` |
| `database "gestionrh" does not exist` | Créer la base : voir section 1 |
| `Port 8080 was already in use` | Lancer avec un autre port : `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| `Password authentication failed` | Vérifier le mot de passe dans `application.properties` (section 1) |
