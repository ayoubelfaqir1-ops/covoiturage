package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.ConducteurMapper;
import ma.youcode.covoiturage.repository.ConducteurRepository;
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
class ConducteurServiceImplTest {

    @Mock
    private ConducteurRepository conducteurRepository;

    @Mock
    private ConducteurMapper conducteurMapper;

    @InjectMocks
    private ConducteurServiceImpl conducteurService;

    private ConducteurRequest request;
    private Conducteur entity;
    private ConducteurResponse response;

    @BeforeEach
    void setUp() {
        request = new ConducteurRequest(
                "El Amrani",
                "Karim",
                "karim@covoiturage.ma",
                "0612345678",
                "PERM-12345"
        );

        entity = Conducteur.builder()
                .id(1L)
                .nom("El Amrani")
                .prenom("Karim")
                .email("karim@covoiturage.ma")
                .telephone("0612345678")
                .numeroPermis("PERM-12345")
                .build();

        response = new ConducteurResponse(
                1L,
                "El Amrani",
                "Karim",
                "karim@covoiturage.ma",
                "0612345678",
                "PERM-12345"
        );
    }

    @Test
    void create_nominal_returnsResponse() {
        when(conducteurRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermisIgnoreCase(request.numeroPermis())).thenReturn(false);
        when(conducteurMapper.toEntity(request)).thenReturn(entity);
        when(conducteurRepository.save(entity)).thenReturn(entity);
        when(conducteurMapper.toResponse(entity)).thenReturn(response);

        ConducteurResponse result = conducteurService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("karim@covoiturage.ma");
        verify(conducteurRepository).save(entity);
    }

    @Test
    void create_emailAlreadyExists_throwsBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");

        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void create_numeroPermisAlreadyExists_throwsBusinessRuleException() {
        when(conducteurRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermisIgnoreCase(request.numeroPermis())).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permis");

        verify(conducteurRepository, never()).save(any());
    }

    @Test
    void findAll_returnsList() {
        when(conducteurRepository.findAll()).thenReturn(List.of(entity));
        when(conducteurMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<ConducteurResponse> results = conducteurService.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
    }

    @Test
    void findById_found_returnsResponse() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conducteurMapper.toResponse(entity)).thenReturn(response);

        ConducteurResponse result = conducteurService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(conducteurRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conducteurService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conducteur non trouvé");
    }

    @Test
    void update_nominal_returnsUpdatedResponse() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), 1L)).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermisIgnoreCaseAndIdNot(request.numeroPermis(), 1L)).thenReturn(false);
        when(conducteurMapper.toResponse(entity)).thenReturn(response);

        ConducteurResponse result = conducteurService.update(1L, 1L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void update_actorMismatch_throwsForbiddenOperationException() {
        assertThatThrownBy(() -> conducteurService.update(2L, 1L, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("pas autorisé à modifier le profil d'un autre conducteur");

        verify(conducteurRepository, never()).findById(any());
    }

    @Test
    void update_driverNotFound_throwsResourceNotFoundException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> conducteurService.update(1L, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_emailTakenByOtherDriver_throwsBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.update(1L, 1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");
    }

    @Test
    void update_permitTakenByOtherDriver_throwsBusinessRuleException() {
        when(conducteurRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(conducteurRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), 1L)).thenReturn(false);
        when(conducteurRepository.existsByNumeroPermisIgnoreCaseAndIdNot(request.numeroPermis(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> conducteurService.update(1L, 1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permis");
    }

    @Test
    void search_noParams_throwsInvalidRequestException() {
        assertThatThrownBy(() -> conducteurService.search(null, "   ", null))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Au moins un paramètre de recherche");
    }

    @Test
    void search_byNomAndPrenom_returnsResults() {
        when(conducteurRepository.search("%el alami%", "%karim%", null))
                .thenReturn(List.of(entity));
        when(conducteurMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<ConducteurResponse> results = conducteurService.search("El Alami", "Karim", null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
    }

    @Test
    void search_byEmail_returnsResults() {
        when(conducteurRepository.search(null, null, "%karim@covoiturage.ma%"))
                .thenReturn(List.of(entity));
        when(conducteurMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<ConducteurResponse> results = conducteurService.search(null, null, "karim@covoiturage.ma");

        assertThat(results).hasSize(1);
    }
}

