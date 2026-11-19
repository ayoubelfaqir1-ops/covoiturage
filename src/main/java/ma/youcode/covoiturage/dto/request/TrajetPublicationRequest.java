package ma.youcode.covoiturage.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrajetPublicationRequest(
    @NotNull(message = "L'identifiant du véhicule est obligatoire")
    Long vehicleId,

    @NotNull(message = "Le nombre de places proposées est obligatoire")
    @Min(value = 1, message = "Le nombre de places proposées doit être d'au moins 1")
    Integer placesProposees,

    @NotEmpty(message = "La liste des arrêts ne peut pas être vide")
    @Size(min = 2, message = "Le parcours doit comporter au moins 2 points d'arrêt (départ et arrivée)")
    List<@Valid PointArretRequest> stops
) {}
