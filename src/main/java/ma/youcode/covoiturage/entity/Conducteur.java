package ma.youcode.covoiturage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conducteur")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Conducteur extends Utilisateur {

    @Column(name = "numero_permis", nullable = false, unique = true)
    private String numeroPermis;

    @OneToMany(mappedBy = "conducteur")
    @Builder.Default
    private List<Vehicule> vehicules = new ArrayList<>();

    @OneToMany(mappedBy = "conducteur")
    @Builder.Default
    private List<Trajet> trajets = new ArrayList<>();

    public void updateProfile(String nom, String prenom, String email, String telephone, String numeroPermis) {
        updateBaseProfile(nom, prenom, email, telephone);
        this.numeroPermis = numeroPermis;
    }
}
