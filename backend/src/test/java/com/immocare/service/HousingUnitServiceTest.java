package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.immocare.exception.BuildingNotFoundException;
import com.immocare.exception.HousingUnitHasDataException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.HousingUnitMapper;
import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.HousingUnit;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HousingUnitServiceTest {

  @Mock private HousingUnitRepository housingUnitRepository;
  @Mock private BuildingRepository buildingRepository;
  @Mock private HousingUnitMapper housingUnitMapper;

  @InjectMocks
  private HousingUnitService service;

  private Building building;
  private HousingUnit unit;

  @BeforeEach
  void setUp() {
    building = new Building();
    building.setId(1L);
    building.setName("Résidence Soleil");
    building.setOwnerName("Jean Dupont");

    unit = new HousingUnit();
    unit.setId(10L);
    unit.setBuilding(building);
    unit.setUnitNumber("A101");
    unit.setFloor(1);
  }

  // ─── getUnitsByBuilding ────────────────────────────────────────────────────

  @Test
  void getUnitsByBuilding_returnsListWhenBuildingExists() {
    when(buildingRepository.existsById(1L)).thenReturn(true);
    when(housingUnitRepository.findByBuildingIdOrderByFloorAscUnitNumberAsc(1L))
        .thenReturn(List.of(unit));

    HousingUnitDTO dto = new HousingUnitDTO();
    dto.setId(10L);
    when(housingUnitMapper.toDTO(unit)).thenReturn(dto);

    List<HousingUnitDTO> result = service.getUnitsByBuilding(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(10L);
  }

  @Test
  void getUnitsByBuilding_throwsWhenBuildingNotFound() {
    when(buildingRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> service.getUnitsByBuilding(99L))
        .isInstanceOf(BuildingNotFoundException.class);
  }

  // ─── getUnitById ──────────────────────────────────────────────────────────

  @Test
  void getUnitById_returnsDTO() {
    when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
    HousingUnitDTO dto = new HousingUnitDTO();
    when(housingUnitMapper.toDTO(unit)).thenReturn(dto);

    HousingUnitDTO result = service.getUnitById(10L);

    assertThat(result).isNotNull();
  }

  @Test
  void getUnitById_throwsWhenNotFound() {
    when(housingUnitRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getUnitById(99L))
        .isInstanceOf(HousingUnitNotFoundException.class)
        .hasMessageContaining("99");
  }

  // ─── createUnit ───────────────────────────────────────────────────────────

  @Test
  void createUnit_successWithRequiredFieldsOnly() {
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
  void createUnit_throwsWhenTerraceCheckedButSurfaceMissing() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("C303");
    request.setFloor(3);
    request.setHasTerrace(true);
    // terraceSurface intentionally omitted

    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "C303"))
        .thenReturn(false);

    assertThatThrownBy(() -> service.createUnit(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Terrace surface");
  }

  @Test
  void createUnit_withTerraceSucceeds() {
    CreateHousingUnitRequest request = new CreateHousingUnitRequest();
    request.setBuildingId(1L);
    request.setUnitNumber("D404");
    request.setFloor(4);
    request.setHasTerrace(true);
    request.setTerraceSurface(new BigDecimal("12.50"));
    request.setTerraceOrientation("S");

    when(buildingRepository.findById(1L)).thenReturn(Optional.of(building));
    when(housingUnitRepository.existsByBuildingIdAndUnitNumberIgnoreCase(1L, "D404"))
        .thenReturn(false);
    when(housingUnitMapper.toEntity(request)).thenReturn(new HousingUnit());
    when(housingUnitRepository.save(any())).thenReturn(unit);
    when(housingUnitMapper.toDTO(unit)).thenReturn(new HousingUnitDTO());

    HousingUnitDTO result = service.createUnit(request);

    assertThat(result).isNotNull();
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
    verify(housingUnitRepository).save(unit);
  }

  @Test
  void updateUnit_throwsWhenUnitNotFound() {
    when(housingUnitRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateUnit(99L, new UpdateHousingUnitRequest()))
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

    verify(housingUnitRepository, never()).delete(any());
  }

  // ─── Owner inheritance ─────────────────────────────────────────────────────

  @Test
  void getUnitById_inheritsOwnerFromBuilding() {
    unit.setOwnerName(null); // no unit-specific owner

    when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
    HousingUnitDTO dto = new HousingUnitDTO();
    when(housingUnitMapper.toDTO(unit)).thenReturn(dto);

    HousingUnitDTO result = service.getUnitById(10L);

    assertThat(result.getEffectiveOwnerName()).isEqualTo("Jean Dupont");
  }

  @Test
  void getUnitById_unitOwnerOverridesBuilding() {
    unit.setOwnerName("Marie Martin");

    when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
    when(housingUnitMapper.toDTO(unit)).thenReturn(new HousingUnitDTO());

    HousingUnitDTO result = service.getUnitById(10L);

    assertThat(result.getEffectiveOwnerName()).isEqualTo("Marie Martin");
  }
}
