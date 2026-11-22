package ma.youcode.covoiturage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.service.ReservationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints de gestion des réservations (F-21 à F-24)")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "F-21 Réserver des places")
    public ReservationResponse book(
            @RequestHeader("X-Passenger-Id") Long passengerId,
            @Valid @RequestBody ReservationRequest request) {
        return reservationService.book(passengerId, request);
    }

    @PatchMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "F-22 Annuler sa réservation")
    public void cancel(
            @RequestHeader("X-Passenger-Id") Long passengerId,
            @PathVariable Long id) {
        reservationService.cancel(passengerId, id);
    }
}
