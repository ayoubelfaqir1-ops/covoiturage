# Décisions : Plateforme de covoiturage multi-étapes

Une entrée par choix non imposé par le brief. **Statut** : *Validé* (choisi par toi) ou *Proposé* (par défaut, à confirmer ou changer). On n'efface jamais une entrée : si tu changes d'avis, ajoute une nouvelle entrée « remplace D-xx ».

---

### D-01 Langue
- Contexte : le brief et la soutenance sont en français.
- Décision : documents et messages d'erreur de l'API en français.
- Conséquence : les messages sont rédigés une fois dans les exceptions (R-34).
- Statut : Validé

### D-02 Utilisateur acteur
- Contexte : le brief exige des 403 mais n'a pas d'authentification.
- Décision : en-têtes `X-Driver-Id` / `X-Passenger-Id`, comparés au propriétaire de la ressource.
- Pourquoi : le plus simple pour produire et tester les 403 dans Postman.
- Conséquence : pas une vraie sécurité (à dire dans le README). En-tête manquant → 400.
- Statut : Validé

### D-03 Anti-surréservation
- Contexte : deux passagers peuvent réserver la dernière place en même temps.
- Décision : verrou pessimiste (`PESSIMISTIC_WRITE`) sur la ligne du trajet pendant réservation, annulation de réservation, terminaison et annulation du trajet.
- Pourquoi : simple, sûr, facile à expliquer.
- Conséquence : les réservations d'un même trajet s'exécutent l'une après l'autre.
- Statut : Validé

### D-04 Traçabilité
- Décision : fichier `traceability.md` (tableau règle → code → test → Postman) en plus des IDs dans le code et les tests.
- Statut : Validé

### D-05 Villes
- Contexte : le brief ne dit pas si une ville est une entité.
- Décision : `String`, nettoyée (`trim`) ; comparaison sans casse (`LOWER`) pour l'unicité sur un parcours et pour la recherche.
- Conséquence : « Casablanca » et « casablanca » sont la même ville.
- Statut : Proposé

### D-06 Désignation des points de montée et de descente
- Décision : la réservation reçoit `villeMontee` et `villeDescente` (une ville est unique par trajet, R-11), pas des ids de points.
- Pourquoi : plus lisible dans Postman et cohérent avec la recherche par villes.
- Conséquence : ville absente du parcours → 400. L'entité `Reservation` garde quand même les références `pointMontee` / `pointDescente`.
- Statut : Proposé

### D-07 Codes 400 vs 409
- Décision : 400 = données incohérentes, même détectées dans le service (montée après descente S-06, places > capacité S-07, parcours invalide). 409 = conflit avec l'état ou unicité (trajet complet, délai dépassé, doublon, déjà terminé).
- Pourquoi : c'est ce que demandent les scénarios 6 et 7 (400) et 4, 9, 10 (409).
- Statut : Proposé

### D-08 Ordre des vérifications
- Décision : 404 (existence) → 403 (propriété) → 400 (cohérence des données) → 409 (état et règles).
- Pourquoi : réponse déterministe quand plusieurs règles sont violées (ex. S-06 donne 400 même si le trajet est COMPLET).
- Statut : Proposé

### D-09 Email passager unique
- Contexte : le brief ne dit « unique » que pour le conducteur, mais prévoit la recherche d'un passager par email.
- Décision : email passager unique (409).
- Statut : Proposé

### D-10 Champs obligatoires
- Décision : tous les champs du brief sont obligatoires, y compris la couleur du véhicule. Téléphone validé par un format simple.
- Statut : Proposé

### D-11 Paramètres de recherche
- Décision : `date` = jour de départ du trajet (premier point) ; `seats` optionnel (défaut 1) ; `maxPrice` optionnel ; `page` (défaut 0) et `size` (défaut 10, max 50).
- Conséquence : un trajet qui passe à une ville après minuit est trouvé par la date de son départ, pas par celle du passage.
- Statut : Proposé

### D-12 Lecture des profils
- Décision : listes et recherches de conducteurs et passagers ouvertes (pas de rôle admin dans le brief) ; profil passager et historique de réservations réservés à leur propriétaire (R-36, R-27).
- Statut : Proposé

### D-13 Horloge injectée
- Décision : bean `Clock` injecté dans les services ; tests avec `Clock.fixed`.
- Pourquoi : tester le délai de 24 h, le départ futur et « terminer après le départ » sans attendre.
- Statut : Proposé

### D-14 Réserver après le départ
- Contexte : un trajet reste PUBLIE tant que le conducteur ne le termine pas.
- Décision : réservation refusée (409) si l'heure de départ est dépassée.
- Statut : Proposé

### D-15 Codes de succès
- Décision : création → 201 ; modification, consultation, terminaison → 200 ; annulation de réservation et de trajet → 204 (S-05, S-11) ; suppression de véhicule → 204.
- Statut : Proposé

### D-16 Date de départ du trajet
- Décision : `Trajet.dateDepart` est copiée depuis l'heure du premier point à la publication ; c'est elle qui sert au tri, à la recherche par date et au délai de 24 h.
- Statut : Proposé

### D-17 Montants
- Décision : `BigDecimal` en base `NUMERIC(10,2)` ; `setScale(2, RoundingMode.HALF_UP)` appliqué au seul endroit du calcul (R-32).
- Statut : Proposé

### D-18 Modification des profils
- Décision : `PUT` avec l'objet complet (pas de `PATCH` partiel).
- Statut : Proposé
