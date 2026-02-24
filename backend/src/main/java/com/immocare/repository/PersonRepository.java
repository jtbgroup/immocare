package com.immocare.repository;

import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.entity.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

    Optional<Person> findByNationalIdIgnoreCase(String nationalId);

    boolean existsByNationalIdIgnoreCaseAndIdNot(String nationalId, Long id);

    boolean existsByNationalIdIgnoreCase(String nationalId);

    /**
     * Search for person picker: matches on last name, first name (combined), or national ID.
     * Returns max results controlled by Pageable.
     */
    @Query("""
        SELECT new com.immocare.model.dto.PersonSummaryDTO(
            p.id, p.lastName, p.firstName, p.city, p.nationalId, false, false
        )
        FROM Person p
        WHERE LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(CONCAT(p.lastName, ' ', p.firstName)) LIKE LOWER(CONCAT('%', :query, '%'))
           OR (p.nationalId IS NOT NULL AND LOWER(p.nationalId) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY p.lastName, p.firstName
    """)
    List<PersonSummaryDTO> searchForPicker(@Param("query") String query, Pageable pageable);

    /**
     * Paginated list search across name fields and national ID.
     */
    @Query("""
        SELECT p FROM Person p
        WHERE LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
           OR (p.nationalId IS NOT NULL AND LOWER(p.nationalId) LIKE LOWER(CONCAT('%', :search, '%')))
           OR (p.email IS NOT NULL AND LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.lastName, p.firstName
    """)
    Page<Person> searchPersons(@Param("search") String search, Pageable pageable);
}
