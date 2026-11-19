package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.entity.Vehicule;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
import ma.youcode.covoiturage.repository.VehiculeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculeServiceImplTest {

    @Mock
    private VehiculeRepository vehiculeRepository;

    @Mock
    private ConducteurRepository conducteurRepository;

    @Mock
    private VehiculeMapper vehiculeMapper;

    @InjectMocks
    private VehiculeServiceImpl vehiculeService;

    private Conducteur conducteur;
    private VehiculeRequest request;
    private Vehicule entity;
    private VehiculeResponse response;

    @BeforeEach
    void setUp() {
        conducteur = Conducteur.builder()
                .id(1L)
                .nom("El Alami")
                .prenom("Karim")
                .email("karim@covoiturage.ma")
                .numeroPermis("PERM-12345")
                .build();

        request = new VehiculeRequest(
                "Dacia",
                "Logan",
                "12345-A-1",
                "Blanc",
                4
        );

        entity = Vehicule.builder()
                .id(10L)
                .marque("Dacia")
                .modele("Logan")
                .immatriculation("12345-A-1")
                .couleur("Blanc")
                .capacite(4)
                .conducteur(conducteur)
                .build();

        response = new VehiculeResponse(
                10L,
                "Dacia",
                "Logan",
                "12345-A-1",
                "Blanc",
                4,
                1L
        );
    }

    @Test
    void create_nominal_returnsResponse() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.existsByImmatriculationIgnoreCase(request.immatriculation())).thenReturn(false);
        when(vehiculeMapper.toEntity(request)).thenReturn(entity);
        when(vehiculeRepository.save(entity)).thenReturn(entity);
        when(vehiculeMapper.toResponse(entity)).thenReturn(response);

        VehiculeResponse result = vehiculeService.create(1L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.immatriculation()).isEqualTo("12345-A-1");
        verify(vehiculeRepository).save(entity);
    }

    @Test
    void create_driverNotFound_throwsResourceNotFoundException() {
        when(conducteurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.create(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conducteur non trouvé");

        verify(vehiculeRepository, never()).save(any());
    }

    @Test
    void create_immatriculationAlreadyExists_throwsBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));
        when(vehiculeRepository.existsByImmatriculationIgnoreCase(request.immatriculation())).thenReturn(true);

        assertThatThrownBy(() -> vehiculeService.create(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("immatriculation");

        verify(vehiculeRepository, never()).save(any());
    }

    @Test
    void create_capaciteTooLow_throwsInvalidRequestException() {
        VehiculeRequest badRequest = new VehiculeRequest("Dacia", "Logan", "12345-A-1", "Blanc", 0);
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));

        assertThatThrownBy(() -> vehiculeService.create(1L, badRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("capacité");
    }

    @Test
    void create_capaciteTooHigh_throwsInvalidRequestException() {
        VehiculeRequest badRequest = new VehiculeRequest("Dacia", "Logan", "12345-A-1", "Blanc", 9);
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(conducteur));

        assertThatThrownBy(() -> vehiculeService.create(1L, badRequest))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("capacité");
    }

    @Test
    void update_nominal_returnsUpdatedResponse() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(request.immatriculation(), 10L)).thenReturn(false);
        when(vehiculeMapper.toResponse(entity)).thenReturn(response);

        VehiculeResponse result = vehiculeService.update(1L, 10L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    void update_vehicleNotFound_throwsResourceNotFoundException() {
        when(vehiculeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.update(1L, 99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_driverMismatch_throwsForbiddenOperationException() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> vehiculeService.update(2L, 10L, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("autorisé à modifier le véhicule d'un autre conducteur");
    }

    @Test
    void update_immatriculationAlreadyExists_throwsBusinessRuleException() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(vehiculeRepository.existsByImmatriculationIgnoreCaseAndIdNot(request.immatriculation(), 10L)).thenReturn(true);

        assertThatThrownBy(() -> vehiculeService.update(1L, 10L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("immatriculation");
    }

    @Test
    void findById_found_returnsResponse() {
        when(vehiculeRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(vehiculeMapper.toResponse(entity)).thenReturn(response);

        VehiculeResponse result = vehiculeService.findById(10L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(vehiculeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehiculeService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("non trouvé");
    }

    @Test
    void findByDriver_nominal_returnsList() {
        when(conducteurRepository.existsById(1L)).thenReturn(true);
        when(vehiculeRepository.findByConducteurId(1L)).thenReturn(List.of(entity));
        when(vehiculeMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<VehiculeResponse> results = vehiculeService.findByDriver(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(10L);
    }

    @Test
    void findByDriver_nullDriverId_throwsInvalidRequestException() {
        assertThatThrownBy(() -> vehiculeService.findByDriver(null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("conducteur");
    }

    @Test
    void findByDriver_driverNotFound_throwsResourceNotFoundException() {
        when(conducteurRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> vehiculeService.findByDriver(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conducteur non trouvé");
    }
}
