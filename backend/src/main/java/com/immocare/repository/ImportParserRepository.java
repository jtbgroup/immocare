package com.immocare.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.immocare.model.entity.ImportParser;

public interface ImportParserRepository extends JpaRepository<ImportParser, Long> {
    List<ImportParser> findByActiveTrueOrderByLabelAsc();
}