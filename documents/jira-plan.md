# Planification Jira : Plateforme de covoiturage multi-étapes

Sprint unique : **16 → 20/11/2026**. Les estimations (en heures, tests inclus) sont des repères à ajuster : le total « indispensable » est d'environ 30 h pour environ 27 h disponibles, donc voir la liste de coupes en section 7.

## 1. Configuration du projet Jira

| Élément | Valeur |
|---|---|
| Clé du projet | `CARP` (à adapter si ta clé est différente) |
| Types | Epic, Story (une par fonctionnalité F-xx), Sub-task (tâches standard) |
| Statuts du tableau | À faire → En cours → En test → Terminé |
| Priorité | Highest = Must (bloque une démo ou un scénario S-xx) ; Medium = Should ; Low = Could |
| Estimation | « Original estimate » en heures |
| Labels | un par module (`conducteurs`, `passagers`, `vehicules`, `trajets`, `recherche`, `reservations`, `cycle-vie`, `livrables`) |
| Version (Fix version) | `Brief covoiturage` |
| Liens | « is blocked by » pour les dépendances de la section 5 |

**Titre d'un ticket** : `F-17 Publier un trajet`. **Description** : rôle, règles (`R-09 à R-14`), cas d'erreur (copiés de `requirements.md`) et critères d'acceptation. **Branche** : `feature/CARP-11-publier-trajet`. **Commit** : `feat(trajet): publier un trajet (F-17, R-09..R-14)`.

## 2. La boucle de travail : un endpoint à la fois (sous-tâches STD)

On ne crée **jamais** tous les controllers, puis tous les services, puis tous les repositories. Chaque story est faite **endpoint par endpoint**, et un endpoint est terminé (testé dans Postman) avant de passer au suivant. Une story à plusieurs endpoints répète la boucle autant de fois.

Les entités, les enums, les exceptions, le handler global, le bean `Clock` et la configuration MapStruct sont faits **une seule fois** dans l'epic Fondations. Tout le reste est créé au moment où l'endpoint en a besoin.

| Ordre | Code | Sous-tâche (par endpoint) |
|---|---|---|
| 1 | STD-1 | DTO `record` (requête et réponse) + méthode du mapper MapStruct |
| 2 | STD-2 | **Méthode du controller** : route, en-tête acteur, `@Valid`, appel du service |
| 3 | STD-3 | **Méthode du service** (interface + implémentation) : règles R-xx, exceptions, logs INFO et WARN, commentaires `// R-xx` |
| 4 | STD-4 | **Méthode du repository**, seulement si le service en a besoin |
| 5 | STD-5 | **Tests unitaires** (repository mocké) : un nominal et un par règle refusée |
| 6 | STD-6 | **Postman** : cas nominal d'abord, puis les cas d'erreur ; l'endpoint est fini quand les codes sont corrects |
| 7 | STD-7 | Commit avec les IDs (F-xx, R-xx), puis endpoint suivant. À la fin de la story : critères cochés, `traceability.md`, push, ticket fermé |

Pour les deux endpoints les plus riches en règles (F-17 publier, F-21 réserver), écrire les tests (STD-5) **avant** l'implémentation du service (STD-3) : la liste des tests sert de check-list.

Exemple pour `POST /api/drivers` : record + mapper → méthode du controller → méthode `create` du service (unicité email et permis) → `existsByEmail` / `existsByNumeroPermis` si besoin → tests (nominal, email pris, permis pris) → requêtes Postman 201, 409, 409, 400 → commit → `GET /api/drivers`.

Dans les tableaux de la section 4, « STD-1 à STD-7 » veut dire : appliquer cette boucle à chaque endpoint de la story.

## 3. Définition de « Terminé » (par story)

- Critères d'acceptation de `requirements.md` cochés
- Tests unitaires verts, requête(s) Postman verte(s)
- Ligne(s) de `traceability.md` remplie(s) ; décision ajoutée dans `decisions.md` si tu as dû trancher
- Code poussé sur GitHub, `main` démarre depuis un `docker compose up` propre

## 4. Epics et stories

Légende : **M** = Must, **S** = Should, **C** = Could. Les stories sans « STD » listé les utilisent toutes.

### EPIC CARP-1 : Fondations (mar. 17, ~3 h)

| Clé | Story | Prio | Est. | Sous-tâches |
|---|---|---|---|---|
| CARP-2 | Initialiser le projet, le dépôt Git et les docs | M | 0,5 h | Spring Initializr (web, data-jpa, validation, postgresql, lombok, mapstruct, springdoc, test) ; dépôt GitHub + `.gitignore` ; pousser `/docs` ; règle de branches |
| CARP-3 | Docker Compose et configuration PostgreSQL | M | 0,5 h | `docker-compose.yml` ; `application.yml` ; vérifier le démarrage et la connexion (T-03) |
| CARP-4 | Entités JPA, enums et relations | M | 1 h | 6 entités + `StatutTrajet`, `StatutReservation` ; `@OrderBy` et composition des points ; contraintes d'unicité ; lazy partout ; démarrage sans erreur |
| CARP-5 | Socle transverse | M | 1 h | Exceptions (400, 403, 404, 409) ; `GlobalExceptionHandler` avec `ProblemDetail` (timestamp, status, title, detail, instance) ; bean `Clock` ; configuration MapStruct ; Swagger ; test rapide d'une erreur JSON (T-09) |

### EPIC CARP-6 : Comptes et véhicules (mar. 17, ~3,5 h)

| Clé | Story | Prio | Est. | Règles | Sous-tâches |
|---|---|---|---|---|---|
| CARP-7 | F-01 à F-05 Conducteurs | M | 1,25 h | R-01, R-02, R-03, R-36 | STD-1 à STD-7 ; recherche par nom ou email ; unicité à la modification (exclure soi-même) |
| CARP-8 | F-06 à F-10 Passagers | M | 1 h | R-03, R-04, R-36 | STD-1 à STD-7 ; recherche par email. *F-11 est dans CARP-19* |
| CARP-9 | F-12 à F-15 Véhicules | M | 1,25 h | R-03, R-05, R-06, R-07 | STD-1 à STD-7 ; capacité 1 à 8 ; propriétaire (403). *F-16 est dans CARP-13* |

### EPIC CARP-10 : Trajets (mer. 18, ~4 h)

| Clé | Story | Prio | Est. | Règles | Sous-tâches |
|---|---|---|---|---|---|
| CARP-11 | F-17 Publier un trajet | M | 2,5 h | R-09 à R-14 | STD-1 à STD-7 ; validation du parcours (moins de 2 points, ville répétée, heures et prix croissants, départ futur) ; place ≤ capacité ; déduire villes et date de départ ; un test par règle (S-07, S-08) |
| CARP-12 | F-18 Détail d'un trajet | M | 1 h | R-25, R-33, T-14 | JOIN FETCH (`findDetailById`) ; places restantes calculées ; vérifier les logs SQL (pas de N+1) |
| CARP-13 | F-16 Supprimer un véhicule | S | 0,5 h | R-07, R-08 | `existsByVehiculeId` ; 409 si lié à un trajet ; 403 si autre conducteur |

### EPIC CARP-14 : Recherche (mer. 18, ~3 h)

| Clé | Story | Prio | Est. | Règles | Sous-tâches |
|---|---|---|---|---|---|
| CARP-15 | F-20 Rechercher des trajets | M | 3 h | R-15 à R-18, T-13 | Requête JPQL unique (double jointure sur `PointArret`, ordre, prix du segment, places restantes en sous-requête) ; requête de comptage ; `Pageable` + tri ; paramètres optionnels (`seats`, `maxPrice`) ; validation (400) ; jeu de trajets pour tester (bon sens, mauvais sens, complet, autre date, prix max) ; Postman S-02 |

### EPIC CARP-16 : Réservations (jeu. 19, ~6,5 h)

| Clé | Story | Prio | Est. | Règles | Sous-tâches |
|---|---|---|---|---|---|
| CARP-17 | F-21 Réserver des places | M | 3,5 h | R-19 à R-26, T-15 | STD-1 à STD-7 ; `findByIdForUpdate` (verrou, D-03) ; `sumPlacesConfirmed` ; ordre des vérifications (D-08) ; calcul du montant `BigDecimal` ; passage à COMPLET ; `@Transactional` ; tests : S-01, S-03, S-04, S-06 |
| CARP-18 | F-22 Annuler sa réservation | M | 2 h | R-23, R-26, R-27 | Verrou du trajet ; règle des 24 h avec `Clock` (test de limite exacte) ; retour à PUBLIE ; 403 autre passager ; S-05, S-10 |
| CARP-19 | F-11 Historique des réservations | S | 0,5 h | R-27 | Tri du plus récent ; 403 autre passager |
| CARP-20 | F-19 Réservations de ses trajets | S | 0,5 h | R-31 | `findByTrajetId` (JOIN FETCH passager) ; 403 autre conducteur |

### EPIC CARP-21 : Cycle de vie (jeu. 19 soir, ~2,5 h)

| Clé | Story | Prio | Est. | Règles | Sous-tâches |
|---|---|---|---|---|---|
| CARP-22 | F-23 Terminer un trajet | M | 1 h | R-28, R-30 | Après l'heure de départ seulement (`Clock`) ; 403, 409 ; S-09 |
| CARP-23 | F-24 Annuler un trajet | M | 1,5 h | R-29, R-30, T-15 | Une transaction : trajet ANNULE + toutes les CONFIRMEE → ANNULEE ; rollback si échec ; S-11 |

### EPIC CARP-24 : Qualité et livrables (au fil de l'eau, finition ven. 20)

| Clé | Story | Prio | Est. | Sous-tâches |
|---|---|---|---|---|
| CARP-25 | Données de démonstration | M | 1 h | Seed avec au moins 5 enregistrements par table ; cohérent avec la démo (Hamid, Salma, Karim, Imane) |
| CARP-26 | Collection Postman S-01 à S-11 | M | 1,5 h | Assembler les requêtes écrites par story ; variables de dates ; scripts de test sur les codes ; exporter dans `/postman` |
| CARP-27 | Revue de traçabilité et des exigences techniques | M | 1 h | `traceability.md` sans cellule vide ; vérifier T-09 à T-15 ; passer en revue `decisions.md` |
| CARP-28 | Diagramme de classes UML (image) | M | 1 h | Brouillon mardi soir, mise à jour vendredi avec le code final |
| CARP-29 | README | M | 1 h | Squelette mardi, finition vendredi |
| CARP-30 | Slides et préparation de la soutenance | M | 2 h | Slides simples ; script de démo ; questions probables ; exercices de modification de code |
| CARP-31 | Test de concurrence : double réservation de la dernière place | C | 1 h | Deux threads, une seule réservation acceptée |
| CARP-32 | Test de repository pour la recherche | C | 1 h | `@DataJpaTest` + Testcontainers PostgreSQL |

## 5. Dépendances

```
CARP-2 → CARP-3 → CARP-4 → CARP-5
CARP-5 → CARP-7, CARP-8, CARP-9          (CRUD indépendants entre eux)
CARP-9 → CARP-11 → CARP-12, CARP-13, CARP-15
CARP-11 + CARP-12 → CARP-17 → CARP-18 → CARP-19, CARP-20
CARP-11 → CARP-22, CARP-23 (CARP-23 aussi après CARP-17)
Tout → CARP-25, CARP-26, CARP-27
```

## 6. Planning par jour (objectifs du sprint)

| Jour | Objectif | Stories | Heures |
|---|---|---|---|
| **Mar. 17** | CRUD des trois modules, app qui démarre, erreurs JSON cohérentes | CARP-2 à 5, 7, 8, 9 ; brouillon UML (CARP-28) ; créer les tickets Jira | ~7,5 h |
| **Mer. 18** | Publier, consulter et **rechercher** des trajets (S-02, S-07, S-08) | CARP-11, 12, 13, 15 | ~7 h |
| **Jeu. 19** | Réserver et annuler (S-01, S-03 à S-06, S-10), terminer et annuler un trajet (S-09, S-11) | CARP-17, 18, 19, 20, 22, 23 | ~9 h (jour le plus chargé) |
| **Ven. 20** | Livrables et démo | CARP-25 à 30, vérification finale ; répétition de la démo | ~7 h |

Point de contrôle chaque soir : tout ce qui a été fait est poussé, le tableau Jira est à jour, `main` démarre.

## 7. Si tu prends du retard (ordre de coupe)

1. Retirer CARP-31 et CARP-32 (Could)
2. Reporter CARP-19 et CARP-20 (Should) au vendredi matin
3. Réduire les tests des modules CRUD (garder nominal + unicité) pour garder du temps aux services trajet et réservation, qui sont les seuls tests exigés
4. Ne jamais couper : CARP-15, 17, 18, 23, car ce sont les scénarios que le jury testera

## 8. Rituel de ticket (rappel)

1. Passer le ticket « En cours » et créer la branche
2. Lire son bloc dans `requirements.md` et sa ligne dans `design.md`
3. Coder tranche par tranche (STD-1 à STD-6)
4. « En test » : lancer les tests et Postman, cocher les critères
5. Mettre à jour la traçabilité, committer avec les IDs, pousser, passer à « Terminé »
