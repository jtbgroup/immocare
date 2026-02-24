package com.immocare.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;

/**
 * Integration tests for BuildingController.
 * Compatible with Spring Boot 4 (no @AutoConfigureMockMvc).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class BuildingControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void createBuilding_WithValidData_ReturnsCreated() throws Exception {
        CreateBuildingRequest request = new CreateBuildingRequest(
                "Résidence Soleil",
                "123 Rue de la Loi",
                "1000",
                "Brussels",
                "Belgium", null);

        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Résidence Soleil"))
                .andExpect(jsonPath("$.city").value("Brussels"))
                .andExpect(jsonPath("$.ownerName").value("Jean Dupont"));
    }

    @Test
    void createBuilding_WithMissingRequiredField_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "name": "Test Building",
                  "streetAddress": "123 Street",
                  "postalCode": "1000"
                }
                """;

        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void createBuilding_WithFieldTooLong_ReturnsBadRequest() throws Exception {
        CreateBuildingRequest request = new CreateBuildingRequest(
                "A".repeat(101),
                "123 Street",
                "1000",
                "Brussels",
                "Belgium",
                null);

        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBuildingById_WhenExists_ReturnsBuilding() throws Exception {
        CreateBuildingRequest createRequest = new CreateBuildingRequest(
                "Test Building",
                "123 Street",
                "1000",
                "Brussels",
                "Belgium",
                null);

        String response = mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long buildingId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/buildings/" + buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(buildingId))
                .andExpect(jsonPath("$.name").value("Test Building"));
    }

    @Test
    void getBuildingById_WhenNotExists_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/buildings/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Building not found"));
    }

    @Test
    void updateBuilding_WithValidData_ReturnsUpdatedBuilding() throws Exception {
        CreateBuildingRequest createRequest = new CreateBuildingRequest(
                "Original Building",
                "123 Street",
                "1000",
                "Brussels",
                "Belgium",
                null);

        String createResponse = mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long buildingId = objectMapper.readTree(createResponse).get("id").asLong();

        UpdateBuildingRequest updateRequest = new UpdateBuildingRequest(
                "Updated Building",
                "456 Avenue",
                "1050",
                "Brussels",
                "Belgium",
                null);

        mockMvc.perform(put("/api/v1/buildings/" + buildingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Building"))
                .andExpect(jsonPath("$.ownerName").value("Marie Martin"));
    }

    @Test
    void deleteBuilding_WhenExists_ReturnsSuccess() throws Exception {
        CreateBuildingRequest createRequest = new CreateBuildingRequest(
                "Building to Delete",
                "123 Street",
                "1000",
                "Brussels",
                "Belgium",
                null);

        String createResponse = mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long buildingId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(delete("/api/v1/buildings/" + buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Building deleted successfully"));

        mockMvc.perform(get("/api/v1/buildings/" + buildingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllBuildings_ReturnsPagedResults() throws Exception {
        for (int i = 1; i <= 3; i++) {
            CreateBuildingRequest request = new CreateBuildingRequest(
                    "Building " + i,
                    "Street " + i,
                    "100" + i,
                    "Brussels",
                    "Belgium",
                    null);

            mockMvc.perform(post("/api/v1/buildings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        mockMvc.perform(get("/api/v1/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void searchBuildings_ByName_ReturnsMatchingResults() throws Exception {
        CreateBuildingRequest request1 = new CreateBuildingRequest(
                "Résidence Soleil",
                "123 Street",
                "1000",
                "Brussels",
                "Belgium",
                null);

        CreateBuildingRequest request2 = new CreateBuildingRequest(
                "Appartements Luna",
                "456 Avenue",
                "1050",
                "Brussels",
                "Belgium",
                null);

        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)));

        mockMvc.perform(post("/api/v1/buildings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)));

        mockMvc.perform(get("/api/v1/buildings?search=Soleil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Résidence Soleil"));
    }
}
