package com.immocare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.Lease;
import com.immocare.model.enums.LeaseStatus;

/**
 * Repository for Lease entity.
 * UC016 Phase 3: estate-scoped queries added via housing_unit → building join.
 */
public interface LeaseRepository extends JpaRepository<Lease, Long>, JpaSpecificationExecutor<Lease> {

    // ─── Estate-scoped queries (Phase 3) ─────────────────────────────────────

    /**
     * All leases for a given housing unit within an estate, newest first.
     */
    @Query("""
            SELECT l FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.housingUnit.id = :unitId
            ORDER BY l.startDate DESC
            """)
    List<Lease> findByEstateIdAndUnitId(
            @Param("estateId") UUID estateId,
            @Param("unitId") Long unitId);

    /**
     * Single lease by ID, verified to belong to the given estate.
     */
    @Query("""
            SELECT l FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.id = :id
            """)
    Optional<Lease> findByEstateIdAndId(
            @Param("estateId") UUID estateId,
            @Param("id") Long id);

    /**
     * All active leases within an estate.
     * Used by EstateService.getDashboard() — Phase 3.
     */
    @Query("""
            SELECT l FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.status = 'ACTIVE'
            """)
    List<Lease> findActiveByEstateId(@Param("estateId") UUID estateId);

    /**
     * Counts leases by estate and status.
     * Used by EstateService.getDashboard() to populate activeLeases count.
     */
    long countByHousingUnit_Building_Estate_IdAndStatus(UUID estateId, LeaseStatus status);

    /**
     * Checks whether an active or draft lease already exists for a unit within an estate.
     * Excludes a given lease ID (used during update).
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.housingUnit.id = :unitId
            AND l.status IN :statuses
            AND l.id <> :excludeId
            """)
    boolean existsByEstateIdAndUnitIdAndStatusInExcluding(
            @Param("estateId") UUID estateId,
            @Param("unitId") Long unitId,
            @Param("statuses") List<LeaseStatus> statuses,
            @Param("excludeId") Long excludeId);

    /**
     * Checks whether an active or draft lease already exists for a unit within an estate.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.housingUnit.id = :unitId
            AND l.status IN :statuses
            """)
    boolean existsByEstateIdAndUnitIdAndStatusIn(
            @Param("estateId") UUID estateId,
            @Param("unitId") Long unitId,
            @Param("statuses") List<LeaseStatus> statuses);

    /**
     * All active/draft leases in an estate — used by LeaseService.getAlerts().
     */
    @Query("""
            SELECT l FROM Lease l
            WHERE l.housingUnit.building.estate.id = :estateId
            AND l.status IN :statuses
            """)
    List<Lease> findByEstateIdAndStatusIn(
            @Param("estateId") UUID estateId,
            @Param("statuses") List<LeaseStatus> statuses);

    /**
     * Paginated lease list within an estate, filtered by specification.
     * Delegates to JpaSpecificationExecutor.
     */
    Page<Lease> findAll(org.springframework.data.jpa.domain.Specification<Lease> spec, Pageable pageable);

    // ─── Legacy queries kept for non-estate-scoped internal use ──────────────

    /**
     * Used by non-scoped LeaseController (legacy — kept until Phase 3 migration is complete).
     */
    List<Lease> findByHousingUnitIdOrderByStartDateDesc(Long housingUnitId);

    boolean existsByHousingUnitIdAndStatusIn(Long housingUnitId, List<LeaseStatus> statuses);

    boolean existsByHousingUnitIdAndStatusInAndIdNot(Long housingUnitId, List<LeaseStatus> statuses,
            Long excludeId);

    Optional<Lease> findFirstByHousingUnitIdAndStatus(Long housingUnitId, LeaseStatus status);

    List<Lease> findByStatusIn(List<LeaseStatus> statuses);

    @Query("SELECT l FROM Lease l JOIN FETCH l.tenants t JOIN FETCH t.person WHERE l.status = 'ACTIVE'")
    List<Lease> findAllActiveWithTenants();

    @Query("""
            SELECT l FROM Lease l
            JOIN l.tenants t
            WHERE t.person.id = :personId
            ORDER BY l.startDate DESC
            """)
    List<Lease> findAllByTenantPersonIdOrderByStartDateDesc(@Param("personId") Long personId);
}
