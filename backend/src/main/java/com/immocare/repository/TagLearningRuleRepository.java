package com.immocare.repository;

import com.immocare.model.entity.TagLearningRule;
import com.immocare.model.enums.TagMatchField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagLearningRuleRepository extends JpaRepository<TagLearningRule, Long> {

    @Query("""
        SELECT r FROM TagLearningRule r
        WHERE r.matchField = :field
        AND LOWER(r.matchValue) = LOWER(:value)
        AND r.confidence >= :minConf
        ORDER BY r.confidence DESC
        """)
    List<TagLearningRule> findSuggestions(
        @Param("field") TagMatchField field,
        @Param("value") String value,
        @Param("minConf") int minConf);

    Optional<TagLearningRule> findByMatchFieldAndMatchValueIgnoreCaseAndSubcategoryId(
        TagMatchField field, String value, Long subcategoryId);
}
