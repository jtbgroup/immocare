package com.immocare.model.dto;

public record ImportRowErrorDTO(int rowNumber, String rawLine, String errorMessage) {}
