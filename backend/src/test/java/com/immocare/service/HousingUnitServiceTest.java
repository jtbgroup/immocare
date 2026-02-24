package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.HousingUnitMapper;
import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;

@ExtendWith(MockitoExtension.class)
class HousingUnitServiceTest {

  @Mock
  private HousingUnitRepository housingUnitRepository;
  @Mock
  private BuildingRepository buildingRepository;
  @Mock
  private HousingUnitMapper housingUnitMapper;

  @InjectMocks
  private HousingUnitService service;

  private Building building;
  private HousingUnit unit;

  @BeforeEach
  void setUp() {
    Person owner = new Person();
    owner.setId(1L);
    owner.setFirstName("Jean");
    owner.setLastName("Dupont");

    building = new Building();
    building.setId(1L);
    building.setName("Residence Soleil");
    building.setOwner(owner);

    unit = new HousingUnit();
    unit.setId(10L);
    unit.setBuilding(building);
    unit.setUnitNumber("A101");
    unit.setFloor(1);
  }

  // ─── createUnit ───────────────────────────────────────────────────────────

  @Test
  void createUnit_success() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("B202");
    request.setFloor(2);

    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "B202"))
        .thenReturn(false);
    when(housingUnitMapper.toEntity(request)).thenReturn(new HousingUnit());
    when(housingUnitRepository.save(any())).thenReturn(unit);
    HousingUnitDTO dto = new HousingUnitDTO();
    when(housingUnitMapper.toDTO(unit)).thenReturn(dto);

    HousingUnitDTO result = service.createUnit(request);

    assertThat(result).isNotNull();
    verify(housingUnitRepository).save(any());
  }

  @Test
  void createUnit_throwsWhenBuildingNotFound() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(99L);
    request.setUnitNumber("A1");
    request.setFloor(0);

    when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.createUnit(request))
        .isInstanceOf(BuildingNotFoundException.class);

    verify(housingUnitRepository, never()).save(any());
  }

  @Test
  void createUnit_throwsOnDuplicateUnitNumber() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("A101");
    request.setFloor(1);

    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "A101"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.createUnit(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("A101");

    verify(housingUnitRepository, never()).save(any());
  }

  @Test
  void createUnit_withTerraceCheckedButNoSurfaceOrOrientationSucceeds() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("C303");
    request.setFloor(3);
    request.setHasTerrace(true);

    HousingUnit mappedEntity = new HousingUnit();
    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "C303"))
        .thenReturn(false);
    when(housingUnitMapper.toEntity(request)).thenReturn(mappedEntity);
    when(housingUnitRepository.save(any())).thenReturn(unit);
    when(housingUnitMapper.toDTO(unit)).thenReturn(new HousingUnitDTO());

    service.createUnit(request);

    assertThat(mappedEntity.getTerraceSurface()).isNull();
    assertThat(mappedEntity.getTerraceOrientation()).isNull();
  }

  @Test
  void createUnit_withTerraceUnchecked_clearsTerraceSurfaceAndOrientation() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("E505");
    request.setFloor(5);
    request.setHasTerrace(false);

    HousingUnit mappedEntity = new HousingUnit();
    mappedEntity.setTerraceSurface(new BigDecimal("10.00"));
    mappedEntity.setTerraceOrientation("N");

    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "E505"))
        .thenReturn(false);
    when(housingUnitMapper.toEntity(request)).thenReturn(mappedEntity);
    when(housingUnitRepository.save(any())).thenReturn(unit);
    when(housingUnitMapper.toDTO(unit)).thenReturn(new HousingUnitDTO());

    service.createUnit(request);

    assertThat(mappedEntity.getTerraceSurface()).isNull();
    assertThat(mappedEntity.getTerraceOrientation()).isNull();
  }

  // ─── updateUnit ───────────────────────────────────────────────────────────

  @Test
  void updateUnit_successWhenUnitExists() {
    UpdateHousingUnitRequest request = new UpdateHousingUnitRequest();
    request.setUnitNumber("A101");
    request.setFloor(2);

    when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
    when(housingUnitRepository.save(any())).thenReturn(unit);
    when(housingUnitMapper.toDTO(unit)).thenReturn(new HousingUnitDTO());

    HousingUnitDTO result = service.updateUnit(10L, request);

    assertThat(result).isNotNull();
  }

  @Test
  void updateUnit_throwsWhenNotFound() {
    UpdateHousingUnitRequest request = new UpdateHousingUnitRequest();
    request.setUnitNumber("A101");
    request.setFloor(1);

    when(housingUnitRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateUnit(99L, request))
        .isInstanceOf(HousingUnitNotFoundException.class);
  }

  // ─── deleteUnit ───────────────────────────────────────────────────────────

  @Test
  void deleteUnit_successWhenNoAssociatedData() {
    when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));

    service.deleteUnit(10L);

    verify(housingUnitRepository).delete(unit);
  }

  @Test
  void deleteUnit_throwsWhenNotFound() {
    when(housingUnitRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.deleteUnit(99L))
        .isInstanceOf(HousingUnitNotFoundException.class);
  }
}