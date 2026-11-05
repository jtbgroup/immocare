package com.immocare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.immocare.model.dto.EstateConfigDTOs;
import com.immocare.model.dto.ImportRowEnrichmentDTO;
import com.immocare.model.dto.SaveAssetLinkRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.ParsedTransaction;
import com.immocare.model.entity.TransactionParser;
import com.immocare.model.enums.AssetType;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.ImportBatchRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.TagLearningRuleRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionParserRegistry;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock
    private TransactionParserRegistry parserRegistry;
    @Mock
    private FinancialTransactionRepository transactionRepository;
    @Mock
    private ImportBatchRepository importBatchRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TagLearningRuleRepository learningRuleRepository;
    @Mock
    private TagSubcategoryRepository subcategoryRepository;
    @Mock
    private PersonBankAccountRepository personBankAccountRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private HousingUnitRepository housingUnitRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private EstateRepository estateRepository;
    @Mock
    private EstateConfigService estateConfigService;
    @Mock
    private TransactionAssetLinkService transactionAssetLinkService;

    @InjectMocks
    private TransactionImportService service;

    @Test
    void importsTransactionLinkedToBuildingWithoutHousingUnit() throws Exception {
        UUID estateId = UUID.randomUUID();
        long buildingId = 42L;
        Estate estate = org.mockito.Mockito.mock(Estate.class);
        Building building = org.mockito.Mockito.mock(Building.class);
        TransactionParser parser = org.mockito.Mockito.mock(TransactionParser.class);
        ParsedTransaction row = ParsedTransaction.builder()
                .transactionDate(LocalDate.of(2026, 1, 15))
                .amount(new BigDecimal("125.00"))
                .direction(ParsedTransaction.Direction.EXPENSE)
                .fingerprint("row-fingerprint")
                .rawLine("2026-01-15;125.00")
                .rowNumber(1)
                .build();

        when(estateRepository.findById(estateId)).thenReturn(Optional.of(estate));
        when(estate.getId()).thenReturn(estateId);
        when(parserRegistry.getOrThrow("test-csv")).thenReturn(parser);
        when(parser.parse(any(InputStream.class))).thenReturn(List.of(row));
        when(estateConfigService.getIntValue(
                estateId, EstateConfigDTOs.KEY_IMPORTS_CSV_SUGGESTION_CONFIDENCE, 3))
                .thenReturn(3);
        when(transactionRepository.existsByImportFingerprintAndEstateId("row-fingerprint", estateId))
                .thenReturn(false);
        when(buildingRepository.findByEstateIdAndId(estateId, buildingId))
                .thenReturn(Optional.of(building));

        List<SaveAssetLinkRequest> assetLinks = List.of(
                new SaveAssetLinkRequest(AssetType.BOILER, 73L, new BigDecimal("125.00"), "Annual service"));
        ImportRowEnrichmentDTO enrichment = new ImportRowEnrichmentDTO(
                "row-fingerprint", null, null, null, buildingId, null, assetLinks);
        var result = service.importFile(
                estateId,
                new MockMultipartFile("file", "statement.csv", "text/csv", new byte[0]),
                "test-csv",
                null,
                List.of(enrichment),
                Set.of("row-fingerprint"),
                null);

        ArgumentCaptor<FinancialTransaction> transactionCaptor =
                ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        FinancialTransaction savedTransaction = transactionCaptor.getValue();
        assertSame(building, savedTransaction.getBuilding());
        assertNull(savedTransaction.getHousingUnit());
        assertEquals(TransactionStatus.CONFIRMED, savedTransaction.getStatus());
        assertEquals(1, result.getImportedCount());
        verify(transactionAssetLinkService).attachAssetLinks(estateId, savedTransaction, assetLinks);
    }
}
