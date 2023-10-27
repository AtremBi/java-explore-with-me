package ru.practicum.explore_with_me.service.category;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explore_with_me.dto.category.CategoryDto;
import ru.practicum.explore_with_me.exception.NotFoundRecordInBD;
import ru.practicum.explore_with_me.mapper.CategoryMapper;
import ru.practicum.explore_with_me.model.Category;
import ru.practicum.explore_with_me.repository.CategoryRepository;
import ru.practicum.explore_with_me.service.event.EventServiceImpl;

import java.util.List;
import java.util.stream.Collectors;

//@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Lazy
    private final EventServiceImpl eventService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
                               @Lazy EventServiceImpl eventService) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.eventService = eventService;
    }

    @Override
    @Transactional
    public CategoryDto save(CategoryDto categoryDto) {
        Category newCategory = categoryMapper.mapToCategory(categoryDto);
        Category savedCategory = categoryRepository.save(newCategory);
        CategoryDto result = categoryMapper.mapToCategoryDto(savedCategory);
        log.info("catSave ID = {}, name = {}", result.getId(), result.getName());
        return result;
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        Pageable pageable = PageRequest.of(from, size, Sort.by("name").ascending());
        List<Category> categories = categoryRepository.findAll(pageable).getContent();
        return categories.stream()
                .map(categoryMapper::mapToCategoryDto).collect(Collectors.toList());
    }

    @Override
    public CategoryDto getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(
                        () -> new NotFoundRecordInBD(
                                String.format("Не найдена категория с ID = %d", id)));
        return categoryMapper.mapToCategoryDto(category);
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, CategoryDto categoryDto) {
        categoryRepository.findById(id).orElseThrow(() -> new NotFoundRecordInBD(
                String.format("Не найдена категория с ID = %d", id))
        );
        Category newCategory = categoryMapper.mapToCategory(categoryDto);
        newCategory.setId(id);
        CategoryDto updatedCategory = categoryMapper.mapToCategoryDto(categoryRepository.save(newCategory));
        log.info("update ID = {}, name = {}",
                updatedCategory.getId(), updatedCategory.getName());
        return updatedCategory;
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category oldCategory = categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(
                        "Не найдена категория с ID = %d", catId)));
        categoryRepository.deleteById(catId);
        log.info("catDelete ID = {}, name = {}", catId, oldCategory.getName());
    }

    @Override
    public Category getCatOrThrow(Long catId, String message) {
        if (message == null || message.isBlank()) {
            message = "Не найдена категория с ID = %d";
        }
        String finalMessage = message;

        return categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundRecordInBD(String.format(finalMessage, catId)));
    }
}

