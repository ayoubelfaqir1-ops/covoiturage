package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.List;

public record TrajetDetailResponse(
    Long id,
    String villeDepart,
    String villeArrivee,
    LocalDateTime dateDepart,
    Integer placesProposees,
    Integer placesRestantes,
    StatutTrajet statut,
    ConducteurResponse conducteur,
    VehiculeResponse vehicule,
    List<PointArretResponse> pointsArret
) {}
