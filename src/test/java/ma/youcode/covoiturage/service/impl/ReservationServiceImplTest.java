package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.entity.enums.StatutReservation;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
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
    private Conducteur conducteur;
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

        conducteur = Conducteur.builder()
                .id(2L)
                .nom("Benani")
                .prenom("Karim")
                .email("karim@test.com")
                .telephone("0622334455")
                .build();

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
                .conducteur(conducteur)
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

    // ==========================================
    // F-22 : Annuler sa réservation (R-23, R-26, R-27, T-15)
    // ==========================================

    @Test
    @DisplayName("F-22 : annulation nominale réussie à plus de 24h du départ (204 No Content - S-05)")
    void cancel_Success_Nominal() {
        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa) // Demain à 12:00 (dans 26 heures par rapport à referenceNow 10:00)
                .pointArretDescente(stopRabat)
                .nombrePlaces(2)
                .montantTotal(new BigDecimal("70.00"))
                .statut(StatutReservation.CONFIRMEE)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        reservationService.cancel(1L, 50L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        verify(reservationRepository).save(reservation);
    }

    @Test
    @DisplayName("F-22 : transition automatique du trajet de COMPLET vers PUBLIE lors d'une annulation (R-26)")
    void cancel_Success_TransitionsCompletToPublie() {
        trajet.marquerComplet(); // Trajet était COMPLET

        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa) // Dans 26h
                .pointArretDescente(stopRabat)
                .nombrePlaces(2)
                .montantTotal(new BigDecimal("70.00"))
                .statut(StatutReservation.CONFIRMEE)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        reservationService.cancel(1L, 50L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(trajet.getStatut()).isEqualTo(StatutTrajet.PUBLIE); // Repassé à PUBLIE
    }

    @Test
    @DisplayName("F-22 : annulation acceptée à exactement 24h avant le départ (R-23)")
    void cancel_Success_Exactly24HoursBeforeDeparture() {
        // Départ exactement dans 24 heures (2026-11-21 à 10:00:00)
        stopCasa = PointArret.builder()
                .id(101L).ville("Casablanca").ordre(0)
                .heurePassage(referenceNow.plusHours(24))
                .prixDepuisDepart(BigDecimal.ZERO).build();

        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .pointArretDescente(stopRabat)
                .nombrePlaces(1)
                .statut(StatutReservation.CONFIRMEE)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        // Exactement 24h avant : doit être accepté sans lever d'exception
        reservationService.cancel(1L, 50L);

        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("F-22 : rejet si la réservation n'existe pas (404)")
    void cancel_ThrowsNotFound_WhenReservationNotFound() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Réservation non trouvée avec l'id : 999");
    }

    @Test
    @DisplayName("F-22 : rejet si un passager tente d'annuler la réservation d'un autre (403 / R-27)")
    void cancel_ThrowsForbidden_WhenNotOwner() {
        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager) // Propriétaire = passager 1L
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .statut(StatutReservation.CONFIRMEE)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        // Passager 2L tente d'annuler la réservation du passager 1L
        assertThatThrownBy(() -> reservationService.cancel(2L, 50L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Vous ne pouvez annuler que vos propres réservations");

        verify(trajetRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("F-22 : rejet si la réservation est déjà annulée (409)")
    void cancel_ThrowsBusinessRule_WhenAlreadyCancelled() {
        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .statut(StatutReservation.ANNULEE) // Déjà annulée
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(1L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cette réservation est déjà annulée");

        verify(trajetRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("F-22 : rejet si annulation à moins de 24h avant le départ (409 / R-23 / S-10)")
    void cancel_ThrowsBusinessRule_WhenLessThan24HoursBeforeDeparture() {
        // Départ dans seulement 2 heures (par exemple referenceNow + 2h)
        stopCasa = PointArret.builder()
                .id(101L).ville("Casablanca").ordre(0)
                .heurePassage(referenceNow.plusHours(2))
                .prixDepuisDepart(BigDecimal.ZERO).build();

        Reservation reservation = Reservation.builder()
                .id(50L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .statut(StatutReservation.CONFIRMEE)
                .build();

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(reservation));
        when(trajetRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.cancel(1L, 50L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Impossible d'annuler une réservation moins de 24 heures avant le départ");
    }

    // ==========================================
    // F-11 : Historique des réservations d'un passager (R-27)
    // ==========================================

    @Test
    @DisplayName("F-11 : consultation nominale de l'historique des réservations (200 OK - CONFIRMEE et ANNULEE)")
    void findByPassager_Success_Nominal() {
        Reservation res1 = Reservation.builder()
                .id(100L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .pointArretDescente(stopRabat)
                .nombrePlaces(2)
                .montantTotal(new BigDecimal("70.00"))
                .statut(StatutReservation.CONFIRMEE)
                .dateReservation(referenceNow.minusDays(1))
                .build();

        Reservation res2 = Reservation.builder()
                .id(101L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .pointArretDescente(stopTanger)
                .nombrePlaces(1)
                .montantTotal(new BigDecimal("120.00"))
                .statut(StatutReservation.ANNULEE)
                .dateReservation(referenceNow.minusDays(3))
                .build();

        when(passagerRepository.existsById(1L)).thenReturn(true);
        when(reservationRepository.findByPassagerIdOrderByDateReservationDesc(1L))
                .thenReturn(List.of(res1, res2));

        ReservationResponse resp1 = new ReservationResponse(100L, 1L, 10L, "Casablanca", "Rabat", 2, new BigDecimal("70.00"), StatutReservation.CONFIRMEE, referenceNow.minusDays(1));
        ReservationResponse resp2 = new ReservationResponse(101L, 1L, 10L, "Casablanca", "Tanger", 1, new BigDecimal("120.00"), StatutReservation.ANNULEE, referenceNow.minusDays(3));
        when(reservationMapper.toResponseList(List.of(res1, res2))).thenReturn(List.of(resp1, resp2));

        List<ReservationResponse> results = reservationService.findByPassager(1L, 1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).statut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(results.get(1).statut()).isEqualTo(StatutReservation.ANNULEE);

        verify(passagerRepository).existsById(1L);
        verify(reservationRepository).findByPassagerIdOrderByDateReservationDesc(1L);
    }

    @Test
    @DisplayName("F-11 : rejet si un passager tente de consulter les réservations d'un autre (403 / R-27)")
    void findByPassager_ThrowsForbidden_WhenActingIdDoesNotMatch() {
        // Passager 2L tente de consulter l'historique du passager 1L
        assertThatThrownBy(() -> reservationService.findByPassager(2L, 1L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Vous ne pouvez consulter que vos propres réservations");

        verify(reservationRepository, never()).findByPassagerIdOrderByDateReservationDesc(any());
    }

    @Test
    @DisplayName("F-11 : rejet si le passager est inconnu (404)")
    void findByPassager_ThrowsNotFound_WhenPassagerDoesNotExist() {
        when(passagerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.findByPassager(99L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Passager non trouvé avec l'id : 99");

        verify(reservationRepository, never()).findByPassagerIdOrderByDateReservationDesc(any());
    }

    // ==========================================
    // F-19 : Consultation des réservations d'un trajet (Conducteur)
    // ==========================================

    @Test
    @DisplayName("F-19 : consultation des réservations de son trajet avec succès (200 OK)")
    void findByTrajet_Success_Nominal() {
        Reservation res1 = Reservation.builder()
                .id(100L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .pointArretDescente(stopRabat)
                .nombrePlaces(2)
                .montantTotal(new BigDecimal("70.00"))
                .statut(StatutReservation.CONFIRMEE)
                .dateReservation(referenceNow.minusDays(1))
                .build();

        Reservation res2 = Reservation.builder()
                .id(101L)
                .passager(passager)
                .trajet(trajet)
                .pointArretMontee(stopCasa)
                .pointArretDescente(stopTanger)
                .nombrePlaces(1)
                .montantTotal(new BigDecimal("120.00"))
                .statut(StatutReservation.ANNULEE)
                .dateReservation(referenceNow.minusDays(3))
                .build();

        when(trajetRepository.findById(10L)).thenReturn(Optional.of(trajet));
        when(reservationRepository.findByTrajetIdOrderByDateReservationDesc(10L))
                .thenReturn(List.of(res1, res2));

        ReservationResponse resp1 = new ReservationResponse(100L, 1L, 10L, "Casablanca", "Rabat", 2, new BigDecimal("70.00"), StatutReservation.CONFIRMEE, referenceNow.minusDays(1));
        ReservationResponse resp2 = new ReservationResponse(101L, 1L, 10L, "Casablanca", "Tanger", 1, new BigDecimal("120.00"), StatutReservation.ANNULEE, referenceNow.minusDays(3));
        when(reservationMapper.toResponseList(List.of(res1, res2))).thenReturn(List.of(resp1, resp2));

        List<ReservationResponse> results = reservationService.findByTrajet(2L, 10L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).statut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(results.get(1).statut()).isEqualTo(StatutReservation.ANNULEE);

        verify(trajetRepository).findById(10L);
        verify(reservationRepository).findByTrajetIdOrderByDateReservationDesc(10L);
    }

    @Test
    @DisplayName("F-19 : rejet si un conducteur consulte les réservations d'un trajet d'un autre conducteur (403 / R-31)")
    void findByTrajet_ThrowsForbidden_WhenDriverNotOwner() {
        // Conducteur 99L tente de consulter les réservations du trajet appartenant au conducteur 2L
        when(trajetRepository.findById(10L)).thenReturn(Optional.of(trajet));

        assertThatThrownBy(() -> reservationService.findByTrajet(99L, 10L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Vous ne pouvez consulter que les réservations de vos propres trajets");

        verify(reservationRepository, never()).findByTrajetIdOrderByDateReservationDesc(any());
    }

    @Test
    @DisplayName("F-19 : rejet si le trajet est introuvable (404)")
    void findByTrajet_ThrowsNotFound_WhenTripDoesNotExist() {
        when(trajetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.findByTrajet(2L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Trajet non trouvé avec l'id : 999");

        verify(reservationRepository, never()).findByTrajetIdOrderByDateReservationDesc(any());
    }
}
