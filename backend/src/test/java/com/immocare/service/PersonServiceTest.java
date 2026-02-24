package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

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

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    PersonRepository personRepository;
    @Mock
    BuildingRepository buildingRepository;
    @Mock
    HousingUnitRepository housingUnitRepository;
    @Mock
    PersonMapper personMapper;

    @InjectMocks
    PersonService personService;

    private Person samplePerson;

    @BeforeEach
    void setUp() {
        samplePerson = new Person();
        samplePerson.setId(1L);
        samplePerson.setLastName("Dupont");
        samplePerson.setFirstName("Jean");
        samplePerson.setCity("Brussels");
        samplePerson.setCountry("Belgium");
    }

    // ---- getAll ----

    @Test
    @DisplayName("getAll without search returns all persons paged")
    void getAll_noSearch_returnsPage() {
        Page<Person> page = new PageImpl<>(List.of(samplePerson));
        when(personRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(buildingRepository.existsByOwnerId(any())).thenReturn(false);
        when(housingUnitRepository.existsByOwnerId(any())).thenReturn(false);
        when(personMapper.toSummaryDTO(any())).thenReturn(new PersonSummaryDTO());

        Page<PersonSummaryDTO> result = personService.getAll(null, PageRequest.of(0, 20));
        assertThat(result).isNotNull();
    }

    // ---- getById ----

    @Test
    @DisplayName("getById existing person returns DTO")
    void getById_existing_returnsDTO() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(samplePerson));
        PersonDTO dto = new PersonDTO();
        dto.setId(1L);
        when(personMapper.toDTO(samplePerson)).thenReturn(dto);
        when(buildingRepository.existsByOwnerId(1L)).thenReturn(false);
        when(housingUnitRepository.existsByOwnerId(1L)).thenReturn(false);
        when(buildingRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(housingUnitRepository.findByOwnerId(1L)).thenReturn(List.of());

        PersonDTO result = personService.getById(1L);
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getById unknown person throws PersonNotFoundException")
    void getById_unknown_throwsNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.getById(99L))
                .isInstanceOf(PersonNotFoundException.class);
    }

    // ---- create ----

    @Test
    @DisplayName("create person sets country to Belgium when null")
    void create_setsDefaultCountry() {
        CreatePersonRequest request = new CreatePersonRequest();
        request.setLastName("Test");
        request.setFirstName("User");
        request.setCountry(null);

        when(personMapper.toEntity(request)).thenReturn(samplePerson);
        when(personRepository.save(samplePerson)).thenReturn(samplePerson);
        PersonDTO dto = new PersonDTO();
        when(personMapper.toDTO(samplePerson)).thenReturn(dto);
        when(buildingRepository.existsByOwnerId(any())).thenReturn(false);
        when(housingUnitRepository.existsByOwnerId(any())).thenReturn(false);
        when(buildingRepository.findByOwnerId(any())).thenReturn(List.of());
        when(housingUnitRepository.findByOwnerId(any())).thenReturn(List.of());

        personService.create(request);

        assertThat(request.getCountry()).isEqualTo("Belgium");
    }

    // ---- update ----

    @Test
    @DisplayName("update existing person saves changes")
    void update_existing_savesChanges() {
        UpdatePersonRequest request = new UpdatePersonRequest();
        request.setLastName("Dupont");
        request.setFirstName("Jean-Marc");

        when(personRepository.findById(1L)).thenReturn(Optional.of(samplePerson));
        when(personRepository.save(samplePerson)).thenReturn(samplePerson);
        PersonDTO dto = new PersonDTO();
        dto.setId(1L);
        when(personMapper.toDTO(samplePerson)).thenReturn(dto);
        when(buildingRepository.existsByOwnerId(any())).thenReturn(false);
        when(housingUnitRepository.existsByOwnerId(any())).thenReturn(false);
        when(buildingRepository.findByOwnerId(any())).thenReturn(List.of());
        when(housingUnitRepository.findByOwnerId(any())).thenReturn(List.of());

        PersonDTO result = personService.update(1L, request);
        assertThat(result).isNotNull();
        verify(personMapper).updateEntity(eq(request), eq(samplePerson));
    }

    // ---- delete ----

    @Test
    @DisplayName("delete unreferenced person succeeds")
    void delete_unreferenced_succeeds() {
        when(personRepository.findById(1L)).thenReturn(Optional.of(samplePerson));
        when(buildingRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(housingUnitRepository.findByOwnerId(1L)).thenReturn(List.of());

        assertThatCode(() -> personService.delete(1L)).doesNotThrowAnyException();

        // Cast to JpaRepository to resolve ambiguity between delete(T) and
        // delete(Specification<T>)
        verify((JpaRepository<Person, Long>) personRepository).delete(samplePerson);
    }

    @Test
    @DisplayName("delete person who owns a building throws PersonReferencedException")
    void delete_ownsBuilding_throwsReferenced() {
        com.immocare.model.entity.Building building = new com.immocare.model.entity.Building();
        building.setId(10L);
        building.setName("Résidence Soleil");
        building.setCity("Brussels");

        when(personRepository.findById(1L)).thenReturn(Optional.of(samplePerson));
        when(buildingRepository.findByOwnerId(1L)).thenReturn(List.of(building));
        when(housingUnitRepository.findByOwnerId(1L)).thenReturn(List.of());

        assertThatThrownBy(() -> personService.delete(1L))
                .isInstanceOf(PersonReferencedException.class);

        // Cast to JpaRepository to resolve ambiguity
        verify((JpaRepository<Person, Long>) personRepository, never()).delete(any(Person.class));
    }

    @Test
    @DisplayName("delete unknown person throws PersonNotFoundException")
    void delete_unknown_throwsNotFound() {
        when(personRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personService.delete(99L))
                .isInstanceOf(PersonNotFoundException.class);
    }

    // ---- searchForPicker ----

    @Test
    @DisplayName("searchForPicker returns empty list when query < 2 chars")
    void searchForPicker_shortQuery_returnsEmpty() {
        List<PersonSummaryDTO> result = personService.searchForPicker("d");
        assertThat(result).isEmpty();
        verify(personRepository, never()).searchForPicker(any(), any());
    }

    @Test
    @DisplayName("searchForPicker returns up to 10 results")
    void searchForPicker_validQuery_returnsResults() {
        PersonSummaryDTO dto = new PersonSummaryDTO(1L, "Dupont", "Jean", "Brussels", null, false, false);
        when(personRepository.searchForPicker(eq("du"), any())).thenReturn(List.of(dto));
        when(buildingRepository.existsByOwnerId(1L)).thenReturn(false);
        when(housingUnitRepository.existsByOwnerId(1L)).thenReturn(false);

        List<PersonSummaryDTO> result = personService.searchForPicker("du");
        assertThat(result).hasSize(1);
    }
}