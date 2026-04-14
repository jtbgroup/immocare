package com.immocare.repository;

import com.immocare.model.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser}.
 *
 * UC016 Phase 1: replaced {@code countByRole} with {@code countByIsPlatformAdminTrue}.
 */
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    long countByIsPlatformAdminTrue();
}
