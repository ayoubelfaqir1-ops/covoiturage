package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Trajet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
