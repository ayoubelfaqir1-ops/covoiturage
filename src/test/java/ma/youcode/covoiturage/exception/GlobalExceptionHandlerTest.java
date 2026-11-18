package ma.youcode.covoiturage.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class TestController {
        @GetMapping("/test/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Conducteur non trouvé avec l'id : 99");
        }

        @GetMapping("/test/conflict")
        public void throwConflict() {
            throw new BusinessRuleException("Cet email est déjà utilisé");
        }

        @GetMapping("/test/forbidden")
        public void throwForbidden() {
            throw new ForbiddenOperationException("Vous ne pouvez pas modifier le profil d'un autre utilisateur");
        }

        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new InvalidRequestException("Paramètres de requête invalides");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturn404ProblemDetail_whenResourceNotFoundExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Ressource non trouvée"))
                .andExpect(jsonPath("$.detail").value("Conducteur non trouvé avec l'id : 99"))
                .andExpect(jsonPath("$.instance").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn409ProblemDetail_whenBusinessRuleExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Règle métier non respectée"))
                .andExpect(jsonPath("$.detail").value("Cet email est déjà utilisé"))
                .andExpect(jsonPath("$.instance").value("/test/conflict"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn403ProblemDetail_whenForbiddenOperationExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Opération interdite"))
                .andExpect(jsonPath("$.detail").value("Vous ne pouvez pas modifier le profil d'un autre utilisateur"))
                .andExpect(jsonPath("$.instance").value("/test/forbidden"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn400ProblemDetail_whenInvalidRequestExceptionThrown() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Requête invalide"))
                .andExpect(jsonPath("$.detail").value("Paramètres de requête invalides"))
                .andExpect(jsonPath("$.instance").value("/test/bad-request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
