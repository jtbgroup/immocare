package com.immocare.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Import batch entity.
 * UC016 Phase 4: estate_id added — import batches are now scoped to an estate.
 */
@Entity
@Table(name = "import_batch")
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(length = 255)
    private String filename;

    @Column(name = "total_rows", nullable = false)
    private int totalRows = 0;

    @Column(name = "imported_count", nullable = false)
    private int importedCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount = 0;

    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    /**
     * Estate this import batch belongs to.
     * UC016 Phase 4 — import batches are scoped to an estate.
     * Nullable for backward compatibility with existing data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_id")
    private Estate estate;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getImportedAt() { return importedAt; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

    public Estate getEstate() { return estate; }
    public void setEstate(Estate estate) { this.estate = estate; }
}
