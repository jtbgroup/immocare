package com.immocare.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.immocare.exception.AssetLinkValidationException;
import com.immocare.model.dto.SaveAssetLinkRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.FinancialTransaction;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.PropertyEquipment;
import com.immocare.model.entity.TransactionAssetLink;
import com.immocare.model.enums.AssetType;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.MeterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionAssetLinkService {

    private final BoilerRepository boilerRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;
    private final MeterRepository meterRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;

    public void attachAssetLinks(UUID estateId, FinancialTransaction transaction,
            List<SaveAssetLinkRequest> linkRequests) {
        if (transaction.getEstate() == null || !transaction.getEstate().getId().equals(estateId)) {
            throw new AssetLinkValidationException("Transaction does not belong to this estate.");
        }

        BigDecimal partialSum = linkRequests.stream()
                .map(SaveAssetLinkRequest::amount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (partialSum.compareTo(transaction.getAmount()) > 0) {
            throw new AssetLinkValidationException(
                    "Sum of asset link amounts (" + partialSum + ") exceeds transaction total ("
                            + transaction.getAmount() + ")");
        }

        for (SaveAssetLinkRequest request : linkRequests) {
            TransactionAssetLink link = new TransactionAssetLink();
            link.setTransaction(transaction);
            link.setAssetType(request.assetType());
            link.setAssetId(request.assetId());
            link.setAmount(request.amount());
            link.setNotes(request.notes());
            resolveAssetContext(estateId, link);
            transaction.getAssetLinks().add(link);
        }
    }

    private void resolveAssetContext(UUID estateId, TransactionAssetLink link) {
        PropertyEquipment equipment = switch (link.getAssetType()) {
            case BOILER -> boilerRepository.findById(link.getAssetId()).orElseThrow(
                    () -> new AssetLinkValidationException("Boiler not found: " + link.getAssetId()));
            case FIRE_EXTINGUISHER -> fireExtinguisherRepository.findById(link.getAssetId()).orElseThrow(
                    () -> new AssetLinkValidationException(
                            "Fire extinguisher not found: " + link.getAssetId()));
            case METER -> meterRepository.findById(link.getAssetId()).orElseThrow(
                    () -> new AssetLinkValidationException("Meter not found: " + link.getAssetId()));
        };

        if ("HOUSING_UNIT".equals(equipment.getOwnerType())) {
            if (!housingUnitRepository.existsByBuilding_Estate_IdAndId(estateId, equipment.getOwnerId())) {
                throw new AssetLinkValidationException("Asset does not belong to this estate.");
            }
            HousingUnit unit = housingUnitRepository.getReferenceById(equipment.getOwnerId());
            link.setHousingUnit(unit);
            link.setBuilding(unit.getBuilding());
        } else if ("BUILDING".equals(equipment.getOwnerType())) {
            Building building = buildingRepository.findByEstateIdAndId(estateId, equipment.getOwnerId())
                    .orElseThrow(() -> new AssetLinkValidationException(
                            "Asset does not belong to this estate."));
            link.setBuilding(building);
        } else {
            throw new AssetLinkValidationException("Asset has an unsupported owner.");
        }
    }
}
