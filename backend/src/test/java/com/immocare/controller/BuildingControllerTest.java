package com.immocare.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for BuildingController.
 * Tests full request-response cycle with actual database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = "classpath:application-test.properties")
class BuildingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createBuilding_WithValidData_ReturnsCreated() throws Exception {
    // Given
    CreateBuildingRequest request = new CreateBuildingRequest(
        "Résidence Soleil",
        "123 Rue de la Loi",
        "1000",
        "Brussels",
        "Belgium",
        "Jean Dupont"
    );
    
    // When & Then
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
    // Given
    String invalidJson = """
        {
          "name": "Test Building",
          "streetAddress": "123 Street",
          "postalCode": "1000"
        }
        """;
    
    // When & Then
    mockMvc.perform(post("/api/v1/buildings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"));
  }

  @Test
  void createBuilding_WithFieldTooLong_ReturnsBadRequest() throws Exception {
    // Given
    CreateBuildingRequest request = new CreateBuildingRequest(
        "A".repeat(101), // Exceeds 100 char limit
        "123 Street",
        "1000",
        "Brussels",
        "Belgium",
        null
    );
    
    // When & Then
    mockMvc.perform(post("/api/v1/buildings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getBuildingById_WhenExists_ReturnsBuilding() throws Exception {
    // Given - Create a building first
    CreateBuildingRequest createRequest = new CreateBuildingRequest(
        "Test Building",
        "123 Street",
        "1000",
        "Brussels",
        "Belgium",
        null
    );
    
    String response = mockMvc.perform(post("/api/v1/buildings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andReturn()
        .getResponse()
        .getContentAsString();
    
    Long buildingId = objectMapper.readTree(response).get("id").asLong();
    
    // When & Then
    mockMvc.perform(get("/api/v1/buildings/" + buildingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(buildingId))
        .andExpect(jsonPath("$.name").value("Test Building"));
  }

  @Test
  void getBuildingById_WhenNotExists_ReturnsNotFound() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/v1/buildings/99999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Building not found"));
  }

  @Test
  void updateBuilding_WithValidData_ReturnsUpdatedBuilding() throws Exception {
    // Given - Create a building first
    CreateBuildingRequest createRequest = new CreateBuildingRequest(
        "Original Building",
        "123 Street",
        "1000",
        "Brussels",
        "Belgium",
        null
    );
    
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
        "Marie Martin"
    );
    
    // When & Then
    mockMvc.perform(put("/api/v1/buildings/" + buildingId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Building"))
        .andExpect(jsonPath("$.ownerName").value("Marie Martin"));
  }

  @Test
  void deleteBuilding_WhenExists_ReturnsSuccess() throws Exception {
    // Given - Create a building first
    CreateBuildingRequest createRequest = new CreateBuildingRequest(
        "Building to Delete",
        "123 Street",
        "1000",
        "Brussels",
        "Belgium",
        null
    );
    
    String createResponse = mockMvc.perform(post("/api/v1/buildings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createRequest)))
        .andReturn()
        .getResponse()
        .getContentAsString();
    
    Long buildingId = objectMapper.readTree(createResponse).get("id").asLong();
    
    // When & Then
    mockMvc.perform(delete("/api/v1/buildings/" + buildingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Building deleted successfully"));
    
    // Verify it's deleted
    mockMvc.perform(get("/api/v1/buildings/" + buildingId))
        .andExpect(status().isNotFound());
  }

  @Test
  void getAllBuildings_ReturnsPagedResults() throws Exception {
    // Given - Create some buildings
    for (int i = 1; i <= 3; i++) {
      CreateBuildingRequest request = new CreateBuildingRequest(
          "Building " + i,
          "Street " + i,
          "100" + i,
          "Brussels",
          "Belgium",
          null
      );
      
      mockMvc.perform(post("/api/v1/buildings")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request)));
    }
    
    // When & Then
    mockMvc.perform(get("/api/v1/buildings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(3)))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void searchBuildings_ByName_ReturnsMatchingResults() throws Exception {
    // Given
    CreateBuildingRequest request1 = new CreateBuildingRequest(
        "Résidence Soleil",
        "123 Street",
        "1000",
        "Brussels",
        "Belgium",
        null
    );
    
    CreateBuildingRequest request2 = new CreateBuildingRequest(
        "Appartements Luna",
        "456 Avenue",
        "1050",
        "Brussels",
        "Belgium",
        null
    );
    
    mockMvc.perform(post("/api/v1/buildings")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request1)));
    
    mockMvc.perform(post("/api/v1/buildings")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request2)));
    
    // When & Then
    mockMvc.perform(get("/api/v1/buildings?search=Soleil"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].name").value("Résidence Soleil"));
  }
}
