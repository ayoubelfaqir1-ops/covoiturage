package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.TrajetMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
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
}

