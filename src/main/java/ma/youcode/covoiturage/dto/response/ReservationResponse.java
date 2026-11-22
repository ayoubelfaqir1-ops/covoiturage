package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutReservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
    Long id,
    Long passagerId,
    Long tripId,
    String villeMontee,
    String villeDescente,
    Integer nombrePlaces,
    BigDecimal montantTotal,
    StatutReservation statut,
    LocalDateTime dateReservation
) {}
