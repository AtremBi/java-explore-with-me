package ru.practicum.explore_with_me.dto.compilation;

import lombok.*;
import ru.practicum.explore_with_me.dto.event.EventShortDto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompilationDto {
    private List<EventShortDto> events;
    private Long id;
    private Boolean pinned;
    @NotBlank
    @Size(min = 20, max = 200)
    private String title;
}
