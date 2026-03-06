package com.immocare.controller;

import com.immocare.model.dto.SaveTagSubcategoryRequest;
import com.immocare.model.dto.TagSubcategoryDTO;
import com.immocare.model.enums.SubcategoryDirection;
import com.immocare.service.TagSubcategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TagSubcategoryController {

    private final TagSubcategoryService tagSubcategoryService;

    public TagSubcategoryController(TagSubcategoryService tagSubcategoryService) {
        this.tagSubcategoryService = tagSubcategoryService;
    }

    @GetMapping("/api/v1/tag-subcategories")
    public List<TagSubcategoryDTO> getAll(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) SubcategoryDirection direction) {
        List<TagSubcategoryDTO> all = tagSubcategoryService.getAll(categoryId);
        if (direction != null) {
            all = all.stream()
                .filter(s -> s.direction() == direction || s.direction() == SubcategoryDirection.BOTH)
                .toList();
        }
        return all;
    }

    @PostMapping("/api/v1/tag-subcategories")
    @ResponseStatus(HttpStatus.CREATED)
    public TagSubcategoryDTO create(@Valid @RequestBody SaveTagSubcategoryRequest req) {
        return tagSubcategoryService.create(req);
    }

    @PutMapping("/api/v1/tag-subcategories/{id}")
    public TagSubcategoryDTO update(@PathVariable Long id, @Valid @RequestBody SaveTagSubcategoryRequest req) {
        return tagSubcategoryService.update(id, req);
    }

    @DeleteMapping("/api/v1/tag-subcategories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagSubcategoryService.delete(id);
    }
}
