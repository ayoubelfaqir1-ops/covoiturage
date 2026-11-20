package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.TrajetDetailResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.service.TrajetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "Trajets", description = "Endpoints de gestion des trajets (F-17 à F-21)")
public class TrajetController {

    private final TrajetService trajetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "F-17 Publier un trajet")
    public TrajetResponse publierTrajet(
            @RequestHeader("X-Driver-Id") Long driverId,
            @Valid @RequestBody TrajetPublicationRequest request) {
        return trajetService.publierTrajet(driverId, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "F-18 Consulter le détail d'un trajet")
    public TrajetDetailResponse getTrajetDetail(@PathVariable Long id) {
        return trajetService.getTrajetDetail(id);
    }
}
