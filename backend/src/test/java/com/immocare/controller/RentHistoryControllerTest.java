package com.immocare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.service.RentHistoryService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@WithMockUser(roles = "ADMIN")
class RentHistoryControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;
        @Autowired
        private RentHistoryService rentHistoryService;

        private static final Long UNIT_ID = 1L;
        private static final Long RENT_ID = 10L;

        private RentHistoryDTO sampleDTO(Long id, boolean current) {
                return new RentHistoryDTO(
                                id, UNIT_ID,
                                new BigDecimal("850.00"),
                                LocalDate.of(2024, 1, 1),
                                current ? null : LocalDate.of(2024, 6, 30),
                                "Initial rate",
                                LocalDateTime.now(),
                                current, 6L);
        }

        // -------------------------------------------------------------------------
        // GET /rents
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("GET /rents — returns history list")
        void getRentHistory_returns200() throws Exception {
                when(rentHistoryService.getRentHistory(UNIT_ID))
                                .thenReturn(List.of(sampleDTO(2L, true), sampleDTO(1L, false)));

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/rents", UNIT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("GET /rents — returns 404 when unit not found")
        void getRentHistory_unitNotFound_returns404() throws Exception {
                when(rentHistoryService.getRentHistory(UNIT_ID))
                                .thenThrow(new HousingUnitNotFoundException(UNIT_ID));

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/rents", UNIT_ID))
                                .andExpect(status().isNotFound());
        }

        // -------------------------------------------------------------------------
        // GET /rents/current
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("GET /rents/current — returns current rent")
        void getCurrentRent_returns200() throws Exception {
                when(rentHistoryService.getCurrentRent(UNIT_ID))
                                .thenReturn(Optional.of(sampleDTO(1L, true)));

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/rents/current", UNIT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.monthlyRent").value(850.00))
                                .andExpect(jsonPath("$.isCurrent").value(true));
        }

        @Test
        @DisplayName("GET /rents/current — returns 204 when no rent")
        void getCurrentRent_noRent_returns204() throws Exception {
                when(rentHistoryService.getCurrentRent(UNIT_ID)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/rents/current", UNIT_ID))
                                .andExpect(status().isNoContent());
        }

        // -------------------------------------------------------------------------
        // POST /rents
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("POST /rents — adds rent, returns 201")
        void addRent_valid_returns201() throws Exception {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), "Initial");

                when(rentHistoryService.addRent(eq(UNIT_ID), any()))
                                .thenReturn(sampleDTO(1L, true));

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/rents", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.monthlyRent").value(850.00));
        }

        @Test
        @DisplayName("POST /rents — rejects negative rent, returns 400")
        void addRent_negativeRent_returns400() throws Exception {
                String badRequest = """
                                { "monthlyRent": -100, "effectiveFrom": "2024-01-01" }
                                """;

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/rents", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badRequest))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /rents — rejects null effectiveFrom, returns 400")
        void addRent_nullDate_returns400() throws Exception {
                String badRequest = """
                                { "monthlyRent": 850.00, "effectiveFrom": null }
                                """;

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/rents", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(badRequest))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /rents — returns 409 on business rule violation")
        void addRent_businessRuleViolation_returns409() throws Exception {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), null);

                when(rentHistoryService.addRent(eq(UNIT_ID), any()))
                                .thenThrow(new IllegalArgumentException("more than 1 year in the future"));

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/rents", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        // -------------------------------------------------------------------------
        // PUT /rents/{rentId}
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("PUT /rents/{rentId} — updates rent, returns 200")
        void updateRent_valid_returns200() throws Exception {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("900.00"), LocalDate.of(2024, 3, 1), "Correction");

                when(rentHistoryService.updateRent(eq(UNIT_ID), eq(RENT_ID), any()))
                                .thenReturn(sampleDTO(RENT_ID, true));

                mockMvc.perform(put("/api/v1/housing-units/{unitId}/rents/{rentId}", UNIT_ID, RENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PUT /rents/{rentId} — returns 400 when record not found")
        void updateRent_notFound_returns400() throws Exception {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("900.00"), LocalDate.of(2024, 3, 1), null);

                when(rentHistoryService.updateRent(eq(UNIT_ID), eq(RENT_ID), any()))
                                .thenThrow(new IllegalArgumentException("Rent record not found"));

                mockMvc.perform(put("/api/v1/housing-units/{unitId}/rents/{rentId}", UNIT_ID, RENT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }

        // -------------------------------------------------------------------------
        // DELETE /rents/{rentId}
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("DELETE /rents/{rentId} — deletes rent, returns 204")
        void deleteRent_valid_returns204() throws Exception {
                doNothing().when(rentHistoryService).deleteRent(UNIT_ID, RENT_ID);

                mockMvc.perform(delete("/api/v1/housing-units/{unitId}/rents/{rentId}", UNIT_ID, RENT_ID))
                                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /rents/{rentId} — returns 400 when record not found")
        void deleteRent_notFound_returns400() throws Exception {
                doThrow(new IllegalArgumentException("Rent record not found"))
                                .when(rentHistoryService).deleteRent(UNIT_ID, RENT_ID);

                mockMvc.perform(delete("/api/v1/housing-units/{unitId}/rents/{rentId}", UNIT_ID, RENT_ID))
                                .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("DELETE /rents/{rentId} — returns 404 when unit not found")
        void deleteRent_unitNotFound_returns404() throws Exception {
                doThrow(new HousingUnitNotFoundException(UNIT_ID))
                                .when(rentHistoryService).deleteRent(UNIT_ID, RENT_ID);

                mockMvc.perform(delete("/api/v1/housing-units/{unitId}/rents/{rentId}", UNIT_ID, RENT_ID))
                                .andExpect(status().isNotFound());
        }
}