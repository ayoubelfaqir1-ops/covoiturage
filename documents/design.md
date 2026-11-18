# Conception : Plateforme de covoiturage multi-étapes

> Les diagrammes UML sont à faire à part ; ce document donne la structure qu'ils doivent refléter. Voir `requirements.md` (quoi) et `decisions.md` (pourquoi).

## 1. Architecture

```
Controller  →  Service (interface)  →  Repository  →  PostgreSQL
   ↑ DTO records      ↑ ServiceImpl       ↑ Spring Data JPA
   └── Mapper (MapStruct) entre Entity et DTO
Exceptions → GlobalExceptionHandler (@RestControllerAdvice) → ProblemDetail
```

Packages sous `com.<groupe>.carpooling` : `controller`, `service`, `service.impl`, `repository`, `entity`, `entity.enums`, `dto`, `mapper`, `exception`, `config`.

Conventions : `XxxService` + `XxxServiceImpl`, `XxxRequest` / `XxxResponse` en `record`, un `XxxMapper` par entité, bean `Clock` dans `config` (D-13).

## 2. Modèle de données (à refléter dans l'UML)

| Entité | Attributs | Relations et contraintes |
|---|---|---|
| Conducteur | id, nom, prenom, email, telephone, numeroPermis | `email` et `numeroPermis` uniques ; 1 → N Vehicule |
| Passager | id, nom, prenom, email, telephone | `email` unique ; 1 → N Reservation |
| Vehicule | id, marque, modele, immatriculation, couleur, capacite | `immatriculation` unique ; N → 1 Conducteur ; capacité 1 à 8 |
| Trajet | id, villeDepart, villeArrivee, dateDepart, placesProposees, statut | N → 1 Conducteur ; N → 1 Vehicule ; 1 → N PointArret (composition) |
| PointArret | id, ville, ordre, heurePassage, prixDepuisDepart | N → 1 Trajet ; unique (trajet, ordre) et (trajet, ville) |
| Reservation | id, nombrePlaces, montantTotal, dateReservation, statut | N → 1 Passager ; N → 1 Trajet ; N → 1 PointArret (montée) ; N → 1 PointArret (descente) |

- `Trajet.pointsArret` : `@OneToMany(cascade = ALL, orphanRemoval = true)` + `@OrderBy("ordre ASC")`. Toutes les relations `@ManyToOne` en `LAZY`.
- Enums en `@Enumerated(EnumType.STRING)` : `StatutTrajet` (PUBLIE, COMPLET, TERMINE, ANNULE), `StatutReservation` (CONFIRMEE, ANNULEE).
- Montants : `BigDecimal` `NUMERIC(10,2)` ; dates : `LocalDateTime`.
- **Jamais stockés** : places restantes, prix d'un segment (R-33). `montantTotal` est stocké car c'est le prix au moment de la réservation.

## 3. Endpoints

Acteur : D = conducteur (`X-Driver-Id`), P = passager (`X-Passenger-Id`), — = ouvert.

| Endpoint | Acteur | OK | Erreurs | Requête → Réponse |
|---|---|---|---|---|
| `POST /api/drivers` | — | 201 | 400, 409 | ConducteurRequest → ConducteurResponse |
| `GET /api/drivers` | — | 200 | n/a | → List\<ConducteurResponse\> |
| `GET /api/drivers/{id}` | — | 200 | 404 | → ConducteurResponse |
| `PUT /api/drivers/{id}` | D | 200 | 400, 403, 404, 409 | ConducteurRequest → ConducteurResponse |
| `GET /api/drivers/search?name=&email=` | — | 200 | 400 | → List\<ConducteurResponse\> |
| `GET /api/drivers/{id}/vehicles` | — | 200 | 404 | → List\<VehiculeResponse\> |
| `POST /api/passengers` | — | 201 | 400, 409 | PassagerRequest → PassagerResponse |
| `GET /api/passengers` | — | 200 | n/a | → List\<PassagerResponse\> |
| `GET /api/passengers/{id}` | P | 200 | 403, 404 | → PassagerResponse |
| `PUT /api/passengers/{id}` | P | 200 | 400, 403, 404, 409 | PassagerRequest → PassagerResponse |
| `GET /api/passengers/search?email=` | — | 200 | 400 | → List\<PassagerResponse\> |
| `GET /api/passengers/{id}/reservations` | P | 200 | 403, 404 | → List\<ReservationResponse\> |
| `POST /api/vehicles` | D | 201 | 400, 404, 409 | VehiculeRequest → VehiculeResponse |
| `GET /api/vehicles/{id}` | — | 200 | 404 | → VehiculeResponse |
| `PUT /api/vehicles/{id}` | D | 200 | 400, 403, 404, 409 | VehiculeRequest → VehiculeResponse |
| `DELETE /api/vehicles/{id}` | D | 204 | 403, 404, 409 | n/a |
| `POST /api/trips` | D | 201 | 400, 403, 404 | TrajetRequest → TrajetDetailResponse |
| `GET /api/trips/{id}` | — | 200 | 404 | → TrajetDetailResponse |
| `GET /api/trips/{id}/reservations` | D | 200 | 403, 404 | → List\<ReservationResponse\> |
| `GET /api/trips/search?...` | — | 200 | 400 | → Page\<TrajetSearchResult\> |
| `PATCH /api/trips/{id}/finish` | D | 200 | 403, 404, 409 | → TrajetDetailResponse |
| `PATCH /api/trips/{id}/cancel` | D | 204 | 403, 404, 409 | n/a |
| `POST /api/reservations` | P | 201 | 400, 404, 409 | ReservationRequest → ReservationResponse |
| `PATCH /api/reservations/{id}/cancel` | P | 204 | 403, 404, 409 | n/a |

Un en-tête acteur manquant ou non numérique → 400.

### DTO (records)

- `ConducteurRequest(nom, prenom, email, telephone, numeroPermis)` / `ConducteurResponse(id, …)`. Même schéma pour Passager (sans permis).
- `VehiculeRequest(marque, modele, immatriculation, couleur, capacite)` / `VehiculeResponse(id, …, conducteurId)`
- `PointArretRequest(ville, heurePassage, prixDepuisDepart)` / `PointArretResponse(id, ville, ordre, heurePassage, prixDepuisDepart)`
- `TrajetRequest(vehicleId, placesProposees, List<PointArretRequest> stops)`
- `TrajetDetailResponse(id, conducteur, vehicule, villeDepart, villeArrivee, dateDepart, placesProposees, placesRestantes, statut, List<PointArretResponse> stops)`
- `TrajetSearchResult(tripId, conducteurNom, vehicule, villeMontee, heureMontee, villeDescente, heureDescente, prixSegment, placesRestantes, dateDepart)`
- `ReservationRequest(tripId, villeMontee, villeDescente, nombrePlaces)`
- `ReservationResponse(id, passagerId, tripId, villeMontee, villeDescente, nombrePlaces, montantTotal, dateReservation, statut)`

## 4. Contrats des services

```
ConducteurService
  create(req) | findAll() | findById(id) | update(actingId, id, req) | search(name, email)      // R-01..R-03, R-36
PassagerService
  create(req) | findAll() | findById(actingId, id) | update(actingId, id, req) | findByEmail(email)   // R-03, R-04, R-36
VehiculeService
  create(driverId, req) | update(driverId, id, req) | findById(id) | findByConducteur(driverId) | delete(driverId, id)   // R-05..R-08
TrajetService
  publish(driverId, req)                      // R-09..R-14
  findById(id)                                // R-25, R-33, T-14
  search(criteria, pageable)                  // R-15..R-18, T-13
  findReservations(driverId, tripId)          // R-31
  finish(driverId, tripId)                    // R-28, R-30
  cancel(driverId, tripId)                    // R-29, R-30, T-15
ReservationService
  book(passengerId, req)                      // R-19..R-26, T-15
  cancel(passengerId, reservationId)          // R-23, R-26, R-27, T-15
  findByPassager(actingId, passagerId)        // R-27
```

### Algorithme de `book` (dans cet ordre, D-08)

1. Charger le passager (404) puis le trajet **avec verrou** `findByIdForUpdate` (404).
2. Trouver le point de montée et de descente par ville (400 si absent) ; vérifier `ordreMontée < ordreDescente` (400) ; `nombrePlaces ≥ 1` (400).
3. Statut PUBLIE (409) ; départ non dépassé (409) ; pas déjà CONFIRMEE (409).
4. `restantes = placesProposees − sumPlacesConfirmed(trip)` ; `nombrePlaces ≤ restantes` (409).
5. `montant = (prixDescente − prixMontée) × nombrePlaces`, `setScale(2, HALF_UP)` ; enregistrer CONFIRMEE.
6. Si `restantes − nombrePlaces == 0` → trajet COMPLET. Log INFO.

`cancel` (réservation) : réservation (404) → propriétaire (403) → verrou du trajet → déjà ANNULEE (409) → `now ≤ dateDepart − 24h` (409) → ANNULEE → trajet COMPLET ⇒ PUBLIE. `cancel` (trajet) : verrou → propriétaire (403) → statut PUBLIE ou COMPLET (409) → réservations CONFIRMEE → ANNULEE → trajet ANNULE, le tout dans une transaction.

## 5. Repositories

| Repository | Méthodes principales |
|---|---|
| ConducteurRepository | `existsByEmail`, `existsByEmailAndIdNot`, `existsByNumeroPermis`, `existsByNumeroPermisAndIdNot`, recherche par nom/prénom (contient, sans casse) et par email |
| PassagerRepository | `existsByEmail`, `existsByEmailAndIdNot`, `findByEmailIgnoreCase` |
| VehiculeRepository | `existsByImmatriculation`, `existsByImmatriculationAndIdNot`, `findByConducteurId` |
| TrajetRepository | `findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`), `findDetailById` (JOIN FETCH points + conducteur + véhicule), `search` (JPQL + `countQuery`), `existsByVehiculeId` |
| ReservationRepository | `sumPlacesConfirmed(tripId)` (`COALESCE(SUM(...), 0)`), `existsByPassagerIdAndTrajetIdAndStatut`, `findByPassagerIdOrderByDateReservationDesc`, `findByTrajetId` (JOIN FETCH passager), `findByTrajetIdAndStatut` |

### Esquisse de la requête de recherche (T-13)

```
SELECT new ...TrajetSearchResult(t.id, ..., pm.ville, pm.heurePassage, pd.ville, pd.heurePassage,
       (pd.prixDepuisDepart - pm.prixDepuisDepart), (t.placesProposees - COALESCE(
         (SELECT SUM(r.nombrePlaces) FROM Reservation r
          WHERE r.trajet = t AND r.statut = CONFIRMEE), 0)), t.dateDepart)
FROM Trajet t JOIN t.pointsArret pm JOIN t.pointsArret pd
WHERE t.statut = PUBLIE
  AND LOWER(pm.ville) = LOWER(:dep) AND LOWER(pd.ville) = LOWER(:arr)
  AND pm.ordre < pd.ordre
  AND t.dateDepart >= :debutJour AND t.dateDepart < :finJour
  AND (t.placesProposees - COALESCE(<sous-requête>, 0)) >= :seats
  AND (:maxPrice IS NULL OR (pd.prixDepuisDepart - pm.prixDepuisDepart) <= :maxPrice)
ORDER BY t.dateDepart ASC
```

C'est une esquisse : à adapter (projection, paramètre `maxPrice` typé, requête de comptage séparée pour la pagination).

## 6. Erreurs (T-09)

| Exception | HTTP | Exemples |
|---|---|---|
| `InvalidRequestException` | 400 | montée après descente, places > capacité, parcours invalide |
| `MethodArgumentNotValidException`, `ConstraintViolationException`, `MissingRequestHeaderException`, `MethodArgumentTypeMismatchException`, `HttpMessageNotReadableException` | 400 | champ manquant, en-tête absent, JSON illisible |
| `ForbiddenOperationException` | 403 | véhicule ou réservation d'un autre |
| `ResourceNotFoundException` | 404 | trajet, véhicule, passager inconnu |
| `BusinessRuleException` | 409 | trajet complet, délai dépassé, doublon, déjà terminé |
| `Exception` (générique) | 500 | erreur interne (message générique, détail dans les logs) |

Format `ProblemDetail` : `status` (code HTTP), `title` (type d'erreur), `detail` (message explicatif en français), `instance` (chemin de la requête) et propriété `timestamp` ; pour la validation, une propriété `errors` (champ → message).

## 7. Transactions, concurrence, logs

- `@Transactional` sur `ReservationServiceImpl.book` / `cancel` et `TrajetServiceImpl.cancel` (et `finish`) ; lectures en `readOnly = true`.
- Verrou pessimiste du trajet sur ces opérations (D-03).
- Logs : `log.info` à chaque action métier réussie (avec ids), `log.warn` à chaque règle refusée, juste avant de lancer l'exception.
- Commentaire `// R-xx` à l'endroit où chaque règle est appliquée ; ID de règle dans le nom des tests.

## 8. Où vit chaque type de règle

| Type | Mécanisme | HTTP |
|---|---|---|
| Format et champs obligatoires | Bean Validation sur les `record` de requête | 400 |
| Cohérence des données dépendant d'autres objets | Service → `InvalidRequestException` | 400 |
| Propriété (autre conducteur ou passager) | Service compare l'acteur au propriétaire | 403 |
| Ressource inexistante | Service → `ResourceNotFoundException` | 404 |
| État et règles métier | Service → `BusinessRuleException` | 409 |
