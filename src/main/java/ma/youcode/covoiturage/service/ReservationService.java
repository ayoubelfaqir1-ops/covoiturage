package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.ReservationRequest;
import ma.youcode.covoiturage.dto.response.ReservationResponse;

public interface ReservationService {

    ReservationResponse book(Long passengerId, ReservationRequest request);

    void cancel(Long passengerId, Long reservationId);
}
