package com.immocare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.model.dto.CreateTransactionRequest;
import com.immocare.model.dto.UpdateTransactionRequest;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.enums.ManualTransactionStatus;
import com.immocare.model.enums.TransactionDirection;
import com.immocare.model.enums.TransactionStatus;
import com.immocare.repository.BankAccountRepository;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.FinancialTransactionRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.MeterRepository;
import com.immocare.repository.TagSubcategoryRepository;
import com.immocare.repository.TransactionAssetLinkRepository;

@ExtendWith(MockitoExtension.class)
class FinancialTransactionServiceTest {

    private static final UUID ESTATE_ID = UUID.randomUUID();
    private static final Long TRANSACTION_ID = 42L;
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2026, 1, 15);

    @Mock
    private FinancialTransactionRepository transactionRepository;
    @Mock
    private TagSubcategoryRepository tagSubcategoryRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionAssetLinkRepository assetLinkRepository;
    @Mock
    private TransactionAssetLinkService transactionAssetLinkService;
    @Mock
    private LearningService learningService;
    @Mock
    private BoilerRepository boilerRepository;
    @Mock
    private FireExtinguisherRepository fireExtinguisherRepository;
    @Mock
    private MeterRepository meterRepository;
    @Mock
    private HousingUnitRepository housingUnitRepository;
    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private LeaseRepository leaseRepository;
    @Mock
    private EstateRepository estateRepository;
    @Mock
    private Estate estate;

    private FinancialTransactionService service;

    @BeforeEach
    void setUp() {
        service = new FinancialTransactionService(
                transactionRepository,
                tagSubcategoryRepository,
                bankAccountRepository,
                assetLinkRepository,
                transactionAssetLinkService,
                learningService,
                boilerRepository,
                fireExtinguisherRepository,
                meterRepository,
                housingUnitRepository,
                buildingRepository,
                leaseRepository,
                estateRepository);

        when(transactionRepository.save(any(FinancialTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(ManualTransactionStatus.class)
    void createPersistsRequestedManualStatus(ManualTransactionStatus status) {
        when(estateRepository.findById(ESTATE_ID)).thenReturn(Optional.of(estate));
        service.create(ESTATE_ID, createRequest(status), null);

        ArgumentCaptor<FinancialTransaction> transactionCaptor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.valueOf(status.name()), transactionCaptor.getValue().getStatus());
    }

    @Test
    void createDefaultsToConfirmedWhenStatusIsAbsent() {
        when(estateRepository.findById(ESTATE_ID)).thenReturn(Optional.of(estate));
        service.create(ESTATE_ID, createRequest(null), null);

        ArgumentCaptor<FinancialTransaction> transactionCaptor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionStatus.CONFIRMED, transactionCaptor.getValue().getStatus());
    }

    @ParameterizedTest
    @EnumSource(ManualTransactionStatus.class)
    void updatePreservesExistingStatus(ManualTransactionStatus status) {
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setEstate(estate);
        transaction.setStatus(TransactionStatus.valueOf(status.name()));
        when(estate.getId()).thenReturn(ESTATE_ID);
        when(transactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));

        var response = service.update(ESTATE_ID, TRANSACTION_ID, updateRequest());

        assertEquals(TransactionStatus.valueOf(status.name()), transaction.getStatus());
        assertEquals(status.name(), response.status().name());
    }

    private CreateTransactionRequest createRequest(ManualTransactionStatus status) {
        return new CreateTransactionRequest(
                TransactionDirection.INCOME,
                TRANSACTION_DATE,
                null,
                LocalDate.of(2026, 1, 1),
                new BigDecimal("125.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                status);
    }

    private UpdateTransactionRequest updateRequest() {
        return new UpdateTransactionRequest(
                TransactionDirection.INCOME,
                TRANSACTION_DATE,
                null,
                LocalDate.of(2026, 1, 1),
                new BigDecimal("125.00"),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of());
    }
}
