package ru.practicum.explore_with_me.mapper;

import ru.practicum.explore_with_me.dto.StatWithHits;
import ru.practicum.explore_with_me.dto.StatsRequestDto;
import ru.practicum.explore_with_me.model.Stat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explore_with_me.dto.StatsResponseDto;

@Mapper(componentModel = "spring")
public interface StatMapper {
    @Mapping(source = "app", target = "app.app")
    Stat mapFromSaveToModel(StatsRequestDto statsRequestDto);

    @Mapping(source = "app.app", target = "app")
    StatsRequestDto mapToDtoForSave(Stat stat);

    @Mapping(source = "app", target = "app")
    StatWithHits mapFromViewToStatDto(StatsResponseDto statsResponseDto);

    @Mapping(source = "app", target = "app")
    StatsResponseDto mapToDtoForView(StatWithHits statDto);
}