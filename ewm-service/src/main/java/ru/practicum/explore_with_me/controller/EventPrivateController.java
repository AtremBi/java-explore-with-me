package ru.practicum.explore_with_me.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore_with_me.dto.event.EventFullDto;
import ru.practicum.explore_with_me.dto.event.EventShortDto;
import ru.practicum.explore_with_me.dto.event.NewEventDto;
import ru.practicum.explore_with_me.dto.event.UpdateEventUserRequest;
import ru.practicum.explore_with_me.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.explore_with_me.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.explore_with_me.dto.request.ParticipationRequestDto;
import ru.practicum.explore_with_me.service.EventService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@Slf4j
@RequiredArgsConstructor
@Validated
public class EventPrivateController {
    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto addEvent(@PathVariable("userId") @Positive Long userId,
                                 @Valid @RequestBody NewEventDto newEventDto) {

        log.info("POST /users/userId/events userId={}, newEvent = {}", userId, newEventDto);
        return eventService.create(userId, newEventDto);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getMyEvents(@PathVariable @Positive Long userId,
                                           @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                                           @Positive @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET event wit id = {}", userId);
        return eventService.getMyEvents(userId, from, size);
    }

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEventById(@PathVariable("userId") @Positive Long userId,
                                     @PathVariable("eventId") @Positive Long eventId) {
        log.info("GET /users/{userId}/events/{eventId}: userId = {}, eventId = {}", userId, eventId);

        return eventService.getMyEventById(userId, eventId);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateEvent(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @RequestBody @Valid UpdateEventUserRequest updateEventUserRequest) {
        log.info("PATCH /users/{}/events/ updateEvent = {}",
                userId, updateEventUserRequest);
        return eventService.updateEventUser(userId, eventId, updateEventUserRequest);
    }

    @GetMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getRequestsOld(@PathVariable @Positive Long userId,
                                                        @PathVariable @Positive Long eventId) {
        log.info("GET /users/{userId}/events//{eventId}/requests userId {}, eventId {}", userId, eventId);
        return eventService.getRequestsEvent(userId, eventId);
    }

    @PatchMapping("/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResult updateRequestsStatus(@Positive @PathVariable Long userId,
                                                               @Positive @PathVariable Long eventId,
                                                               @Valid @RequestBody EventRequestStatusUpdateRequest
                                                                       eventRequestStatusUpdateRequest) {

        log.info("PATCH /users/{userId}/events//{eventId}/requests userId = {}, eventId = {}," +
                " body = {}", userId, eventId, eventRequestStatusUpdateRequest);

        return eventService.updateRequestStatus(userId, eventId, eventRequestStatusUpdateRequest);
    }

}
