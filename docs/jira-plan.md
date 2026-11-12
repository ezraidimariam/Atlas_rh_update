# Plan Jira — AtlasRH HRMS V1

## Epic RH — Référentiel du personnel

### US1 — Gérer les départements

**En tant que responsable RH**, je veux créer, modifier, consulter et supprimer les départements afin de structurer l'entreprise.

Critères d'acceptation :
- Les opérations CRUD REST sont disponibles et utilisent des DTO.
- Le nom est obligatoire et unique (insensible à la casse).
- La suppression d'un département employé est refusée.

Tâches : entité et migration Liquibase ; repository/service/mapper ; endpoints Swagger ; tests d'unicité et de suppression.

### US2 — Gérer les employés

**En tant que responsable RH**, je veux gérer les fiches employés, rechercher et consulter les équipes.

Critères d'acceptation :
- Matricule et e-mail uniques ; département existant requis.
- Le manager est un autre employé.
- Recherche par nom, filtre par département, pagination et tri.
- L'équipe directe d'un manager peut être consultée.

Tâches : entité/relation hiérarchique ; DTO et mapper MapStruct ; services et requêtes paginées ; validation et tests.

### US3 — Gérer les contrats

**En tant que responsable RH**, je veux créer et terminer les contrats et consulter leur historique.

Critères d'acceptation :
- Les types sont CDI, CDD ou STAGE ; salaire positif ; dates cohérentes.
- CDD et STAGE exigent une date de fin.
- Un seul contrat daté en cours par employé.
- L'historique est paginé et trié par date de début décroissante.

Tâches : entité/migration ; validation du service ; endpoints ; tests de règles de contrat.

## Epic Congés — Demandes et soldes

### US4 — Décider les demandes de l'équipe

**En tant que manager**, je veux voir les demandes en attente de mon équipe et les approuver ou refuser.

Critères d'acceptation :
- Seul le manager direct peut décider.
- Seules les demandes en attente sont décidables.
- Un refus exige un commentaire.
- Une approbation vérifie le solde année par année.

Tâches : requêtes de demandes en attente ; règles de décision ; vérification de solde ; tests d'autorisation métier et statuts.

### US5 — Faire et suivre une demande

**En tant qu'employé**, je veux faire, consulter et annuler mes demandes.

Critères d'acceptation :
- Jours ouvrés calculés automatiquement, week-ends exclus et bornes incluses.
- Dates inversées, périodes entièrement non ouvrées et chevauchements refusés.
- Filtre par statut et annulation possible seulement en attente par l'employé concerné.

Tâches : entité/type de congé ; calculateur de jours ouvrés ; endpoints paginés ; tests de calendrier et de cycle de vie.

### US6 — Consulter solde et ancienneté

**En tant qu'employé**, je veux voir mes soldes calculés et mon ancienneté.

Critères d'acceptation :
- Les soldes sont calculés par type et année, sans saisie manuelle.
- Seuls les jours des demandes approuvées sont déduits.
- Une demande pluriannuelle est comptabilisée séparément sur chaque année.
- L'ancienneté est renvoyée en années et mois à la date du jour.

Tâches : calcul des jours approuvés annuels ; endpoints solde/ancienneté ; tests sur les années.

## Epic qualité et livraison

Tâches : configuration Maven/Java 17/PostgreSQL ; migrations Liquibase et jeu de démonstration ; gestion globale d'erreurs ; Swagger ; README et UML ; tests unitaires ; démonstration Swagger.
