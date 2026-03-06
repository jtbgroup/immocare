package com.immocare.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.CategoryNotFoundException;
import com.immocare.exception.SubcategoryInUseException;
import com.immocare.exception.SubcategoryNotFoundException;
import com.immocare.model.dto.SaveTagSubcategoryRequest;
import com.immocare.model.dto.TagSubcategoryDTO;
import com.immocare.model.entity.TagCategory;
import com.immocare.model.entity.TagSubcategory;
import com.immocare.repository.TagCategoryRepository;
import com.immocare.repository.TagSubcategoryRepository;

@Service
@Transactional(readOnly = true)
public class TagSubcategoryService {

    private final TagSubcategoryRepository tagSubcategoryRepository;
    private final TagCategoryRepository tagCategoryRepository;

    public TagSubcategoryService(TagSubcategoryRepository tagSubcategoryRepository,
            TagCategoryRepository tagCategoryRepository) {
        this.tagSubcategoryRepository = tagSubcategoryRepository;
        this.tagCategoryRepository = tagCategoryRepository;
    }

    public List<TagSubcategoryDTO> getAll(Long categoryId) {
        List<TagSubcategory> list = categoryId != null
                ? tagSubcategoryRepository.findByCategoryIdOrderByNameAsc(categoryId)
                : tagSubcategoryRepository.findAllByOrderByCategoryNameAscNameAsc();
        return list.stream().map(this::toDTO).toList();
    }

    @Transactional
    public TagSubcategoryDTO create(SaveTagSubcategoryRequest req) {
        TagCategory category = tagCategoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + req.categoryId()));
        if (tagSubcategoryRepository.existsByCategoryIdAndNameIgnoreCase(req.categoryId(), req.name())) {
            throw new IllegalArgumentException("A subcategory with this name already exists in the category.");
        }
        TagSubcategory sub = new TagSubcategory();
        sub.setCategory(category);
        sub.setName(req.name());
        sub.setDirection(req.direction());
        sub.setDescription(req.description());
        return toDTO(tagSubcategoryRepository.save(sub));
    }

    @Transactional
    public TagSubcategoryDTO update(Long id, SaveTagSubcategoryRequest req) {
        TagSubcategory sub = tagSubcategoryRepository.findById(id)
                .orElseThrow(() -> new SubcategoryNotFoundException("Subcategory not found: " + id));
        TagCategory category = tagCategoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + req.categoryId()));
        if (tagSubcategoryRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(req.categoryId(), req.name(), id)) {
            throw new IllegalArgumentException("A subcategory with this name already exists in the category.");
        }
        // BR-US086-AC9: direction change safety — only allowed if no usage
        if (!sub.getDirection().equals(req.direction()) && tagSubcategoryRepository.countUsage(id) > 0) {
            throw new IllegalArgumentException("Cannot change direction: subcategory is already in use.");
        }
        sub.setCategory(category);
        sub.setName(req.name());
        sub.setDirection(req.direction());
        sub.setDescription(req.description());
        return toDTO(tagSubcategoryRepository.save(sub));
    }

    @Transactional
    public void delete(Long id) {
        TagSubcategory sub = tagSubcategoryRepository.findById(id)
                .orElseThrow(() -> new SubcategoryNotFoundException("Subcategory not found: " + id));
        long usage = tagSubcategoryRepository.countUsage(id);
        if (usage > 0) {
            throw new SubcategoryInUseException(
                    "This subcategory is used on " + usage + " transaction(s) and cannot be deleted.");
        }
        tagSubcategoryRepository.delete(sub);
    }

    private TagSubcategoryDTO toDTO(TagSubcategory s) {
        long usage = tagSubcategoryRepository.countUsage(s.getId());
        return new TagSubcategoryDTO(
                s.getId(), s.getCategory().getId(), s.getCategory().getName(),
                s.getName(), s.getDirection(), s.getDescription(),
                usage, s.getCreatedAt(), s.getUpdatedAt());
    }
}
