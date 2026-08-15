package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.PublicationDetailDto;
import de.paladinsinn.torg.codex.api.dto.PublicationSummaryDto;
import de.paladinsinn.torg.codex.domain.model.Publication;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
public interface PublicationMapper {
    PublicationSummaryDto toSummary(Publication publication);
    PublicationDetailDto toDetail(Publication publication);
}
