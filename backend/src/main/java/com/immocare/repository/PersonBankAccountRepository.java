package com.immocare.repository;

import com.immocare.model.entity.PersonBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonBankAccountRepository extends JpaRepository<PersonBankAccount, Long> {

    /** All IBANs for a given person, primary first. */
    List<PersonBankAccount> findByPersonIdOrderByPrimaryDescCreatedAtAsc(Long personId);

    /** Lookup by IBAN for reconciliation (case-insensitive). */
    @Query("SELECT pba FROM PersonBankAccount pba WHERE UPPER(pba.iban) = UPPER(:iban)")
    Optional<PersonBankAccount> findByIban(@Param("iban") String iban);

    /** Check uniqueness before create/update. */
    boolean existsByIbanIgnoreCaseAndIdNot(String iban, Long excludeId);

    boolean existsByIbanIgnoreCase(String iban);

    /** Count primary accounts for a person (should never exceed 1). */
    long countByPersonIdAndPrimaryTrue(Long personId);

    /** Reset the primary flag for all accounts of a person except the given one. */
    @Query("UPDATE PersonBankAccount pba SET pba.primary = false WHERE pba.person.id = :personId AND pba.id <> :excludeId")
    @org.springframework.data.jpa.repository.Modifying
    void clearPrimaryExcept(@Param("personId") Long personId, @Param("excludeId") Long excludeId);
}
