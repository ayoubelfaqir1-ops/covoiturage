package ma.youcode.covoiturage.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
    @NotNull(message = "L'identifiant du trajet est obligatoire")
    Long tripId,

    @NotBlank(message = "La ville de montée est obligatoire")
    String villeMontee,

    @NotBlank(message = "La ville de descente est obligatoire")
    String villeDescente,

    @NotNull(message = "Le nombre de places est obligatoire")
    @Min(value = 1, message = "Le nombre de places doit être au moins de 1")
    Integer nombrePlaces
) {}
