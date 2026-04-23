package com.immocare.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.EstateMember;
import com.immocare.model.entity.EstateMemberId;

/**
 * Repository for {@link EstateMember}.
 * UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 1).
 */
@Repository
public interface EstateMemberRepository extends JpaRepository<EstateMember, EstateMemberId> {

    List<EstateMember> findByEstateIdOrderByUserUsernameAsc(UUID estateId);

    List<EstateMember> findByUserId(Long userId);

    boolean existsByEstateIdAndUserId(UUID estateId, Long userId);

    Optional<EstateMember> findByEstateIdAndUserId(UUID estateId, Long userId);

    long countByEstateId(UUID estateId);

    long countByEstateIdAndRole(UUID estateId, String role);
}
