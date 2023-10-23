package ru.practicum.explore_with_me.repository;

import ru.practicum.explore_with_me.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Optional<Application> findByApp(String appName);
}