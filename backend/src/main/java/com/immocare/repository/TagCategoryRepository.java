package com.immocare.repository;

import com.immocare.model.entity.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for TagCategory entity.
 * UC004_ESTATE_PLACEHOLDER Phase 4: all queries are now scoped to an estate.
 */
public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {

    // ─── Estate-scoped queries (Phase 4) ─────────────────────────────────────

    List<TagCategory> findByEstateIdOrderByNameAsc(UUID estateId);

    boolean existsByEstateIdAndNameIgnoreCase(UUID estateId, String name);

    boolean existsByEstateIdAndNameIgnoreCaseAndIdNot(UUID estateId, String name, Long id);

    boolean existsByEstateIdAndId(UUID estateId, Long id);

    // ─── Legacy (kept for backward compatibility) ─────────────────────────────

    List<TagCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
