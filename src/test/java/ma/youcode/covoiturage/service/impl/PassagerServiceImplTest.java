package ma.youcode.covoiturage.service.impl;

import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.entity.Passager;
import ma.youcode.covoiturage.exception.BusinessRuleException;
import ma.youcode.covoiturage.exception.ForbiddenOperationException;
import ma.youcode.covoiturage.exception.InvalidRequestException;
import ma.youcode.covoiturage.exception.ResourceNotFoundException;
import ma.youcode.covoiturage.mapper.PassagerMapper;
import ma.youcode.covoiturage.repository.PassagerRepository;
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
class PassagerServiceImplTest {

    @Mock
    private PassagerRepository passagerRepository;

    @Mock
    private PassagerMapper passagerMapper;

    @InjectMocks
    private PassagerServiceImpl passagerService;

    private PassagerRequest request;
    private Passager entity;
    private PassagerResponse response;

    @BeforeEach
    void setUp() {
        request = new PassagerRequest(
                "Benali",
                "Sara",
                "sara.benali@example.com",
                "+212612345678"
        );

        entity = Passager.builder()
                .id(1L)
                .nom("Benali")
                .prenom("Sara")
                .email("sara.benali@example.com")
                .telephone("+212612345678")
                .build();

        response = new PassagerResponse(
                1L,
                "Benali",
                "Sara",
                "sara.benali@example.com",
                "+212612345678"
        );
    }

    @Test
    void create_nominal_returnsResponse() {
        when(passagerRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(passagerMapper.toEntity(request)).thenReturn(entity);
        when(passagerRepository.save(entity)).thenReturn(entity);
        when(passagerMapper.toResponse(entity)).thenReturn(response);

        PassagerResponse result = passagerService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("sara.benali@example.com");
        verify(passagerRepository).save(entity);
    }

    @Test
    void create_emailAlreadyExists_throwsBusinessRuleException() {
        when(passagerRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> passagerService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");

        verify(passagerRepository, never()).save(any());
    }

    @Test
    void findById_found_returnsResponse() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(passagerMapper.toResponse(entity)).thenReturn(response);

        PassagerResponse result = passagerService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(passagerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passagerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Passager non trouvé");
    }

    @Test
    void update_nominal_returnsUpdatedResponse() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(passagerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), 1L)).thenReturn(false);
        when(passagerMapper.toResponse(entity)).thenReturn(response);

        PassagerResponse result = passagerService.update(1L, 1L, request);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void update_actorMismatch_throwsForbiddenOperationException() {
        assertThatThrownBy(() -> passagerService.update(2L, 1L, request))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("autorisé à modifier le profil d'un autre passager");

        verify(passagerRepository, never()).findById(any());
    }

    @Test
    void update_passengerNotFound_throwsResourceNotFoundException() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passagerService.update(1L, 1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_emailTakenByOtherPassenger_throwsBusinessRuleException() {
        when(passagerRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(passagerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> passagerService.update(1L, 1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("email");
    }

    @Test
    void findAll_returnsList() {
        when(passagerRepository.findAll()).thenReturn(List.of(entity));
        when(passagerMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<PassagerResponse> results = passagerService.findAll();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(1L);
    }

    @Test
    void searchByEmail_nominal_returnsResults() {
        when(passagerRepository.findByEmailContainingIgnoreCase("sara"))
                .thenReturn(List.of(entity));
        when(passagerMapper.toResponseList(List.of(entity))).thenReturn(List.of(response));

        List<PassagerResponse> results = passagerService.searchByEmail("sara");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).email()).isEqualTo("sara.benali@example.com");
    }

    @Test
    void searchByEmail_nullOrBlank_throwsInvalidRequestException() {
        assertThatThrownBy(() -> passagerService.searchByEmail("   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("email");
    }
}
