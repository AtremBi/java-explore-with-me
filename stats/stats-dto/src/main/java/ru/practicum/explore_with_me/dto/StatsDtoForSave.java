package ru.practicum.explore_with_me.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatsDtoForSave {
    private Long id;
    @NotBlank(message = "Имя приложения не должно быть пустым.")
    private String app;
    @NotBlank(message = "uri не должен быть пустой.")
    private String uri;
    @NotBlank(message = "IP-адрес, не должен быть пустым.")
    private String ip;
    @NotNull(message = "Отсутствует параметр 'timestamp'")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}