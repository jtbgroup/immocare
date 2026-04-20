package com.immocare.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateHasBuildingsException;
import com.immocare.exception.EstateNameTakenException;
import com.immocare.model.dto.EstateDTOs.CreateEstateRequest;
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstatePlatformConfigDTOs;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.BoilerServiceValidityRuleRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.PlatformConfigRepository;
import com.immocare.repository.UserRepository;
import com.immocare.service.EstateService;

/**
 * Integration tests verifying estate creation, config seeding, and deletion.
 * UC016 Phase 6.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Estate Creation — Integration Tests")
class EstateCreationIntegrationTest {

    @Autowired EstateService estateService;
    @Autowired EstateRepository estateRepository;
    @Autowired EstateMemberRepository estateMemberRepository;
    @Autowired UserRepository userRepository;
    @Autowired BuildingRepository buildingRepository;
    @Autowired PlatformConfigRepository platformConfigRepository;
    @Autowired BoilerServiceValidityRuleRepository boilerRuleRepository;

    // ─── Config seeding ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Creating an estate seeds all default config entries")
    void createEstate_seedsAllDefaultConfigEntries() {
        AppUser admin = createPlatformAdmin();
        authenticateAs(admin);

        EstateDTO dto = estateService.createEstate(
                new CreateEstateRequest("Config Seeding Test " + UUID.randomUUID(), null, null),
                admin.getId());

        List<com.immocare.model.entity.PlatformConfig> configs =
                platformConfigRepository.findByEstateIdOrderByConfigKeyAsc(dto.id());

        assertThat(configs).isNotEmpty();
        assertThat(configs).hasSizeGreaterThanOrEqualTo(EstatePlatformConfigDTOs.DEFAULT_CONFIG.size());

        // Verify key entries are present
        assertThat(configs).anyMatch(c ->
                EstatePlatformConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS.equals(c.getConfigKey()));
        assertThat(configs).anyMatch(c ->
                (EstatePlatformConfigDTOs.KEY_ASSET_MAPPING_PREFIX + "BOILER").equals(c.getConfigKey()));
    }

    @Test
    @DisplayName("Creating an estate seeds the default boiler validity rule")
    void createEstate_seedsDefaultBoilerValidityRule() {
        AppUser admin = createPlatformAdmin();
        authenticateAs(admin);

        EstateDTO dto = estateService.createEstate(
                new CreateEstateRequest("Boiler Rule Seeding Test " + UUID.randomUUID(), null, null),
                admin.getId());

        List<com.immocare.model.entity.BoilerServiceValidityRule> rules =
                boilerRuleRepository.findByEstateIdOrderByValidFromDesc(dto.id());

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getValidityDurationMonths()).isEqualTo(24);
        assertThat(rules.get(0).getValidFrom()).isEqualTo(java.time.LocalDate.of(1900, 1, 1));
    }

    @Test
    @DisplayName("Creating an estate with firstManagerId assigns MANAGER role to that user")
    void createEstate_withFirstManager_assignsManagerRole() {
        AppUser admin = createPlatformAdmin();
        AppUser firstManager = createRegularUser("first_manager_" + UUID.randomUUID());
        authenticateAs(admin);

        EstateDTO dto = estateService.createEstate(
                new CreateEstateRequest(
                        "First Manager Test " + UUID.randomUUID(), null, firstManager.getId()),
                admin.getId());

        assertThat(estateMemberRepository.existsByEstateIdAndUserId(dto.id(), firstManager.getId())).isTrue();

        EstateMember membership = estateMemberRepository
                .findByEstateIdAndUserId(dto.id(), firstManager.getId())
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(EstateRole.MANAGER);
    }

    @Test
    @DisplayName("Creating an estate with a duplicate name throws EstateNameTakenException")
    void createEstate_duplicateName_returns409() {
        AppUser admin = createPlatformAdmin();
        authenticateAs(admin);

        String uniqueName = "Duplicate Name Test " + UUID.randomUUID();
        estateService.createEstate(
                new CreateEstateRequest(uniqueName, null, null), admin.getId());

        assertThatThrownBy(() -> estateService.createEstate(
                new CreateEstateRequest(uniqueName, null, null), admin.getId()))
                .isInstanceOf(EstateNameTakenException.class);
    }

    // ─── Deletion tests ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Deleting an estate that has buildings throws EstateHasBuildingsException")
    void deleteEstate_withBuildings_returns409() {
        AppUser admin = createPlatformAdmin();
        authenticateAs(admin);

        EstateDTO dto = estateService.createEstate(
                new CreateEstateRequest("Delete With Buildings " + UUID.randomUUID(), null, null),
                admin.getId());

        // Add a building to the estate
        Estate estate = estateRepository.findById(dto.id()).orElseThrow();
        Building building = new Building();
        building.setName("Orphan Building");
        building.setStreetAddress("Test Street 1");
        building.setPostalCode("1000");
        building.setCity("Brussels");
        building.setCountry("Belgium");
        building.setEstate(estate);
        buildingRepository.save(building);

        assertThatThrownBy(() -> estateService.deleteEstate(dto.id()))
                .isInstanceOf(EstateHasBuildingsException.class)
                .hasMessageContaining("1 building");
    }

    @Test
    @DisplayName("Deleting an empty estate also removes its members")
    void deleteEstate_empty_cascadesMembers() {
        AppUser admin = createPlatformAdmin();
        AppUser member = createRegularUser("cascade_member_" + UUID.randomUUID());
        authenticateAs(admin);

        EstateDTO dto = estateService.createEstate(
                new CreateEstateRequest(
                        "Cascade Delete Test " + UUID.randomUUID(), null, member.getId()),
                admin.getId());

        UUID estateId = dto.id();
        assertThat(estateMemberRepository.existsByEstateIdAndUserId(estateId, member.getId())).isTrue();

        estateService.deleteEstate(estateId);

        assertThat(estateRepository.existsById(estateId)).isFalse();
        assertThat(estateMemberRepository.existsByEstateIdAndUserId(estateId, member.getId())).isFalse();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser createPlatformAdmin() {
        AppUser user = new AppUser();
        user.setUsername("pa_" + UUID.randomUUID());
        user.setEmail("pa_" + UUID.randomUUID() + "@test.com");
        user.setIsPlatformAdmin(true);
        user.setPasswordHash("$2b$12$dummy_hash_for_tests_only_xxxxx");
        return userRepository.save(user);
    }

    private AppUser createRegularUser(String username) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setIsPlatformAdmin(false);
        user.setPasswordHash("$2b$12$dummy_hash_for_tests_only_xxxxx");
        return userRepository.save(user);
    }

    private void authenticateAs(AppUser user) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
