package ru.practicum.explore_with_me;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.explore_with_me.dto.StatsRequestDto;
import ru.practicum.explore_with_me.dto.StatsResponseDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class StatsClient {
    private final RestTemplate restTemplate;
    @Value("${statsServerUrl}")
    private String statsServer;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ResponseEntity<List<StatsResponseDto>> getStats(LocalDateTime start, LocalDateTime end,
                                                           String[] uris, boolean unique) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        String urisForExchange = Arrays.stream(uris)
                .filter(n -> ((n != null) && (!n.isEmpty())))
                .collect(Collectors.joining(", "));
        Map<String, Object> uriVariables = Map.of(
                "start", start.format(formatter),
                "end", end.format(formatter),
                "uris", urisForExchange,
                "unique", unique);

        String uri = statsServer + "/stats?start={start}&end={end}&{uris}&{unique}";
        log.info("GET STATS - {}", uri);
        ParameterizedTypeReference<List<StatsResponseDto>> parTypeRef =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<List<StatsResponseDto>> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity,
                parTypeRef, uriVariables);
        log.info(response.toString());
        return response;
    }

    public void save(StatsRequestDto statsRequestDto) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<StatsRequestDto> requestEntity = new HttpEntity<>(statsRequestDto, httpHeaders);
        restTemplate.exchange(statsServer + "hit", HttpMethod.POST, requestEntity, StatsRequestDto.class);
    }
}