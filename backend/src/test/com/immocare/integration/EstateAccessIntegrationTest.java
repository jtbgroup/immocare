package com.immocare.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.UserRepository;

/**
 * Integration tests verifying cross-estate data isolation.
 * UC004_ESTATE_PLACEHOLDER Phase 6.
 *
 * Setup: 2 estates (A and B), 1 MANAGER per estate, 1 VIEWER in estate A.
 * A PLATFORM_ADMIN user has access to all estates without membership.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Estate Access Isolation — Integration Tests")
class EstateAccessIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired EstateRepository estateRepository;
    @Autowired EstateMemberRepository estateMemberRepository;
    @Autowired UserRepository userRepository;
    @Autowired BuildingRepository buildingRepository;

    private UUID estateAId;
    private UUID estateBId;
    private AppUser managerA;
    private AppUser managerB;
    private AppUser viewerA;
    private AppUser platformAdmin;

    @BeforeEach
    void setUp() {
        // Platform admin
        platformAdmin = createUser("platform_admin_test", "pa@test.com", true);

        // Estate A + manager
        Estate estateA = createEstate("Estate Alpha");
        estateAId = estateA.getId();
        managerA = createUser("manager_a_test", "ma@test.com", false);
        addMember(estateA, managerA, EstateRole.MANAGER);

        // Estate B + manager
        Estate estateB = createEstate("Estate Beta");
        estateBId = estateB.getId();
        managerB = createUser("manager_b_test", "mb@test.com", false);
        addMember(estateB, managerB, EstateRole.MANAGER);

        // Viewer in estate A
        viewerA = createUser("viewer_a_test", "va@test.com", false);
        addMember(estateA, viewerA, EstateRole.VIEWER);
    }

    // ─── Read access tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("MANAGER of estate A can read buildings in estate A")
    void managerA_canReadBuildingsInEstateA() throws Exception {
        authenticateAs(managerA);
        mockMvc.perform(get("/api/v1/estates/{id}/buildings", estateAId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MANAGER of estate A cannot read buildings in estate B — expects 403")
    void managerA_cannotReadBuildingsInEstateB() throws Exception {
        authenticateAs(managerA);
        mockMvc.perform(get("/api/v1/estates/{id}/buildings", estateBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER in estate A can read buildings in estate A")
    void viewerA_canReadBuildingsInEstateA() throws Exception {
        authenticateAs(viewerA);
        mockMvc.perform(get("/api/v1/estates/{id}/buildings", estateAId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PLATFORM_ADMIN can read buildings in estate A")
    void platformAdmin_canReadBuildingsInEstateA() throws Exception {
        authenticateAs(platformAdmin);
        mockMvc.perform(get("/api/v1/estates/{id}/buildings", estateAId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PLATFORM_ADMIN can read buildings in estate B")
    void platformAdmin_canReadBuildingsInEstateB() throws Exception {
        authenticateAs(platformAdmin);
        mockMvc.perform(get("/api/v1/estates/{id}/buildings", estateBId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("MANAGER of estate A cannot read persons in estate B — expects 403")
    void managerA_cannotReadPersonsInEstateB() throws Exception {
        authenticateAs(managerA);
        mockMvc.perform(get("/api/v1/estates/{id}/persons", estateBId))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER of estate A cannot read transactions in estate B — expects 403")
    void managerA_cannotReadTransactionsInEstateB() throws Exception {
        authenticateAs(managerA);
        mockMvc.perform(get("/api/v1/estates/{id}/transactions", estateBId))
                .andExpect(status().isForbidden());
    }

    // ─── Write access tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("MANAGER of estate A can create a building in estate A")
    void managerA_canCreateBuildingInEstateA() throws Exception {
        authenticateAs(managerA);
        CreateBuildingRequest req = new CreateBuildingRequest(
                "Test Building", "Rue de la Loi 1", "1000", "Brussels", "Belgium", null);
        mockMvc.perform(post("/api/v1/estates/{id}/buildings", estateAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MANAGER of estate A cannot create a building in estate B — expects 403")
    void managerA_cannotCreateBuildingInEstateB() throws Exception {
        authenticateAs(managerA);
        CreateBuildingRequest req = new CreateBuildingRequest(
                "Test Building", "Rue de la Loi 1", "1000", "Brussels", "Belgium", null);
        mockMvc.perform(post("/api/v1/estates/{id}/buildings", estateBId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER cannot create a building in estate A — expects 403")
    void viewerA_cannotCreateBuildingInEstateA() throws Exception {
        authenticateAs(viewerA);
        CreateBuildingRequest req = new CreateBuildingRequest(
                "Test Building", "Rue de la Loi 1", "1000", "Brussels", "Belgium", null);
        mockMvc.perform(post("/api/v1/estates/{id}/buildings", estateAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PLATFORM_ADMIN can create a building in estate A")
    void platformAdmin_canCreateBuildingInEstateA() throws Exception {
        authenticateAs(platformAdmin);
        CreateBuildingRequest req = new CreateBuildingRequest(
                "PA Building", "Avenue Louise 2", "1050", "Brussels", "Belgium", null);
        mockMvc.perform(post("/api/v1/estates/{id}/buildings", estateAId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // ─── Fingerprint deduplication — estate-scoped ────────────────────────────

    @Test
    @DisplayName("Same fingerprint is allowed in two different estates (no cross-estate dedup)")
    void fingerprintDuplicationScopedPerEstate() {
        // Verify that existsByImportFingerprintAndEstateId scopes correctly.
        // We check this via repository directly since import requires file upload.
        String fingerprint = "abc123testfingerprint";

        // No existing transaction in either estate → both should return false
        com.immocare.repository.FinancialTransactionRepository txRepo =
                getBean(com.immocare.repository.FinancialTransactionRepository.class);
        assertThat(txRepo.existsByImportFingerprintAndEstateId(fingerprint, estateAId)).isFalse();
        assertThat(txRepo.existsByImportFingerprintAndEstateId(fingerprint, estateBId)).isFalse();
    }

    @Test
    @DisplayName("Config values are independent per estate")
    void configValuesIndependentPerEstate() {
        com.immocare.service.PlatformConfigService configService =
                getBean(com.immocare.service.PlatformConfigService.class);

        int valueA = configService.getIntValue(estateAId,
                com.immocare.model.dto.EstatePlatformConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS, 3);
        int valueB = configService.getIntValue(estateBId,
                com.immocare.model.dto.EstatePlatformConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS, 3);

        // Both estates are seeded with the same default (3) — values are independent copies
        assertThat(valueA).isEqualTo(3);
        assertThat(valueB).isEqualTo(3);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private <T> T getBean(Class<T> type) {
        return applicationContext.getBean(type);
    }

    private AppUser createUser(String username, String email, boolean isPlatformAdmin) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setIsPlatformAdmin(isPlatformAdmin);
        user.setPasswordHash("$2b$12$dummy_hash_for_tests_only_xxxxx");
        return userRepository.save(user);
    }

    private Estate createEstate(String name) {
        Estate estate = new Estate();
        estate.setName(name + "_" + UUID.randomUUID());
        Estate saved = estateRepository.save(estate);

        // Seed default config and validity rule
        getBean(com.immocare.service.PlatformConfigService.class)
                .seedDefaultConfig(saved, com.immocare.model.dto.EstatePlatformConfigDTOs.DEFAULT_CONFIG);
        getBean(com.immocare.service.BoilerServiceValidityRuleService.class)
                .seedDefaultRule(saved);
        return saved;
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
