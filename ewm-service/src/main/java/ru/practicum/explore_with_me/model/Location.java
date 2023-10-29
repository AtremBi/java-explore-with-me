package ru.practicum.explore_with_me.model;

import lombok.*;

import javax.persistence.Embeddable;

@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
@Embeddable
public class Location {
    private Float lat;
    private Float lon;
}
