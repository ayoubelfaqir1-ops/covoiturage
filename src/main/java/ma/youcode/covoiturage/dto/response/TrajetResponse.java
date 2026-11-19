package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.List;

public record TrajetResponse(
    Long id,
    String villeDepart,
    String villeArrivee,
    LocalDateTime dateDepart,
    Integer placesProposees,
    StatutTrajet statut,
    Long conducteurId,
    Long vehiculeId,
    List<PointArretResponse> pointsArret
) {}
