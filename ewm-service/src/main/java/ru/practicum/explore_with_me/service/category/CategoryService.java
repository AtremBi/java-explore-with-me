package ru.practicum.explore_with_me.service.category;

import ru.practicum.explore_with_me.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long id);

    CategoryDto save(CategoryDto categoryDto);

    CategoryDto update(Long id, CategoryDto categoryDto);

    void delete(Long catId);

}