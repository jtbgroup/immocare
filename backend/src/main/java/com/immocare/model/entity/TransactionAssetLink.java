package com.immocare.model.entity;

import com.immocare.model.enums.AssetType;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Links a financial transaction to a physical asset (BOILER / FIRE_EXTINGUISHER / METER).
 * housing_unit and building are resolved server-side from the asset's ownership — never
 * provided directly by the client.
 * amount is optional: null = full transaction amount attributed to this asset.
 * When multiple links exist, partial amounts can be entered and their sum must not
 * exceed the transaction total (BR-UC015-15).
 */
@Entity
@Table(name = "transaction_asset_link")
public class TransactionAssetLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private FinancialTransaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private AssetType assetType;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    /** Resolved server-side from the asset's ownership — never set by the client. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "housing_unit_id")
    private HousingUnit housingUnit;

    /** Resolved server-side from the asset's ownership — never set by the client. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    /** Partial amount for this asset link. Null = full transaction amount. */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FinancialTransaction getTransaction() { return transaction; }
    public void setTransaction(FinancialTransaction transaction) { this.transaction = transaction; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public Long getAssetId() { return assetId; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }

    public HousingUnit getHousingUnit() { return housingUnit; }
    public void setHousingUnit(HousingUnit housingUnit) { this.housingUnit = housingUnit; }

    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
