package ru.practicum.explore_with_me.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class StatsResponseDto {
    private String app;
    private String uri;
    private Long hits;
}
