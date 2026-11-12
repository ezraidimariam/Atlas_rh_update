# Préparer le débriefing technique (15 minutes)

## Déroulé suggéré

1. **Contexte et architecture (2 min)** — besoin RH, absence d'authentification en V1 et séparation controller/service/repository.
2. **Modèle (3 min)** — cinq entités, département obligatoire, manager auto-référencé, contrats et type/statut des congés.
3. **Démonstration Swagger (6 min)** — consulter les données de démonstration ; créer un employé ; créer une demande ; vérifier la liste du manager ; approuver/refuser ; montrer solde et ancienneté ; vérifier un conflit.
4. **Règles et tests (2 min)** — pagination, jours ouvrés, chevauchement, solde par année, MapStruct, Bean Validation et erreurs HTTP.
5. **Limites et évolutions (2 min)** — authentification/autorisation, jours fériés, SMTP, audit et concurrence des approbations.

## Questions à savoir expliquer

- Pourquoi les controllers ne renvoient-ils pas les entités ? Pour préserver le contrat REST et éviter de révéler le modèle JPA ou de déclencher des chargements relationnels imprévus.
- Quand le solde est-il contrôlé ? À l'approbation, au moment où les jours deviennent réellement consommés.
- Comment les années sont-elles traitées ? La période est découpée par année civile ; les jours ouvrés approuvés et demandés sont comparés pour chaque segment.
- Pourquoi Liquibase et `ddl-auto: validate` ? Les changements de schéma sont versionnés et l'ORM ne crée ni ne met à jour silencieusement les tables.
- Que signifie « manager direct » ? L'identifiant fourni doit correspondre au manager associé à l'employé de la demande. L'authentification n'étant pas activée en V1, l'identité est simulée par l'URL.

## Démo guidée

Les exemples et identifiants initiaux sont dans le README. Les valeurs de congés du jeu de démonstration ne représentent pas une règle légale. Pour une démonstration reproductible, lancer PostgreSQL, démarrer l'application et utiliser les opérations dans Swagger UI.
