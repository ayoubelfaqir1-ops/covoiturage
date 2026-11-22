package ma.youcode.covoiturage.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TrajetRepository trajetRepository;

    @Mock
    private PassagerRepository passagerRepository;

    @Mock
    private ReservationMapper reservationMapper;

    private ReservationServiceImpl reservationService;

    private Clock fixedClock;
    private Passager passager;
    private Trajet trajet;
    private PointArret stopCasa;
    private PointArret stopRabat;
    private PointArret stopTanger;

    // Horloge figée au 2026-11-20 à 10:00:00 UTC pour des tests temporels 100% déterministes
    private final LocalDateTime referenceNow = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(referenceNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        reservationService = new ReservationServiceImpl(
                reservationRepository,
                trajetRepository,
                passagerRepository,
                reservationMapper,
                fixedClock
        );

        passager = Passager.builder()
                .id(1L)
                .nom("Alami")
                .prenom("Sara")
                .email("sara@test.com")
                .telephone("0611223344")
                .build();

        stopCasa = PointArret.builder()
                .id(101L)
                .ville("Casablanca")
                .ordre(0)
                .heurePassage(referenceNow.plusDays(1).plusHours(2)) // Demain à 12:00
                .prixDepuisDepart(BigDecimal.ZERO)
                .build();

        stopRabat = PointArret.builder()
                .id(102L)
                .ville("Rabat")
                .ordre(1)
                .heurePassage(referenceNow.plusDays(1).plusHours(3)) // Demain à 13:00
                .prixDepuisDepart(new BigDecimal("35.00"))
                .build();

        stopTanger = PointArret.builder()
                .id(103L)
                .ville("Tanger")
                .ordre(2)
                .heurePassage(referenceNow.plusDays(1).plusHours(6)) // Demain à 16:00
                .prixDepuisDepart(new BigDecimal("120.00"))
                .build();

        trajet = Trajet.builder()
                .id(10L)
                .villeDepart("Casablanca")
                .villeArrivee("Tanger")
                .dateDepart(referenceNow.plusDays(1).plusHours(2))
                .placesProposees(3)
                .statut(StatutTrajet.PUBLIE)
                .pointsArret(new ArrayList<>(List.of(stopCasa, stopRabat, stopTanger)))
                .build();
    }

    // ==========================================
    // Cas Nominaux (201 Created)
    // ==========================================

    @Test
    @DisplayName("F-21 : réservation nominale réussie (R-19..R-26, 201 Created)")
    void book_Success_Nominal() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 2);

        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.existsByTrajetIdAndPassagerIdAndStatut(10L, 1L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sumPlacesConfirmedByTrajetId(10L)).thenReturn(0);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponse mockResponse = new ReservationResponse(
                50L,
                1L,
                10L,
                "Casablanca",
                "Rabat",
                2,
                new BigDecimal("70.00"),
                StatutReservation.CONFIRMEE,
                referenceNow
        );
        when(reservationMapper.toResponse(any())).thenReturn(mockResponse);

        ReservationResponse result = reservationService.book(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.passagerId()).isEqualTo(1L);
        assertThat(result.tripId()).isEqualTo(10L);
        assertThat(result.nombrePlaces()).isEqualTo(2);
        assertThat(result.montantTotal()).isEqualByComparingTo("70.00");
        assertThat(result.statut()).isEqualTo(StatutReservation.CONFIRMEE);

        // Vérification de l'entité enregistrée
        Reservation captured = captor.getValue();
        assertThat(captured.getPassager()).isEqualTo(passager);
        assertThat(captured.getTrajet()).isEqualTo(trajet);
        assertThat(captured.getPointArretMontee().getVille()).isEqualTo("Casablanca");
        assertThat(captured.getPointArretDescente().getVille()).isEqualTo("Rabat");
        assertThat(captured.getMontantTotal()).isEqualByComparingTo("70.00"); // (35.00 - 0.00) * 2 = 70.00
        assertThat(captured.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);

        // Il reste 3 - 2 = 1 place, le trajet doit RESTER PUBLIE
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
    }

    @Test
    @DisplayName("F-21 : transition automatique du trajet vers COMPLET quand 0 place restante (R-26)")
    void book_Success_TransitionsToComplet_WhenAllSeatsBooked() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Tanger", 3);

        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.existsByTrajetIdAndPassagerIdAndStatut(10L, 1L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sumPlacesConfirmedByTrajetId(10L)).thenReturn(0);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.book(1L, request);

        // 3 places demandées sur 3 disponibles -> bascule automatique à COMPLET
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.COMPLET);
    }

    // ==========================================
    // Cas d'Erreurs 404 (Introuvable)
    // ==========================================

    @Test
    @DisplayName("F-21 : rejet si passager introuvable (404)")
    void book_ThrowsNotFound_WhenPassengerNotFound() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 1);
        when(passagerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.book(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Passager non trouvé avec l'id : 99");

        verify(trajetRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("F-21 : rejet si trajet introuvable (404)")
    void book_ThrowsNotFound_WhenTripNotFound() {
        ReservationRequest request = new ReservationRequest(999L, "Casablanca", "Rabat", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trajet non trouvé avec l'id : 999");
    }

    // ==========================================
    // Cas d'Erreurs 400 (Invalide)
    // ==========================================

    @Test
    @DisplayName("F-21 : rejet si la ville de montée n'appartient pas au parcours (400)")
    void book_ThrowsInvalidRequest_WhenBoardingStopNotFound() {
        ReservationRequest request = new ReservationRequest(10L, "Marrakech", "Rabat", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("La ville de montée 'Marrakech' n'appartient pas au parcours du trajet");
    }

    @Test
    @DisplayName("F-21 : rejet si la ville de descente n'appartient pas au parcours (400)")
    void book_ThrowsInvalidRequest_WhenAlightingStopNotFound() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Agadir", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("La ville de descente 'Agadir' n'appartient pas au parcours du trajet");
    }

    @Test
    @DisplayName("F-21 : rejet si montée après descente dans l'ordre chronologique (400 / R-20 / S-06)")
    void book_ThrowsInvalidRequest_WhenStopsWrongOrder() {
        // Rabat (ordre 1) vers Casablanca (ordre 0) : mauvais sens !
        ReservationRequest request = new ReservationRequest(10L, "Rabat", "Casablanca", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("La ville de montée doit précéder la ville de descente");
    }

    // ==========================================
    // Cas d'Erreurs 409 (Conflit de règles métier)
    // ==========================================

    @Test
    @DisplayName("F-21 : rejet si le trajet n'est pas au statut PUBLIE (409 / R-19 / S-04)")
    void book_ThrowsBusinessRule_WhenTripNotPublie() {
        trajet.marquerComplet(); // Devient COMPLET
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Impossible de réserver un trajet qui n'est pas au statut PUBLIE");
    }

    @Test
    @DisplayName("F-21 : rejet si le trajet est déjà parti dans le passé (409)")
    void book_ThrowsBusinessRule_WhenTripAlreadyDeparted() {
        // Départ il y a 2 heures
        stopCasa = PointArret.builder()
                .id(101L).ville("Casablanca").ordre(0)
                .heurePassage(referenceNow.minusHours(2))
                .prixDepuisDepart(BigDecimal.ZERO).build();

        trajet = Trajet.builder()
                .id(10L).villeDepart("Casablanca").villeArrivee("Tanger")
                .dateDepart(referenceNow.minusHours(2))
                .placesProposees(3).statut(StatutTrajet.PUBLIE)
                .pointsArret(new ArrayList<>(List.of(stopCasa, stopRabat, stopTanger)))
                .build();

        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Impossible de réserver un trajet déjà parti");
    }

    @Test
    @DisplayName("F-21 : rejet si le passager a déjà une réservation confirmée sur ce trajet (409 / R-22)")
    void book_ThrowsBusinessRule_WhenPassengerAlreadyHasConfirmedReservation() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 1);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.existsByTrajetIdAndPassagerIdAndStatut(10L, 1L, StatutReservation.CONFIRMEE))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vous avez déjà une réservation confirmée sur ce trajet");
    }

    @Test
    @DisplayName("F-21 : rejet si places demandées supérieures aux places restantes (409 / R-21)")
    void book_ThrowsBusinessRule_WhenNotEnoughSeatsRemaining() {
        ReservationRequest request = new ReservationRequest(10L, "Casablanca", "Rabat", 2);
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(passager));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.existsByTrajetIdAndPassagerIdAndStatut(10L, 1L, StatutReservation.CONFIRMEE))
                .thenReturn(false);
        when(reservationRepository.sumPlacesConfirmedByTrajetId(10L)).thenReturn(2); // 3 - 2 = 1 place restante

        assertThatThrownBy(() -> reservationService.book(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nombre de places insuffisant (demandées : 2, restantes : 1)");
    }
}
