package ru.practicum.explore_with_me.service.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.WebClientService;
import ru.practicum.explore_with_me.dto.StatsResponseDto;
import ru.practicum.explore_with_me.dto.comment.CommentEvent;
import ru.practicum.explore_with_me.dto.event.*;
import ru.practicum.explore_with_me.dto.filter.EventFilter;
import ru.practicum.explore_with_me.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.explore_with_me.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.explore_with_me.dto.request.ParticipationRequestDto;
import ru.practicum.explore_with_me.dto.user.UserDto;
import ru.practicum.explore_with_me.exception.*;
import ru.practicum.explore_with_me.mapper.EventMapper;
import ru.practicum.explore_with_me.mapper.LocationMapper;
import ru.practicum.explore_with_me.mapper.UserMapper;
import ru.practicum.explore_with_me.model.*;
import ru.practicum.explore_with_me.repository.CategoryRepository;
import ru.practicum.explore_with_me.repository.CommentRepository;
import ru.practicum.explore_with_me.repository.EventRepository;
import ru.practicum.explore_with_me.repository.UserRepository;
import ru.practicum.explore_with_me.service.request.ParticipationRequestService;
import ru.practicum.explore_with_me.util.QPredicates;
import ru.practicum.explore_with_me.util.UtilService;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static ru.practicum.explore_with_me.model.QEvent.event;

@Slf4j
@Service
@RequiredArgsConstructor
@Import({WebClientService.class})
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UtilService utilService;
    private final UserMapper userMapper;
    private final WebClientService statsClient;
    private final ParticipationRequestService participationRequestService;
    private final LocationMapper locationMapper;
    private static final String nameApp = "ewm-service";
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User initiator = getUserOrThrow(userId, "Не найден пользователь ID = {}");
        Category category = getCatOrThrow(newEventDto.getCategoryId(), "Не найдена категория ID = {}");

        checkDateEvent(newEventDto.getEventDate(), 2);

        Event newEvent = eventMapper.mapFromNewToModel(newEventDto);
        newEvent.setInitiator(initiator);
        newEvent.setCategory(category);
        newEvent.setEventState(EventState.PENDING);
        newEvent.setCreatedOn(LocalDateTime.now());
        Event savedEvent = eventRepository.save(newEvent);
        EventFullDto result = eventMapper.mapFromModelToFullDtoWhenCreate(savedEvent, 0, 0);
        log.info("Создано событие ID = {} и кратким описанием: {}", savedEvent.getId(),
                savedEvent.getAnnotation());
        return result;
    }

    @Override
    public List<EventShortDto> getMyEvents(Long userId, Integer from, Integer size) {
        UserDto userFromDb = userMapper.mapToUserDto(getUserOrThrow(userId, "Не найден пользователь = {}"));
        Pageable pageable = PageRequest.of(from, size, Sort.by("id").ascending());

        Predicate predicate = event.initiator.id.ne(userId);

        List<Event> events = eventRepository.findAll(predicate, pageable).toList();
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<CommentEvent> commentsCount = commentRepository.getCommentsEvents(eventIds);

        Map<Long, Long> commentsMap = commentsCount.stream()
                .collect(toMap(CommentEvent::getEventId, CommentEvent::getCommentCount));

        for (Event eve : events) {
            eve.setComments(commentsMap.getOrDefault(eve.getId(), 0L));
        }

        List<EventShortDto> result = events.stream()
                .map(eventMapper::mapToShortDto).collect(Collectors.toList());

        log.info("getMyEvents ({} событий), userId = {} и name = {}",
                result.size(), userFromDb.getId(), userFromDb.getName());
        return result;
    }

    @Override
    public List<EventFullDto> getEventsForAdmin(List<Long> userIds, List<EventState> states, List<Long> categories,
                                                LocalDateTime rangeStart, LocalDateTime rangeEnd, String text,
                                                Integer from, Integer size) {

        log.info("GET /admin/events users={},states={},categories={},rangeStart={},rangeEnd={}" +
                ",from={},size={}", userIds, states, categories, rangeStart, rangeEnd, from, size);
        Pageable pageable = PageRequest.of(from, size, Sort.by("id").ascending());

        EventFilter eventFilter = EventFilter.builder()
                .userIds(userIds)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .text("%" + text.trim().toLowerCase() + "%").build();

        BooleanBuilder booleanBuilderForStates = new BooleanBuilder();
        if (states != null) {
            for (EventState state : states) {
                BooleanExpression eq = event.eventState.eq(state);
                booleanBuilderForStates.andAnyOf(eq);
            }
        }
        Predicate predicateForText = null;
        if (eventFilter.getText() != null && eventFilter.getText().isBlank()) {
            predicateForText = QPredicates.builder()
                    .add(event.annotation.likeIgnoreCase(eventFilter.getText()))
                    .add(event.description.likeIgnoreCase(eventFilter.getText()))
                    .buildOr();
        }

        QPredicates qPredicatesWithoutStatesAndText = QPredicates.builder()
                .add(eventFilter.getUserIds(), event.initiator.id::in)
                .add(eventFilter.getCategories(), event.category.id::in)
                .add(eventFilter.getPaid(), event.paid::eq)
                .add(eventFilter.getRangeStart(), event.eventDate::after)
                .add(eventFilter.getRangeEnd(), event.eventDate::before);

        Predicate filterForAll = qPredicatesWithoutStatesAndText
                .add(predicateForText)
                .add(booleanBuilderForStates.getValue())
                .buildAnd();
        List<Event> events;
        if (filterForAll == null) {
            events = eventRepository.findAll(pageable).toList();
        } else {
            events = eventRepository.findAll(filterForAll, pageable).toList();
        }

        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);

        return eventMapper.mapFromModelListToFullDtoList(events);
    }

    @Override
    public List<EventShortDto> getEventsForAll(String text, List<Long> categoriesIds, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                               Boolean onlyAvailable, String sort,
                                               Integer from, Integer size,
                                               HttpServletRequest httpServletRequest) {
        log.info("GET /events text:{},\ncategories:{}," +
                        "paid:{},rangeStart:{},rangeEnd:{},\nonlyAvailable:{},sort:{},from:{}, size:{}",
                text, categoriesIds, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        Sort sortForResponse;

        if (rangeStart != null && !rangeStart.isBefore(rangeEnd)) {
            throw new InvalidDateTimeException("rangeEnd не может быть меньше rangeStart");
        }

        if (sort == null || sort.isBlank() || sort.equalsIgnoreCase("EVENT_DATE")) {
            sortForResponse = Sort.by("eventDate").ascending();
        } else if (!sort.isBlank() && sort.equals("VIEWS")) {
            sortForResponse = Sort.by("views").ascending();
        } else {
            throw new InvalidSortException(String.format("Не корректный параметр сортировки = %s", sort));
        }
        try {
            statsClient.save(
                    nameApp,
                    httpServletRequest.getRequestURI(),
                    httpServletRequest.getRemoteAddr(),
                    LocalDateTime.now()
            );
        } catch (StatsException e) {
            log.error("Ошибка при отправке статистики");
        }

        Pageable pageable = PageRequest.of(from, size, sortForResponse);

        EventFilter eventFilter = EventFilter.builder()
                .text("%" + text.trim().toLowerCase() + "%")
                .categories(categoriesIds)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .build();
        Predicate predicateForText = null;
        if (!eventFilter.getText().isBlank()) {
            predicateForText = QPredicates.builder()
                    .add(event.annotation.likeIgnoreCase(eventFilter.getText()))
                    .add(event.description.likeIgnoreCase(eventFilter.getText()))
                    .buildOr();
        }

        QPredicates qPredicatesWithoutStatesAndText = QPredicates.builder()
                .add(eventFilter.getCategories(), event.category.id::in)
                .add(eventFilter.getPaid(), event.paid::eq)
                .add(eventFilter.getRangeStart(), event.eventDate::after)
                .add(eventFilter.getRangeEnd(), event.eventDate::before);

        Predicate filterForAll = qPredicatesWithoutStatesAndText
                .add(predicateForText)
                .buildAnd();
        List<Event> events = eventRepository.findAll(filterForAll, pageable).toList();

        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);

        return eventMapper.mapFromModelListToShortDtoList(events);
    }

    @Override
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest updateEventAdminRequest) {
        Event eventFromDb = getEventOrThrow(eventId, "Не найдено событие ID = %d");
        checkStateAction(eventFromDb, updateEventAdminRequest);

        eventFromDb = updateEventsFieldsByAdmin(eventFromDb, updateEventAdminRequest);

        eventFromDb = eventRepository.save(eventFromDb);
        List<Event> events = List.of(eventFromDb);
        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);
        Event result = events.get(0);
        result.setComments(commentRepository.countCommentsForEvent(eventId));
        log.info("updateEventAdmin ID = {}", eventId);
        return eventMapper.mapFromModelToFullDto(result);
    }

    @Override
    public EventFullDto getEventById(Long eventId, HttpServletRequest httpServletRequest) {
        Event event = getEventOrThrow(eventId, "Не найдено событие ID = %d");

        if (!event.getEventState().equals(EventState.PUBLISHED)) {
            throw new NotFoundRecordInBD("Событие должно быть опубликовано");
        }

        List<Event> events = List.of(event);
        statsClient.save(nameApp, httpServletRequest.getRequestURI(),
                httpServletRequest.getRemoteAddr(), LocalDateTime.now());
        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);
        Event result = events.get(0);
        result.setComments(commentRepository.countCommentsForEvent(result.getId()));
        log.info("getEventById ID = {}", eventId);
        return eventMapper.mapFromModelToFullDto(result);
    }

    @Override
    public EventFullDto getMyEventById(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId, "Событие не найдено ID = %d, userId = " + userId);

        List<Event> events = List.of(event);
        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);
        Event result = events.get(0);
        result.setComments(commentRepository.countCommentsForEvent(result.getId()));
        log.info("Отправлен ответ на запрос события ID = {}", eventId);
        return eventMapper.mapFromModelToFullDto(result);
    }

    @Override
    public EventFullDto updateEventUser(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        User userFromDb = getUserOrThrow(userId,
                "Не найден пользователь ID = {}");
        Event eventFromDb = getEventOrThrow(eventId, "Событие не найдено");
        if (updateEventUserRequest.getCategory() != null) {
            getCatOrThrow(updateEventUserRequest.getCategory(),
                    "Не найдена категория ID = {}");
        }

        if (!Objects.equals(userFromDb.getId(), eventFromDb.getInitiator().getId())) {
            throw new OperationFailedException("Пользователь не является инициатором события");
        }
        if (eventFromDb.getEventState().equals(EventState.PUBLISHED)) {
            throw new OperationFailedException("Событие уже опубликовано");
        }

        if (updateEventUserRequest.getEventDate() != null) {
            checkDateEvent(updateEventUserRequest.getEventDate(), 1);
        }

        eventFromDb = updateEventsFieldsByUser(eventFromDb, updateEventUserRequest);
        Event savedEvent = eventRepository.save(eventFromDb);

        List<Event> events = List.of(savedEvent);
        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);
        Event result = events.get(0);
        result.setComments(commentRepository.countCommentsForEvent(result.getId()));
        log.info("Выполнено обновление события ID = {}", eventId);

        return eventMapper.mapFromModelToFullDto(result);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsEvent(Long userId, Long eventId) {
        List<ParticipationRequestDto> result;
        getUserOrThrow(userId, "Не найден пользователь ID = %d");
        getEventOrThrow(eventId, "Событие не найдено");
        result = participationRequestService.getRequestsForEvent(eventId);
        return result;
    }

    @Override
    public EventFullDto publish(Long eventId) {
        Event eventFromDb = getEventOrThrow(eventId, "Событие не найдено");

        if (eventFromDb.getEventState() != EventState.PENDING)
            throw new OperationFailedException("Публиковать можно только события в статусе ожидания");

        checkDateEvent(eventFromDb.getEventDate(), 1);

        eventFromDb.setEventState(EventState.PUBLISHED);
        eventFromDb.setPublishedOn(LocalDateTime.now());
        Event saved = eventRepository.save(eventFromDb);
        log.info("Опубликовано событие с ID = {}", eventId);
        saved.setComments(commentRepository.countCommentsForEvent(saved.getId()));

        return eventMapper.mapFromModelToFullDto(saved);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest eventRequestStatusUpdateRequest) {
        getUserOrThrow(userId, "Не найден пользователь с ID = %d");
        Event eventFromDb = getEventOrThrow(eventId, "Событие не найдено");

        List<Event> events = setViewsAndConfirmedRequests(List.of(eventFromDb));

        eventFromDb = events.get(0);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (eventFromDb.getParticipantLimit() == 0 || !eventFromDb.getRequestModeration()) {
            return result;
        }
        if (eventFromDb.getParticipantLimit() <= eventFromDb.getConfirmedRequests()) {
            throw new OperationFailedException("Закончились свободные места на участие в событии или " +
                    "запрос на участие уже был подтверждён");
        }

        List<ParticipationRequestDto> requests =
                participationRequestService.findRequestByIds(eventRequestStatusUpdateRequest.getRequestIds());
        for (ParticipationRequestDto request : requests) {
            if (request.getStatus() != StatusRequest.PENDING) {
                throw new OperationFailedException("Статус запроса != \"PENDING\"");
            }
            if (checkParticipantLimit(eventFromDb) &&
                    eventRequestStatusUpdateRequest.getStatus() == StatusRequest.CONFIRMED) {
                request.setStatus(StatusRequest.CONFIRMED);
                result.getConfirmedRequests().add(request);
                participationRequestService.updateRequest(request.getId(), StatusRequest.CONFIRMED);
                events = setViewsAndConfirmedRequests(events);
            } else {
                request.setStatus(StatusRequest.REJECTED);
                result.getRejectedRequests().add(request);
            }

        }
        return result;
    }


    private boolean checkParticipantLimit(Event event) {
        if (event.getParticipantLimit() - event.getConfirmedRequests() > 0) {
            return true;
        }
        return false;
    }

    private Event getEventOrThrow(Long eventId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найдено событие ID = %d";
        }
        String finalMessage = message;
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, eventId)));
    }

    private User getUserOrThrow(Long userId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найден пользователь с ID = %d";
        }
        String finalMessage = message;

        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, userId)));
    }

    private Category getCatOrThrow(Long catId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найдена категория с ID = %d";
        }
        String finalMessage = message;

        return categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, catId)));
    }

    private void checkStateAction(Event oldEvent, UpdateEventAdminRequest newEvent) {

        if (newEvent.getStateAction() == StateAction.PUBLISH_EVENT) {
            if (oldEvent.getEventState() != EventState.PENDING) {
                throw new OperationFailedException("опубликовать событие можно публиковать только в состоянии ожидания");
            }
        }
        if (newEvent.getStateAction() == StateAction.REJECT_EVENT) {
            if (oldEvent.getEventState() == EventState.PUBLISHED) {
                throw new OperationFailedException("Нельзя отменить событие т.к оно уже опубликовано");
            }
        }
        if (oldEvent.getEventState().equals(EventState.CANCELED)
                && newEvent.getStateAction().equals(StateAction.PUBLISH_EVENT)) {
            throw new OperationFailedException("Невозможно отменить опубликованное событие");
        }
    }

    private void checkDateEvent(LocalDateTime newEventDateTime, int plusHours) {


        LocalDateTime now = LocalDateTime.now().plusHours(plusHours);
        if (newEventDateTime.toLocalDate().isBefore(LocalDate.now())) {
            throw new InvalidDateTimeException("Нельзя выбрать уже наступившую дату");
        }
        if (now.isAfter(newEventDateTime)) {
            throw new OperationFailedException(
                    String.format("Дата начала события, должна быть позже текущего момента на %s ч", plusHours));
        }
    }

    private Event updateEventsFieldsByAdmin(Event oldEvent, UpdateEventAdminRequest updateEvent) {
        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isBlank()) {
            oldEvent.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getCategory() != null) {
            oldEvent.getCategory().setId(updateEvent.getCategory());
        }
        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isBlank()) {
            oldEvent.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getEventDate() != null) {
            checkDateEvent(updateEvent.getEventDate(), 1);
            oldEvent.setEventDate(updateEvent.getEventDate());
        }
        if (updateEvent.getLocation() != null) {
            oldEvent.setLocation(locationMapper.mapToModel(updateEvent.getLocation()));
        }
        if (updateEvent.getPaid() != null) {
            oldEvent.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (updateEvent.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEvent.getRequestModeration());
        }
        if (StateAction.CANCEL_REVIEW.equals(updateEvent.getStateAction()) ||
                StateAction.REJECT_EVENT.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.CANCELED);
        }
        if (StateAction.SEND_TO_REVIEW.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.PENDING);
        }
        if (StateAction.PUBLISH_EVENT.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.PUBLISHED);
            oldEvent.setPublishedOn(LocalDateTime.now());
        }
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isBlank()) {
            oldEvent.setTitle(updateEvent.getTitle());
        }
        return oldEvent;
    }

    private Event updateEventsFieldsByUser(Event oldEvent, UpdateEventUserRequest updateEvent) {
        if (updateEvent.getCategory() != null) {
            oldEvent.getCategory().setId(updateEvent.getCategory());
        }
        if (updateEvent.getAnnotation() != null && !updateEvent.getAnnotation().isBlank()) {
            oldEvent.setAnnotation(updateEvent.getAnnotation());
        }
        if (updateEvent.getEventDate() != null) {
            oldEvent.setEventDate(updateEvent.getEventDate());
        }
        if (updateEvent.getLocation() != null) {
            oldEvent.setLocation(locationMapper.mapToModel(updateEvent.getLocation()));
        }
        if (updateEvent.getDescription() != null && !updateEvent.getDescription().isBlank()) {
            oldEvent.setDescription(updateEvent.getDescription());
        }
        if (updateEvent.getRequestModeration() != null) {
            oldEvent.setRequestModeration(updateEvent.getRequestModeration());
        }
        if (updateEvent.getPaid() != null) {
            oldEvent.setPaid(updateEvent.getPaid());
        }
        if (updateEvent.getParticipantLimit() != null) {
            oldEvent.setParticipantLimit(updateEvent.getParticipantLimit());
        }
        if (StateAction.CANCEL_REVIEW.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.CANCELED);
        }
        if (StateAction.SEND_TO_REVIEW.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.PENDING);
        }
        if (StateAction.PUBLISH_EVENT.equals(updateEvent.getStateAction())) {
            oldEvent.setEventState(EventState.PUBLISHED);
            oldEvent.setPublishedOn(LocalDateTime.now());
        }
        if (updateEvent.getTitle() != null && !updateEvent.getTitle().isBlank()) {
            oldEvent.setTitle(updateEvent.getTitle());
        }
        return oldEvent;
    }

    private List<Event> setViewsAndConfirmedRequests(List<Event> events) {
        List<StatsResponseDto> stats = utilService.getViews(events);
        events = utilService.fillViews(events, stats);

        Map<Event, List<ParticipationRequest>> confirmedRequests = utilService.prepareConfirmedRequest(events);
        events = utilService.fillConfirmedRequests(events, confirmedRequests);
        return events;
    }

}