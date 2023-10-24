package ru.practicum.explore_with_me.exception;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotFoundRecordInBD extends RuntimeException {
    public NotFoundRecordInBD(String message) {
        super(message);
        log.error(message);
    }
}