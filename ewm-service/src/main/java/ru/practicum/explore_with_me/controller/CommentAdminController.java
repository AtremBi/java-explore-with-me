package ru.practicum.explore_with_me.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explore_with_me.dto.comment.CommentEvent;
import ru.practicum.explore_with_me.service.comment.CommentService;

import javax.validation.constraints.PositiveOrZero;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/comments")
@Validated
public class CommentAdminController {
    private final CommentService commentService;

    @DeleteMapping("/{comId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByAdmin(@PathVariable @PositiveOrZero Long comId) {
        log.info("DELETE /admin/comments/{}", comId, comId);
        commentService.deleteCommentByAdmin(comId);
    }

    @GetMapping("0/{eventId}")
    public List<CommentEvent> checkFunction(@PathVariable Long eventId) {
        log.info("Проверка функции");
        return commentService.checkFunction(eventId);
    }
}