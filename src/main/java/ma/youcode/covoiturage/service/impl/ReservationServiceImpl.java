package ma.youcode.covoiturage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ReservationMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
import ma.youcode.covoiturage.repository.ReservationRepository;
import ma.youcode.covoiturage.repository.TrajetRepository;
import ma.youcode.covoiturage.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final TrajetRepository trajetRepository;
    private final PassagerRepository passagerRepository;
    private final ReservationMapper reservationMapper;
    private final Clock clock;

    @Override
    @Transactional
    public ReservationResponse book(Long passengerId, ReservationRequest request) {
        log.info("Tentative de réservation par le passager {} sur le trajet {}", passengerId, request.tripId());

        // 1. Passager et trajet existent (404 si introuvable - Règle D-08 étape 1)
        Passager passager = passagerRepository.findById(passengerId)
                .orElseThrow(() -> new ResourceNotFoundException("Passager non trouvé avec l'id : " + passengerId));

        // Verrou pessimiste (PESSIMISTIC_WRITE / SELECT ... FOR UPDATE) pour éliminer les race conditions
        Trajet trajet = trajetRepository.findByIdForUpdate(request.tripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trajet non trouvé avec l'id : " + request.tripId()));

        // 2. Validation des paramètres & villes du parcours (400 - Règle D-08 étape 2, R-20)
        if (request.nombrePlaces() == null || request.nombrePlaces() < 1) {
            throw new InvalidRequestException("Le nombre de places doit être supérieur ou égal à 1");
        }

        PointArret montee = trajet.getPointsArret().stream()
                .filter(p -> p.getVille().equalsIgnoreCase(request.villeMontee().trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("La ville de montée '" + request.villeMontee() + "' n'appartient pas au parcours du trajet"));

        PointArret descente = trajet.getPointsArret().stream()
                .filter(p -> p.getVille().equalsIgnoreCase(request.villeDescente().trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("La ville de descente '" + request.villeDescente() + "' n'appartient pas au parcours du trajet"));

        if (montee.getOrdre() >= descente.getOrdre()) {
            throw new InvalidRequestException("La ville de montée doit précéder la ville de descente dans l'ordre chronologique du parcours");
        }

        // 3. Statut du trajet == PUBLIE (409 - Règle D-08 étape 3, R-19)
        if (trajet.getStatut() != StatutTrajet.PUBLIE) {
            throw new BusinessRuleException("Impossible de réserver un trajet qui n'est pas au statut PUBLIE (statut actuel : " + trajet.getStatut() + ")");
        }

        // 4. Trajet pas encore parti (409 - Règle D-08 étape 4)
        LocalDateTime now = LocalDateTime.now(clock);
        if (trajet.getDateDepart().isBefore(now) || montee.getHeurePassage().isBefore(now)) {
            throw new BusinessRuleException("Impossible de réserver un trajet déjà parti ou dont l'arrêt de montée est dépassé");
        }

        // 5. Passager n'a pas déjà de réservation CONFIRMEE sur ce trajet (409 - Règle D-08 étape 5, R-22)
        if (reservationRepository.existsByTrajetIdAndPassagerIdAndStatut(trajet.getId(), passager.getId(), StatutReservation.CONFIRMEE)) {
            throw new BusinessRuleException("Vous avez déjà une réservation confirmée sur ce trajet");
        }

        // 6. Places restantes suffisantes (409 - Règle D-08 étape 6, R-21, R-25)
        int placesOccupees = reservationRepository.sumPlacesConfirmedByTrajetId(trajet.getId());
        int placesRestantes = Math.max(0, trajet.getPlacesProposees() - placesOccupees);

        if (request.nombrePlaces() > placesRestantes) {
            throw new BusinessRuleException("Nombre de places insuffisant (demandées : " + request.nombrePlaces() + ", restantes : " + placesRestantes + ")");
        }

        // 7. Calcul du montant total : (prix descente - prix montée) * nombrePlaces (R-24, R-32)
        BigDecimal prixSegment = descente.getPrixDepuisDepart().subtract(montee.getPrixDepuisDepart());
        BigDecimal montantTotal = prixSegment.multiply(BigDecimal.valueOf(request.nombrePlaces()))
                .setScale(2, RoundingMode.HALF_UP);

        // 8. Création et sauvegarde de la réservation
        Reservation reservation = Reservation.builder()
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(montee)
                .pointArretDescente(descente)
                .nombrePlaces(request.nombrePlaces())
                .montantTotal(montantTotal)
                .dateReservation(now)
                .statut(StatutReservation.CONFIRMEE)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("Réservation #{} confirmée avec succès pour le passager {} sur le trajet {} (Montant: {} DH)",
                savedReservation.getId(), passager.getId(), trajet.getId(), montantTotal);

        // 9. Bascule automatique vers COMPLET si plus aucune place restante (R-26)
        if (placesRestantes - request.nombrePlaces() == 0) {
            trajet.marquerComplet();
            log.info("Trajet #{} basculé automatiquement au statut COMPLET (0 place restante)", trajet.getId());
        }

        return reservationMapper.toResponse(savedReservation);
    }
}
