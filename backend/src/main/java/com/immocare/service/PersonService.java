package com.immocare.service;

import com.immocare.exception.PersonNotFoundException;
import com.immocare.exception.PersonReferencedException;
import com.immocare.mapper.PersonMapper;
import com.immocare.model.dto.*;
import com.immocare.model.entity.Person;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PersonRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final PersonMapper personMapper;

    public PersonService(PersonRepository personRepository,
                         BuildingRepository buildingRepository,
                         HousingUnitRepository housingUnitRepository,
                         PersonMapper personMapper) {
        this.personRepository = personRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.personMapper = personMapper;
    }

    // ---- List & Search ----

    public Page<PersonSummaryDTO> getAll(String search, Pageable pageable) {
        Page<Person> page;
        if (search != null && !search.isBlank()) {
            page = personRepository.searchPersons(search.trim(), pageable);
        } else {
            page = personRepository.findAll(pageable);
        }
        return page.map(p -> {
            PersonSummaryDTO dto = personMapper.toSummaryDTO(p);
            enrichSummaryFlags(dto, p.getId());
            return dto;
        });
    }

    public List<PersonSummaryDTO> searchForPicker(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        List<PersonSummaryDTO> results = personRepository.searchForPicker(
                query.trim(), PageRequest.of(0, 10));
        // Enrich isOwner/isTenant flags
        results.forEach(dto -> enrichSummaryFlags(dto, dto.getId()));
        return results;
    }

    // ---- Get by ID ----

    public PersonDTO getById(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));
        return buildFullDTO(person);
    }

    // ---- Create ----

    @Transactional
    public PersonDTO create(CreatePersonRequest request) {
        validateNationalIdUniqueness(request.getNationalId(), null);
        normalizeCountry(request);

        Person person = personMapper.toEntity(request);
        Person saved = personRepository.save(person);
        return buildFullDTO(saved);
    }

    // ---- Update ----

    @Transactional
    public PersonDTO update(Long id, UpdatePersonRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));

        validateNationalIdUniquenessOnUpdate(request.getNationalId(), id);
        normalizeCountryOnUpdate(request);

        personMapper.updateEntity(request, person);
        Person saved = personRepository.save(person);
        return buildFullDTO(saved);
    }

    // ---- Delete ----

    @Transactional
    public void delete(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new PersonNotFoundException(id));

        List<String> ownedBuildings = buildingRepository.findByOwnerId(id).stream()
                .map(b -> b.getName() + " (" + b.getCity() + ")")
                .collect(Collectors.toList());

        List<String> ownedUnits = housingUnitRepository.findByOwnerId(id).stream()
                .map(u -> u.getBuilding().getName() + " - Unit " + u.getUnitNumber())
                .collect(Collectors.toList());

        // Lease tenant check is a stub until UC010 is implemented
        List<String> activeLeases = new ArrayList<>();
        // When UC010 is implemented, add: leaseTenantRepository.findByPersonId(id) ...

        if (!ownedBuildings.isEmpty() || !ownedUnits.isEmpty() || !activeLeases.isEmpty()) {
            throw new PersonReferencedException(id, ownedBuildings, ownedUnits, activeLeases);
        }

        personRepository.delete(person);
    }

    // ---- Private helpers ----

    private PersonDTO buildFullDTO(Person person) {
        PersonDTO dto = personMapper.toDTO(person);

        // Derived flags
        boolean isOwner = buildingRepository.existsByOwnerId(person.getId())
                       || housingUnitRepository.existsByOwnerId(person.getId());
        dto.setOwner(isOwner);
        dto.setTenant(false); // Will be updated when UC010 is implemented

        // Related buildings owned
        List<PersonDTO.OwnedBuildingDTO> buildings = buildingRepository.findByOwnerId(person.getId())
                .stream()
                .map(b -> new PersonDTO.OwnedBuildingDTO(b.getId(), b.getName(), b.getCity()))
                .collect(Collectors.toList());
        dto.setOwnedBuildings(buildings);

        // Related units owned
        List<PersonDTO.OwnedUnitDTO> units = housingUnitRepository.findByOwnerId(person.getId())
                .stream()
                .map(u -> new PersonDTO.OwnedUnitDTO(
                        u.getId(), u.getUnitNumber(),
                        u.getBuilding().getId(), u.getBuilding().getName()))
                .collect(Collectors.toList());
        dto.setOwnedUnits(units);

        // Leases (stub until UC010)
        dto.setLeases(new ArrayList<>());

        return dto;
    }

    private void enrichSummaryFlags(PersonSummaryDTO dto, Long personId) {
        boolean isOwner = buildingRepository.existsByOwnerId(personId)
                       || housingUnitRepository.existsByOwnerId(personId);
        dto.setOwner(isOwner);
        dto.setTenant(false); // Will be updated when UC010 is implemented
    }

    private void validateNationalIdUniqueness(String nationalId, Long excludeId) {
        if (nationalId == null || nationalId.isBlank()) return;
        if (personRepository.existsByNationalIdIgnoreCase(nationalId.trim())) {
            throw new IllegalArgumentException(
                    "A person with national ID '" + nationalId + "' already exists.");
        }
    }

    private void validateNationalIdUniquenessOnUpdate(String nationalId, Long currentId) {
        if (nationalId == null || nationalId.isBlank()) return;
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
