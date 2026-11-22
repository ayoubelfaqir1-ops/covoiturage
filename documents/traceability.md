# Traçabilité : règle → code → test → Postman

Colonne « Appliquée dans » : emplacement **prévu** (voir `design.md`). Complète « Test unitaire » (nom de la méthode de test) et « Postman » au fur et à mesure ; une cellule vide à la fin = règle oubliée ou non testée. Cocher « Fait » seulement quand les trois colonnes sont remplies.

| Règle | Résumé | Appliquée dans (prévu) | Test unitaire | Postman | Fait |
|---|---|---|---|---|---|
| R-01 | Email conducteur unique | `ConducteurServiceImpl.create/update` | | | ☐ |
| R-02 | Permis unique | `ConducteurServiceImpl.create/update` | | | ☐ |
| R-03 | Champs obligatoires | Bean Validation sur les `Request` | | | ☐ |
| R-04 | Email passager unique | `PassagerServiceImpl.create/update` | | | ☐ |
| R-05 | Immatriculation unique | `VehiculeServiceImpl.create/update` | | | ☐ |
| R-06 | Capacité 1 à 8 | Bean Validation `VehiculeRequest` | | | ☐ |
| R-07 | Véhicule du conducteur propriétaire | `VehiculeServiceImpl.update/delete` | `delete_otherDriver_throwsForbiddenOperationException` | F-16 | ☑ |
| R-08 | Suppression si aucun trajet | `VehiculeServiceImpl.delete` | `delete_linkedToTrips_throwsBusinessRuleException` | F-16 | ☑ |
| R-09 | Au moins 2 points, départ = premier, arrivée = dernier | `TrajetServiceImpl.publish` | | | ☐ |
| R-10 | Départ futur, heures strictement croissantes | `TrajetServiceImpl.publish` | | | ☐ |
| R-11 | Ville unique sur le parcours | `TrajetServiceImpl.publish` | | | ☐ |
| R-12 | Prix 0 puis strictement croissant | `TrajetServiceImpl.publish` | | | ☐ |
| R-13 | Places proposées 1 à capacité | `TrajetServiceImpl.publish` | | S-07 | ☐ |
| R-14 | Publier avec son propre véhicule | `TrajetServiceImpl.publish` | | S-08 | ☐ |
| R-15 | Seulement PUBLIE avec assez de places | `TrajetRepository.search` | `searchTrajets_Success` | S-02 | ☑ |
| R-16 | Villes = points du parcours, bon sens | `TrajetRepository.search` | `searchTrajets_Success` | S-02 | ☑ |
| R-17 | Prix max sur le segment | `TrajetRepository.search` | `searchTrajets_Success` | S-02 | ☑ |
| R-18 | Pagination, tri par heure de départ | `TrajetRepository.search` | `searchTrajets_Success`, `searchTrajets_ClampsSizeTo50` | S-02 | ☑ |
| R-19 | Réserver seulement un trajet PUBLIE | `ReservationServiceImpl.book` | | S-04 | ☐ |
| R-20 | Points du trajet, montée avant descente | `ReservationServiceImpl.book` | | S-06 | ☐ |
| R-21 | Places réservées ≤ places proposées | `ReservationServiceImpl.book` | | | ☐ |
| R-22 | Une seule CONFIRMEE par passager et trajet | `ReservationServiceImpl.book` | | | ☐ |
| R-23 | Annulation jusqu'à 24 h avant le départ | `ReservationServiceImpl.cancel` | | S-05, S-10 | ☐ |
| R-24 | Montant = (prix descente − prix montée) × places | `ReservationServiceImpl.book` | | S-01, S-03 | ☐ |
| R-25 | Places restantes calculées | `ReservationRepository.sumPlacesConfirmed`, `TrajetServiceImpl.findById` | `getTrajetDetail_Success` | S-01 / F-18 | ☑ |
| R-26 | Statut PUBLIE ↔ COMPLET automatique | `ReservationServiceImpl.book/cancel` | | S-03, S-05 | ☐ |
| R-27 | Passager : SES réservations seulement | `ReservationServiceImpl.cancel/findByPassager` | | | ☐ |
| R-28 | Terminer seulement après le départ | `TrajetServiceImpl.finish` | | S-09 | ☐ |
| R-29 | Annuler trajet + réservations ensemble | `TrajetServiceImpl.cancel` (`@Transactional`) | | S-11 | ☐ |
| R-30 | TERMINE / ANNULE = statuts finaux | `TrajetServiceImpl.finish/cancel` | | | ☐ |
| R-31 | Conducteur : réservations de SES trajets | `TrajetServiceImpl.findReservations` | | | ☐ |
| R-32 | Montants `BigDecimal` à 2 décimales | calcul du montant (un seul endroit) | | S-01, S-03 | ☐ |
| R-33 | Places restantes et prix de segment non stockés | modèle (pas de colonne) + `search`/`findById` | `getTrajetDetail_Success` | F-18 | ☑ |
| R-34 | Messages d'erreur clairs | `GlobalExceptionHandler` + exceptions | | | ☐ |
| R-35 | Séparation des rôles | en-têtes + routes par rôle | | | ☐ |
| R-36 | Profil : le sien seulement | `ConducteurServiceImpl.update`, `PassagerServiceImpl.findById/update` | | | ☐ |

## Exigences techniques à vérifier à la fin

| ID | Vérification | Fait |
|---|---|---|
| T-09 | `ProblemDetail` avec timestamp, code, type, message, chemin | ☐ |
| T-10 | INFO sur actions métier, WARN sur règles refusées | ☐ |
| T-13 | Recherche = une seule requête JPQL | `TrajetRepository.search` | ☑ |
| T-14 | Détail du trajet sans N+1 (vérifier les logs SQL) | ☑ |
| T-15 | `@Transactional` sur réservation, annulation de réservation, annulation de trajet | ☐ |
