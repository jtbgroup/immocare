package com.immocare.repository;

import com.immocare.model.entity.TagSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagSubcategoryRepository extends JpaRepository<TagSubcategory, Long> {

    List<TagSubcategory> findByCategoryIdOrderByNameAsc(Long categoryId);

    List<TagSubcategory> findAllByOrderByCategoryNameAscNameAsc();

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);

    @Query("SELECT COUNT(t) FROM FinancialTransaction t WHERE t.subcategory.id = :id")
    long countUsage(@Param("id") Long id);
}
