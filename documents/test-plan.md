# Plan de test : Plateforme de covoiturage multi-étapes

## 1. Jeu de données de base (Postman + seed)

| Entité | Données |
|---|---|
| Conducteurs | **Hamid** (1), **Nadia** (2) |
| Passagers | **Salma** (1), **Karim** (2), **Imane** (3) |
| Véhicules | Dacia Logan, capacité 4 (Hamid) ; Renault Clio, capacité 4 (Nadia) |
| Trajet A (Hamid) | Rabat 17:00 (0 DH) → Casablanca 18:15 (40) → Settat 19:15 (70) → Marrakech 21:30 (120) ; 3 places ; départ dans **4 jours** |
| Trajet B (Hamid) | Rabat → Marrakech ; départ dans **environ 2 h** ; Salma a 1 réservation CONFIRMEE (pour S-10) |
| Trajet C (Hamid) | Rabat → Marrakech ; départ dans 5 jours ; 2 réservations CONFIRMEE de passagers différents (pour S-11) |

Les dates sont calculées dans Postman (script de pré-requête avec des variables `{{tripDateA}}`, etc.) pour que « futur » reste vrai.
Ordre d'exécution conseillé : S-01 → S-11 dans l'ordre (S-05 libère la place prise en S-03).

## 2. Scénarios de validation (collection Postman)

| # | Requête | Précondition | Résultat attendu | Règles |
|---|---|---|---|---|
| S-01 | Salma réserve 1 place Settat → Marrakech sur A | A PUBLIE, 3 places | 201 ; montant `50.00` ; places restantes 2 ; A reste PUBLIE | R-24, R-25, R-26 |
| S-02 | Karim recherche Settat → Casablanca (date de A) | après S-01 | 200 ; page vide (le trajet de Hamid n'apparaît pas) | R-16 |
| S-03 | Karim réserve 2 places Casablanca → Marrakech sur A | après S-01 | 201 ; montant `160.00` ; A devient COMPLET | R-24, R-26 |
| S-04 | Imane réserve 1 place sur A | A COMPLET | 409 « trajet complet » | R-19 |
| S-05 | Karim annule sa réservation (A part dans 4 jours) | après S-03 | 204 ; A de nouveau PUBLIE (2 places restantes) | R-23, R-26 |
| S-06 | Imane réserve avec montée à Marrakech et descente à Settat | A PUBLIE | 400 « la montée doit précéder la descente » | R-20 |
| S-07 | Hamid publie avec 5 places dans un véhicule de capacité 4 | véhicule Dacia | 400 « 4 places au maximum » | R-13 |
| S-08 | Hamid publie avec le véhicule de Nadia | véhicule Clio | 403 | R-14 |
| S-09 | Hamid termine A (avant 17:00 le jour du départ) | A PUBLIE | 409 « le trajet n'est pas encore parti » | R-28 |
| S-10 | Salma annule sa réservation du trajet B (départ dans ~2 h) | B + réservation de Salma | 409 « délai de 24 h dépassé » | R-23 |
| S-11 | Hamid annule le trajet C (2 réservations) | C PUBLIE avec 2 CONFIRMEE | 204 ; trajet ANNULE et les 2 réservations ANNULEE | R-29, T-15 |

Après S-11, vérifier par `GET /api/passengers/{id}/reservations` que les deux réservations sont ANNULEE.

## 3. Requêtes Postman supplémentaires (cas d'erreur courants)

- Créer un conducteur avec email ou permis déjà utilisé → 409
- Créer un véhicule avec capacité 0 ou 9 → 400
- Supprimer un véhicule lié à un trajet → 409
- Publier avec départ passé, ville répétée, prix non croissants → 400
- Réserver deux fois le même trajet (même passager) → 409
- Réserver plus de places que restantes → 409
- Modifier le profil d'un autre conducteur (mauvais `X-Driver-Id`) → 403
- Consulter un trajet inexistant → 404
- Requête sans en-tête acteur → 400
- Recherche avec `maxPrice` inférieur au prix du segment → 200 + page vide
- Recherche avec `page` et `size` : ordre par heure de départ

## 4. Tests unitaires (JUnit 5, Mockito, AssertJ)

Nom de test conseillé : `méthode_shouldX_whenY_R-xx`. Pour chaque règle : un test nominal et un test de refus. Utiliser `Clock.fixed` pour toutes les règles de temps.

### TrajetServiceImpl

| Test | Règle |
|---|---|
| publish nominal : statut PUBLIE, ordres 0..n-1, villes et date de départ déduites | R-09 |
| publish : conducteur ou véhicule inconnu → 404 | n/a |
| publish : véhicule d'un autre conducteur → 403 | R-14 |
| publish : moins de 2 points → 400 | R-09 |
| publish : départ non futur → 400 | R-10 |
| publish : heures non strictement croissantes → 400 | R-10 |
| publish : ville répétée (casse ignorée) → 400 | R-11 |
| publish : premier prix ≠ 0, ou prix non strictement croissants → 400 | R-12 |
| publish : places 0, ou > capacité → 400 | R-13 |
| findById : places restantes calculées ; inconnu → 404 | R-25, R-33 |
| findReservations : autre conducteur → 403 | R-31 |
| finish nominal (après le départ) | R-28 |
| finish : avant le départ → 409 ; autre conducteur → 403 ; déjà TERMINE ou ANNULE → 409 | R-28, R-30 |
| cancel nominal : trajet ANNULE, toutes les CONFIRMEE → ANNULEE | R-29 |
| cancel : autre conducteur → 403 ; statut final → 409 | R-30 |
| cancel : si une étape échoue, rien n'est modifié (rollback, test d'intégration optionnel) | R-29 |

### ReservationServiceImpl

| Test | Règle |
|---|---|
| book nominal : montant 50.00, statut CONFIRMEE, trajet reste PUBLIE | R-24, R-26 |
| book : dernières places → trajet COMPLET, montant 160.00 | R-26 |
| book : trajet COMPLET, ANNULE ou TERMINE → 409 | R-19 |
| book : montée après descente, ou même ville → 400 | R-20 |
| book : ville absente du parcours → 400 | R-20 |
| book : places supérieures aux restantes → 409 | R-21 |
| book : passager déjà CONFIRMEE sur le trajet → 409 | R-22 |
| book : trajet ou passager inconnu → 404 | n/a |
| book : départ dépassé → 409 | D-14 |
| book : arrondi `BigDecimal` à 2 décimales | R-32 |
| cancel nominal : ANNULEE ; trajet COMPLET → PUBLIE | R-26 |
| cancel : moins de 24 h → 409 ; exactement 24 h → accepté | R-23 |
| cancel : réservation d'un autre passager → 403 ; déjà ANNULEE → 409 | R-27 |
| findByPassager : autre passager → 403 | R-27 |

### Autres services (conseillé, court)

ConducteurServiceImpl, PassagerServiceImpl, VehiculeServiceImpl : création nominale + chaque unicité (409) ; suppression de véhicule lié à un trajet (409) ; capacité hors 1 à 8.

### Recherche (recommandé, hors exigence)

Test de repository (`@DataJpaTest` avec PostgreSQL via Testcontainers, ou vérification par Postman) avec plusieurs trajets : bon sens, mauvais sens, trajet complet, autre date, `maxPrice` sur le segment, pagination et tri.

## 5. Critères de fin de test

- [ ] Les 11 scénarios passent dans Postman avec les codes attendus
- [ ] Tous les tests unitaires passent (`mvn test`)
- [ ] Chaque règle R-xx a au moins un test ou un scénario (voir `traceability.md`)
- [ ] L'application démarre sans erreur et se connecte à la base
