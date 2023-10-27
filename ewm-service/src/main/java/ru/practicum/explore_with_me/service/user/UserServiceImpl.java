package ru.practicum.explore_with_me.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.dto.user.UserDto;
import ru.practicum.explore_with_me.exception.NotFoundRecordInBD;
import ru.practicum.explore_with_me.mapper.UserMapper;
import ru.practicum.explore_with_me.model.User;
import ru.practicum.explore_with_me.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class UserServiceImpl {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public List<UserDto> findByIds(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from, size, Sort.by("id").ascending());
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageable).getContent();
        }
        return users.stream()
                .map(userMapper::mapToUserDto).collect(Collectors.toList());
    }

    public UserDto check(Long userId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найден пользователь ID = %d";
        }

        String finalMessage = message;
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, userId)));

        return userMapper.mapToUserDto(user);
    }

    @Transactional
    public UserDto save(UserDto userDto) {
        User newUser = userMapper.mapToUser(userDto);
        User savedUser = userRepository.save(newUser);
        UserDto result = userMapper.mapToUserDto(savedUser);
        log.info("Сохранен пользователь ID = {}, name = {}", result.getId(), result.getName());
        return result;
    }

    @Transactional
    public void delete(Long userId) {
        UserDto oldUser = check(userId, "Пользователь ID = %d не найден");
        userRepository.deleteById(userId);
        log.info("Удален пользователь ID = {} name = {}", userId, oldUser.getName());
    }

    public User getUserOrThrow(Long userId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найден пользователь с ID = %d";
        }
        String finalMessage = message;

        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, userId)));
    }
}