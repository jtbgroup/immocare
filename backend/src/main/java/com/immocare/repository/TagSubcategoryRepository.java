package com.immocare.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.immocare.model.entity.TagSubcategory;

/**
 * Repository for TagSubcategory entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all queries are now scoped to an estate via
 * tag_category.estate_id.
 */
public interface TagSubcategoryRepository extends JpaRepository<TagSubcategory, Long> {

    // ─── Estate-scoped queries (Phase 4) ─────────────────────────────────────

    List<TagSubcategory> findByCategoryEstateIdOrderByCategoryNameAscNameAsc(UUID estateId);

    List<TagSubcategory> findByCategoryIdAndCategoryEstateId(Long categoryId, UUID estateId);

    boolean existsByCategoryEstateIdAndId(UUID estateId, Long id);

    /**
     * Verifies that a subcategory belongs to the given estate via category → estate
     * chain.
     * Alternative naming for existsByCategoryEstateIdAndId.
     */
    default boolean existsByCategory_EstateIdAndId(UUID estateId, Long id) {
        return existsByCategoryEstateIdAndId(estateId, id);
    }

    /**
     * All subcategories within a category belonging to the given estate, ordered by
     * name.
     * Used by TagSubcategoryService.getAll().
     */
    @Query("""
            SELECT s FROM TagSubcategory s
            WHERE s.category.estate.id = :estateId
            AND s.category.id = :categoryId
            ORDER BY s.name ASC
            """)
    List<TagSubcategory> findByCategory_EstateIdAndCategoryIdOrderByNameAsc(
            @Param("estateId") UUID estateId,
            @Param("categoryId") Long categoryId);

    /**
     * All subcategories within the given estate, ordered by category name then
     * subcategory name.
     * Used by TagSubcategoryService.getAll().
     */
    @Query("""
            SELECT s FROM TagSubcategory s
            WHERE s.category.estate.id = :estateId
            ORDER BY s.category.name ASC, s.name ASC
            """)
    List<TagSubcategory> findByCategory_EstateIdOrderByCategory_NameAscNameAsc(
            @Param("estateId") UUID estateId);

    /**
     * Check if a subcategory name already exists in a category within an estate
     * (case-insensitive).
     * Used by TagSubcategoryService.create() to enforce unique names.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM TagSubcategory s
            WHERE s.category.estate.id = :estateId
            AND s.category.id = :categoryId
            AND LOWER(s.name) = LOWER(:name)
            """)
    boolean existsByCategory_EstateIdAndCategoryIdAndNameIgnoreCase(
            @Param("estateId") UUID estateId,
            @Param("categoryId") Long categoryId,
            @Param("name") String name);

    /**
     * Check if a subcategory name exists in a category within an estate, excluding
     * a specific subcategory (case-insensitive).
     * Used by TagSubcategoryService.update() to enforce unique names during
     * updates.
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM TagSubcategory s
            WHERE s.category.estate.id = :estateId
            AND s.category.id = :categoryId
            AND LOWER(s.name) = LOWER(:name)
            AND s.id != :excludeId
            """)
    boolean existsByCategory_EstateIdAndCategoryIdAndNameIgnoreCaseAndIdNot(
            @Param("estateId") UUID estateId,
            @Param("categoryId") Long categoryId,
            @Param("name") String name,
            @Param("excludeId") Long excludeId);

    /**
     * Count how many financial transactions use this subcategory.
     * Used by TagSubcategoryService to validate deletion and check for direction
     * changes.
     */
    @Query("""
            SELECT COUNT(ft) FROM FinancialTransaction ft
            WHERE ft.subcategory.id = :subcategoryId
            """)
    long countUsage(@Param("subcategoryId") Long subcategoryId);

    // ─── Legacy (kept for backward compatibility with learning rules) ─────────

    List<TagSubcategory> findAllByOrderByCategoryNameAscNameAsc();
}
