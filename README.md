# AtlasRH HRMS V1

API REST de gestion RH construite avec Java 17, Spring Boot, PostgreSQL, Liquibase et MapStruct.

## Prérequis et lancement

- JDK 17 (le projet cible Java 17).
- Maven 3.8 ou supérieur.
- Docker Desktop avec Docker Compose.

Depuis la racine du projet, démarrer PostgreSQL et l'API :

```powershell
docker compose up -d postgres
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

Liquibase crée le schéma et insère les données de démonstration au premier démarrage. Hibernate est configuré en `validate` : il ne modifie pas le schéma.

- Arrêter PostgreSQL sans effacer les données : `docker compose down`.
- Guide complet (configuration, ports, démarrage/arrêt) : [POSTGRESQL_SETUP.md](POSTGRESQL_SETUP.md).
- Swagger UI : http://localhost:8082/swagger-ui.html
- OpenAPI JSON : http://localhost:8082/v3/api-docs

Les exemples du jeu de données utilisent des identifiants `1` à `5` (manager RH `1`, manager informatique `2`, employés `3` à `5`). Le jeu de données n'est fourni qu'à titre de démonstration.

## Architecture

Le code est organisé dans `src/main/java/com/atlasrh` :

| Package | Responsabilité |
|---|---|
| `controller` | Endpoints HTTP, validation d'entrée et codes de réponse. |
| `service` | Règles métier et coordination des repositories/mappers. |
| `repository` | Requêtes Spring Data JPA. |
| `entity` | Les cinq entités persistées : `Department`, `Employee`, `Contract`, `LeaveType`, `LeaveRequest`. |
| `dto.request` / `dto.response` | Objets échangés avec l'API ; les entités ne sont jamais renvoyées. |
| `mapper` | Conversion Entity/DTO par MapStruct. |
| `enums` | Types de contrat et statuts de demande. |
| `exception` | Exceptions métier et format d'erreur REST commun. |

Les controllers appellent uniquement les services. Le schéma et les données de démonstration sont versionnés dans `src/main/resources/db/changelog`.

## Endpoints

Toutes les routes sont préfixées par `/api`. Il n'y a pas d'authentification dans cette V1 : les identifiants dans les URL permettent de simuler le collaborateur ou le manager concerné.

| Méthode | Route | Fonction |
|---|---|---|
| `POST` | `/departments` | Créer un département |
| `GET` | `/departments`, `/departments/{id}` | Lister/consulter les départements |
| `PUT`, `DELETE` | `/departments/{id}` | Modifier/supprimer (suppression interdite si employés associés) |
| `POST` | `/employees` | Créer un employé |
| `GET` | `/employees`, `/employees/{id}` | Rechercher/lister ou consulter |
| `PUT` | `/employees/{id}` | Modifier |
| `GET` | `/employees/managers/{managerId}/team` | Consulter l'équipe directe |
| `POST` | `/employees/{employeeId}/contracts` | Créer un contrat |
| `GET` | `/employees/{employeeId}/contracts` | Historique paginé des contrats |
| `PATCH` | `/contracts/{contractId}/terminate` | Terminer un contrat en cours |
| `POST` | `/employees/{employeeId}/leave-requests` | Créer une demande |
| `GET` | `/employees/{employeeId}/leave-requests` | Lister ses demandes avec filtre optionnel `status` |
| `PATCH` | `/employees/{employeeId}/leave-requests/{requestId}/cancel` | Annuler une demande en attente |
| `GET` | `/managers/{managerId}/leave-requests/pending` | Demandes en attente de l'équipe |
| `POST` | `/managers/{managerId}/leave-requests/{requestId}/approve` | Approuver si le solde de chaque année suffit |
| `POST` | `/managers/{managerId}/leave-requests/{requestId}/reject` | Refuser avec commentaire obligatoire |
| `GET` | `/employees/{employeeId}/leave-balances?year=2026` | Solde calculé (année courante par défaut) |
| `GET` | `/employees/{employeeId}/seniority` | Ancienneté en années et mois |
| `GET` | `/leave-types` | Types de congé et jours annuels configurés |

Les listes utilisent `page`, `size` et `sort` (syntaxe Spring `sort=lastName,asc`). La recherche des employés accepte `search` et `departmentId`.

## Exemples

Créer un employé :

```http
POST /api/employees
Content-Type: application/json
```

```json
{
  "employeeNumber": "RH010",
  "lastName": "Martin",
  "firstName": "Aya",
  "email": "aya.martin@example.test",
  "position": "Chargée RH",
  "hireDate": "2024-02-01",
  "departmentId": 1,
  "managerId": 1
}
```

Créer une demande de congé :

```http
POST /api/employees/3/leave-requests
Content-Type: application/json
```

```json
{
  "leaveTypeId": 1,
  "startDate": "2026-12-21",
  "endDate": "2026-12-23",
  "reason": "Congé personnel"
}
```

Refuser une demande :

```http
POST /api/managers/1/leave-requests/2/reject
Content-Type: application/json
```

```json
{ "comment": "Période déjà couverte par un collègue." }
```

Une erreur suit le format `timestamp`, `status`, `error`, `message`, `path` et, pour les erreurs Bean Validation, `validationErrors`. Les conflits d'unicité renvoient `409`, les données/règles invalides `400`, et les ressources inconnues `404`.

## Règles métier

- Les noms de départements, matricules et e-mails sont uniques ; un département avec des employés n'est pas supprimable.
- Un manager doit être un autre employé ; les contrats CDD/STAGE doivent avoir une date de fin et un seul contrat daté en cours est autorisé par employé.
- Les jours de congé sont calculés du lundi au vendredi, dates incluses. Une demande en attente ou approuvée bloque les dates qui se chevauchent.
- Le solde est calculé par type et année : jours annuels configurés moins jours ouvrés de congés approuvés. Pour une demande couvrant plusieurs années, chaque année est contrôlée séparément au moment de l'approbation.
- Un manager direct seulement peut décider une demande en attente. Un refus requiert un commentaire ; seul le collaborateur concerné peut annuler une demande en attente.
- Les nombres de jours fournis pour « Congé annuel » (25) et « Congé exceptionnel » (5) dans le changelog sont des valeurs de démonstration configurables, pas des règles légales.

## Tests

```powershell
mvn test
```

Les tests de services mockent la persistance et les tests de règles de calendrier sont indépendants de PostgreSQL.

## Livrables complémentaires

- Diagramme UML : [docs/class-diagram.puml](docs/class-diagram.puml)
- Plan Jira : [docs/jira-plan.md](docs/jira-plan.md)
- Préparation au débriefing : [docs/debrief-guide.md](docs/debrief-guide.md)
- PostgreSQL avec Docker : [POSTGRESQL_SETUP.md](POSTGRESQL_SETUP.md)

Les intégrations SMTP, l'organigramme dédié et JPA Auditing sont des bonus et ne sont pas inclus dans le périmètre obligatoire de cette V1.
