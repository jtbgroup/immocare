package com.immocare.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BulkPatchTransactionRequest;
import com.immocare.model.dto.BulkPatchTransactionResult;
import com.immocare.model.dto.ConfirmBatchRequest;
import com.immocare.model.dto.ConfirmTransactionRequest;
import com.immocare.model.dto.CreateTransactionRequest;
import com.immocare.model.dto.FinancialTransactionDTO;
import com.immocare.model.dto.PagedTransactionResponse;
import com.immocare.model.dto.StatisticsFilter;
import com.immocare.model.dto.TransactionFilter;
import com.immocare.model.dto.TransactionStatisticsDTO;
import com.immocare.model.dto.UpdateTransactionRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.service.FinancialTransactionService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * REST controller for Financial Transaction management.
 * UC016 Phase 4: all routes are now scoped to an estate.
 *
 * Endpoints:
 *   GET    /api/v1/estates/{estateId}/transactions
 *   GET    /api/v1/estates/{estateId}/transactions/{id}
 *   POST   /api/v1/estates/{estateId}/transactions
 *   PUT    /api/v1/estates/{estateId}/transactions/{id}
 *   DELETE /api/v1/estates/{estateId}/transactions/{id}
 *   PATCH  /api/v1/estates/{estateId}/transactions/{id}/confirm
 *   POST   /api/v1/estates/{estateId}/transactions/confirm-batch
 *   PATCH  /api/v1/estates/{estateId}/transactions/bulk
 *   GET    /api/v1/estates/{estateId}/transactions/statistics
 *   GET    /api/v1/estates/{estateId}/transactions/export
 */
@RestController
@PreAuthorize("@security.isMemberOf(#estateId)")
public class TransactionController {

    private final FinancialTransactionService transactionService;

    public TransactionController(FinancialTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/api/v1/estates/{estateId}/transactions")
    public PagedTransactionResponse getAll(
            @PathVariable UUID estateId,
            @RequestParam(required = false) TransactionDirection direction,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate accountingFrom,
            @RequestParam(required = false) LocalDate accountingTo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long importBatchId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) Long assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate,desc") String sort) {

        TransactionFilter filter = new TransactionFilter(direction, from, to, accountingFrom, accountingTo,
                categoryId, subcategoryId, bankAccountId, buildingId, unitId,
                status, search, importBatchId, assetType, assetId);

        String[] sortParts = sort.split(",");
        Sort.Direction sortDir = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDir, sortParts[0]));

        return transactionService.getAll(estateId, filter, pageable);
    }

    @GetMapping("/api/v1/estates/{estateId}/transactions/{id}")
    public FinancialTransactionDTO getById(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        return transactionService.getById(estateId, id);
    }

    @PostMapping("/api/v1/estates/{estateId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public FinancialTransactionDTO create(
            @PathVariable UUID estateId,
            @Valid @RequestBody CreateTransactionRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return transactionService.create(estateId, req, currentUser);
    }

    @PutMapping("/api/v1/estates/{estateId}/transactions/{id}")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public FinancialTransactionDTO update(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest req) {
        return transactionService.update(estateId, id, req);
    }

    @DeleteMapping("/api/v1/estates/{estateId}/transactions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public void delete(
            @PathVariable UUID estateId,
            @PathVariable Long id) {
        transactionService.delete(estateId, id);
    }

    @PatchMapping("/api/v1/estates/{estateId}/transactions/{id}/confirm")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public FinancialTransactionDTO confirm(
            @PathVariable UUID estateId,
            @PathVariable Long id,
            @RequestBody ConfirmTransactionRequest req,
            @AuthenticationPrincipal AppUser currentUser) {
        return transactionService.confirm(estateId, id, req, currentUser);
    }

    @PostMapping("/api/v1/estates/{estateId}/transactions/confirm-batch")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public Map<String, Integer> confirmBatch(
            @PathVariable UUID estateId,
            @Valid @RequestBody ConfirmBatchRequest req) {
        int count = transactionService.confirmBatch(estateId, req.batchId());
        return Map.of("confirmedCount", count);
    }

    @PatchMapping("/api/v1/estates/{estateId}/transactions/bulk")
    @PreAuthorize("@security.isManagerOf(#estateId)")
    public BulkPatchTransactionResult bulkPatch(
            @PathVariable UUID estateId,
            @Valid @RequestBody BulkPatchTransactionRequest req) {
        return transactionService.bulkPatch(estateId, req);
    }

    @GetMapping("/api/v1/estates/{estateId}/transactions/statistics")
    public TransactionStatisticsDTO getStatistics(
            @PathVariable UUID estateId,
            @RequestParam(required = false) LocalDate accountingFrom,
            @RequestParam(required = false) LocalDate accountingTo,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) TransactionDirection direction) {

        StatisticsFilter filter = new StatisticsFilter(accountingFrom, accountingTo,
                buildingId, unitId, bankAccountId, direction);
        return transactionService.getStatistics(estateId, filter);
    }

    @GetMapping("/api/v1/estates/{estateId}/transactions/export")
    public void exportCsv(
            @PathVariable UUID estateId,
            @RequestParam(required = false) TransactionDirection direction,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) LocalDate accountingFrom,
            @RequestParam(required = false) LocalDate accountingTo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long importBatchId,
            @RequestParam(required = false) AssetType assetType,
            @RequestParam(required = false) Long assetId,
            HttpServletResponse response) throws IOException {

        TransactionFilter filter = new TransactionFilter(direction, from, to, accountingFrom, accountingTo,
                categoryId, subcategoryId, bankAccountId, buildingId, unitId,
                status, search, importBatchId, assetType, assetId);
        transactionService.exportCsv(estateId, filter, response);
    }
}
