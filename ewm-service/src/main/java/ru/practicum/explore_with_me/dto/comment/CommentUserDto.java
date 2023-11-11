package ru.practicum.explore_with_me.dto.comment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class CommentUserDto {
    @PositiveOrZero
    private Long eventId;
    @NotBlank
    @Size
    private String text;
}