package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.service.VehiculeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Véhicules", description = "Endpoints de gestion des véhicules (F-12 à F-15)")
public class VehiculeController {

    private final VehiculeService vehiculeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "F-12 Ajouter un véhicule pour un conducteur")
    public VehiculeResponse create(
            @RequestHeader("X-Driver-Id") Long actingDriverId,
            @Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.create(actingDriverId, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "F-13 Modifier un véhicule")
    public VehiculeResponse update(
            @RequestHeader("X-Driver-Id") Long actingDriverId,
            @PathVariable Long id,
            @Valid @RequestBody VehiculeRequest request) {
        return vehiculeService.update(actingDriverId, id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "F-14 Consulter un véhicule par son id")
    public VehiculeResponse findById(@PathVariable Long id) {
        return vehiculeService.findById(id);
    }

    @GetMapping
    @Operation(summary = "F-15 Lister les véhicules par conducteur")
    public List<VehiculeResponse> findByDriver(
            @RequestHeader(value = "X-Driver-Id", required = false) Long actingDriverId,
            @RequestParam(value = "driverId", required = false) Long driverId) {
        Long targetDriverId = (actingDriverId != null) ? actingDriverId : driverId;
        return vehiculeService.findByDriver(targetDriverId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "F-16 Supprimer un véhicule")
    public void delete(
            @RequestHeader("X-Driver-Id") Long actingDriverId,
            @PathVariable Long id) {
        vehiculeService.delete(actingDriverId, id);
    }
}
