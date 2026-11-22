package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.response.ReservationResponse;
import ma.youcode.covoiturage.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReservationMapper {

    @Mapping(source = "passager.id", target = "passagerId")
    @Mapping(source = "trajet.id", target = "tripId")
    @Mapping(source = "pointArretMontee.ville", target = "villeMontee")
    @Mapping(source = "pointArretDescente.ville", target = "villeDescente")
    ReservationResponse toResponse(Reservation entity);

    List<ReservationResponse> toResponseList(List<Reservation> entities);
}
