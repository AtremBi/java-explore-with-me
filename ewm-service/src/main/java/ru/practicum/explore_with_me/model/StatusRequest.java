package ru.practicum.explore_with_me.model;

import lombok.Getter;

@Getter
public enum StatusRequest {
    CONFIRMED,
    PENDING,
    REJECTED,
    CANCELED
}
