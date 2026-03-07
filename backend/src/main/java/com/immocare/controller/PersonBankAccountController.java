package com.immocare.controller;

import com.immocare.model.dto.PersonBankAccountDTO;
import com.immocare.model.dto.SavePersonBankAccountRequest;
import com.immocare.service.PersonBankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/persons/{personId}/bank-accounts")
@PreAuthorize("hasRole('ADMIN')")
public class PersonBankAccountController {

    private final PersonBankAccountService service;

    public PersonBankAccountController(PersonBankAccountService service) {
        this.service = service;
    }

    /** GET /api/v1/persons/{personId}/bank-accounts */
    @GetMapping
    public List<PersonBankAccountDTO> getAll(@PathVariable Long personId) {
        return service.getByPerson(personId);
    }

    /** POST /api/v1/persons/{personId}/bank-accounts */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonBankAccountDTO create(@PathVariable Long personId,
                                       @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.create(personId, req);
    }

    /** PUT /api/v1/persons/{personId}/bank-accounts/{id} */
    @PutMapping("/{id}")
    public PersonBankAccountDTO update(@PathVariable Long personId,
                                       @PathVariable Long id,
                                       @Valid @RequestBody SavePersonBankAccountRequest req) {
        return service.update(personId, id, req);
    }

    /** DELETE /api/v1/persons/{personId}/bank-accounts/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long personId, @PathVariable Long id) {
        service.delete(personId, id);
    }
}
