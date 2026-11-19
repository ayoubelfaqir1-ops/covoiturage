package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.request.VehiculeRequest;
import ma.youcode.covoiturage.dto.response.VehiculeResponse;
import ma.youcode.covoiturage.entity.Vehicule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VehiculeMapper {

    Vehicule toEntity(VehiculeRequest request);

    @Mapping(source = "conducteur.id", target = "conducteurId")
    VehiculeResponse toResponse(Vehicule entity);

    List<VehiculeResponse> toResponseList(List<Vehicule> entities);
}
