package com.immocare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.exception.PersonReferencedException;
import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.service.PersonService;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@WithMockUser(roles = "ADMIN")
class PersonControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    PersonService personService;

    // ---- GET /api/v1/persons ----

    @Test
    @DisplayName("GET /persons returns 200 with paged list")
    void getAll_returns200() throws Exception {
        PersonSummaryDTO dto = new PersonSummaryDTO(1L, "Dupont", "Jean", "Brussels", null, false, false);
        when(personService.getAll(any(), any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/persons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastName").value("Dupont"));
    }

    // ---- GET /api/v1/persons/search ----

    @Test
    @DisplayName("GET /persons/search?q=du returns picker results")
    void searchForPicker_returns200() throws Exception {
        PersonSummaryDTO dto = new PersonSummaryDTO(1L, "Dupont", "Jean", "Brussels", null, false, false);
        when(personService.searchForPicker("du")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/persons/search").param("q", "du"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastName").value("Dupont"));
    }

    // ---- GET /api/v1/persons/{id} ----

    @Test
    @DisplayName("GET /persons/1 returns 200 with full PersonDTO")
    void getById_existing_returns200() throws Exception {
        PersonDTO dto = new PersonDTO();
        dto.setId(1L);
        dto.setLastName("Dupont");
        dto.setFirstName("Jean");
        when(personService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/persons/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Dupont"));
    }

    @Test
    @DisplayName("GET /persons/999 returns 404")
    void getById_unknown_returns404() throws Exception {
        when(personService.getById(999L)).thenThrow(new PersonNotFoundException(999L));

        mockMvc.perform(get("/api/v1/persons/999"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/persons ----

    @Test
    @DisplayName("POST /persons with valid body returns 201")
    void create_validBody_returns201() throws Exception {
        CreatePersonRequest request = new CreatePersonRequest();
        request.setLastName("Martin");
        request.setFirstName("Marie");

        PersonDTO dto = new PersonDTO();
        dto.setId(2L);
        dto.setLastName("Martin");
        dto.setFirstName("Marie");
        when(personService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/persons")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2L));
    }

    @Test
    @DisplayName("POST /persons with missing lastName returns 400")
    void create_missingLastName_returns400() throws Exception {
        CreatePersonRequest request = new CreatePersonRequest();
        request.setFirstName("Marie");
        // lastName is missing → @NotBlank

        mockMvc.perform(post("/api/v1/persons")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /api/v1/persons/{id} ----

    @Test
    @DisplayName("PUT /persons/1 with valid body returns 200")
    void update_validBody_returns200() throws Exception {
        UpdatePersonRequest request = new UpdatePersonRequest();
        request.setLastName("Dupont");
        request.setFirstName("Jean-Marc");

        PersonDTO dto = new PersonDTO();
        dto.setId(1L);
        when(personService.update(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/persons/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- DELETE /api/v1/persons/{id} ----

    @Test
    @DisplayName("DELETE /persons/1 unreferenced returns 204")
    void delete_unreferenced_returns204() throws Exception {
        doNothing().when(personService).delete(1L);

        mockMvc.perform(delete("/api/v1/persons/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /persons/1 referenced returns 409")
    void delete_referenced_returns409() throws Exception {
        doThrow(new PersonReferencedException(1L,
                List.of("Résidence Soleil"), List.of(), List.of()))
                .when(personService).delete(1L);

        mockMvc.perform(delete("/api/v1/persons/1").with(csrf()))
                .andExpect(status().isConflict());
    }
}
