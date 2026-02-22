package com.immocare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.InvalidPebScoreDateException;
import com.immocare.exception.InvalidValidityPeriodException;
import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.model.entity.PebScore;
import com.immocare.service.PebScoreService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class PebScoreControllerTest {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        PebScoreService pebScoreService;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                objectMapper.findAndRegisterModules();
        }

        // ─── POST /peb-scores ─────────────────────────────────────────────────────

        @Test
        @WithMockUser(roles = "ADMIN")
        void addScore_valid_returns201() throws Exception {
                CreatePebScoreRequest req = new CreatePebScoreRequest();
                req.setPebScore(PebScore.B);
                req.setScoreDate(LocalDate.now());

                PebScoreDTO dto = new PebScoreDTO();
                dto.setPebScore(PebScore.B);
                dto.setStatus("CURRENT");

                when(pebScoreService.addScore(eq(1L), any())).thenReturn(dto);

                mockMvc.perform(post("/api/v1/housing-units/1/peb-scores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.pebScore").value("B"))
                                .andExpect(jsonPath("$.status").value("CURRENT"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void addScore_missingScore_returns400() throws Exception {
                CreatePebScoreRequest req = new CreatePebScoreRequest();
                req.setScoreDate(LocalDate.now());
                // pebScore is null → @NotNull should trigger

                mockMvc.perform(post("/api/v1/housing-units/1/peb-scores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void addScore_unitNotFound_returns404() throws Exception {
                when(pebScoreService.addScore(eq(99L), any()))
                                .thenThrow(new HousingUnitNotFoundException(99L));

                CreatePebScoreRequest req = new CreatePebScoreRequest();
                req.setPebScore(PebScore.C);
                req.setScoreDate(LocalDate.now());

                mockMvc.perform(post("/api/v1/housing-units/99/peb-scores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void addScore_invalidDate_returns400() throws Exception {
                when(pebScoreService.addScore(eq(1L), any()))
                                .thenThrow(new InvalidPebScoreDateException("Score date cannot be in the future"));

                CreatePebScoreRequest req = new CreatePebScoreRequest();
                req.setPebScore(PebScore.A);
                req.setScoreDate(LocalDate.now());

                mockMvc.perform(post("/api/v1/housing-units/1/peb-scores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void addScore_invalidValidityPeriod_returns400() throws Exception {
                when(pebScoreService.addScore(eq(1L), any()))
                                .thenThrow(new InvalidValidityPeriodException("Valid until must be after score date"));

                CreatePebScoreRequest req = new CreatePebScoreRequest();
                req.setPebScore(PebScore.B);
                req.setScoreDate(LocalDate.now());
                req.setValidUntil(LocalDate.now().minusDays(1));

                mockMvc.perform(post("/api/v1/housing-units/1/peb-scores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // ─── GET /peb-scores ──────────────────────────────────────────────────────

        @Test
        @WithMockUser(roles = "ADMIN")
        void getHistory_returnsListSortedNewestFirst() throws Exception {
                PebScoreDTO dto1 = new PebScoreDTO();
                dto1.setPebScore(PebScore.B);
                dto1.setStatus("CURRENT");
                PebScoreDTO dto2 = new PebScoreDTO();
                dto2.setPebScore(PebScore.D);
                dto2.setStatus("HISTORICAL");

                when(pebScoreService.getHistory(1L)).thenReturn(List.of(dto1, dto2));

                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].pebScore").value("B"))
                                .andExpect(jsonPath("$[1].pebScore").value("D"));
        }

        // ─── GET /peb-scores/current ──────────────────────────────────────────────

        @Test
        @WithMockUser(roles = "ADMIN")
        void getCurrentScore_withScore_returns200() throws Exception {
                PebScoreDTO dto = new PebScoreDTO();
                dto.setPebScore(PebScore.B);
                dto.setStatus("CURRENT");
                when(pebScoreService.getCurrentScore(1L)).thenReturn(Optional.of(dto));

                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores/current"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pebScore").value("B"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getCurrentScore_noScore_returns204() throws Exception {
                when(pebScoreService.getCurrentScore(1L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores/current"))
                                .andExpect(status().isNoContent());
        }

        // ─── GET /peb-scores/improvements ─────────────────────────────────────────

        @Test
        @WithMockUser(roles = "ADMIN")
        void getImprovements_withData_returns200() throws Exception {
                PebImprovementDTO dto = new PebImprovementDTO();
                dto.setFirstScore(PebScore.D);
                dto.setCurrentScore(PebScore.B);
                dto.setGradesImproved(2);
                dto.setYearsCovered(4);

                when(pebScoreService.getImprovementSummary(1L)).thenReturn(Optional.of(dto));

                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores/improvements"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.gradesImproved").value(2))
                                .andExpect(jsonPath("$.firstScore").value("D"))
                                .andExpect(jsonPath("$.currentScore").value("B"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getImprovements_noHistory_returns204() throws Exception {
                when(pebScoreService.getImprovementSummary(1L)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores/improvements"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void getHistory_unauthenticated_returns401() throws Exception {
                mockMvc.perform(get("/api/v1/housing-units/1/peb-scores"))
                                .andExpect(status().isUnauthorized());
        }
}