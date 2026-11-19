package ma.youcode.covoiturage.service;

import ma.youcode.covoiturage.dto.request.TrajetPublicationRequest;
import ma.youcode.covoiturage.dto.response.TrajetResponse;

public interface TrajetService {

    TrajetResponse publierTrajet(Long driverId, TrajetPublicationRequest request);
}
