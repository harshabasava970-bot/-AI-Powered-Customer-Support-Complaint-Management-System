package com.supportportal.service;

import com.supportportal.dto.CategoryDto;
import com.supportportal.entity.ComplaintCategory;
import com.supportportal.exception.DuplicateResourceException;
import com.supportportal.exception.ResourceNotFoundException;
import com.supportportal.repository.ComplaintCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final ComplaintCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return categoryRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findActive() {
        return categoryRepository.findByActiveTrue().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto findById(Long id) {
        return toDto(getCategory(id));
    }

    public CategoryDto create(CategoryDto dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Category already exists: " + dto.getName());
        }
        ComplaintCategory cat = ComplaintCategory.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .active(true)
                .build();
        return toDto(categoryRepository.save(cat));
    }

    public CategoryDto update(Long id, CategoryDto dto) {
        ComplaintCategory cat = getCategory(id);
        if (!cat.getName().equalsIgnoreCase(dto.getName())
                && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + dto.getName());
        }
        cat.setName(dto.getName());
        cat.setDescription(dto.getDescription());
        cat.setActive(dto.isActive());
        return toDto(categoryRepository.save(cat));
    }

    public void toggleActive(Long id) {
        ComplaintCategory cat = getCategory(id);
        cat.setActive(!cat.isActive());
        categoryRepository.save(cat);
    }

    public ComplaintCategory getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    public CategoryDto toDto(ComplaintCategory cat) {
        return CategoryDto.builder()
                .id(cat.getId())
                .name(cat.getName())
                .description(cat.getDescription())
                .active(cat.isActive())
                .build();
    }
}
