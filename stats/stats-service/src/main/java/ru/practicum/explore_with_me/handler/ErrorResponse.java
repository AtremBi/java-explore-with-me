package ru.practicum.explore_with_me.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ErrorResponse {
    @Getter
    private final String error;
}