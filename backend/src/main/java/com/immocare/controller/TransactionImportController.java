package com.immocare.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.immocare.exception.ParseException;
import com.immocare.model.dto.ImportBatchResultDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.service.TransactionImportService;

import lombok.RequiredArgsConstructor;

/**
 * POST /api/v1/transactions/import
 *
 * Replaces the old CSV-only import. Now accepts any file type
 * and dispatches to the appropriate parser based on parserCode.
 *
 * Multipart fields:
 * file — the CSV or PDF file
 * parserCode — e.g. "keytrade-csv-20260102"
 * bankAccountId — (optional) own bank account id to link
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionImportController {

    private final TransactionImportService importService;

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportBatchResultDTO> importFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("parserCode") String parserCode,
            @RequestPart(value = "bankAccountId", required = false) String bankAccountIdStr,
            @AuthenticationPrincipal AppUser currentUser) {

        Long bankAccountId = (bankAccountIdStr != null && !bankAccountIdStr.isBlank())
                ? Long.parseLong(bankAccountIdStr)
                : null;

        try {
            ImportBatchResultDTO result = importService.importFile(
                    file, parserCode.trim(), bankAccountId, currentUser);
            return ResponseEntity.ok(result);

        } catch (ParseException e) {
            return ResponseEntity.badRequest()
                    .body(ImportBatchResultDTO.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ImportBatchResultDTO.error(e.getMessage()));
        }
    }
}