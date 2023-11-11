package ru.practicum.explore_with_me.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore_with_me.dto.comment.CommentForView;
import ru.practicum.explore_with_me.dto.comment.CommentUserDto;
import ru.practicum.explore_with_me.service.comment.CommentService;

import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/comments")
@Validated
public class CommentUserController {
    private final CommentService commentService;

    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentForView addCommentByUser(@PathVariable("userId") @Positive Long userId,
                                           @Valid @RequestBody CommentUserDto commentUserDto) {
        log.info("Post /comments/user/{}",
                userId, commentUserDto.getEventId(), userId);
        return commentService.addComment(userId, commentUserDto);
    }

    @GetMapping("/{comId}/user/{userId}")
    public CommentForView getByIdForUser(@PathVariable("comId") Long commId,
                                         @PathVariable("userId") Long userId) {
        log.info("Get /comments/{}/user/{}", commId, userId, commId, userId);
        return commentService.getCommentById(userId, commId);
    }

    @PatchMapping("/{comId}/user/{userId}")
    public CommentForView updateComment(@PathVariable @PositiveOrZero Long comId,
                                        @PathVariable @PositiveOrZero Long userId,
                                        @RequestBody @Valid CommentUserDto updateCommentDto) {
        log.info("Patch /comments/{}/user/{}/ ", comId, userId, comId, userId);
        return commentService.updateComment(comId, userId, updateCommentDto);
    }

    @DeleteMapping("/{comId}/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByUser(@PathVariable @PositiveOrZero Long comId,
                             @PathVariable @PositiveOrZero Long userId) {
        log.info("Удаление комментария с ID = {} пользователем с ID = {}.", userId, comId);
        commentService.deleteCommentByUser(comId, userId);
    }

    @GetMapping("/event/{eventId}")
    public List<CommentForView> getAllCommentEvent(@PathVariable @PositiveOrZero Long eventId,
                                                   @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                                   @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /comments/event/{}?from={}size={}", eventId, eventId, from, size);
        return commentService.getCommentsForEvent(eventId, from, size);
    }
}