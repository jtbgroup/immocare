package com.immocare.repository;

import com.immocare.model.entity.PebScoreHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for PEB score history.
 * UC004 - Manage PEB Scores.
 */
@Repository
public interface PebScoreRepository extends JpaRepository<PebScoreHistory, Long> {

    /** All scores for a unit, newest first (BR-UC004-02). */
    List<PebScoreHistory> findByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);

    /** The single most recent score for a unit (current score). */
    Optional<PebScoreHistory> findFirstByHousingUnitIdOrderByScoreDateDesc(Long housingUnitId);

    /** Used to block housing unit deletion when PEB data exists. */
    boolean existsByHousingUnitId(Long housingUnitId);
}
