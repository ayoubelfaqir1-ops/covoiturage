package ma.youcode.covoiturage.entity;

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
@Table(name = "passager")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Passager extends Utilisateur {

    @OneToMany(mappedBy = "passager")
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    public void updateProfile(String nom, String prenom, String email, String telephone) {
        updateBaseProfile(nom, prenom, email, telephone);
    }
}
