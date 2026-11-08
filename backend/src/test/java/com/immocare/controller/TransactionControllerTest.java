package com.immocare.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.immocare.exception.GlobalExceptionHandler;
import com.immocare.model.dto.CreateTransactionRequest;
import com.immocare.model.dto.PagedTransactionResponse;
import com.immocare.model.dto.TransactionFilter;
import com.immocare.model.enums.ManualTransactionStatus;
import com.immocare.service.FinancialTransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private static final UUID ESTATE_ID = UUID.randomUUID();

    @Mock
    private FinancialTransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TransactionController(transactionService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper().registerModule(new JavaTimeModule())))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @EnumSource(ManualTransactionStatus.class)
    void acceptsAllowedStatusWhenCreating(ManualTransactionStatus expectedStatus) throws Exception {
        performCreateRequest("\"status\":\"" + expectedStatus + "\"")
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateTransactionRequest> requestCaptor = ArgumentCaptor
                .forClass(CreateTransactionRequest.class);
        verify(transactionService).create(eq(ESTATE_ID), requestCaptor.capture(), isNull());
        assertEquals(expectedStatus, requestCaptor.getValue().status());
    }

    @Test
    void acceptsMissingStatusWhenCreating() throws Exception {
        performCreateRequest(null).andExpect(status().isCreated());

        ArgumentCaptor<CreateTransactionRequest> requestCaptor = ArgumentCaptor
                .forClass(CreateTransactionRequest.class);
        verify(transactionService).create(eq(ESTATE_ID), requestCaptor.capture(), isNull());
        assertNull(requestCaptor.getValue().status());
    }

    @Test
    void rejectsReconciledStatusWhenCreating() throws Exception {
        assertStatusRejected("RECONCILED");
    }

    @Test
    void rejectsUnknownStatusWhenCreating() throws Exception {
        assertStatusRejected("CANCELLED");
    }

    @Test
    void transactionListResponseDoesNotIncludeAggregateTotals() throws Exception {
        when(transactionService.getAll(eq(ESTATE_ID), any(TransactionFilter.class), any(Pageable.class)))
                .thenReturn(new PagedTransactionResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/estates/{estateId}/transactions", ESTATE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalIncome").doesNotExist())
                .andExpect(jsonPath("$.totalExpenses").doesNotExist())
                .andExpect(jsonPath("$.netBalance").doesNotExist());
    }

    private void assertStatusRejected(String statusValue) throws Exception {
        performCreateRequest("\"status\":\"" + statusValue + "\"")
                .andExpect(status().isBadRequest());

        verifyNoInteractions(transactionService);
    }

    private org.springframework.test.web.servlet.ResultActions performCreateRequest(String statusField)
            throws Exception {
        String requestBody = """
                {
                  "direction": "INCOME",
                  "transactionDate": "2026-01-15",
                  "accountingMonth": "2026-01-01",
                  "amount": 125.00
                  %s
                }
                """.formatted(statusField == null ? "" : ", " + statusField);
        return mockMvc.perform(post("/api/v1/estates/{estateId}/transactions", ESTATE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }
}
 
                 
                