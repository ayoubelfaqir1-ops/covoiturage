package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.response.PointArretResponse;
import ma.youcode.covoiturage.dto.response.TrajetDetailResponse;
import ma.youcode.covoiturage.dto.response.TrajetResponse;
import ma.youcode.covoiturage.entity.PointArret;
import ma.youcode.covoiturage.entity.Trajet;
import ma.youcode.covoiturage.mapper.ConducteurMapper;
import ma.youcode.covoiturage.mapper.VehiculeMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
    componentModel = "spring",
    uses = {ConducteurMapper.class, VehiculeMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TrajetMapper {

    @Mapping(source = "conducteur.id", target = "conducteurId")
    @Mapping(source = "vehicule.id", target = "vehiculeId")
    TrajetResponse toResponse(Trajet entity);

    List<TrajetResponse> toResponseList(List<Trajet> entities);

    PointArretResponse toPointArretResponse(PointArret entity);

    List<PointArretResponse> toPointArretResponseList(List<PointArret> entities);

    @Mapping(target = "id", source = "trajet.id")
    @Mapping(target = "placesRestantes", source = "placesRestantes")
    TrajetDetailResponse toDetailResponse(Trajet trajet, Integer placesRestantes);
}
