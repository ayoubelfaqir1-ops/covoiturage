package ma.youcode.covoiturage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ma.youcode.covoiturage.entity.enums.StatutTrajet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trajet")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ville_depart", nullable = false)
    private String villeDepart;

    @Column(name = "ville_arrivee", nullable = false)
    private String villeArrivee;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Column(name = "places_proposees", nullable = false)
    private Integer placesProposees;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTrajet statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducteur_id", nullable = false)
    private Conducteur conducteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre ASC")
    @Builder.Default
    private List<PointArret> pointsArret = new ArrayList<>();

    @OneToMany(mappedBy = "trajet")
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();
    

    public void publier() {
        this.statut = StatutTrajet.PUBLIE;
    }

    public void marquerComplet() {
        if (this.statut != StatutTrajet.PUBLIE) {
            throw new IllegalStateException("Seul un trajet PUBLIE peut devenir COMPLET");
        }
        this.statut = StatutTrajet.COMPLET;
    }

    public void reouvrir() {
        if (this.statut == StatutTrajet.COMPLET) {
            this.statut = StatutTrajet.PUBLIE;
        }
    }

    public void terminer() {
        if (this.statut == StatutTrajet.TERMINE || this.statut == StatutTrajet.ANNULE) {
            throw new IllegalStateException("Un trajet déjà terminé ou annulé ne peut pas être terminé");
        }
        this.statut = StatutTrajet.TERMINE;
    }

    public void annuler() {
        if (this.statut == StatutTrajet.TERMINE || this.statut == StatutTrajet.ANNULE) {
            throw new IllegalStateException("Un trajet déjà terminé ou annulé ne peut pas être annulé");
        }
        this.statut = StatutTrajet.ANNULE;
    }

    public void addPointArret(PointArret pointArret) {
        pointsArret.add(pointArret);
        pointArret.setTrajet(this);
    }

    public void removePointArret(PointArret pointArret) {
        pointsArret.remove(pointArret);
        pointArret.setTrajet(null);
    }
}
