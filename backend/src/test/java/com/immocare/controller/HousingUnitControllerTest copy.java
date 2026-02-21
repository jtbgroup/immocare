package com.immocare.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.Building;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class HousingUnitControllerTest {

  @Autowired private WebApplicationContext context;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BuildingRepository buildingRepository;
  @Autowired private HousingUnitRepository housingUnitRepository;

  private MockMvc mockMvc;
  private Long buildingId;

  @BeforeEach
  void setUp() {
    mockMvc = webAppContextSetup(context).build();

    Building b = new Building();
    b.setName("Test Building");
    b.setStreetAddress("1 Test Street");
    b.setPostalCode("1000");
    b.setCity("Brussels");
    b.setCountry("Belgium");
    buildingId = buildingRepository.save(b).getId();
  }

  // ─── GET /api/v1/buildings/{id}/units ─────────────────────────────────────

  @Test
  void getUnitsByBuilding_returnsEmptyList() throws Exception {
    mockMvc.perform(get("/api/v1/buildings/{id}/units", buildingId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void getUnitsByBuilding_returnsNotFoundForUnknownBuilding() throws Exception {
    mockMvc.perform(get("/api/v1/buildings/9999/units"))
        .andExpect(status().isNotFound());
  }

  // ─── POST /api/v1/units ───────────────────────────────────────────────────

  @Test
  void createUnit_withRequiredFields_returns201() throws Exception {
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    req.setUnitNumber("A101");
    req.setFloor(1);

    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.unitNumber").value("A101"))
        .andExpect(jsonPath("$.floor").value(1))
        .andExpect(jsonPath("$.hasTerrace").value(false))
        .andExpect(jsonPath("$.hasGarden").value(false));
  }

  @Test
  void createUnit_withTerrace_returns201() throws Exception {
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    req.setUnitNumber("T101");
    req.setFloor(1);
    req.setHasTerrace(true);
    req.setTerraceSurface(new BigDecimal("12.50"));
    req.setTerraceOrientation("S");

    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hasTerrace").value(true))
        .andExpect(jsonPath("$.terraceOrientation").value("S"));
  }

  @Test
  void createUnit_withTerraceCheckedButNoSurfaceOrOrientation_returns201() throws Exception {
    // Surface and orientation are optional even when hasTerrace = true
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    req.setUnitNumber("T102");
    req.setFloor(1);
    req.setHasTerrace(true);

    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hasTerrace").value(true));
  }

  @Test
  void createUnit_duplicateUnitNumber_returns409() throws Exception {
    CreateHousingUnitRequest req1 = new CreateHousingUnitRequest();
    req1.setBuildingId(buildingId);
    req1.setUnitNumber("A101");
    req1.setFloor(1);
    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req1)))
        .andExpect(status().isCreated());

    CreateHousingUnitRequest req2 = new CreateHousingUnitRequest();
    req2.setBuildingId(buildingId);
    req2.setUnitNumber("A101");
    req2.setFloor(2);
    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req2)))
        .andExpect(status().isConflict());
  }

  @Test
  void createUnit_missingRequiredField_returns400() throws Exception {
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    // unitNumber missing
    req.setFloor(1);

    mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  // ─── GET /api/v1/units/{id} ───────────────────────────────────────────────

  @Test
  void getUnitById_returnsUnit() throws Exception {
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    req.setUnitNumber("B202");
    req.setFloor(2);

    String response = mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn().getResponse().getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(get("/api/v1/units/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unitNumber").value("B202"));
  }

  @Test
  void getUnitById_returns404WhenNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/units/9999"))
        .andExpect(status().isNotFound());
  }

  // ─── PUT /api/v1/units/{id} ───────────────────────────────────────────────

  @Test
  void updateUnit_successfullyChangesFloor() throws Exception {
    CreateHousingUnitRequest create = new CreateHousingUnitRequest();
    create.setBuildingId(buildingId);
    create.setUnitNumber("C303");
    create.setFloor(3);

    String created = mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(create)))
        .andReturn().getResponse().getContentAsString();

    Long id = objectMapper.readTree(created).get("id").asLong();

    UpdateHousingUnitRequest update = new UpdateHousingUnitRequest();
    update.setUnitNumber("C303");
    update.setFloor(5);

    mockMvc.perform(put("/api/v1/units/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.floor").value(5));
  }

  // ─── DELETE /api/v1/units/{id} ────────────────────────────────────────────

  @Test
  void deleteUnit_returns200() throws Exception {
    CreateHousingUnitRequest req = new CreateHousingUnitRequest();
    req.setBuildingId(buildingId);
    req.setUnitNumber("D404");
    req.setFloor(4);

    String created = mockMvc.perform(post("/api/v1/units")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn().getResponse().getContentAsString();

    Long id = objectMapper.readTree(created).get("id").asLong();

    mockMvc.perform(delete("/api/v1/units/{id}", id))
        .andExpect(status().isOk());
  }

  @Test
  void deleteUnit_returns404WhenNotFound() throws Exception {
    mockMvc.perform(delete("/api/v1/units/9999"))
        .andExpect(status().isNotFound());
  }
}
