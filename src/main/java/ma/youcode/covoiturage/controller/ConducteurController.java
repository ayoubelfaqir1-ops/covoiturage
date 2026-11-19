package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.service.ConducteurService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Conducteurs", description = "Endpoints de gestion des conducteurs (F-01 à F-05)")
public class ConducteurController {

    private final ConducteurService conducteurService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "F-01 Créer un conducteur")
    public ConducteurResponse create(@Valid @RequestBody ConducteurRequest request) {
        return conducteurService.create(request);
    }

    @GetMapping
    @Operation(summary = "F-02 Lister les conducteurs")
    public List<ConducteurResponse> findAll() {
        return conducteurService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "F-03 Consulter un conducteur par son id")
    public ConducteurResponse findById(@PathVariable Long id) {
        return conducteurService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "F-04 Modifier le profil d'un conducteur")
    public ConducteurResponse update(
            @RequestHeader("X-Driver-Id") Long actingId,
            @PathVariable Long id,
            @Valid @RequestBody ConducteurRequest request) {
        return conducteurService.update(actingId, id, request);
    }

    @GetMapping("/search")
    @Operation(summary = "F-05 Rechercher des conducteurs par nom, prénom ou email")
    public List<ConducteurResponse> search(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) String prenom,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name) {
        String effectiveNom = (nom != null && !nom.isBlank()) ? nom : name;
        return conducteurService.search(effectiveNom, prenom, email);
    }
}
