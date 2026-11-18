# Exigences : Plateforme de covoiturage multi-étapes

> Le brief (PDF) reste la source. Ce document ajoute ce qu'il ne dit pas : hypothèses, cas d'erreur et critères d'acceptation.
> IDs : **F** = fonctionnalité, **R** = règle métier, **T** = exigence technique, **S** = scénario de validation (1 à 11), **D** = décision (voir `decisions.md`).

## 1. Objectif et acteurs

API REST backend permettant à des **conducteurs** de publier des trajets à plusieurs points d'arrêt et à des **passagers** de réserver des places sur la totalité ou une partie du trajet (segment). Le prix d'un segment et les places restantes sont toujours calculés.

| Acteur | Peut | Ne peut pas |
|---|---|---|
| Conducteur | Créer son compte, enregistrer SES véhicules, publier avec SES véhicules, consulter les réservations de SES trajets, terminer ou annuler SES trajets | Réserver une place |
| Passager | Créer son compte, rechercher et consulter les trajets, réserver, consulter et annuler SES réservations | Publier un trajet, voir les réservations des autres |

## 2. Hypothèses et hors périmètre

- Pas d'authentification : l'acteur est identifié par l'en-tête `X-Driver-Id` ou `X-Passenger-Id` (D-02).
- Pas de paiement, pas de notation, pas de notification.
- Une place réservée est occupée sur tout le trajet, quels que soient les points de montée et de descente (simplification du brief).
- Les décisions non précisées par le brief sont dans `decisions.md`.

## 3. Fonctionnalités

### Conducteurs

**F-01 Créer un conducteur** : `POST /api/drivers` | R-01, R-02, R-03
- Nominal : 201 + conducteur créé avec son id.
- Erreurs : email déjà utilisé → 409 | permis déjà utilisé → 409 | champ obligatoire manquant ou email invalide → 400.
- [ ] Tests unitaires (nominal + chaque règle refusée) | [ ] Postman

**F-02 Lister les conducteurs** : `GET /api/drivers` : 200 + liste (vide si aucun).

**F-03 Consulter un conducteur** : `GET /api/drivers/{id}` : 200 | inconnu → 404.

**F-04 Modifier son profil** : `PUT /api/drivers/{id}` + `X-Driver-Id` | R-01, R-02, R-03, R-36
- Nominal : 200 + profil mis à jour.
- Erreurs : id différent de l'en-tête → 403 | email ou permis pris par un autre conducteur → 409 | 400 | 404.

**F-05 Rechercher un conducteur par nom ou email** : `GET /api/drivers/search?name=&email=`
- Nominal : 200 + liste (nom ou prénom contenant le texte, sans casse ; email exact sans casse).
- Erreurs : aucun paramètre → 400.

### Passagers

**F-06 Créer un passager** : `POST /api/passengers` | R-03, R-04 : 201 | email déjà utilisé → 409 | 400.

**F-07 Consulter son profil** : `GET /api/passengers/{id}` + `X-Passenger-Id` | R-36 : 200 | autre passager → 403 | 404.

**F-08 Modifier ses informations** : `PUT /api/passengers/{id}` + `X-Passenger-Id` | R-03, R-04, R-36 : 200 | 403 | 409 (email pris) | 400 | 404.

**F-09 Lister les passagers** : `GET /api/passengers` : 200 + liste.

**F-10 Rechercher un passager par email** : `GET /api/passengers/search?email=` : 200 + liste (0 ou 1) | paramètre absent → 400.

**F-11 Consulter ses réservations (historique)** : `GET /api/passengers/{id}/reservations` + `X-Passenger-Id` | R-27
- Nominal : 200 + toutes ses réservations (CONFIRMEE et ANNULEE), les plus récentes d'abord.
- Erreurs : autre passager → 403 | passager inconnu → 404.

### Véhicules

**F-12 Ajouter un véhicule** : `POST /api/vehicles` + `X-Driver-Id` | R-03, R-05, R-06
- 201 | immatriculation déjà utilisée → 409 | capacité hors 1 à 8 ou champ manquant → 400 | conducteur inconnu → 404.

**F-13 Modifier un véhicule** : `PUT /api/vehicles/{id}` + `X-Driver-Id` | R-05, R-06, R-07
- 200 | véhicule d'un autre conducteur → 403 | immatriculation prise → 409 | 400 | 404.

**F-14 Consulter un véhicule** : `GET /api/vehicles/{id}` : 200 | 404.

**F-15 Lister les véhicules d'un conducteur** : `GET /api/drivers/{id}/vehicles` : 200 | conducteur inconnu → 404.

**F-16 Supprimer un véhicule** : `DELETE /api/vehicles/{id}` + `X-Driver-Id` | R-07, R-08
- 204 | véhicule d'un autre → 403 | véhicule lié à au moins un trajet (quel que soit son statut) → 409 | 404.

### Trajets

**F-17 Publier un trajet** : `POST /api/trips` + `X-Driver-Id` | R-09 à R-14
- Corps : `vehicleId`, `placesProposees`, `stops` (liste ordonnée : `ville`, `heurePassage`, `prixDepuisDepart`).
- Nominal : 201 + détail du trajet ; statut PUBLIE ; `ordre` = position dans la liste (0 = départ) ; ville de départ et d'arrivée déduites du premier et du dernier point.
- Erreurs :
  - conducteur ou véhicule inconnu → 404
  - véhicule d'un autre conducteur → 403 (S-08)
  - moins de 2 points, ville répétée, heures non strictement croissantes, premier prix ≠ 0, prix non strictement croissants, départ non futur → 400
  - places proposées < 1 ou > capacité du véhicule → 400 (S-07)
- [ ] Un test unitaire par règle refusée | [ ] Postman S-07, S-08

**F-18 Consulter le détail d'un trajet** : `GET /api/trips/{id}` | T-14, R-25, R-33
- 200 + conducteur, véhicule, parcours trié par ordre, places proposées, **places restantes (calculées)**, statut.
- 404 si inconnu.
- [ ] Une seule requête (JOIN FETCH), pas de N+1

**F-19 Consulter les réservations de ses trajets** : `GET /api/trips/{id}/reservations` + `X-Driver-Id` | R-31
- 200 + réservations du trajet (passager, segment, places, montant, statut) | trajet d'un autre conducteur → 403 | 404.

**F-20 Rechercher des trajets** : `GET /api/trips/search?departureCity=&arrivalCity=&date=&seats=&maxPrice=&page=&size=` | R-15 à R-18, R-33, T-13
- Obligatoires : `departureCity`, `arrivalCity`, `date`. Optionnels : `seats` (défaut 1), `maxPrice`, `page` (défaut 0), `size` (défaut 10, max 50).
- Nominal : 200 + page de résultats triés par heure de départ du trajet. Chaque résultat contient le prix du **segment** et les places restantes.
- Cas vide : ville de départ après la ville d'arrivée sur le parcours (S-02), trajet non PUBLIE, pas assez de places → 200 + page vide.
- Erreurs : paramètre obligatoire manquant ou invalide, `seats` < 1, `size` hors limites → 400.
- [ ] Une seule requête JPQL (+ requête de comptage) | [ ] Postman S-02

### Réservations

**F-21 Réserver des places** : `POST /api/reservations` + `X-Passenger-Id` | R-19 à R-26, T-15
- Corps : `tripId`, `villeMontee`, `villeDescente`, `nombrePlaces` (D-06).
- Nominal (S-01, S-03) : 201 + réservation CONFIRMEE avec montant `(prix descente − prix montée) × nombrePlaces` ; le trajet devient COMPLET si plus aucune place.
- Erreurs (dans cet ordre, D-08) :
  - passager ou trajet inconnu → 404
  - ville absente du parcours, montée non avant descente → 400 (S-06) | `nombrePlaces` < 1 → 400
  - trajet non PUBLIE (COMPLET, TERMINE, ANNULE) → 409 (S-04)
  - trajet déjà parti → 409
  - passager déjà CONFIRMEE sur ce trajet → 409
  - places demandées > places restantes → 409
- [ ] Verrou pessimiste sur le trajet pendant la réservation (D-03)
- [ ] INFO au succès, WARN à chaque refus | [ ] Postman S-01, S-03, S-04, S-06

**F-22 Annuler sa réservation** : `PATCH /api/reservations/{id}/cancel` + `X-Passenger-Id` | R-23, R-26, R-27, T-15
- Nominal (S-05) : 204 ; réservation ANNULEE ; un trajet COMPLET redevient PUBLIE.
- Erreurs : réservation d'un autre passager → 403 | inconnue → 404 | déjà annulée → 409 | moins de 24 h avant le départ → 409 (S-10).
- Limite : exactement 24 h avant le départ est accepté (« jusqu'à 24 heures »).
- [ ] Test de limite à 24 h avec `Clock` fixé

### Cycle de vie

**F-23 Terminer un trajet** : `PATCH /api/trips/{id}/finish` + `X-Driver-Id` | R-28, R-30
- Nominal : 200 + trajet TERMINE (seulement après l'heure de départ).
- Erreurs : autre conducteur → 403 | 404 | avant l'heure de départ → 409 (S-09) | trajet déjà TERMINE ou ANNULE → 409.

**F-24 Annuler un trajet** : `PATCH /api/trips/{id}/cancel` + `X-Driver-Id` | R-29, R-30, T-15
- Nominal (S-11) : 204 ; trajet ANNULE et **toutes ses réservations CONFIRMEE passent à ANNULEE dans la même transaction**.
- Erreurs : autre conducteur → 403 | 404 | trajet TERMINE ou ANNULE → 409.
- [ ] Échec en bloc : si une étape échoue, rien n'est modifié

## 4. Règles métier

| ID | Règle | Erreur |
|---|---|---|
| R-01 | Email conducteur unique | 409 |
| R-02 | Numéro de permis unique | 409 |
| R-03 | Champs obligatoires renseignés (conducteur, passager, véhicule) | 400 |
| R-04 | Email passager unique (D-09) | 409 |
| R-05 | Immatriculation unique | 409 |
| R-06 | Capacité = places passagers (hors conducteur), entre 1 et 8 | 400 |
| R-07 | Un véhicule appartient à un seul conducteur, qui ne gère que ses véhicules | 403 |
| R-08 | Suppression d'un véhicule seulement s'il n'est lié à aucun trajet | 409 |
| R-09 | Un trajet a au moins 2 points : le premier est le départ, le dernier l'arrivée | 400 |
| R-10 | Départ dans le futur ; heures de passage strictement croissantes | 400 |
| R-11 | Une ville n'apparaît qu'une fois sur le parcours | 400 |
| R-12 | Prix depuis le départ = 0 au premier point, puis strictement croissant | 400 |
| R-13 | Places proposées entre 1 et la capacité du véhicule | 400 |
| R-14 | Le conducteur publie uniquement avec l'un de ses véhicules | 403 |
| R-15 | La recherche renvoie seulement les trajets PUBLIE avec assez de places restantes | n/a |
| R-16 | Villes cherchées = n'importe quels points du parcours, départ avant arrivée | n/a |
| R-17 | Le prix maximal s'applique au prix du segment, pas du trajet complet | n/a |
| R-18 | Résultats paginés, triés par heure de départ du trajet | n/a |
| R-19 | On ne réserve que sur un trajet PUBLIE | 409 |
| R-20 | Les deux points appartiennent au trajet, la montée est avant la descente | 400 |
| R-21 | Les places réservées ne dépassent jamais les places proposées | 409 |
| R-22 | Un passager n'a qu'une réservation CONFIRMEE par trajet | 409 |
| R-23 | Un passager annule jusqu'à 24 h avant le départ | 409 |
| R-24 | Montant = (prix descente − prix montée) × nombre de places | n/a |
| R-25 | Places restantes = places proposées − places des réservations CONFIRMEE | n/a |
| R-26 | Statut du trajet mis à jour après chaque réservation ou annulation (PUBLIE ↔ COMPLET) | n/a |
| R-27 | Un passager ne voit et n'annule que SES réservations | 403 |
| R-28 | Terminer un trajet seulement après son heure de départ | 409 |
| R-29 | Annuler un trajet annule toutes ses réservations CONFIRMEE dans la même opération | n/a |
| R-30 | TERMINE et ANNULE sont des statuts finaux (aucune modification) | 409 |
| R-31 | Le conducteur ne consulte que les réservations de SES trajets | 403 |
| R-32 | Tous les montants en `BigDecimal`, 2 décimales | n/a |
| R-33 | Places restantes et prix d'un segment sont calculés, jamais stockés | n/a |
| R-34 | Messages d'erreur clairs (en français, D-01) | n/a |
| R-35 | Séparation des rôles : un conducteur ne réserve pas, un passager ne publie pas | 403/400 |
| R-36 | Un utilisateur ne consulte ou ne modifie que son propre profil | 403 |

## 5. Exigences techniques

| ID | Exigence |
|---|---|
| T-01 | Backend REST uniquement, testé via Postman ou Swagger |
| T-02 | Spring Boot, Java 17+, Maven |
| T-03 | PostgreSQL lancé avec Docker Compose |
| T-04 | Spring Data JPA (Hibernate) |
| T-05 | Architecture en couches : Controller, Service, Repository, Entity, DTO, Mapper |
| T-06 | DTO en `record`, convertis avec MapStruct |
| T-07 | Bean Validation pour le format ; règles métier vérifiées dans les services |
| T-08 | Services : interface + implémentation |
| T-09 | `@RestControllerAdvice` central ; réponses `ProblemDetail` (timestamp, code HTTP, type d'erreur, message, chemin) |
| T-10 | Logs SLF4J : INFO pour les actions métier, WARN pour les règles refusées |
| T-11 | Stream API, Java Time API, expressions lambda |
| T-12 | Tests unitaires JUnit 5, Mockito, AssertJ (services trajet et réservation : nominal + règles refusées) |
| T-13 | Recherche en une seule requête JPQL (deux points du même trajet, ordre respecté, prix du segment, places restantes, pagination) |
| T-14 | Détail d'un trajet sans N+1 (JOIN FETCH ou projection) |
| T-15 | `@Transactional` : réservation, annulation de réservation, annulation de trajet |
| T-16 | Points d'arrêt en composition (n'existent pas sans leur trajet), triés par ordre |
