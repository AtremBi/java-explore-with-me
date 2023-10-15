package ru.practicum.explore_with_me.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.practicum.explore_with_me.model.Application;
import org.springframework.stereotype.Service;
import ru.practicum.explore_with_me.repository.ApplicationRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {
    private final ApplicationRepository applicationRepository;

    public Optional<Application> getByName(String appMame) {
        return applicationRepository.findByApp(appMame);
    }

    public Application save(Application application) {
        Application app = applicationRepository.save(application);
        log.info("Выполнено сохранение записи о новом приложении {}.", application);
        return app;
    }
}