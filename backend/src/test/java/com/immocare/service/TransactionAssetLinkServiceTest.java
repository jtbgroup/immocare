package com.immocare.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.model.dto.SaveAssetLinkRequest;
import com.immocare.model.entity.Boiler;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.enums.AssetType;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.MeterRepository;

@ExtendWith(MockitoExtension.class)
class TransactionAssetLinkServiceTest {

    @Mock
    private BoilerRepository boilerRepository;
    @Mock
    private FireExtinguisherRepository fireExtinguisherRepository;
    @Mock
    private MeterRepository meterRepository;
    @Mock
    private HousingUnitRepository housingUnitRepository;
    @Mock
    private BuildingRepository buildingRepository;

    @InjectMocks
    private TransactionAssetLinkService service;

    @Test
    void linksAssetsOwnedByDifferentBuildingsInTheSameEstate() {
        UUID estateId = UUID.randomUUID();
        Estate estate = org.mockito.Mockito.mock(Estate.class);
        Building building = org.mockito.Mockito.mock(Building.class);
        Building otherBuilding = org.mockito.Mockito.mock(Building.class);
        HousingUnit unit = org.mockito.Mockito.mock(HousingUnit.class);
        Boiler buildingBoiler = org.mockito.Mockito.mock(Boiler.class);
        Boiler unitBoiler = org.mockito.Mockito.mock(Boiler.class);
        FinancialTransaction transaction = new FinancialTransaction();
        transaction.setEstate(estate);
        transaction.setAmount(new BigDecimal("250.00"));

        when(estate.getId()).thenReturn(estateId);
        when(boilerRepository.findById(1L)).thenReturn(Optional.of(buildingBoiler));
        when(buildingBoiler.getOwnerType()).thenReturn("BUILDING");
        when(buildingBoiler.getOwnerId()).thenReturn(10L);
        when(buildingRepository.findByEstateIdAndId(estateId, 10L)).thenReturn(Optional.of(building));
        when(boilerRepository.findById(2L)).thenReturn(Optional.of(unitBoiler));
        when(unitBoiler.getOwnerType()).thenReturn("HOUSING_UNIT");
        when(unitBoiler.getOwnerId()).thenReturn(20L);
        when(housingUnitRepository.existsByBuilding_Estate_IdAndId(estateId, 20L)).thenReturn(true);
        when(housingUnitRepository.getReferenceById(20L)).thenReturn(unit);
        when(unit.getBuilding()).thenReturn(otherBuilding);

        service.attachAssetLinks(estateId, transaction, List.of(
                new SaveAssetLinkRequest(AssetType.BOILER, 1L, new BigDecimal("100.00"), null),
                new SaveAssetLinkRequest(AssetType.BOILER, 2L, new BigDecimal("150.00"), null)));

        assertEquals(2, transaction.getAssetLinks().size());
        assertSame(building, transaction.getAssetLinks().get(0).getBuilding());
        assertSame(unit, transaction.getAssetLinks().get(1).getHousingUnit());
        assertSame(otherBuilding, transaction.getAssetLinks().get(1).getBuilding());
    }
}
