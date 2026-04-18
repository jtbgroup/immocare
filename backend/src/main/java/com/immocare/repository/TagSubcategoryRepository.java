package com.immocare.repository;

import com.immocare.model.entity.TagSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TagSubcategory entity.
 * UC016 Phase 4: estate-scoped queries added via category join.
 */
public interface TagSubcategoryRepository extends JpaRepository<TagSubcategory, Long> {

    // ─── Estate-scoped queries (Phase 4) ─────────────────────────────────────

    List<TagSubcategory> findByCategory_EstateIdOrderByCategory_NameAscNameAsc(UUID estateId);

    List<TagSubcategory> findByCategory_EstateIdAndCategoryIdOrderByNameAsc(UUID estateId, Long categoryId);

    boolean existsByCategory_EstateIdAndCategoryIdAndNameIgnoreCase(UUID estateId, Long categoryId, String name);

    boolean existsByCategory_EstateIdAndCategoryIdAndNameIgnoreCaseAndIdNot(
            UUID estateId, Long categoryId, String name, Long id);

    /**
     * Verifies that a subcategory belongs to a category that is in the given estate.
     */
    boolean existsByCategory_EstateIdAndId(UUID estateId, Long id);

    // ─── Legacy queries (kept for non-scoped internal use) ───────────────────

    List<TagSubcategory> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<TagSubcategory> findAllByOrderByCategoryNameAscNameAsc();

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t WHERE t.subcategory.id = :id")
    long countUsage(@Param("id") Long id);
}
