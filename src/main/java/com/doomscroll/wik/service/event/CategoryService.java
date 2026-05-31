package com.doomscroll.wik.service.event;

import com.doomscroll.wik.dto.CategoryDto;
import com.doomscroll.wik.entity.Category;
import com.doomscroll.wik.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByIsActiveOrderByDisplayOrderAsc(true).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
        return toDto(category);
    }

    public CategoryDto toDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .parentCategoryId(category.getParentCategory() != null ? category.getParentCategory().getId() : null)
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}
