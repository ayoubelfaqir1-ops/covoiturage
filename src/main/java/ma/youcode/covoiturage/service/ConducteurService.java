package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;

import java.util.List;

public interface ConducteurService {

    ConducteurResponse create(ConducteurRequest request);

    List<ConducteurResponse> findAll();

    ConducteurResponse findById(Long id);

    ConducteurResponse update(Long actingId, Long id, ConducteurRequest request);

    List<ConducteurResponse> search(String nom, String prenom, String email);
}
