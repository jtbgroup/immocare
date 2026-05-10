package com.immocare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.PebScoreHistory;

/**
 * Repository for PEB score history.
 * UC005 - Manage PEB Scores.
 */
@Repository
public interface PebScoreRepository extends JpaRepository<PebScoreHistory, Long> {

    /** All scores for a unit, newest first (BR-UC005-02). */
    List<PebScoreHistory> findByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);

    /** The single most recent score for a unit (current score). */
    Optional<PebScoreHistory> findFirstByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);

    /** Used to block housing unit deletion when PEB data exists. */
    boolean existsByHousingUnitId(Long housingUnitId);

    @Query("""
            SELECT p FROM PebScoreHistory p
            WHERE p.housingUnit.building.estate.id = :estateId
              AND p.scoreDate = (
                  SELECT MAX(p2.scoreDate)
                  FROM PebScoreHistory p2
                  WHERE p2.housingUnit.id = p.housingUnit.id
              )
              AND p.validUntil IS NOT NULL
            """)
    List<PebScoreHistory> findCurrentScoresWithValidUntilByEstateId(@Param("estateId") UUID estateId);

}
