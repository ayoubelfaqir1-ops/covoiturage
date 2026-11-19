package ma.youcode.covoiturage.repository;

import ma.youcode.covoiturage.entity.Passager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassagerRepository extends JpaRepository<Passager, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    List<Passager> findByEmailContainingIgnoreCase(String email);
}
