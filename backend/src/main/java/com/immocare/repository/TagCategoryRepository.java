package com.immocare.repository;

import com.immocare.model.entity.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagCategoryRepository extends JpaRepository<TagCategory, Long> {

    List<TagCategory> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
