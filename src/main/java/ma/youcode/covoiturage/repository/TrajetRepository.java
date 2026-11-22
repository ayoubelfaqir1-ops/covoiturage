package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.dto.response.TrajetSearchResult;
import ma.youcode.covoiturage.entity.Trajet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    @Query("""
            SELECT t FROM Trajet t
            JOIN FETCH t.conducteur c
            JOIN FETCH t.vehicule v
            LEFT JOIN FETCH t.pointsArret p
            WHERE t.id = :id
            """)
    Optional<Trajet> findDetailById(@Param("id") Long id);

    boolean existsByVehiculeId(Long vehiculeId);

    @Query(
        value = """
            SELECT new ma.youcode.covoiturage.dto.response.TrajetSearchResult(
                t.id,
                c.nom,
                c.prenom,
                pm.ville,
                pm.heurePassage,
                pd.ville,
                pd.heurePassage,
                (pd.prixDepuisDepart - pm.prixDepuisDepart),
                (CAST(t.placesProposees AS long) - COALESCE(
                    (SELECT SUM(r.nombrePlaces)
                     FROM Reservation r
                     WHERE r.trajet.id = t.id
                       AND r.statut = ma.youcode.covoiturage.entity.enums.StatutReservation.CONFIRMEE), 0L)
                ),
                t.statut
            )
            FROM Trajet t
            JOIN t.conducteur c
            JOIN t.pointsArret pm
            JOIN t.pointsArret pd
            WHERE t.statut = ma.youcode.covoiturage.entity.enums.StatutTrajet.PUBLIE
              AND LOWER(pm.ville) = LOWER(:departureCity)
              AND LOWER(pd.ville) = LOWER(:arrivalCity)
              AND pm.ordre < pd.ordre
              AND pm.heurePassage >= :startOfDay AND pm.heurePassage < :endOfDay
              AND (CAST(t.placesProposees AS long) - COALESCE(
                    (SELECT SUM(r.nombrePlaces)
                     FROM Reservation r
                     WHERE r.trajet.id = t.id
                       AND r.statut = ma.youcode.covoiturage.entity.enums.StatutReservation.CONFIRMEE), 0L)
                  ) >= :seats
              AND (:maxPrice IS NULL OR (pd.prixDepuisDepart - pm.prixDepuisDepart) <= :maxPrice)
            ORDER BY pm.heurePassage ASC
        """,
        countQuery = """
            SELECT COUNT(t)
            FROM Trajet t
            JOIN t.pointsArret pm
            JOIN t.pointsArret pd
            WHERE t.statut = ma.youcode.covoiturage.entity.enums.StatutTrajet.PUBLIE
              AND LOWER(pm.ville) = LOWER(:departureCity)
              AND LOWER(pd.ville) = LOWER(:arrivalCity)
              AND pm.ordre < pd.ordre
              AND pm.heurePassage >= :startOfDay AND pm.heurePassage < :endOfDay
              AND (CAST(t.placesProposees AS long) - COALESCE(
                    (SELECT SUM(r.nombrePlaces)
                     FROM Reservation r
                     WHERE r.trajet.id = t.id
                       AND r.statut = ma.youcode.covoiturage.entity.enums.StatutReservation.CONFIRMEE), 0L)
                  ) >= :seats
              AND (:maxPrice IS NULL OR (pd.prixDepuisDepart - pm.prixDepuisDepart) <= :maxPrice)
        """
    )
    Page<TrajetSearchResult> search(
            @Param("departureCity") String departureCity,
            @Param("arrivalCity") String arrivalCity,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            @Param("seats") int seats,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}
