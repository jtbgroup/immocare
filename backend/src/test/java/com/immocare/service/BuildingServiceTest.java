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
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

  @Mock
  private BuildingRepository buildingRepository;
  @Mock
  private HousingUnitRepository housingUnitRepository;
  @Mock
  private PersonRepository personRepository;
  @Mock
  private BuildingMapper buildingMapper;

  @InjectMocks
  private BuildingService buildingService;

  @Test
  void createBuilding_WithValidData_ReturnsSavedBuilding() {
    CreateBuildingRequest request = new CreateBuildingRequest(
        "Résidence Soleil", "123 Rue de la Loi", "1000", "Brussels", "Belgium", 0L);

    Building building = new Building();
    building.setName("Résidence Soleil");

    Building savedBuilding = new Building();
    savedBuilding.setId(1L);
    savedBuilding.setName("Résidence Soleil");

    BuildingDTO expectedDTO = new BuildingDTO(
        1L, "Résidence Soleil", "123 Rue de la Loi",
        "1000", "Brussels", "Belgium", null, null, null, null, null, 0L);

    when(buildingMapper.toEntity(request)).thenReturn(building);
    when(buildingRepository.save(building)).thenReturn(savedBuilding);
    when(buildingMapper.toDTO(savedBuilding)).thenReturn(expectedDTO);

    BuildingDTO result = buildingService.createBuilding(request);

    assertNotNull(result);
    assertEquals("Résidence Soleil", result.name());
    verify(buildingRepository).save(building);
  }

  @Test
  void getBuildingById_WhenExists_ReturnsBuilding() {
    Long buildingId = 1L;
    Building building = new Building();
    building.setId(buildingId);
    building.setName("Test Building");

    BuildingDTO expectedDTO = new BuildingDTO(
        buildingId, "Test Building", "123 Street",
        "1000", "Brussels", "Belgium", null, null, null, null, null, 0L);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
    when(housingUnitRepository.countByBuildingId(buildingId)).thenReturn(0L);
    when(buildingMapper.toDTO(building)).thenReturn(expectedDTO);

    BuildingDTO result = buildingService.getBuildingById(buildingId);

    assertNotNull(result);
    assertEquals("Test Building", result.name());
  }

  @Test
  void getBuildingById_WhenNotExists_ThrowsException() {
    when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BuildingNotFoundException.class,
        () -> buildingService.getBuildingById(99L));
  }

  @Test
  void updateBuilding_WithValidData_ReturnsUpdatedBuilding() {
    Long buildingId = 1L;
    Building building = new Building();
    building.setId(buildingId);
    building.setName("Old Name");

    UpdateBuildingRequest request = new UpdateBuildingRequest(
        "New Name", "New Street", "2000", "Liège", "Belgium", null);

    BuildingDTO expectedDTO = new BuildingDTO(
        buildingId, "New Name", "New Street",
        "2000", "Liège", "Belgium", null, null, null, null, null, 0L);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
    when(housingUnitRepository.countByBuildingId(buildingId)).thenReturn(0L);
    when(buildingRepository.save(any())).thenReturn(building);
    when(buildingMapper.toDTO(building)).thenReturn(expectedDTO);

    BuildingDTO result = buildingService.updateBuilding(buildingId, request);

    assertNotNull(result);
    assertEquals("New Name", result.name());
  }

  @Test
  void deleteBuilding_WhenNoUnits_DeletesSuccessfully() {
    Long buildingId = 1L;
    Building building = new Building();
    building.setId(buildingId);

    when(buildingRepository.findById(buildingId)).thenReturn(Optional.of(building));
    when(housingUnitRepository.countByBuildingId(buildingId)).thenReturn(0L);

    buildingService.deleteBuilding(buildingId);

    verify(buildingRepository).delete(building);
  }

  @Test
  void deleteBuilding_WhenNotExists_ThrowsException() {
    when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(BuildingNotFoundException.class,
        () -> buildingService.deleteBuilding(99L));
  }
}