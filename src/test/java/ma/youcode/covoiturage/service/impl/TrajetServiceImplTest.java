package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.PointArretRequest;
import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.PointArretResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.Conducteur;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrajetServiceImplTest {

    @Mock
    private TrajetRepository trajetRepository;

    @Mock
    private ConducteurRepository conducteurRepository;

    @Mock
    private VehiculeRepository vehiculeRepository;

    @Mock
    private TrajetMapper trajetMapper;

    private TrajetServiceImpl trajetService;

    private Clock fixedClock;
    private Conducteur conducteur;
    private Vehicule vehicule;
    private TrajetPublicationRequest validRequest;
    private TrajetResponse expectedResponse;

    // Horloge figée au 2026-11-20 à 10:00:00 UTC pour des tests temporels 100% déterministes
    private final LocalDateTime referenceNow = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(referenceNow.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        trajetService = new TrajetServiceImpl(
                trajetRepository,
                conducteurRepository,
                vehiculeRepository,
                trajetMapper,
                fixedClock
        );

        conducteur = Conducteur.builder()
                .id(1L)
                .nom("El Alami")
                .prenom("Karim")
                .email("karim@covoiturage.ma")
                .numeroPermis("PERM-12345")
                .build();

        vehicule = Vehicule.builder()
                .id(10L)
                .marque("Dacia")
                .modele("Logan")
                .immatriculation("12345-A-1")
                .couleur("Blanc")
                .capacite(4)
                .conducteur(conducteur)
                .build();

        List<PointArretRequest> stops = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), BigDecimal.ZERO),
                new PointArretRequest("Rabat", referenceNow.plusHours(3), new BigDecimal("35.00")),
                new PointArretRequest("Tanger", referenceNow.plusHours(6), new BigDecimal("120.00"))
        );

        validRequest = new TrajetPublicationRequest(10L, 3, stops);

        List<PointArretResponse> stopResponses = List.of(
                new PointArretResponse(100L, "Casablanca", 0, referenceNow.plusHours(2), BigDecimal.ZERO),
                new PointArretResponse(101L, "Rabat", 1, referenceNow.plusHours(3), new BigDecimal("35.00")),
                new PointArretResponse(102L, "Tanger", 2, referenceNow.plusHours(6), new BigDecimal("120.00"))
        );

        expectedResponse = new TrajetResponse(
                1L,
                "Casablanca",
                "Tanger",
                referenceNow.plusHours(2),
                3,
                StatutTrajet.PUBLIE,
                1L,
                10L,
                stopResponses
        );
    }

    @Test
    @DisplayName("Cas nominal (S-08) : publication réussie d'un trajet avec arrêts ordonnés")
    void publierTrajet_Success() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));
        when(trajetRepository.save(any(Trajet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(trajetMapper.toResponse(any(Trajet.class))).thenReturn(expectedResponse);

        TrajetResponse result = trajetService.publierTrajet(1L, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.villeDepart()).isEqualTo("Casablanca");
        assertThat(result.villeArrivee()).isEqualTo("Tanger");
        assertThat(result.placesProposees()).isEqualTo(3);
        assertThat(result.statut()).isEqualTo(StatutTrajet.PUBLIE);

        ArgumentCaptor<Trajet> captor = ArgumentCaptor.forClass(Trajet.class);
        verify(trajetRepository).save(captor.capture());
        Trajet saved = captor.getValue();

        assertThat(saved.getStatut()).isEqualTo(StatutTrajet.PUBLIE);
        assertThat(saved.getVilleDepart()).isEqualTo("Casablanca");
        assertThat(saved.getVilleArrivee()).isEqualTo("Tanger");
        assertThat(saved.getPointsArret()).hasSize(3);
        assertThat(saved.getPointsArret().get(0).getOrdre()).isEqualTo(0);
        assertThat(saved.getPointsArret().get(1).getOrdre()).isEqualTo(1);
        assertThat(saved.getPointsArret().get(2).getOrdre()).isEqualTo(2);
    }

    @Test
    @DisplayName("R-07 : rejet si le conducteur connecté n'existe pas")
    void publierTrajet_ThrowsResourceNotFound_WhenDriverNotFound() {
        when(conducteurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.publierTrajet(99L, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conducteur non trouvé avec l'id : 99");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-07 : rejet si le véhicule n'existe pas")
    void publierTrajet_ThrowsResourceNotFound_WhenVehicleNotFound() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Véhicule non trouvé avec l'id : 10");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-36 : rejet si le véhicule appartient à un autre conducteur")
    void publierTrajet_ThrowsForbiddenOperation_WhenVehicleBelongsToOtherDriver() {
        Conducteur autreConducteur = Conducteur.builder().id(2L).build();
        Vehicule vehiculeAutre = Vehicule.builder().id(10L).capacite(4).conducteur(autreConducteur).build();

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehiculeAutre));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, validRequest))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Vous n'êtes pas autorisé à publier un trajet avec le véhicule d'un autre conducteur");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-09 : rejet si les places proposées sont inférieures à 1")
    void publierTrajet_ThrowsInvalidRequest_WhenPlacesLessThanOne() {
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 0, validRequest.stops());

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Le nombre de places proposées doit être compris entre 1 et la capacité du véhicule");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-09 : rejet si les places proposées dépassent la capacité du véhicule")
    void publierTrajet_ThrowsInvalidRequest_WhenPlacesExceedCapacity() {
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 5, validRequest.stops());

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Le nombre de places proposées doit être compris entre 1 et la capacité du véhicule (4)");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-10 : rejet si moins de 2 points d'arrêt sont fournis")
    void publierTrajet_ThrowsInvalidRequest_WhenLessThanTwoStops() {
        List<PointArretRequest> oneStop = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), BigDecimal.ZERO)
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, oneStop);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Un trajet doit comporter au moins 2 points d'arrêt");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-10 : rejet en cas de ville dupliquée dans le trajet (insensible à la casse)")
    void publierTrajet_ThrowsInvalidRequest_WhenDuplicateCity() {
        List<PointArretRequest> duplicateStops = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), BigDecimal.ZERO),
                new PointArretRequest("Rabat", referenceNow.plusHours(3), new BigDecimal("35.00")),
                new PointArretRequest("casablanca", referenceNow.plusHours(5), new BigDecimal("70.00"))
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, duplicateStops);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Une ville ne peut pas être répétée dans un même trajet");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-11 : rejet si la date du premier arrêt est dans le passé ou présente")
    void publierTrajet_ThrowsInvalidRequest_WhenFirstStopInPast() {
        List<PointArretRequest> pastStops = List.of(
                new PointArretRequest("Casablanca", referenceNow.minusHours(1), BigDecimal.ZERO),
                new PointArretRequest("Rabat", referenceNow.plusHours(2), new BigDecimal("35.00"))
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, pastStops);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("La date de départ du premier arrêt doit être strictement dans le futur");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-11 : rejet si les heures de passage ne sont pas strictement chronologiques")
    void publierTrajet_ThrowsInvalidRequest_WhenStopsNotChronological() {
        List<PointArretRequest> nonChronologicalStops = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), BigDecimal.ZERO),
                new PointArretRequest("Rabat", referenceNow.plusHours(1), new BigDecimal("35.00")) // Arrivée avant le départ !
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, nonChronologicalStops);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Les heures de passage doivent être strictement chronologiques");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-12 : rejet si le premier arrêt a un prix non nul")
    void publierTrajet_ThrowsInvalidRequest_WhenFirstStopPriceNotZero() {
        List<PointArretRequest> invalidPriceStops = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), new BigDecimal("10.00")),
                new PointArretRequest("Rabat", referenceNow.plusHours(3), new BigDecimal("35.00"))
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, invalidPriceStops);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Le prix depuis le départ pour le premier arrêt doit être égal à 0");

        verify(trajetRepository, never()).save(any());
    }

    @Test
    @DisplayName("R-12 : rejet si les prix depuis le départ ne sont pas strictement croissants")
    void publierTrajet_ThrowsInvalidRequest_WhenPricesNotStrictlyIncreasing() {
        List<PointArretRequest> decreasingPriceStops = List.of(
                new PointArretRequest("Casablanca", referenceNow.plusHours(2), BigDecimal.ZERO),
                new PointArretRequest("Rabat", referenceNow.plusHours(3), new BigDecimal("50.00")),
                new PointArretRequest("Tanger", referenceNow.plusHours(5), new BigDecimal("50.00")) // Même prix que l'arrêt précédent !
        );
        TrajetPublicationRequest request = new TrajetPublicationRequest(10L, 3, decreasingPriceStops);

        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(vehicule));

        assertThatThrownBy(() -> trajetService.publierTrajet(1L, request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Les prix depuis le départ doivent être strictement croissants");

        verify(trajetRepository, never()).save(any());
    }
}
