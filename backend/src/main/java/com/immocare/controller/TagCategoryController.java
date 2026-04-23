package com.immocare.controller;

import com.immocare.model.dto.SaveTagCategoryRequest;
import com.immocare.model.dto.TagCategoryDTO;
import com.immocare.service.TagCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for TagCategory management.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/tag-categories
 *   POST   /api/v1/estates/{estateId}/tag-categories
 *   PUT    /api/v1/estates/{estateId}/tag-categories/{id}
 *   DELETE /api/v1/estates/{estateId}/tag-categories/{id}
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/tag-categories")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class TagCategoryController {

    private final TagCategoryService tagCategoryService;

    public TagCategoryController(TagCategoryService tagCategoryService) {
        this.tagCategoryService = tagCategoryService;
    }

    @GetMapping
    public List<TagCategoryDTO> getAll(@PathVariable UUID estateId) {
        return tagCategoryService.getAll(estateId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public TagCategoryDTO create(
            @PathVariable UUID estateId,
            @Valid @RequestBody SaveTagCategoryRequest req) {
        return tagCategoryService.create(estateId, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public TagCategoryDTO update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody SaveTagCategoryRequest req) {
        return tagCategoryService.update(estateId, id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public void delete(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        tagCategoryService.delete(estateId, id);
    }
}
