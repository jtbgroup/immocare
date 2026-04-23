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

import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.entity.Person;

/**
 * Repository for Person entity.
 * UC004_ESTATE_PLACEHOLDER Phase 3: all queries are now scoped to an estate.
 */
public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

    // ─── Estate-scoped queries (Phase 3) ─────────────────────────────────────

    /**
     * Full-text search within an estate across name fields, email and national ID.
     */
    @Query("""
            SELECT p FROM Person p
            WHERE p.estate.id = :estateId
            AND (LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(CONCAT(p.lastName,  ' ', p.firstName)) LIKE LOWER(CONCAT('%', :search, '%'))
              OR (p.nationalId IS NOT NULL AND LOWER(p.nationalId) LIKE LOWER(CONCAT('%', :search, '%')))
              OR (p.email IS NOT NULL AND LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))))
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    Page<Person> searchByEstate(
            @Param("estateId") UUID estateId,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Returns all persons in an estate, ordered by last name then first name.
     */
    Page<Person> findByEstateIdOrderByLastNameAsc(UUID estateId, Pageable pageable);

    /**
     * Person picker search within an estate — returns at most {@code pageable.pageSize} results.
     */
    @Query("""
            SELECT new com.immocare.model.dto.PersonSummaryDTO(
                p.id, p.lastName, p.firstName, p.city, p.nationalId, false, false
            )
            FROM Person p
            WHERE p.estate.id = :estateId
            AND (LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
              OR LOWER(CONCAT(p.lastName,  ' ', p.firstName)) LIKE LOWER(CONCAT('%', :query, '%'))
              OR (p.nationalId IS NOT NULL AND LOWER(p.nationalId) LIKE LOWER(CONCAT('%', :query, '%'))))
            ORDER BY p.lastName ASC, p.firstName ASC
            """)
    List<PersonSummaryDTO> searchForPickerByEstate(
            @Param("estateId") UUID estateId,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Checks national ID uniqueness within an estate (used during create).
     */
    boolean existsByEstateIdAndNationalIdIgnoreCase(UUID estateId, String nationalId);

    /**
     * Checks national ID uniqueness within an estate, excluding a given person (used during update).
     */
    boolean existsByEstateIdAndNationalIdIgnoreCaseAndIdNot(UUID estateId, String nationalId, Long id);

    /**
     * Verifies that a person belongs to the given estate.
     */
    boolean existsByEstateIdAndId(UUID estateId, Long personId);

    /**
     * Counts persons in an estate.
     */
    long countByEstateId(UUID estateId);

    // ─── Legacy queries kept for non-estate-scoped internal use ──────────────

    /**
     * Used by TransactionImportService for IBAN-based lease suggestion.
     * Estate scoping for this path is deferred to Phase 4.
     */
    Optional<Person> findByNationalIdIgnoreCase(String nationalId);
}
