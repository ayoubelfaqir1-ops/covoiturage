package ma.youcode.covoiturage.dto.response;

import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrajetSearchResult(
    Long trajetId,
    String conducteurNom,
    String conducteurPrenom,
    String villeDepart,
    LocalDateTime heureDepart,
    String villeArrivee,
    LocalDateTime heureArrivee,
    BigDecimal prixSegment,
    Long placesRestantes,
    StatutTrajet statut
) {}
