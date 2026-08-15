package de.paladinsinn.torg.codex.data.mapper;

import org.mapstruct.Mapper;

/**
 * Shared MapStruct conversions for value objects used across catalog
 * entity&#8596;domain mappers: {@code ClearanceLevel}, {@code DifficultyNumber},
 * and {@code VehicleWeapon}.
 */
@Mapper(componentModel = "spring")
public interface ValueObjectMapper {

    default de.paladinsinn.torg.codex.domain.model.ClearanceLevel toDomain(
            de.paladinsinn.torg.codex.data.model.ClearanceLevel value) {
        return value == null
                ? null
                : de.paladinsinn.torg.codex.domain.model.ClearanceLevel.valueOf(value.name());
    }

    default de.paladinsinn.torg.codex.data.model.ClearanceLevel toEntity(
            de.paladinsinn.torg.codex.domain.model.ClearanceLevel value) {
        return value == null
                ? null
                : de.paladinsinn.torg.codex.data.model.ClearanceLevel.valueOf(value.name());
    }

    de.paladinsinn.torg.codex.domain.model.DifficultyNumber toDomain(
            de.paladinsinn.torg.codex.data.model.DifficultyNumber value);

    de.paladinsinn.torg.codex.data.model.DifficultyNumber toEntity(
            de.paladinsinn.torg.codex.domain.model.DifficultyNumber value);

    de.paladinsinn.torg.codex.domain.model.VehicleWeapon toDomain(
            de.paladinsinn.torg.codex.data.model.VehicleWeapon value);

    de.paladinsinn.torg.codex.data.model.VehicleWeapon toEntity(
            de.paladinsinn.torg.codex.domain.model.VehicleWeapon value);
}
