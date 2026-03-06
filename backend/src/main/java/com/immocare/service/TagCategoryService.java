package com.immocare.service;

import com.immocare.exception.CategoryHasSubcategoriesException;
import com.immocare.exception.CategoryNotFoundException;
import com.immocare.model.dto.SaveTagCategoryRequest;
import com.immocare.model.dto.TagCategoryDTO;
import com.immocare.model.entity.TagCategory;
import com.immocare.repository.TagCategoryRepository;
import com.immocare.repository.TagSubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TagCategoryService {

    private final TagCategoryRepository tagCategoryRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;

    public TagCategoryService(TagCategoryRepository tagCategoryRepository,
                              TagSubcategoryRepository tagSubcategoryRepository) {
        this.tagCategoryRepository = tagCategoryRepository;
        this.tagSubcategoryRepository = tagSubcategoryRepository;
    }

    public List<TagCategoryDTO> getAll() {
        return tagCategoryRepository.findAllByOrderByNameAsc().stream()
            .map(c -> toDTO(c, c.getSubcategories().size()))
            .toList();
    }

    @Transactional
    public TagCategoryDTO create(SaveTagCategoryRequest req) {
        if (tagCategoryRepository.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }
        TagCategory category = new TagCategory();
        category.setName(req.name());
        category.setDescription(req.description());
        return toDTO(tagCategoryRepository.save(category), 0);
    }

    @Transactional
    public TagCategoryDTO update(Long id, SaveTagCategoryRequest req) {
        TagCategory category = tagCategoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        if (tagCategoryRepository.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }
        category.setName(req.name());
        category.setDescription(req.description());
        return toDTO(tagCategoryRepository.save(category), category.getSubcategories().size());
    }

    @Transactional
    public void delete(Long id) {
        TagCategory category = tagCategoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));
        int count = category.getSubcategories().size();
        if (count > 0) {
            throw new CategoryHasSubcategoriesException(
                "This category contains " + count + " subcategory" + (count > 1 ? "s" : "") + ". Delete them first.");
        }
        tagCategoryRepository.delete(category);
    }

    private TagCategoryDTO toDTO(TagCategory c, int subcategoryCount) {
        return new TagCategoryDTO(c.getId(), c.getName(), c.getDescription(),
            subcategoryCount, c.getCreatedAt(), c.getUpdatedAt());
    }
}
