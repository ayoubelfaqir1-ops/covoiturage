package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.request.ConducteurRequest;
import ma.youcode.covoiturage.dto.response.ConducteurResponse;
import ma.youcode.covoiturage.entity.Conducteur;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ConducteurMapper {

    Conducteur toEntity(ConducteurRequest request);

    ConducteurResponse toResponse(Conducteur entity);

    List<ConducteurResponse> toResponseList(List<Conducteur> entities);
}
