package ru.practicum.explore_with_me.service.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.dto.comment.CommentEvent;
import ru.practicum.explore_with_me.dto.comment.CommentForView;
import ru.practicum.explore_with_me.dto.comment.CommentUserDto;
import ru.practicum.explore_with_me.exception.FoundConflictInDB;
import ru.practicum.explore_with_me.exception.NotFoundRecordInBD;
import ru.practicum.explore_with_me.exception.OperationFailedException;
import ru.practicum.explore_with_me.mapper.CommentMapper;
import ru.practicum.explore_with_me.model.Comment;
import ru.practicum.explore_with_me.model.Event;
import ru.practicum.explore_with_me.model.EventState;
import ru.practicum.explore_with_me.model.User;
import ru.practicum.explore_with_me.repository.CommentRepository;
import ru.practicum.explore_with_me.repository.EventRepository;
import ru.practicum.explore_with_me.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentForView addComment(Long userId, CommentUserDto inputCommentDto) {
        User userFromDb = getUserOrThrow(userId, "Не найден пользователь с ID = %d");
        Event eventFromDb = getEventOrThrow(inputCommentDto.getEventId(), "Не найдено событие с ID = %d");

        if (!eventFromDb.getEventState().equals(EventState.PUBLISHED)) {
            if (eventFromDb.getInitiator().getId().equals(userId)) {
                Comment comment = commentMapper.mapToModelFromDto(inputCommentDto, eventFromDb, userFromDb);
                comment.setCreatedOn(LocalDateTime.now());
                Comment result = commentRepository.save(comment);
                return commentMapper.mapToView(result);
            } else {
                throw new OperationFailedException("Нельзя создать комментарий к неопубликованному событию");
            }
        }
        Comment comment = commentMapper.mapToModelFromDto(inputCommentDto, eventFromDb, userFromDb);
        comment.setCreatedOn(LocalDateTime.now());
        Comment result = commentRepository.save(comment);
        log.info("Создан комментарий с ID = {} в БД", result.getId());
        return commentMapper.mapToView(result);
    }

    @Override
    public CommentForView getCommentById(Long userId, Long comId) {
        checkUserOrThrow(userId, "Не найден пользователь с ID = %d");
        Comment commentFromDb = getCommentOrThrow(comId, "Комментарий не найден ID = %d");
        Event eventFromDb = getEventOrThrow(commentFromDb.getEvent().getId(), "Не найдено событие %d");
        if (!eventFromDb.getEventState().equals(EventState.PUBLISHED)) {
            if (eventFromDb.getInitiator().getId().equals(userId)) {
                CommentForView result = commentMapper.mapToView(commentFromDb);
                return result;
            } else {
                throw new OperationFailedException("Нельзя создать комментарий к неопубликованному событию");
            }
        }
        return commentMapper.mapToView(commentFromDb);
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long comId, Long userId) {
        checkUserOrThrow(userId, "Не найден пользователь с ID = %d");
        Comment commentFromDb = getCommentOrThrow(comId, "Комментарий не найден с ID = %d");
        if (checkAuthorComment(commentFromDb, userId)) {
            commentRepository.deleteById(comId);
            log.info("Пользователем с ID = {} удалён комментарий с ID = {}", userId, comId);
            return;
        }
        throw new OperationFailedException("Удалить комментарий может только автор");
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long comId) {
        commentRepository.findById(comId).orElseThrow(() -> new NotFoundRecordInBD("Комментарий не найден"));
        commentRepository.deleteById(comId);
    }

    @Override
    @Transactional
    public CommentForView updateComment(Long comId, Long userId, CommentUserDto inputCommentDto) {
        Comment commentFromDb = getCommentOrThrow(comId, "Комментарий с ID = %d");

        checkAuthorCommentOrThrow(userId, commentFromDb);

        if (commentFromDb.getCreatedOn().isBefore(LocalDateTime.now().minusHours(2))) {
            throw new OperationFailedException("Прошло больше 2 часов после создания комментария, " +
                    "редактировать его уже нельзя");
        }
        Comment comment = updateFieldsByUser(inputCommentDto, commentFromDb);
        Comment result = commentRepository.save(comment);

        log.info("Обновлен комментарий ID = {}", comId);
        return commentMapper.mapToView(result);
    }

    @Override
    public List<CommentForView> getCommentsForEvent(Long eventId, int from, int size) {
        Pageable pageable = PageRequest.of(from == 0 ? 0 : (from / size), size);
        Event eventFromDb = getEventOrThrow(eventId, "Не найдено событие с ID = %");
        if (!eventFromDb.getEventState().equals(EventState.PUBLISHED)) {
            if (eventFromDb.getInitiator().getId().equals(eventId)) {
                List<Comment> comments = commentRepository.findAllByEvent_Id(eventId, pageable);
                return commentMapper.mapFromModelLisToViewList(comments);
            } else {
                return Collections.emptyList();
            }
        }
        List<Comment> comments = commentRepository.findAllByEvent_Id(eventId, pageable);
        return commentMapper.mapFromModelLisToViewList(comments);
    }

    @Override
    public Comment getCommentOrThrow(Long comId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найден комментарий с ID = %d";
        }
        String finalMessage = message;
        return commentRepository.findById(comId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, comId)));
    }

    private Comment updateFieldsByUser(CommentUserDto newComment, Comment oldComment) {
        String text = newComment.getText();
        return oldComment.toBuilder()
                .text(text)
                .editedOn(LocalDateTime.now())
                .isEdited(true).build();
    }

    private boolean checkAuthorComment(Comment comment, Long userId) {
        return comment.getUser().getId().equals(userId);
    }

    private void checkAuthorCommentOrThrow(Long userId, Comment comment) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new FoundConflictInDB(String.format("Пользователь с ID = %d не является автором", userId));
        }
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

    private void checkUserOrThrow(Long userId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найден пользователь с ID = %d";
        }

        if (!userRepository.existsById(userId)){
            throw new NotFoundRecordInBD(String.format(message, userId));
        }
    }

    @Override
    public List<CommentEvent> checkFunction(Long eventId) {
        return commentRepository.getCommentsEvents(List.of(eventId));
    }

}
