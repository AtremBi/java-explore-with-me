package ru.practicum.explore_with_me.service.request;

import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.dto.request.ParticipationRequestDto;
import ru.practicum.explore_with_me.model.StatusRequest;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequestsByUserId(Long userId);

    ParticipationRequestDto create(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsForEvent(Long eventId);

    List<ParticipationRequestDto> findRequestByIds(List<Long> ids);

    @Transactional
    ParticipationRequestDto updateRequest(Long idRequest, StatusRequest status);

}
