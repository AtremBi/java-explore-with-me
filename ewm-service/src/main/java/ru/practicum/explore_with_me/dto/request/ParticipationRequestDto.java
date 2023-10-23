package ru.practicum.explore_with_me.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.practicum.explore_with_me.model.StatusRequest;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class ParticipationRequestDto {
    private Long id;
    private LocalDateTime created;
    private Long event;
    private Long requester;
    private StatusRequest status;
}
