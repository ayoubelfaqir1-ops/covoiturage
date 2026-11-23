package ma.youcode.covoiturage.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.PageResponse;
import ma.youcode.covoiturage.dto.response.TrajetDetailResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.dto.response.TrajetSearchResult;
import org.springframework.data.domain.Pageable;

public interface TrajetService {

    TrajetResponse publierTrajet(Long driverId, TrajetPublicationRequest request);

    TrajetDetailResponse getTrajetDetail(Long trajetId);

    PageResponse<TrajetSearchResult> searchTrajets(
            String departureCity,
            String arrivalCity,
            LocalDate date,
            Integer seats,
            BigDecimal maxPrice,
            Pageable pageable
    );

    TrajetDetailResponse finish(Long driverId, Long tripId);

    void cancel(Long driverId, Long tripId);
}
