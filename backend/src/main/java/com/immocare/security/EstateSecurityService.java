package com.immocare.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.immocare.model.entity.AppUser;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.EstateMemberRepository;

/**
 * Spring Security helper used in {@code @PreAuthorize} SpEL expressions.
 * Registered as {@code "security"} so expressions can use {@code @security.isPlatformAdmin()}, etc.
 *
 * UC004_ESTATE_PLACEHOLDER — Manage Estates (Phase 1).
 */
@Service("security")
public class EstateSecurityService {

    private final EstateMemberRepository estateMemberRepository;

    public EstateSecurityService(EstateMemberRepository estateMemberRepository) {
        this.estateMemberRepository = estateMemberRepository;
    }

    /**
     * Returns true if the current user has the platform-admin flag set.
     */
    public boolean isPlatformAdmin() {
        AppUser user = currentUser();
        return user != null && user.isPlatformAdmin();
    }

    /**
     * Returns true if the current user is PLATFORM_ADMIN
     * or has the MANAGER role in the given estate.
     */
    public boolean isManagerOf(UUID estateId) {
        if (isPlatformAdmin()) return true;
        AppUser user = currentUser();
        if (user == null) return false;
        return estateMemberRepository.findByEstateIdAndUserId(estateId, user.getId())
                .map(m -> m.getRole() == EstateRole.MANAGER)
                .orElse(false);
    }

    /**
     * Returns true if the current user is PLATFORM_ADMIN
     * or has any role (MANAGER or VIEWER) in the given estate.
     */
    public boolean isMemberOf(UUID estateId) {
        if (isPlatformAdmin()) return true;
        AppUser user = currentUser();
        if (user == null) return false;
        return estateMemberRepository.existsByEstateIdAndUserId(estateId, user.getId());
    }

    /**
     * Returns the authenticated {@link AppUser}, or null if not authenticated.
     */
    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUser)) {
            return null;
        }
        return (AppUser) auth.getPrincipal();
    }
}
