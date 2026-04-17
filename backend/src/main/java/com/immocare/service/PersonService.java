package com.immocare.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.exception.PersonReferencedException;
import com.immocare.mapper.PersonMapper;
import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.PersonBankAccountDTO;
import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseTenantRepository;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.PersonRepository;

/**
 * Service for Person management.
 * UC016 Phase 3: all operations are now scoped to an estate.
 */
@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final LeaseTenantRepository leaseTenantRepository;
    private final PersonBankAccountRepository personBankAccountRepository;
    private final EstateRepository estateRepository;

    public PersonService(PersonRepository personRepository,
            PersonMapper personMapper,
            BuildingRepository buildingRepository,
            HousingUnitRepository housingUnitRepository,
            LeaseTenantRepository leaseTenantRepository,
            PersonBankAccountRepository personBankAccountRepository,
            EstateRepository estateRepository) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.leaseTenantRepository = leaseTenantRepository;
        this.personBankAccountRepository = personBankAccountRepository;
        this.estateRepository = estateRepository;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PersonSummaryDTO> getAll(UUID estateId, String search, Pageable pageable) {
        Page<Person> page = (search != null && !search.isBlank())
                ? personRepository.searchByEstate(estateId, search.trim(), pageable)
                : personRepository.findByEstateIdOrderByLastNameAsc(estateId, pageable);

        return page.map(p -> {
            PersonSummaryDTO dto = personMapper.toSummaryDTO(p);
            enrichSummaryFlags(dto, p.getId());
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> searchForPicker(UUID estateId, String q) {
        if (q == null || q.trim().length() < 2) return List.of();
        List<PersonSummaryDTO> results = personRepository
                .searchForPickerByEstate(estateId, q.trim(), PageRequest.of(0, 10));
        results.forEach(dto -> enrichSummaryFlags(dto, dto.getId()));
        return results;
    }

    @Transactional(readOnly = true)
    public PersonDTO getById(UUID estateId, Long id) {
        verifyPersonBelongsToEstate(estateId, id);
        Person person = findOrThrow(id);
        return buildFullDTO(person);
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public PersonDTO create(UUID estateId, CreatePersonRequest request) {
        Estate estate = findEstateOrThrow(estateId);
        normalizeCountry(request);
        normalizeNationalId(request);
        validateNationalIdUniqueness(estateId, request.getNationalId(), null);

        Person person = personMapper.toEntity(request);
        person.setEstate(estate);
        person = personRepository.save(person);
        return buildFullDTO(person);
    }

    public PersonDTO update(UUID estateId, Long id, UpdatePersonRequest request) {
        verifyPersonBelongsToEstate(estateId, id);
        Person person = findOrThrow(id);

        normalizeCountryOnUpdate(request);
        normalizeNationalIdOnUpdate(request);
        validateNationalIdUniquenessOnUpdate(estateId, request.getNationalId(), id);

        personMapper.updateEntity(request, person);
        person = personRepository.save(person);
        return buildFullDTO(person);
    }

    public void delete(UUID estateId, Long id) {
        verifyPersonBelongsToEstate(estateId, id);
        Person person = findOrThrow(id);

        List<String> buildings = buildingRepository.findByOwnerId(id).stream()
                .map(b -> b.getName() + " (" + b.getCity() + ")")
                .collect(Collectors.toList());

        List<String> units = housingUnitRepository.findByOwnerId(id).stream()
                .map(u -> u.getUnitNumber() + " — " + u.getBuilding().getName())
                .collect(Collectors.toList());

        if (!buildings.isEmpty() || !units.isEmpty()) {
            throw new PersonReferencedException(id, buildings, units, List.of());
        }

        personRepository.delete(person);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Verifies that the person belongs to the given estate.
     * Throws {@link PersonNotFoundException} if the person does not exist,
     * or {@link EstateAccessDeniedException} if it exists but belongs to another estate.
     */
    private void verifyPersonBelongsToEstate(UUID estateId, Long personId) {
        if (!personRepository.existsById(personId)) {
            throw new PersonNotFoundException(personId);
        }
        if (!personRepository.existsByEstateIdAndId(estateId, personId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Person findOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
    }

    private Estate findEstateOrThrow(UUID estateId) {
        return estateRepository.findById(estateId)
                .orElseThrow(() -> new EstateNotFoundException(estateId));
    }

    private PersonDTO buildFullDTO(Person person) {
        PersonDTO dto = personMapper.toDTO(person);

        boolean isOwner = buildingRepository.existsByOwnerId(person.getId())
                || housingUnitRepository.existsByOwnerId(person.getId());
        boolean isTenant = leaseTenantRepository.existsByPersonId(person.getId());
        dto.setOwner(isOwner);
        dto.setTenant(isTenant);

        List<PersonDTO.OwnedBuildingDTO> buildings = buildingRepository.findByOwnerId(person.getId())
                .stream()
                .map(b -> new PersonDTO.OwnedBuildingDTO(b.getId(), b.getName(), b.getCity()))
                .collect(Collectors.toList());
        dto.setOwnedBuildings(buildings);

        List<PersonDTO.OwnedUnitDTO> units = housingUnitRepository.findByOwnerId(person.getId())
                .stream()
                .map(u -> new PersonDTO.OwnedUnitDTO(
                        u.getId(), u.getUnitNumber(),
                        u.getBuilding().getId(), u.getBuilding().getName()))
                .collect(Collectors.toList());
        dto.setOwnedUnits(units);

        List<PersonDTO.TenantLeaseDTO> leases = leaseTenantRepository
                .findByPersonId(person.getId())
                .stream()
                .map(lt -> {
                    var lease = lt.getLease();
                    var unit = lease.getHousingUnit();
                    return new PersonDTO.TenantLeaseDTO(
                            lease.getId(), unit.getId(), unit.getUnitNumber(),
                            unit.getBuilding().getName(), lease.getStatus().name());
                })
                .collect(Collectors.toList());
        dto.setLeases(leases);

        List<PersonBankAccountDTO> bankAccounts = personBankAccountRepository
                .findByPersonIdOrderByPrimaryDescCreatedAtAsc(person.getId())
                .stream()
                .map(pba -> new PersonBankAccountDTO(
                        pba.getId(), pba.getPerson().getId(),
                        pba.getIban(), pba.getLabel(),
                        pba.isPrimary(), pba.getCreatedAt()))
                .collect(Collectors.toList());
        dto.setBankAccounts(bankAccounts);

        return dto;
    }

    private void enrichSummaryFlags(PersonSummaryDTO dto, Long personId) {
        boolean isOwner = buildingRepository.existsByOwnerId(personId)
                || housingUnitRepository.existsByOwnerId(personId);
        boolean isTenant = leaseTenantRepository.existsByPersonId(personId);
        dto.setOwner(isOwner);
        dto.setTenant(isTenant);
    }

    private void normalizeNationalId(CreatePersonRequest request) {
        if (request.getNationalId() != null && request.getNationalId().isBlank()) {
            request.setNationalId(null);
        }
    }

    private void normalizeNationalIdOnUpdate(UpdatePersonRequest request) {
        if (request.getNationalId() != null && request.getNationalId().isBlank()) {
            request.setNationalId(null);
        }
    }

    private void validateNationalIdUniqueness(UUID estateId, String nationalId, Long excludeId) {
        if (nationalId == null || nationalId.isBlank()) return;
        if (personRepository.existsByEstateIdAndNationalIdIgnoreCase(estateId, nationalId.trim())) {
            throw new IllegalArgumentException(
                    "A person with national ID '" + nationalId + "' already exists.");
        }
    }

    private void validateNationalIdUniquenessOnUpdate(UUID estateId, String nationalId, Long currentId) {
        if (nationalId == null || nationalId.isBlank()) return;
        if (personRepository.existsByEstateIdAndNationalIdIgnoreCaseAndIdNot(
                estateId, nationalId.trim(), currentId)) {
            throw new IllegalArgumentException(
                    "A person with national ID '" + nationalId + "' already exists.");
        }
    }

    private void normalizeCountry(CreatePersonRequest request) {
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            request.setCountry("Belgium");
        }
    }

    private void normalizeCountryOnUpdate(UpdatePersonRequest request) {
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            request.setCountry("Belgium");
        }
    }
}
