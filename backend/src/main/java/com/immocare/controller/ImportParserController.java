package com.immocare.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.ImportParserDTO;
import com.immocare.repository.ImportParserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/import-parsers")
@RequiredArgsConstructor
public class ImportParserController {

    private final ImportParserRepository repo;

    @GetMapping
    public List<ImportParserDTO> getAll() {
        return repo.findByActiveTrueOrderByLabelAsc()
                .stream()
                .map(ImportParserDTO::from)
                .toList();
    }
}