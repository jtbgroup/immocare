package com.immocare.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.immocare.model.entity.AppUser;

/**
 * Repository for {@link AppUser}.
 * UC016 Phase 1: role-based methods replaced by isPlatformAdmin queries.
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /** BR-UC007-06: guard against deleting the last platform admin. */
    long countByIsPlatformAdminTrue();
}
