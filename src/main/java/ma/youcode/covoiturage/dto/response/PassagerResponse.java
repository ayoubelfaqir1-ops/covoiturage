package ma.youcode.covoiturage.dto.response;

public record PassagerResponse(
    Long id,
    String nom,
    String prenom,
    String email,
    String telephone
) {}
