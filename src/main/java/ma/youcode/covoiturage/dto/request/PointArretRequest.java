package ma.youcode.covoiturage.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PointArretRequest(
    @NotBlank(message = "La ville de l'arrêt est obligatoire")
    String ville,

    @NotNull(message = "L'heure de passage est obligatoire")
    LocalDateTime heurePassage,

    @NotNull(message = "Le prix depuis le départ est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix depuis le départ ne peut pas être négatif")
    BigDecimal prixDepuisDepart
) {}
