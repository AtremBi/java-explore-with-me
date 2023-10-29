package ru.practicum.explore_with_me.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.WebClientService;
import ru.practicum.explore_with_me.dto.StatsResponseDto;
import ru.practicum.explore_with_me.exception.StatsException;
import ru.practicum.explore_with_me.model.Event;
import ru.practicum.explore_with_me.model.ParticipationRequest;
import ru.practicum.explore_with_me.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Import({WebClientService.class})
public class UtilService {
    private final ParticipationRequestRepository requestRepository;
    private final WebClientService statsClient;

    public List<StatsResponseDto> getViews(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> uris = new ArrayList<>();
        LocalDateTime start = null;
        for (Event event : events) {
            uris.add("/events/" + event.getId());
            if (start == null) {
                start = event.getCreatedOn();
            } else if (start.isBefore(event.getCreatedOn())) {
                start = event.getCreatedOn();
            }
        }
        List<StatsResponseDto> stats = new ArrayList<>();
        try {
            stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        } catch (StatsException e) {
            log.error(String.format("Ошибка сервиса статистики при получении информации об %s:\n %s", uris,
                    e.getMessage()));
        }
        return stats;
    }

    public List<Event> fillViews(List<Event> events, List<StatsResponseDto> stats) {
        List<Event> result = new ArrayList<>();
        if (stats != null && !stats.isEmpty()) {
            for (Event ev : events) {

                for (StatsResponseDto statsResponseDto : stats) {
                    String[] statsFields = statsResponseDto.getUri().split("/");
                    if (Integer.parseInt(statsFields[2]) == ev.getId()) {
                        ev.setViews(statsResponseDto.getHits());
                        result.add(ev);
                    }
                }
            }
        } else {
            for (Event event : events) {
                event.setViews(0);
                result.add(event);
            }
        }

        return result;
    }

    public Map<Event, List<ParticipationRequest>> prepareConfirmedRequest(List<Event> events) {
        List<Long> list1 = new ArrayList<>();
        for (Event event1 : events) {
            list1.add(event1.getId());
        }
        List<ParticipationRequest> confirmedRequests = requestRepository.findConfirmedRequests(list1);
        Map<Event, List<ParticipationRequest>> result = new HashMap<>();
        for (ParticipationRequest request : confirmedRequests) {
            Event event = request.getEvent();
            List<ParticipationRequest> list = result.get(event);
            if (list == null) {
                list = new ArrayList<>();
            }
            list.add(request);
            result.put(event, list);
        }
        return result;
    }

    public List<Event> fillConfirmedRequests(List<Event> events, Map<Event, List<ParticipationRequest>> confirmedRequests) {
        if (confirmedRequests == null || confirmedRequests.isEmpty()) {
            for (Event event : events) {
                event.setConfirmedRequests(0);
            }
            return events;
        }

        for (Event event : events) {
            if (confirmedRequests.get(event) != null && confirmedRequests.get(event).isEmpty()) {
                event.setConfirmedRequests(0);
            } else {
                if (confirmedRequests.get(event) != null) {
                    event.setConfirmedRequests(confirmedRequests.get(event).size());
                }
            }
        }
        return events;
    }
}
