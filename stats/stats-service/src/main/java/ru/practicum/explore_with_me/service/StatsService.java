package ru.practicum.explore_with_me.service;

import ru.practicum.explore_with_me.dto.StatWithHits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.explore_with_me.dto.StatsResponseDto;
import ru.practicum.explore_with_me.mapper.StatMapper;
import ru.practicum.explore_with_me.model.Application;
import ru.practicum.explore_with_me.model.Stat;
import org.springframework.stereotype.Service;
import ru.practicum.explore_with_me.repository.StatRepository;
import ru.practicum.explore_with_me.dto.StatsRequestDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsService {
    private final StatRepository statRepository;
    private final ApplicationService applicationService;
    private final StatMapper statMapper;

    public void save(StatsRequestDto statDto) {
        Application application = applicationService.getByName(statDto.getApp())
                .orElseGet(() -> applicationService.save(new Application(statDto.getApp())));

        Stat stat = statMapper.mapFromSaveToModel(statDto);
        stat.setApp(application);
        statRepository.save(stat);
    }

    public List<StatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        List<StatWithHits> result;
        if (unique) {
            if (uris == null || uris.isEmpty()) {
                log.info("Получение статистики: эндпоинтов нет, unique = true");
                result = statRepository.findAllUniqueWhenUriIsEmpty(start, end);
            } else {
                log.info("Получение статистики: эндпоинты есть, unique = true");
                result = statRepository.findAllUniqueWhenUriIsNotEmpty(start, end, uris);
            }
        } else {
            if (uris == null || uris.isEmpty()) {
                log.info("Получение статистики: эндпоинтов нет, unique = false");
                result = statRepository.findAllWhenUriIsEmpty(start, end);
            } else {
                log.info("Получение статистики: эндпоинты есть, unique = false");
                result = statRepository.findAllWhenStarEndUris(start, end, uris);
            }
        }

        return result.stream().map(statMapper::mapToDtoForView).collect(Collectors.toList());
    }
}