package com.immocare.controller;

import com.immocare.model.dto.SaveTagCategoryRequest;
import com.immocare.model.dto.TagCategoryDTO;
import com.immocare.service.TagCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TagCategoryController {

    private final TagCategoryService tagCategoryService;

    public TagCategoryController(TagCategoryService tagCategoryService) {
        this.tagCategoryService = tagCategoryService;
    }

    @GetMapping("/api/v1/tag-categories")
    public List<TagCategoryDTO> getAll() {
        return tagCategoryService.getAll();
    }

    @PostMapping("/api/v1/tag-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public TagCategoryDTO create(@Valid @RequestBody SaveTagCategoryRequest req) {
        return tagCategoryService.create(req);
    }

    @PutMapping("/api/v1/tag-categories/{id}")
    public TagCategoryDTO update(@PathVariable Long id, @Valid @RequestBody SaveTagCategoryRequest req) {
        return tagCategoryService.update(id, req);
    }

    @DeleteMapping("/api/v1/tag-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        tagCategoryService.delete(id);
    }
}
