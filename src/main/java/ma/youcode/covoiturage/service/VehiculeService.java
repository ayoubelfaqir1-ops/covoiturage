package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;

import java.util.List;

public interface VehiculeService {

    VehiculeResponse create(Long actingDriverId, VehiculeRequest request);

    VehiculeResponse update(Long actingDriverId, Long id, VehiculeRequest request);

    VehiculeResponse findById(Long id);

    List<VehiculeResponse> findByDriver(Long driverId);
    
    void delete(Long driverId, Long id);
}
