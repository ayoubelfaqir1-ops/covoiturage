package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.service.PassagerService;
import ma.youcode.covoiturage.service.ReservationService;
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
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Tag(name = "Passagers", description = "Endpoints de gestion des passagers (F-06 à F-10)")
public class PassagerController {

    private final PassagerService passagerService;
    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "F-06 Créer un passager")
    public PassagerResponse create(@Valid @RequestBody PassagerRequest request) {
        return passagerService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "F-07 Consulter un passager par son id")
    public PassagerResponse findById(@PathVariable Long id) {
        return passagerService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "F-08 Modifier le profil d'un passager")
    public PassagerResponse update(
            @RequestHeader("X-Passenger-Id") Long actingId,
            @PathVariable Long id,
            @Valid @RequestBody PassagerRequest request) {
        return passagerService.update(actingId, id, request);
    }

    @GetMapping
    @Operation(summary = "F-09 Lister tous les passagers")
    public List<PassagerResponse> findAll() {
        return passagerService.findAll();
    }

    @GetMapping("/search")
    @Operation(summary = "F-10 Rechercher des passagers par email")
    public List<PassagerResponse> search(@RequestParam(required = false) String email) {
        return passagerService.searchByEmail(email);
    }

    @GetMapping("/{id}/reservations")
    @Operation(summary = "F-11 Historique des réservations d'un passager")
    public List<ReservationResponse> findReservations(
            @RequestHeader("X-Passenger-Id") Long actingId,
            @PathVariable Long id) {
        return reservationService.findByPassager(actingId, id);
    }
}
