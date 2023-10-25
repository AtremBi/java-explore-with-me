package ru.practicum.explore_with_me.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class errorHandler {
    @ExceptionHandler(InvalidDateTimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public InvalidDateTimeException handleInvalidDateTimeException(final InvalidDateTimeException ex) {
        log.error("Ошибка 400 (InvalidDateTimeException) {}", ex.getMessage());
        return new InvalidDateTimeException(ex.getMessage());
    }
}
