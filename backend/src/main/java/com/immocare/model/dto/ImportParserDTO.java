package com.immocare.model.dto;

import com.immocare.model.entity.ImportParser;

import lombok.Value;

@Value
public class ImportParserDTO {
    Long id;
    String code;
    String label;
    String description;
    String format;
    String bankHint;
    boolean active;

    public static ImportParserDTO from(ImportParser p) {
        return new ImportParserDTO(
                p.getId(), p.getCode(), p.getLabel(),
                p.getDescription(), p.getFormat(), p.getBankHint(), p.isActive());
    }
}