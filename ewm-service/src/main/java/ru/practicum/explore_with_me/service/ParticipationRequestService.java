package ru.practicum.explore_with_me.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.dto.request.ParticipationRequestDto;
import ru.practicum.explore_with_me.exception.NotFoundRecordInBD;
import ru.practicum.explore_with_me.exception.OperationFailedException;
import ru.practicum.explore_with_me.mapper.ParticipationRequestMapper;
import ru.practicum.explore_with_me.model.*;
import ru.practicum.explore_with_me.repository.EventRepository;
import ru.practicum.explore_with_me.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestService {

    private final ParticipationRequestRepository participationRequestRepository;

    private final UserService userService;
    private final ParticipationRequestMapper requestMapper;
    private final EventRepository eventRepository;

    public ParticipationRequestDto create(Long userId, Long eventId) {
        User userFromDb = userService.getUserOrThrow(userId, "Не найден пользователь ID = %d");
        Event eventFromDb = getEventOrThrow(eventId, "Не найдено событие ID = %d");
        if (userId.equals(eventFromDb.getInitiator().getId())) {
            throw new OperationFailedException(String.format("Инициатор с ID = %d события не может создать " +
                    "запрос на участие в своём событии с ID = %d", userId, eventId));
        }
        if (eventFromDb.getEventState() != EventState.PUBLISHED) {
            throw new OperationFailedException(
                    "Нельзя участвовать в неопубликованном событии"
            );
        }
        if (participationRequestRepository.countAllByRequester_IdAndEvent_Id(userId, eventId) != 0) {
            throw new OperationFailedException(
                    String.format("Нельзя добавить повторный запрос на участие в событии ID = %d", eventId));
        }
        if (eventFromDb.getParticipantLimit() != 0) {
            List<ParticipationRequest> confirmedRequests =
                    participationRequestRepository.findConfirmedRequests(eventId);

            if (confirmedRequests.size() == eventFromDb.getParticipantLimit()) {
                throw new OperationFailedException("Достигнут лимит запросов");
            }
        }
        ParticipationRequest participationRequest = new ParticipationRequest();
        participationRequest.setRequester(userFromDb);

        if(eventFromDb.getParticipantLimit().equals(0)){
            participationRequest.setStatusRequest(StatusRequest.CONFIRMED);
        } else if (eventFromDb.getRequestModeration()) {
            participationRequest.setStatusRequest(StatusRequest.PENDING);
        } else {
            participationRequest.setStatusRequest(StatusRequest.CONFIRMED);
        }

        participationRequest.setEvent(eventFromDb);
        participationRequest.setCreated(LocalDateTime.now());
        participationRequest = participationRequestRepository.save(participationRequest);
        log.info("Сохранена заявка на участие в событии ID = {} пользователя ID = {}", eventId, userId);
        return requestMapper.mapToDto(participationRequest);
    }

    public List<ParticipationRequestDto> getRequestsByUserId(Long userId) {
        userService.getUserOrThrow(userId, "getRequestsByUserId не найден пользователь ID = %d");
        List<ParticipationRequestDto> result = participationRequestRepository.findAllByRequesterIdOrderByIdAsc(userId).stream()
                .map(requestMapper::mapToDto).collect(Collectors.toList());
        log.info("Выдан ответ на запрос участия пользователя ID = {} в чужих событиях", userId);
        return result;
    }

    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        userService.check(userId, "Не найден пользователь ID = %d");
        getEventOrThrow(requestId, "Не найдено событие ID = %d");
        ParticipationRequest participationRequest = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundRecordInBD(String.format("Не найдена заявка ID = %d", requestId)));

        if (!Objects.equals(participationRequest.getRequester().getId(), userId)) {
            throw new OperationFailedException("Отменить запрос может только инициатор");
        }
        participationRequest.setStatusRequest(StatusRequest.CANCELED);
        log.info("Выполнена отмена заявки на событие ID = {}, пользователем ID = {}", requestId, userId);
        return requestMapper.mapToDto(participationRequest);
    }

    public List<ParticipationRequestDto> findRequestByIds(List<Long> requestIds) {
        List<ParticipationRequest> result = participationRequestRepository.findByIdInOrderByIdAsc(requestIds);
        List<ParticipationRequestDto> requestDtos = requestMapper.mapListToDtoList(result);
        log.info("Выдан список заявок на участие в событиях по переданному списку ID заявок");
        return requestDtos;
    }

    public List<ParticipationRequestDto> getRequestsForEvent(Long eventId) {
        List<ParticipationRequest> result = participationRequestRepository.findAllByEvent_Id(eventId);
        return requestMapper.mapListToDtoList(result);
    }

    @Transactional
    public ParticipationRequestDto updateRequest(Long idRequest, StatusRequest status) {
        Optional<ParticipationRequest> request = participationRequestRepository.findById(idRequest);
        ParticipationRequest result = null;
        if (request.isPresent()) {
            request.get().setStatusRequest(status);
            result = participationRequestRepository.save(request.get());
        }
        return requestMapper.mapToDto(result);
    }

    private Event getEventOrThrow(Long eventId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найдено событие с ID = %d";
        }
        String finalMessage = message;
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, eventId)));
    }
}