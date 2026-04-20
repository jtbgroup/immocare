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
 * Business logic for Person management.
 * UC016 Phase 3: all operations are scoped to an estate.
 */
@Service
@Transactional(readOnly = true)
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

        /**
         * Returns a paginated list of persons within the estate.
         * Supports optional full-text search across name, email, and national ID.
         */
        public Page<PersonSummaryDTO> getAll(UUID estateId, String search, Pageable pageable) {
                Page<Person> page = (search != null && !search.isBlank())
                                ? personRepository.searchByEstate(estateId, search, pageable)
                                : personRepository.findByEstateIdOrderByLastNameAsc(estateId, pageable);

                return page.map(p -> {
                        PersonSummaryDTO dto = personMapper.toSummaryDTO(p);
                        dto.setOwner(isOwner(p.getId()));
                        dto.setTenant(leaseTenantRepository.existsByPersonId(p.getId()));
                        return dto;
                });
        }

        /**
         * Person picker — returns up to 10 matches for autocomplete fields.
         * Requires at least 2 characters.
         */
        public List<PersonSummaryDTO> searchForPicker(UUID estateId, String query) {
                if (query == null || query.trim().length() < 2) {
                        return List.of();
                }
                return personRepository.searchForPickerByEstate(
                                estateId, query.trim(), PageRequest.of(0, 10));
        }

        /**
         * Returns the full person detail DTO including owned buildings, owned units,
         * active leases, and registered IBANs.
         */
        public PersonDTO getById(UUID estateId, Long id) {
                Person person = findPersonInEstate(estateId, id);
                return buildFullDTO(person);
        }

        // ─── Commands ─────────────────────────────────────────────────────────────

        @Transactional
        public PersonDTO create(UUID estateId, CreatePersonRequest request) {
                Estate estate = estateRepository.findById(estateId)
                                .orElseThrow(() -> new EstateNotFoundException(estateId));

                // National ID uniqueness within estate (if provided)
                if (request.getNationalId() != null && !request.getNationalId().isBlank()) {
                        if (personRepository.existsByEstateIdAndNationalIdIgnoreCase(
                                        estateId, request.getNationalId())) {
                                throw new IllegalArgumentException(
                                                "A person with this national ID already exists in this estate: "
                                                                + request.getNationalId());
                        }
                }

                Person person = personMapper.toEntity(request);
                person.setEstate(estate);
                return buildFullDTO(personRepository.save(person));
        }

        @Transactional
        public PersonDTO update(UUID estateId, Long id, UpdatePersonRequest request) {
                Person person = findPersonInEstate(estateId, id);

                // National ID uniqueness within estate, excluding self
                if (request.getNationalId() != null && !request.getNationalId().isBlank()) {
                        if (personRepository.existsByEstateIdAndNationalIdIgnoreCaseAndIdNot(
                                        estateId, request.getNationalId(), id)) {
                                throw new IllegalArgumentException(
                                                "A person with this national ID already exists in this estate: "
                                                                + request.getNationalId());
                        }
                }

                personMapper.updateEntity(request, person);
                return buildFullDTO(personRepository.save(person));
        }

        @Transactional
        public void delete(UUID estateId, Long id) {
                Person person = findPersonInEstate(estateId, id);

                // Collect referencing entities
                List<String> ownedBuildings = buildingRepository.findByOwner_Id(id)
                                .stream()
                                .map(b -> b.getName() + " (" + b.getCity() + ")")
                                .collect(Collectors.toList());

                List<String> ownedUnits = housingUnitRepository.findByOwner_Id(id)
                                .stream()
                                .map(u -> u.getUnitNumber() + " — " + u.getBuilding().getName())
                                .collect(Collectors.toList());

                List<String> activeLeases = leaseTenantRepository.findByPersonId(id)
                                .stream()
                                .filter(lt -> {
                                        var status = lt.getLease().getStatus();
                                        return status == com.immocare.model.enums.LeaseStatus.ACTIVE
                                                        || status == com.immocare.model.enums.LeaseStatus.DRAFT;
                                })
                                .map(lt -> "Lease #" + lt.getLease().getId()
                                                + " — " + lt.getLease().getHousingUnit().getUnitNumber())
                                .collect(Collectors.toList());

                if (!ownedBuildings.isEmpty() || !ownedUnits.isEmpty() || !activeLeases.isEmpty()) {
                        throw new PersonReferencedException(id, ownedBuildings, ownedUnits, activeLeases);
                }

                personRepository.delete(person);
        }

        // ─── Helpers ──────────────────────────────────────────────────────────────

        /**
         * Loads a person and verifies they belong to the given estate.
         * Throws {@link PersonNotFoundException} if the person does not exist at all,
         * or {@link EstateAccessDeniedException} if they belong to a different estate.
         */
        private Person findPersonInEstate(UUID estateId, Long personId) {
                Person person = personRepository.findById(personId)
                                .orElseThrow(() -> new PersonNotFoundException(personId));
                if (!person.getEstate().getId().equals(estateId)) {
                        throw new EstateAccessDeniedException();
                }
                return person;
        }

        private boolean isOwner(Long personId) {
                return buildingRepository.findByOwner_Id(personId).size() > 0
                                || housingUnitRepository.countByOwner_Id(personId) > 0;
        }

        /**
         * Builds the full {@link PersonDTO} with all related data populated.
         */
        private PersonDTO buildFullDTO(Person person) {
                PersonDTO dto = personMapper.toDTO(person);

                // Derived flags
                dto.setOwner(isOwner(person.getId()));
                dto.setTenant(leaseTenantRepository.existsByPersonId(person.getId()));

                // Owned buildings
                List<PersonDTO.OwnedBuildingDTO> ownedBuildings = buildingRepository
                                .findByOwner_Id(person.getId())
                                .stream()
                                .map(b -> new PersonDTO.OwnedBuildingDTO(b.getId(), b.getName(), b.getCity()))
                                .collect(Collectors.toList());
                dto.setOwnedBuildings(ownedBuildings);

                // Owned units
                List<PersonDTO.OwnedUnitDTO> ownedUnits = housingUnitRepository
                                .findByOwner_Id(person.getId())
                                .stream()
                                .map(u -> new PersonDTO.OwnedUnitDTO(
                                                u.getId(),
                                                u.getUnitNumber(),
                                                u.getBuilding().getId(),
                                                u.getBuilding().getName()))
                                .collect(Collectors.toList());
                dto.setOwnedUnits(ownedUnits);

                // Lease memberships
                List<PersonDTO.TenantLeaseDTO> leases = leaseTenantRepository
                                .findByPersonId(person.getId())
                                .stream()
                                .map(lt -> new PersonDTO.TenantLeaseDTO(
                                                lt.getLease().getId(),
                                                lt.getLease().getHousingUnit().getId(),
                                                lt.getLease().getHousingUnit().getUnitNumber(),
                                                lt.getLease().getHousingUnit().getBuilding().getName(),
                                                lt.getLease().getStatus().name()))
                                .collect(Collectors.toList());
                dto.setLeases(leases);

                // Registered IBANs
                List<PersonBankAccountDTO> bankAccounts = personBankAccountRepository
                                .findByPersonIdOrderByPrimaryDescCreatedAtAsc(person.getId())
                                .stream()
                                .map(pba -> new PersonBankAccountDTO(
                                                pba.getId(),
                                                person.getId(),
                                                pba.getIban(),
                                                pba.getLabel(),
                                                pba.isPrimary(),
                                                pba.getCreatedAt()))
                                .collect(Collectors.toList());
                dto.setBankAccounts(bankAccounts);

                return dto;
        }
}