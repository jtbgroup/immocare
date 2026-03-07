package com.immocare.service;

import com.immocare.model.dto.PersonBankAccountDTO;
import com.immocare.model.dto.SavePersonBankAccountRequest;
import com.immocare.model.entity.Person;
import com.immocare.model.entity.PersonBankAccount;
import com.immocare.repository.PersonBankAccountRepository;
import com.immocare.repository.PersonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<PersonBankAccountDTO> getByPerson(Long personId) {
        return repo.findByPersonIdOrderByPrimaryDescCreatedAtAsc(personId)
                .stream().map(this::toDTO).toList();
    }

    /** Add a new IBAN to a person. */
    @Transactional
    public PersonBankAccountDTO create(Long personId, SavePersonBankAccountRequest req) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + personId));

        String normalized = normalizeIban(req.iban());
        if (repo.existsByIbanIgnoreCase(normalized)) {
            throw new IllegalArgumentException("IBAN already registered: " + normalized);
        }

        PersonBankAccount pba = new PersonBankAccount();
        pba.setPerson(person);
        pba.setIban(normalized);
        pba.setLabel(req.label());
        pba.setPrimary(req.primary());

        PersonBankAccount saved = repo.save(pba);

        // Enforce single primary: clear other primaries if this one is primary
        if (saved.isPrimary()) {
            repo.clearPrimaryExcept(personId, saved.getId());
        }

        return toDTO(saved);
    }

    /** Update label / primary flag of an existing IBAN. */
    @Transactional
    public PersonBankAccountDTO update(Long personId, Long id, SavePersonBankAccountRequest req) {
        PersonBankAccount pba = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person bank account not found: " + id));

        if (!pba.getPerson().getId().equals(personId)) {
            throw new IllegalArgumentException("Account does not belong to person: " + personId);
        }

        String normalized = normalizeIban(req.iban());
        if (repo.existsByIbanIgnoreCaseAndIdNot(normalized, id)) {
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
    public void delete(Long personId, Long id) {
        PersonBankAccount pba = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person bank account not found: " + id));
        if (!pba.getPerson().getId().equals(personId)) {
            throw new IllegalArgumentException("Account does not belong to person: " + personId);
        }
        repo.delete(pba);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Strips spaces and uppercases the IBAN. */
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
                pba.getCreatedAt()
        );
    }
}
