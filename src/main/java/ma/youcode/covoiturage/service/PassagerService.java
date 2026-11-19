package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;

import java.util.List;

public interface PassagerService {

    PassagerResponse create(PassagerRequest request);

    PassagerResponse findById(Long id);

    PassagerResponse update(Long actingId, Long id, PassagerRequest request);

    List<PassagerResponse> findAll();

    List<PassagerResponse> searchByEmail(String email);
}
