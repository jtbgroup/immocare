package com.immocare.service;

import com.immocare.exception.CategoryHasSubcategoriesException;
import com.immocare.exception.CategoryNotFoundException;
import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.model.dto.SaveTagCategoryRequest;
import com.immocare.model.dto.TagCategoryDTO;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.TagCategory;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.TagCategoryRepository;
import com.immocare.repository.TagSubcategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for TagCategory management.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all operations are now scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
public class TagCategoryService {

    private final TagCategoryRepository tagCategoryRepository;
    private final TagSubcategoryRepository tagSubcategoryRepository;
    private final EstateRepository estateRepository;

    public TagCategoryService(TagCategoryRepository tagCategoryRepository,
                               TagSubcategoryRepository tagSubcategoryRepository,
                               EstateRepository estateRepository) {
        this.tagCategoryRepository = tagCategoryRepository;
        this.tagSubcategoryRepository = tagSubcategoryRepository;
        this.estateRepository = estateRepository;
    }

    public List<TagCategoryDTO> getAll(UUID estateId) {
        return tagCategoryRepository.findByEstateIdOrderByNameAsc(estateId).stream()
                .map(c -> toDTO(c, c.getSubcategories().size()))
                .toList();
    }

    @Transactional
    public TagCategoryDTO create(UUID estateId, SaveTagCategoryRequest req) {
        Estate estate = findEstateOrThrow(estateId);

        if (tagCategoryRepository.existsByEstateIdAndNameIgnoreCase(estateId, req.name())) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }

        TagCategory category = new TagCategory();
        category.setEstate(estate);
        category.setName(req.name());
        category.setDescription(req.description());
        return toDTO(tagCategoryRepository.save(category), 0);
    }

    @Transactional
    public TagCategoryDTO update(UUID estateId, Long id, SaveTagCategoryRequest req) {
        verifyCategoryBelongsToEstate(estateId, id);

        TagCategory category = tagCategoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));

        if (tagCategoryRepository.existsByEstateIdAndNameIgnoreCaseAndIdNot(estateId, req.name(), id)) {
            throw new IllegalArgumentException("A category with this name already exists.");
        }

        category.setName(req.name());
        category.setDescription(req.description());
        return toDTO(tagCategoryRepository.save(category), category.getSubcategories().size());
    }

    @Transactional
    public void delete(UUID estateId, Long id) {
        verifyCategoryBelongsToEstate(estateId, id);

        TagCategory category = tagCategoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + id));

        int count = category.getSubcategories().size();
        if (count > 0) {
            throw new CategoryHasSubcategoriesException(
                    "This category contains " + count + " subcategory" + (count > 1 ? "s" : "") + ". Delete them first.");
        }

        tagCategoryRepository.delete(category);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void verifyCategoryBelongsToEstate(UUID estateId, Long categoryId) {
        if (!tagCategoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException("Category not found: " + categoryId);
        }
        if (!tagCategoryRepository.existsByEstateIdAndId(estateId, categoryId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }

    private TagCategoryDTO toDTO(TagCategory c, int subcategoryCount) {
        return new TagCategoryDTO(c.getId(), c.getName(), c.getDescription(),
                subcategoryCount, c.getCreatedAt(), c.getUpdatedAt());
    }
}
