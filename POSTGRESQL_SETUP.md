# PostgreSQL avec Docker Compose

Le projet utilise PostgreSQL 16 dans un conteneur Docker. PostgreSQL n'a pas besoin d'être installé sur le poste. Le conteneur écoute sur le port interne `5432`, publié sur le port hôte `5434` par défaut. Spring Boot est lancé sur l'hôte avec Maven et se connecte à `localhost:5434`. Liquibase crée les tables et charge les données de démonstration au démarrage de l'application.

## Prérequis

- Docker Desktop installé et démarré sous Windows.
- JDK 17 et Maven 3.8+ pour lancer l'API.

## Démarrer PostgreSQL

Depuis la racine du projet, dans PowerShell :

```powershell
docker compose up -d postgres
docker compose ps
docker compose logs -f postgres
```

Attendez que le conteneur soit `healthy`. Pour quitter l'affichage des logs, appuyez sur `Ctrl+C` ; le conteneur reste démarré.

## Démarrer l'API et Liquibase

Dans une autre fenêtre PowerShell, depuis la racine du projet :

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

Au démarrage, Spring Boot attend la connexion PostgreSQL et Liquibase applique `src/main/resources/db/changelog/db.changelog-master.yaml`, qui inclut le schéma et les données de démonstration.

- Swagger UI : http://localhost:8082/swagger-ui.html
- OpenAPI JSON : http://localhost:8082/v3/api-docs
- HTTP Spring Boot : `localhost:8082` par défaut
- PostgreSQL depuis l'hôte : `localhost:5434`
- PostgreSQL dans le conteneur : `5432`
- Base : `atlasrh`
- Utilisateur : `atlasrh`
- Mot de passe local de démonstration : `atlasrh`

Le mot de passe par défaut est réservé au développement local. Ne le réutilisez pas dans un environnement partagé ou de production.

## Configuration personnalisée

Compose accepte les variables d'environnement suivantes, qui ont des valeurs locales par défaut :

| Variable | Valeur par défaut | Utilisation |
|---|---|---|
| `POSTGRES_DB` | `atlasrh` | Base créée par l'image PostgreSQL |
| `POSTGRES_USER` | `atlasrh` | Utilisateur PostgreSQL |
| `POSTGRES_PASSWORD` | `atlasrh` | Mot de passe local |
| `POSTGRES_PORT` | `5434` | Port hôte publié (le port interne reste `5432`) |
| `SERVER_PORT` | `8082` | Port HTTP de Spring Boot |

Spring Boot accepte `DB_URL`, `DB_USERNAME` et `DB_PASSWORD`. Si les identifiants PostgreSQL sont personnalisés, définissez les variables correspondantes dans la fenêtre PowerShell qui démarre l'API. Exemple :

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5434/atlasrh"
$env:DB_USERNAME = "atlasrh"
$env:DB_PASSWORD = "mot-de-passe-local"
$env:SERVER_PORT = "8082"
mvn spring-boot:run
```

Si le port hôte `5434` est déjà occupé, vérifiez un port libre, par exemple `5435`, puis utilisez le même port pour Compose et l'URL JDBC :

```powershell
$env:POSTGRES_PORT = "5435"
docker compose up -d postgres
```

Dans la fenêtre qui démarre l'API, renseignez le port personnalisé :

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5435/atlasrh"
mvn spring-boot:run
```

Lors d'une communication entre conteneurs du même réseau Compose, utilisez le nom du service et le port interne (`postgres:5432`), pas le port hôte. Dans la configuration actuelle, Spring Boot s'exécute sur l'hôte et utilise donc `localhost:5434`.

## Arrêter et conserver les données

Depuis la racine du projet :

```powershell
docker compose stop postgres
```

Redémarrer le service arrêté :

```powershell
docker compose start postgres
```

Pour arrêter et retirer le conteneur sans supprimer les données persistées :

```powershell
docker compose down
```

Les données sont stockées dans le volume nommé `atlasrh-postgres-data` et survivent à `stop` et `down`. Ne lancez pas `docker compose down -v` sauf si vous souhaitez explicitement effacer les données PostgreSQL locales.
