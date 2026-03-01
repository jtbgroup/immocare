package com.immocare.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.PersonNotFoundException;
import com.immocare.exception.PersonReferencedException;
import com.immocare.mapper.PersonMapper;
import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PersonRepository;

@Service
@Transactional
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;

    public PersonService(PersonRepository personRepository,
            PersonMapper personMapper,
            BuildingRepository buildingRepository,
            HousingUnitRepository housingUnitRepository) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    @Transactional(readOnly = true)
    public Page<PersonSummaryDTO> getAll(String search, Pageable pageable) {
        Page<Person> page = (search != null && !search.isBlank())
                ? personRepository.searchPersons(search.trim(), pageable)
                : personRepository.findAll(pageable);
        return page.map(p -> {
            PersonSummaryDTO dto = personMapper.toSummaryDTO(p);
            enrichSummaryFlags(dto, p.getId());
            return dto;
        });
    }

    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> searchForPicker(String q) {
        if (q == null || q.trim().length() < 2)
            return List.of();
        List<PersonSummaryDTO> results = personRepository
                .searchForPicker(q.trim(), PageRequest.of(0, 10));
        results.forEach(dto -> enrichSummaryFlags(dto, dto.getId()));
        return results;
    }

    @Transactional(readOnly = true)
    public PersonDTO getById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        return buildFullDTO(person);
    }

    public PersonDTO create(CreatePersonRequest request) {
        normalizeCountry(request);
        normalizeNationalId(request);
        validateNationalIdUniqueness(request.getNationalId(), null);
        Person person = personMapper.toEntity(request);
        person = personRepository.save(person);
        return buildFullDTO(person);
    }

    public PersonDTO update(Long id, UpdatePersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        normalizeCountryOnUpdate(request);
        normalizeNationalIdOnUpdate(request);
        validateNationalIdUniquenessOnUpdate(request.getNationalId(), id);
        personMapper.updateEntity(request, person);
        person = personRepository.save(person);
        return buildFullDTO(person);
    }

    public void delete(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));

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

    private PersonDTO buildFullDTO(Person person) {
        PersonDTO dto = personMapper.toDTO(person);

        boolean isOwner = buildingRepository.existsByOwnerId(person.getId())
                || housingUnitRepository.existsByOwnerId(person.getId());
        dto.setOwner(isOwner);
        dto.setTenant(false); // Will be updated when UC010 is implemented

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

        dto.setLeases(new ArrayList<>());

        return dto;
    }

    private void enrichSummaryFlags(PersonSummaryDTO dto, Long personId) {
        boolean isOwner = buildingRepository.existsByOwnerId(personId)
                || housingUnitRepository.existsByOwnerId(personId);
        dto.setOwner(isOwner);
        dto.setTenant(false); // Will be updated when UC010 is implemented
    }

    /**
     * Converts a blank national ID to null so that multiple persons without a
     * national ID can coexist (PostgreSQL UNIQUE allows multiple NULLs).
     */
    private void normalizeNationalId(CreatePersonRequest request) {
        if (request.getNationalId() != null && request.getNationalId().isBlank()) {
            request.setNationalId(null);
        }
    }

    /**
     * Converts a blank national ID to null for update requests.
     */
    private void normalizeNationalIdOnUpdate(UpdatePersonRequest request) {
        if (request.getNationalId() != null && request.getNationalId().isBlank()) {
            request.setNationalId(null);
        }
    }

    private void validateNationalIdUniqueness(String nationalId, Long excludeId) {
        if (nationalId == null || nationalId.isBlank())
            return;
        if (personRepository.existsByNationalIdIgnoreCase(nationalId.trim())) {
            throw new IllegalArgumentException(
                    "A person with national ID '" + nationalId + "' already exists.");
        }
    }

    private void validateNationalIdUniquenessOnUpdate(String nationalId, Long currentId) {
        if (nationalId == null || nationalId.isBlank())
            return;
        if (personRepository.existsByNationalIdIgnoreCaseAndIdNot(nationalId.trim(), currentId)) {
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