package ma.youcode.covoiturage.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConducteurRequest(
    @NotBlank(message = "Le nom est obligatoire")
    String nom,

    @NotBlank(message = "Le prénom est obligatoire")
    String prenom,

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    String email,

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^(\\+212|0)[5-7][0-9]{8}$", message = "Format de téléphone marocain invalide (ex: 0612345678 ou +212612345678)")
    String telephone,

    @NotBlank(message = "Le numéro de permis est obligatoire")
    String numeroPermis
) {}
