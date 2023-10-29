package ru.practicum.explore_with_me.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore_with_me.dto.StatsRequestDto;
import ru.practicum.explore_with_me.dto.StatsResponseDto;
import ru.practicum.explore_with_me.service.StatsService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @GetMapping("/stats")
    public List<StatsResponseDto> getStats(@RequestParam("start") LocalDateTime start,
                                           @RequestParam("end") LocalDateTime end,
                                           @RequestParam(value = "uris", required = false) List<String> uris,
                                           @RequestParam(value = "unique", defaultValue = "false") boolean unique) {
        log.info("Получение статистики с {} по {} по эндпоинтам ({}). unique = {}.", start, end,
                uris, unique);
        return statsService.getStats(start, end, uris, unique);
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public void add(@Valid @RequestBody StatsRequestDto statsRequestDto) {
        log.info("Сохранение статистики {}.", statsRequestDto);
        statsService.save(statsRequestDto);
    }
}
