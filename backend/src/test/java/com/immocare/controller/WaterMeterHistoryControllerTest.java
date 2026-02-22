package com.immocare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.immocare.model.dto.AssignMeterRequest;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.dto.WaterMeterHistoryDTO;
import com.immocare.service.WaterMeterHistoryService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class WaterMeterHistoryControllerTest {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;
        @Autowired
        WaterMeterHistoryService meterService;

        private static final Long UNIT_ID = 1L;

        // -------------------------------------------------------------------------
        // GET /active
        // -------------------------------------------------------------------------

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /active — returns 200 with active meter")
        void getActiveMeter_found_returns200() throws Exception {
                WaterMeterHistoryDTO dto = stubDTO("WM-2024-001", LocalDate.of(2024, 1, 1), null, true);
                when(meterService.getActiveMeter(UNIT_ID)).thenReturn(Optional.of(dto));

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/meters/active", UNIT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.meterNumber").value("WM-2024-001"))
                                .andExpect(jsonPath("$.isActive").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /active — returns 204 when no meter assigned")
        void getActiveMeter_notFound_returns204() throws Exception {
                when(meterService.getActiveMeter(UNIT_ID)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/meters/active", UNIT_ID))
                                .andExpect(status().isNoContent());
        }

        // -------------------------------------------------------------------------
        // GET / (history)
        // -------------------------------------------------------------------------

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET / — returns full meter history")
        void getMeterHistory_returns200WithList() throws Exception {
                List<WaterMeterHistoryDTO> history = List.of(
                                stubDTO("WM-002", LocalDate.of(2024, 6, 1), null, true),
                                stubDTO("WM-001", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 31), false));
                when(meterService.getMeterHistory(UNIT_ID)).thenReturn(history);

                mockMvc.perform(get("/api/v1/housing-units/{unitId}/meters", UNIT_ID))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].meterNumber").value("WM-002"))
                                .andExpect(jsonPath("$[1].status").value("Replaced"));
        }

        // -------------------------------------------------------------------------
        // POST / (assign)
        // -------------------------------------------------------------------------

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST / — assigns first meter, returns 201")
        void assignMeter_validRequest_returns201() throws Exception {
                AssignMeterRequest req = new AssignMeterRequest();
                req.setMeterNumber("WM-2024-001");
                req.setInstallationDate(LocalDate.of(2024, 1, 1));

                WaterMeterHistoryDTO dto = stubDTO("WM-2024-001", LocalDate.of(2024, 1, 1), null, true);
                when(meterService.assignMeter(eq(UNIT_ID), any())).thenReturn(dto);

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/meters", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.meterNumber").value("WM-2024-001"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST / — blank meter number → 400")
        void assignMeter_blankMeterNumber_returns400() throws Exception {
                AssignMeterRequest req = new AssignMeterRequest();
                req.setMeterNumber("");
                req.setInstallationDate(LocalDate.of(2024, 1, 1));

                mockMvc.perform(post("/api/v1/housing-units/{unitId}/meters", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // -------------------------------------------------------------------------
        // PUT /replace
        // -------------------------------------------------------------------------

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("PUT /replace — replaces meter, returns 200")
        void replaceMeter_validRequest_returns200() throws Exception {
                ReplaceMeterRequest req = new ReplaceMeterRequest();
                req.setNewMeterNumber("WM-2024-002");
                req.setNewInstallationDate(LocalDate.of(2024, 6, 1));

                WaterMeterHistoryDTO dto = stubDTO("WM-2024-002", LocalDate.of(2024, 6, 1), null, true);
                when(meterService.replaceMeter(eq(UNIT_ID), any())).thenReturn(dto);

                mockMvc.perform(put("/api/v1/housing-units/{unitId}/meters/replace", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.meterNumber").value("WM-2024-002"));
        }

        // -------------------------------------------------------------------------
        // DELETE /active
        // -------------------------------------------------------------------------

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /active — removes meter, returns 200 with updated DTO")
        void removeMeter_validRequest_returns200() throws Exception {
                RemoveMeterRequest req = new RemoveMeterRequest();
                req.setRemovalDate(LocalDate.of(2024, 12, 31));

                WaterMeterHistoryDTO dto = stubDTO("WM-2024-001", LocalDate.of(2024, 1, 1),
                                LocalDate.of(2024, 12, 31), false);
                when(meterService.removeMeter(eq(UNIT_ID), any())).thenReturn(dto);

                mockMvc.perform(delete("/api/v1/housing-units/{unitId}/meters/active", UNIT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isActive").value(false))
                                .andExpect(jsonPath("$.status").value("Replaced"));
        }

        // -------------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------------

        private WaterMeterHistoryDTO stubDTO(String number, LocalDate install,
                        LocalDate removal, boolean active) {
                return new WaterMeterHistoryDTO(1L, UNIT_ID, number, null, install,
                                removal, LocalDateTime.now(), active,
                                active ? 6L : 5L, active ? "Active" : "Replaced");
        }
}
