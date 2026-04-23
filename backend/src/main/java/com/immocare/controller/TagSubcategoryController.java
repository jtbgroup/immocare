package com.immocare.controller;

import com.immocare.model.dto.SaveTagSubcategoryRequest;
import com.immocare.model.dto.TagSubcategoryDTO;
import com.immocare.model.enums.SubcategoryDirection;
import com.immocare.service.TagSubcategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for TagSubcategory management.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/tag-subcategories
 *   POST   /api/v1/estates/{estateId}/tag-subcategories
 *   PUT    /api/v1/estates/{estateId}/tag-subcategories/{id}
 *   DELETE /api/v1/estates/{estateId}/tag-subcategories/{id}
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/tag-subcategories")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class TagSubcategoryController {

    private final TagSubcategoryService tagSubcategoryService;

    public TagSubcategoryController(TagSubcategoryService tagSubcategoryService) {
        this.tagSubcategoryService = tagSubcategoryService;
    }

    @GetMapping
    public List<TagSubcategoryDTO> getAll(
            @PathVariable UUID estateId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) SubcategoryDirection direction) {

        List<TagSubcategoryDTO> all = tagSubcategoryService.getAll(estateId, categoryId);
        if (direction != null) {
            all = all.stream()
                    .filter(s -> s.direction() == direction || s.direction() == SubcategoryDirection.BOTH)
                    .toList();
        }
        return all;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public TagSubcategoryDTO create(
            @PathVariable UUID estateId,
            @Valid @RequestBody SaveTagSubcategoryRequest req) {
        return tagSubcategoryService.create(estateId, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public TagSubcategoryDTO update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody SaveTagSubcategoryRequest req) {
        return tagSubcategoryService.update(estateId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public void delete(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        tagSubcategoryService.delete(estateId, id);
    }
}
