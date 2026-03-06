package com.immocare.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.dto.PagedTransactionResponse;
import com.immocare.model.dto.ParsedCsvRow;
import com.immocare.model.dto.TransactionFilter;
import com.immocare.model.entity.AppUser;
import com.immocare.service.CsvImportService;
import com.immocare.service.FinancialTransactionService;

@RestController
public class TransactionImportController {

    private final CsvImportService csvImportService;
    private final FinancialTransactionService transactionService;

    public TransactionImportController(CsvImportService csvImportService,
            FinancialTransactionService transactionService) {
        this.csvImportService = csvImportService;
        this.transactionService = transactionService;
    }

    @PostMapping("/api/v1/transactions/import")
    public ImportBatchResultDTO importCsv(@RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUser currentUser) {
        var config = csvImportService.loadMappingConfig();
        List<ParsedCsvRow> rows = csvImportService.parsePreview(file, config);
        return csvImportService.importBatch(rows, currentUser);
    }

    @GetMapping("/api/v1/transactions/import/{batchId}")
    public PagedTransactionResponse getBatch(@PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TransactionFilter filter = new TransactionFilter(null, null, null, null, null, null, null,
                null, null, null, null, null, batchId, null, null);
        return transactionService.getAll(filter, PageRequest.of(page, size));
    }
}
