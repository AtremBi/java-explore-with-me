package ru.practicum.explore_with_me.service.user;

import ru.practicum.explore_with_me.dto.user.UserDto;
import ru.practicum.explore_with_me.model.User;

import java.util.List;

public interface UserService {

    List<UserDto> findByIds(List<Long> ids, int from, int size);

    UserDto save(UserDto userDto);

    void delete(Long userId);

    UserDto check(Long userId, String message);

    User getUserOrThrow(Long userId, String message);
}