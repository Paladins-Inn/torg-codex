package de.paladinsinn.torg.codex.api.mapper;
import de.paladinsinn.torg.codex.api.dto.TagDetailDto;
import de.paladinsinn.torg.codex.api.dto.TagSummaryDto;
import de.paladinsinn.torg.codex.data.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
@Mapper(componentModel = "spring")
public interface TagMapper {
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    TagSummaryDto toSummary(Tag tag);
    @Mapping(target = "publications", expression = "java(java.util.List.of())")
    @Mapping(target = "parentId", source = "parent")
    TagDetailDto toDetail(Tag tag);
}
