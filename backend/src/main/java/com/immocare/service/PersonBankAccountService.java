package com.immocare.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.PersonNotFoundException;
import com.immocare.model.dto.PersonBankAccountDTO;
import com.immocare.model.dto.SavePersonBankAccountRequest;
import com.immocare.model.entity.Person;
import com.immocare.model.entity.PersonBankAccount;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.PersonRepository;

/**
 * Service for PersonBankAccount management.
 * UC004_ESTATE_PLACEHOLDER Phase 3: operations verify that the person belongs
 * to the active estate.
 */
@Service
@Transactional(readOnly = true)
public class PersonBankAccountService {

    private final PersonBankAccountRepository repo;
    private final PersonRepository personRepository;

    public PersonBankAccountService(PersonBankAccountRepository repo,
            PersonRepository personRepository) {
        this.repo = repo;
        this.personRepository = personRepository;
    }

    /** All IBANs for a person, primary first. */
    public List<PersonBankAccountDTO> getByPerson(UUID estateId, Long personId) {
        verifyPersonBelongsToEstate(estateId, personId);
        return repo.findByPersonIdOrderByPrimaryDescCreatedAtAsc(personId)
                .stream().map(this::toDTO).toList();
    }

    /** Add a new IBAN to a person. */
    @Transactional
    public PersonBankAccountDTO create(UUID estateId, Long personId, SavePersonBankAccountRequest req) {
        verifyPersonBelongsToEstate(estateId, personId);
        Person person = findPersonOrThrow(personId);

        String normalized = normalizeIban(req.iban());
        if (repo.existsByIbanIgnoreCaseAndEstateId(normalized, estateId)) {
            throw new IllegalArgumentException("IBAN already registered: " + normalized);
        }

        PersonBankAccount pba = new PersonBankAccount();
        pba.setPerson(person);
        pba.setEstateId(estateId);
        pba.setIban(normalized);
        pba.setLabel(req.label());
        pba.setPrimary(req.primary());

        PersonBankAccount saved = repo.save(pba);
        if (saved.isPrimary()) {
            repo.clearPrimaryExcept(personId, saved.getId());
        }

        return toDTO(saved);
    }

    /** Update label / primary flag of an existing IBAN. */
    @Transactional
    public PersonBankAccountDTO update(UUID estateId, Long personId, Long id,
            SavePersonBankAccountRequest req) {
        verifyPersonBelongsToEstate(estateId, personId);
        PersonBankAccount pba = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person bank account not found: " + id));

        if (!pba.getPerson().getId().equals(personId)) {
            throw new IllegalArgumentException("Account does not belong to person: " + personId);
        }

        String normalized = normalizeIban(req.iban());
        if (repo.existsByIbanIgnoreCaseAndEstateIdAndIdNot(normalized, estateId, id)) {
            throw new IllegalArgumentException("IBAN already registered: " + normalized);
        }

        pba.setIban(normalized);
        pba.setLabel(req.label());
        pba.setPrimary(req.primary());

        PersonBankAccount saved = repo.save(pba);
        if (saved.isPrimary()) {
            repo.clearPrimaryExcept(personId, saved.getId());
        }

        return toDTO(saved);
    }

    /** Delete an IBAN from a person. */
    @Transactional
    public void delete(UUID estateId, Long personId, Long id) {
        verifyPersonBelongsToEstate(estateId, personId);
        PersonBankAccount pba = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person bank account not found: " + id));
        if (!pba.getPerson().getId().equals(personId)) {
            throw new IllegalArgumentException("Account does not belong to person: " + personId);
        }
        repo.delete(pba);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void verifyPersonBelongsToEstate(UUID estateId, Long personId) {
        if (!personRepository.existsById(personId)) {
            throw new PersonNotFoundException(personId);
        }
        if (!personRepository.existsByEstateIdAndId(estateId, personId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private Person findPersonOrThrow(Long personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }

    private String normalizeIban(String raw) {
        return raw == null ? null : raw.replaceAll("\\s", "").toUpperCase();
    }

    private PersonBankAccountDTO toDTO(PersonBankAccount pba) {
        return new PersonBankAccountDTO(
                pba.getId(),
                pba.getPerson().getId(),
                pba.getIban(),
                pba.getLabel(),
                pba.isPrimary(),
                pba.getCreatedAt());
    }
}
