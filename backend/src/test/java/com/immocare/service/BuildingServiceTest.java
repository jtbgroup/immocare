package com.immocare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.mapper.BuildingMapper;
import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import com.immocare.repository.BuildingRepository;

/**
 * Unit tests for BuildingService.
 */
@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

  @Mock
  private BuildingRepository buildingRepository;

  @Mock
  private BuildingMapper buildingMapper;

  @InjectMocks
  private BuildingService buildingService;

  @Test
  void createBuilding_WithValidData_ReturnsSavedBuilding() {
    // Given
    CreateBuildingRequest request = new CreateBuildingRequest(
        "Résidence Soleil",
        "123 Rue de la Loi",
        "1000",
        "Brussels",
        "Belgium",
        0l);

    Building building = new Building();
    building.setName("Résidence Soleil");

    Building savedBuilding = new Building();
    savedBuilding.setId(1L);
    savedBuilding.setName("Résidence Soleil");

    BuildingDTO expectedDTO = new BuildingDTO(
        1L, "Résidence Soleil", "123 Rue de la Loi",
        "1000", "Brussels", "Belgium", null,
        null, null, null, null, 0L);

    when(buildingMapper.toEntity(request)).thenReturn(building);
    when(buildingRepository.save(building)).thenReturn(savedBuilding);
    when(buildingMapper.toDTO(savedBuilding)).thenReturn(expectedDTO);

    // When
    BuildingDTO result = buildingService.createBuilding(request);

    // Then
    assertNotNull(result);
    assertEquals("Résidence Soleil", result.name());
    verify(buildingRepository).save(building);
  }

  @Test
  void getBuildingById_WhenExists_ReturnsBuilding() {
    // Given
    Long buildingId = 1L;
    Building building = new Building();
    building.setId(buildingId);
    building.setName("Test Building");

    BuildingDTO expectedDTO = new BuildingDTO(
        buildingId, "Test Building", "123 Street",
        "1000", "Brussels", "Belgium", null,
        null, null, null, null, 0L);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
    when(buildingMapper.toDTO(building)).thenReturn(expectedDTO);

    // When
    BuildingDTO result = buildingService.getBuildingById(buildingId);

    // Then
    assertNotNull(result);
    assertEquals(buildingId, result.id());
    assertEquals("Test Building", result.name());
  }

  @Test
  void getBuildingById_WhenNotExists_ThrowsException() {
    // Given
    Long buildingId = 999L;
    when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(BuildingNotFoundException.class, () -> {
      buildingService.getBuildingById(buildingId);
    });
  }

  @Test
  void updateBuilding_WithValidData_ReturnsUpdatedBuilding() {
    // Given
    Long buildingId = 1L;
    UpdateBuildingRequest request = new UpdateBuildingRequest(
        "Updated Building",
        "456 Avenue Louise",
        "1050",
        "Brussels",
        "Belgium",
        null);

    Building existingBuilding = new Building();
    existingBuilding.setId(buildingId);
    existingBuilding.setName("Old Name");

    Building updatedBuilding = new Building();
    updatedBuilding.setId(buildingId);
    updatedBuilding.setName("Updated Building");

    BuildingDTO expectedDTO = new BuildingDTO(
        buildingId, "Updated Building", "456 Avenue Louise",
        "1050", "Brussels", "Belgium", null,
        null, null, null, null, 0L);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(existingBuilding));
    when(buildingRepository.save(any(Building.class))).thenReturn(updatedBuilding);
    when(buildingMapper.toDTO(updatedBuilding)).thenReturn(expectedDTO);

    // When
    BuildingDTO result = buildingService.updateBuilding(buildingId, request);

    // Then
    assertNotNull(result);
    assertEquals("Updated Building", result.name());
    verify(buildingMapper).updateEntityFromRequest(request, existingBuilding);
    verify(buildingRepository).save(existingBuilding);
  }

  @Test
  void updateBuilding_WhenNotExists_ThrowsException() {
    // Given
    Long buildingId = 999L;
    UpdateBuildingRequest request = new UpdateBuildingRequest(
        "Updated Building", "456 Street", "1050",
        "Brussels", "Belgium", null);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(BuildingNotFoundException.class, () -> {
      buildingService.updateBuilding(buildingId, request);
    });
  }

  @Test
  void deleteBuilding_WhenNoUnits_DeletesSuccessfully() {
    // Given
    Long buildingId = 1L;
    Building building = new Building();
    building.setId(buildingId);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));

    // When
    buildingService.deleteBuilding(buildingId);

    // Then
    verify(buildingRepository).delete(building);
  }

  @Test
  void deleteBuilding_WhenNotExists_ThrowsException() {
    // Given
    Long buildingId = 999L;
    when(buildingRepository.findById(buildingId)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(BuildingNotFoundException.class, () -> {
      buildingService.deleteBuilding(buildingId);
    });
  }
}
