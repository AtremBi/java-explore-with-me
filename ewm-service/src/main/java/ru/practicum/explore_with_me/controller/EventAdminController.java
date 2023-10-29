package ru.practicum.explore_with_me.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore_with_me.dto.event.EventFullDto;
import ru.practicum.explore_with_me.dto.event.UpdateEventAdminRequest;
import ru.practicum.explore_with_me.model.EventState;
import ru.practicum.explore_with_me.service.event.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/events")
@Slf4j
@RequiredArgsConstructor
@Validated
public class EventAdminController {
    private final EventService eventService;

    @GetMapping
    public List<EventFullDto> getEventsForAdmin(@RequestParam(required = false) List<Long> users,
                                                @RequestParam(required = false) List<EventState> states,
                                                @RequestParam(required = false) List<Long> categories,
                                                @RequestParam(required = false) LocalDateTime rangeStart,
                                                @RequestParam(required = false) LocalDateTime rangeEnd,
                                                @RequestParam(required = false, defaultValue = "") String text,
                                                @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                                @Positive @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /admin/events users={},states={},categories={},\nrangeStart={},rangeEnd={}" +
                ",from={},size={}", users, states, categories, rangeStart, rangeEnd, from, size);

        return eventService.getEventsForAdmin(users, states, categories,
                rangeStart, rangeEnd, text, from, size);
    }

    @PatchMapping("/{eventId}/publish")
    public EventFullDto publishEvent(@Positive @PathVariable Long eventId) {
        log.info("PATCH publishEvent id = {}", eventId);
        return eventService.publish(eventId);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@PositiveOrZero @PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventAdminRequest updateEventAdminRequest) {
        log.info("PATCH /admin/events event id{}, {}", eventId,
                updateEventAdminRequest);
        return eventService.updateEventAdmin(eventId, updateEventAdminRequest);
    }
}
