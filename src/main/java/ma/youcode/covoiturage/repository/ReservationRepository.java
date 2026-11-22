package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Reservation;
import ma.youcode.covoiturage.entity.enums.StatutReservation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT COALESCE(SUM(r.nombrePlaces), 0)
        FROM Reservation r
        WHERE r.trajet.id = :trajetId
          AND r.statut = ma.youcode.covoiturage.entity.enums.StatutReservation.CONFIRMEE
    """)
    int sumPlacesConfirmedByTrajetId(@Param("trajetId") Long trajetId);

    Boolean existsByTrajetIdAndPassagerIdAndStatut(Long trajetId, Long passagerId, StatutReservation statut);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.pointArretMontee
        JOIN FETCH r.pointArretDescente
        WHERE r.passager.id = :passagerId
        ORDER BY r.dateReservation DESC
    """)
    List<Reservation> findByPassagerIdOrderByDateReservationDesc(@Param("passagerId") Long passagerId);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.passager
        JOIN FETCH r.pointArretMontee
        JOIN FETCH r.pointArretDescente
        WHERE r.trajet.id = :trajetId
        ORDER BY r.dateReservation DESC
    """)
    List<Reservation> findByTrajetIdOrderByDateReservationDesc(@Param("trajetId") Long trajetId);
}