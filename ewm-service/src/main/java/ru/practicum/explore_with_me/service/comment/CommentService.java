package ru.practicum.explore_with_me.service.comment;

import ru.practicum.explore_with_me.dto.comment.CommentEvent;
import ru.practicum.explore_with_me.dto.comment.CommentForView;
import ru.practicum.explore_with_me.dto.comment.CommentUserDto;
import ru.practicum.explore_with_me.model.Comment;

import java.util.List;

public interface CommentService {

    CommentForView addComment(Long userId, CommentUserDto inputCommentDto);

    CommentForView updateComment(Long comId, Long userId, CommentUserDto inputCommentDto);

    CommentForView getCommentById(Long userId, Long comId);

    void deleteCommentByUser(Long comId, Long userId);

    void deleteCommentByAdmin(Long comId);

    List<CommentForView> getCommentsForEvent(Long eventId, int from, int size);

    Comment getCommentOrThrow(Long comId, String message);

    List<CommentEvent> checkFunction(Long eventId);
}