package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse book(Long passengerId, ReservationRequest request);

    void cancel(Long passengerId, Long reservationId);

    List<ReservationResponse> findByPassager(Long actingId, Long passagerId);

    List<ReservationResponse> findByTrajet(Long driverId, Long tripId);
}
