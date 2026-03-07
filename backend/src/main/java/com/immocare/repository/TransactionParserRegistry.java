package com.immocare.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.immocare.model.entity.TransactionParser;

/**
 * Registry of all available {@link TransactionParser} implementations.
 * Spring auto-discovers all @Component parsers and injects them here.
 */
@Component
public class TransactionParserRegistry {

    private final Map<String, TransactionParser> parsers;

    public TransactionParserRegistry(List<TransactionParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(TransactionParser::getCode, Function.identity()));
    }

    public Optional<TransactionParser> find(String code) {
        return Optional.ofNullable(parsers.get(code));
    }

    public TransactionParser getOrThrow(String code) {
        return find(code).orElseThrow(() -> new IllegalArgumentException("No parser registered for code: " + code));
    }

    public List<String> availableCodes() {
        return List.copyOf(parsers.keySet());
    }
}
