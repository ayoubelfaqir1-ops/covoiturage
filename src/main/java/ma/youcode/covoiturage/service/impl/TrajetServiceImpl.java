package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.PageResponse;
import ma.youcode.covoiturage.dto.response.TrajetDetailResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.dto.response.TrajetSearchResult;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TrajetServiceImpl implements TrajetService {

    private final TrajetRepository trajetRepository;
    private final ConducteurRepository conducteurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ReservationRepository reservationRepository;
    private final TrajetMapper trajetMapper;
    private final Clock clock;

    @Override
    @Transactional
    public TrajetResponse publierTrajet(Long driverId, TrajetPublicationRequest request) {
        // R-07: Vérifier l'existence du conducteur
        Conducteur conducteur = conducteurRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Conducteur non trouvé avec l'id : " + driverId));

        // R-07: Vérifier l'existence du véhicule
        Vehicule vehicule = vehiculeRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule non trouvé avec l'id : " + request.vehicleId()));

        // R-36: Vérifier que le véhicule appartient bien au conducteur connecté
        if (vehicule.getConducteur() == null || !vehicule.getConducteur().getId().equals(driverId)) {
            log.warn("Tentative de publication de trajet refusée : le véhicule {} n'appartient pas au conducteur {}", request.vehicleId(), driverId);
            throw new ForbiddenOperationException("Vous n'êtes pas autorisé à publier un trajet avec le véhicule d'un autre conducteur");
        }

        // Validation des règles métier du parcours (R-09 à R-12)
        validerParcours(vehicule, request.placesProposees(), request.stops());

        // Création de l'entité Trajet, affectation des arrêts ordonnés et persistance
        Trajet trajet = Trajet.builder()
                        .dateDepart(request.stops().get(0).heurePassage())
                        .villeDepart(request.stops().get(0).ville())
                        .villeArrivee(request.stops().get(request.stops().size() - 1).ville())
                        .vehicule(vehicule)
                        .conducteur(conducteur)
                        .placesProposees(request.placesProposees())
                        .statut(StatutTrajet.PUBLIE)
                        .build();
        
        for (int i = 0; i < request.stops().size(); i++) {
            PointArretRequest stopDto = request.stops().get(i);

            PointArret stop = PointArret.builder()
                    .ville(stopDto.ville())
                    .heurePassage(stopDto.heurePassage())
                    .prixDepuisDepart(stopDto.prixDepuisDepart())
                    .ordre(i) 
                    .build();

            trajet.addPointArret(stop); 
        }   
        Trajet savedTrajet = trajetRepository.save(trajet);
        return trajetMapper.toResponse(savedTrajet);
    }

    private void validerParcours(Vehicule vehicule, Integer placesProposees, List<PointArretRequest> stops) {
        // R-09: Places proposées entre 1 et la capacité du véhicule
        if (placesProposees == null || placesProposees < 1 || placesProposees > vehicule.getCapacite()) {
            throw new InvalidRequestException("Le nombre de places proposées doit être compris entre 1 et la capacité du véhicule (" + vehicule.getCapacite() + ")");
        }

        // R-10: Au moins 2 points d'arrêt (départ et arrivée)
        if (stops == null || stops.size() < 2) {
            throw new InvalidRequestException("Un trajet doit comporter au moins 2 points d'arrêt (départ et arrivée)");
        }

        // R-10: Villes uniques (insensible à la casse)
        Set<String> villesUniques = new HashSet<>();
        for (PointArretRequest stop : stops) {
            if (stop.ville() == null || stop.ville().isBlank()) {
                throw new InvalidRequestException("La ville d'un point d'arrêt ne peut pas être vide");
            }
            String villeNormalisee = stop.ville().trim().toLowerCase();
            if (!villesUniques.add(villeNormalisee)) {
                throw new InvalidRequestException("Une ville ne peut pas être répétée dans un même trajet : " + stop.ville());
            }
        }

        // R-11: La date de départ du premier arrêt doit être dans le futur
        LocalDateTime now = LocalDateTime.now(clock);
        PointArretRequest premierArret = stops.get(0);
        if (premierArret.heurePassage() == null || !premierArret.heurePassage().isAfter(now)) {
            throw new InvalidRequestException("La date de départ du premier arrêt doit être strictement dans le futur");
        }

        // R-12: Le prix depuis le départ pour le premier arrêt doit être égal à 0
        if (premierArret.prixDepuisDepart() == null || premierArret.prixDepuisDepart().compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidRequestException("Le prix depuis le départ pour le premier arrêt doit être égal à 0");
        }

        // R-11 & R-12: Validation de la progression chronologique et tarifaire entre arrêts consécutifs
        for (int i = 0; i < stops.size() - 1; i++) {
            PointArretRequest actuel = stops.get(i);
            PointArretRequest suivant = stops.get(i + 1);

            // R-11: Heures de passage strictement croissantes
            if (suivant.heurePassage() == null || !suivant.heurePassage().isAfter(actuel.heurePassage())) {
                throw new InvalidRequestException("Les heures de passage doivent être strictement chronologiques : l'arrêt "
                        + suivant.ville() + " (" + suivant.heurePassage() + ") doit être postérieur à "
                        + actuel.ville() + " (" + actuel.heurePassage() + ")");
            }

            // R-12: Prix depuis le départ strictement croissants
            if (suivant.prixDepuisDepart() == null || suivant.prixDepuisDepart().compareTo(actuel.prixDepuisDepart()) <= 0) {
                throw new InvalidRequestException("Les prix depuis le départ doivent être strictement croissants : l'arrêt "
                        + suivant.ville() + " (" + suivant.prixDepuisDepart() + ") doit avoir un prix supérieur à "
                        + actuel.ville() + " (" + actuel.prixDepuisDepart() + ")");
            }
        }
    }

    @Override 
    public TrajetDetailResponse getTrajetDetail(Long id) {

        Trajet trajet = trajetRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet non trouvé avec l'id : " + id));

        int placesOccupees = reservationRepository.sumPlacesConfirmedByTrajetId(trajet.getId());
        int placesRestantes = Math.max(0, trajet.getPlacesProposees() - placesOccupees);

        return trajetMapper.toDetailResponse(trajet, placesRestantes);
    }

    @Override
    public PageResponse<TrajetSearchResult> searchTrajets(
            String departureCity,
            String arrivalCity,
            LocalDate date,
            Integer seats,
            BigDecimal maxPrice,
            Pageable pageable) {

        // 1. Validation des paramètres obligatoires (Règles F-20)
        if (departureCity == null || departureCity.trim().isEmpty()) {
            throw new InvalidRequestException("La ville de départ est obligatoire");
        }
        if (arrivalCity == null || arrivalCity.trim().isEmpty()) {
            throw new InvalidRequestException("La ville d'arrivée est obligatoire");
        }
        if (date == null) {
            throw new InvalidRequestException("La date de départ est obligatoire");
        }

        // 2. Gestion des valeurs par défaut et contraintes
        int effectiveSeats = (seats != null) ? seats : 1;
        if (effectiveSeats < 1) {
            throw new InvalidRequestException("Le nombre de places demandées doit être au moins de 1");
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException("Le prix maximum doit être strictement positif");
        }

        // 3. Plafonner la taille de page à 50 max (exigence F-20 : défaut 10, max 50)
        int requestedSize = (pageable != null && pageable.getPageSize() > 0) ? pageable.getPageSize() : 10;
        int safeSize = Math.min(requestedSize, 50);
        int pageNumber = (pageable != null && pageable.getPageNumber() >= 0) ? pageable.getPageNumber() : 0;
        Pageable safePageable = PageRequest.of(pageNumber, safeSize);

        // 4. Délimitation de la journée : du début (00:00:00) au début du jour suivant
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        // 5. Exécution de la requête JPQL unique
        Page<TrajetSearchResult> page = trajetRepository.search(
                departureCity.trim(),
                arrivalCity.trim(),
                startOfDay,
                endOfDay,
                effectiveSeats,
                maxPrice,
                safePageable
        );

        // 6. Encapsulation dans le contrat PageResponse
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public TrajetDetailResponse finish(Long driverId, Long tripId) {
        log.info("Tentative de clôture du trajet #{} par le conducteur {}", tripId, driverId);

        // 1. Recherche du trajet avec verrou pessimiste (404 si introuvable)
        Trajet trajet = trajetRepository.findByIdForUpdate(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet non trouvé avec l'id : " + tripId));

        // 2. R-36 / R-31 : Conducteur termine SES trajets seulement (403)
        if (!trajet.getConducteur().getId().equals(driverId)) {
            log.warn("Tentative de terminer le trajet #{} par un conducteur non propriétaire (reçu: {}, propriétaire: {})",
                    tripId, driverId, trajet.getConducteur().getId());
            throw new ForbiddenOperationException("Vous ne pouvez terminer que vos propres trajets");
        }

        // 3. R-30 : TERMINE et ANNULE sont des statuts finaux (409)
        if (trajet.getStatut() == StatutTrajet.TERMINE || trajet.getStatut() == StatutTrajet.ANNULE) {
            log.warn("Tentative de terminer le trajet #{} ayant déjà le statut final {}", tripId, trajet.getStatut());
            throw new BusinessRuleException("Impossible de terminer un trajet déjà " + trajet.getStatut());
        }

        // 4. R-28 : Terminer un trajet seulement après son heure de départ (409 / S-09)
        LocalDateTime now = LocalDateTime.now(clock);
        if (!now.isAfter(trajet.getDateDepart())) {
            log.warn("Tentative de terminer le trajet #{} avant son heure de départ (now: {}, départ: {})",
                    tripId, now, trajet.getDateDepart());
            throw new BusinessRuleException("Un trajet ne peut être terminé qu'après son heure de départ");
        }

        // 5. Clôture du trajet et persistance
        trajet.terminer();
        Trajet savedTrajet = trajetRepository.save(trajet);
        log.info("Trajet #{} marqué comme TERMINE avec succès par le conducteur {}", tripId, driverId);

        // 6. Construction de la réponse détaillée avec calcul des places restantes
        int placesOccupees = reservationRepository.sumPlacesConfirmedByTrajetId(savedTrajet.getId());
        int placesRestantes = Math.max(0, savedTrajet.getPlacesProposees() - placesOccupees);

        return trajetMapper.toDetailResponse(savedTrajet, placesRestantes);
    }
}
