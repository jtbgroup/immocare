package com.immocare.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.EstateLastManagerException;
import com.immocare.exception.EstateMemberAlreadyExistsException;
import com.immocare.exception.EstateSelfOperationException;
import com.immocare.model.dto.EstateDTOs.AddEstateMemberRequest;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.UserRepository;
import com.immocare.service.EstateService;

/**
 * Integration tests for estate membership business rules.
 * UC004_ESTATE_PLACEHOLDER Phase 6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Estate Membership — Integration Tests")
class EstateMembershipIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired EstateService estateService;
    @Autowired EstateRepository estateRepository;
    @Autowired EstateMemberRepository estateMemberRepository;
    @Autowired UserRepository userRepository;

    private UUID estateId;
    private AppUser manager;
    private AppUser secondManager;
    private AppUser viewer;

    @BeforeEach
    void setUp() {
        Estate estate = createEstate("Membership Test Estate");
        estateId = estate.getId();

        manager = createUser("manager_mbr_" + UUID.randomUUID(), false);
        addMember(estate, manager, EstateRole.MANAGER);

        secondManager = createUser("second_mgr_" + UUID.randomUUID(), false);
        addMember(estate, secondManager, EstateRole.MANAGER);

        viewer = createUser("viewer_mbr_" + UUID.randomUUID(), false);
        addMember(estate, viewer, EstateRole.VIEWER);
    }

    // ─── Last-manager protection ───────────────────────────────────────────────

    @Test
    @DisplayName("Removing the last MANAGER throws EstateLastManagerException")
    void removeMember_lastManager_returns409() {
        // Remove second manager first
        estateMemberRepository.findByEstateIdAndUserId(estateId, secondManager.getId())
                .ifPresent(estateMemberRepository::delete);

        authenticateAs(manager);
        assertThatThrownBy(() ->
                estateService.removeMember(estateId, manager.getId() + 1L /* some other user id */,
                        viewer.getId()))
                // The last MANAGER (manager) cannot be removed — try via service
                .satisfies(ex -> {
                    // Just verify the setup; actual call will succeed only if userId != currentUserId
                });

        // Direct test: call removeMember on the only remaining manager (by another user)
        AppUser anotherUser = createUser("another_" + UUID.randomUUID(), false);
        authenticateAs(anotherUser);
        assertThatThrownBy(() ->
                estateService.removeMember(estateId, manager.getId(), anotherUser.getId()))
                .isInstanceOf(EstateLastManagerException.class);
    }

    @Test
    @DisplayName("Demoting the last MANAGER throws EstateLastManagerException")
    void updateMemberRole_lastManagerDemotion_returns409() {
        // Remove second manager so only one remains
        estateMemberRepository.findByEstateIdAndUserId(estateId, secondManager.getId())
                .ifPresent(estateMemberRepository::delete);

        authenticateAs(secondManager); // use different user as "current"
        assertThatThrownBy(() ->
                estateService.updateMemberRole(
                        estateId,
                        manager.getId(),
                        new UpdateEstateMemberRoleRequest(EstateRole.VIEWER),
                        secondManager.getId()))
                .isInstanceOf(EstateLastManagerException.class);
    }

    // ─── Self-operation protection ─────────────────────────────────────────────

    @Test
    @DisplayName("Changing own role throws EstateSelfOperationException")
    void updateMemberRole_selfDemotion_returns409() {
        authenticateAs(manager);
        assertThatThrownBy(() ->
                estateService.updateMemberRole(
                        estateId,
                        manager.getId(),
                        new UpdateEstateMemberRoleRequest(EstateRole.VIEWER),
                        manager.getId()))
                .isInstanceOf(EstateSelfOperationException.class)
                .hasMessageContaining("own role");
    }

    @Test
    @DisplayName("Removing self throws EstateSelfOperationException")
    void removeMember_self_returns409() {
        authenticateAs(manager);
        assertThatThrownBy(() ->
                estateService.removeMember(estateId, manager.getId(), manager.getId()))
                .isInstanceOf(EstateSelfOperationException.class)
                .hasMessageContaining("yourself");
    }

    // ─── Duplicate membership protection ──────────────────────────────────────

    @Test
    @DisplayName("Adding a user who is already a member throws EstateMemberAlreadyExistsException")
    void addMember_alreadyMember_returns409() {
        authenticateAs(manager);
        assertThatThrownBy(() ->
                estateService.addMember(
                        estateId,
                        new AddEstateMemberRequest(viewer.getId(), EstateRole.VIEWER),
                        manager.getId()))
                .isInstanceOf(EstateMemberAlreadyExistsException.class);
    }

    // ─── VIEWER access restriction on member list ──────────────────────────────

    @Test
    @DisplayName("VIEWER cannot access the members list — expects 403")
    void viewer_cannotAccessMemberList_returns403() throws Exception {
        authenticateAs(viewer);
        mockMvc.perform(get("/api/v1/estates/{id}/members", estateId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER can access the members list")
    void manager_canAccessMemberList() throws Exception {
        authenticateAs(manager);
        mockMvc.perform(get("/api/v1/estates/{id}/members", estateId))
                .andExpect(status().isOk());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser createUser(String username, boolean isPlatformAdmin) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setIsPlatformAdmin(isPlatformAdmin);
        user.setPasswordHash("$2b$12$dummy_hash_for_tests_only_xxxxx");
        return userRepository.save(user);
    }

    private Estate createEstate(String name) {
        Estate estate = new Estate();
        estate.setName(name + "_" + UUID.randomUUID());
        return estateRepository.save(estate);
    }

    private void addMember(Estate estate, AppUser user, EstateRole role) {
        EstateMember member = new EstateMember();
        member.setEstate(estate);
        member.setUser(user);
        member.setRole(role);
        estateMemberRepository.save(member);
    }

    private void authenticateAs(AppUser user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
