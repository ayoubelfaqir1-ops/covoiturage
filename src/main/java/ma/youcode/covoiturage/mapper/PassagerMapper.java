package ma.youcode.covoiturage.mapper;

import ma.youcode.covoiturage.dto.request.PassagerRequest;
import ma.youcode.covoiturage.dto.response.PassagerResponse;
import ma.youcode.covoiturage.entity.Passager;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PassagerMapper {

    Passager toEntity(PassagerRequest request);

    PassagerResponse toResponse(Passager entity);

    List<PassagerResponse> toResponseList(List<Passager> entities);
}
