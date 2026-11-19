package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Conducteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConducteurRepository extends JpaRepository<Conducteur, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNumeroPermisIgnoreCase(String numeroPermis);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByNumeroPermisIgnoreCaseAndIdNot(String numeroPermis, Long id);

    List<Conducteur> findByEmailIgnoreCase(String email);

    @Query("SELECT c FROM Conducteur c WHERE " +
           "(:nom IS NULL OR LOWER(c.nom) LIKE :nom) AND " +
           "(:prenom IS NULL OR LOWER(c.prenom) LIKE :prenom) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE :email)")
    List<Conducteur> search(
            @Param("nom") String nom,
            @Param("prenom") String prenom,
            @Param("email") String email);
}
