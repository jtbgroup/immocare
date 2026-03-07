package com.immocare.model.entity;

import java.io.InputStream;
import java.util.List;

import com.immocare.exception.ParseException;
import com.immocare.repository.TransactionParserRegistry;

/**
 * Strategy interface for bank statement parsers.
 *
 * Each implementation handles one specific file format from one bank.
 * Implementations are Spring @Component beans and auto-registered
 * via {@link TransactionParserRegistry}.
 *
 * Naming convention for codes: {bank}-{format}-{yyyyMMdd}
 * Examples: keytrade-csv-20260102, keytrade-pdf-20260301
 */
public interface TransactionParser {

    /** Unique code matching import_parser.code in DB. */
    String getCode();

    /**
     * Parse the input stream and return a list of raw parsed rows.
     * Does NOT persist anything — pure parsing logic.
     *
     * @param input raw file bytes
     * @return ordered list of parsed transactions (may include duplicates)
     * @throws ParseException if the file is unreadable or malformed
     */
    List<ParsedTransaction> parse(InputStream input) throws ParseException;

    /** Human-readable description shown in import UI. */
    default String getDescription() {
        return "";
    }
}
