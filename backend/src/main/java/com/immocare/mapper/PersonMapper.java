package com.immocare.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.model.entity.Person;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    /**
     * Convert Person entity to PersonDTO.
     * Derived fields (isOwner, isTenant, ownedBuildings, ownedUnits, leases)
     * are ignored here and populated manually in PersonService.buildFullDTO().
     */
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "ownedBuildings", ignore = true)
    @Mapping(target = "ownedUnits", ignore = true)
    @Mapping(target = "leases", ignore = true)
    PersonDTO toDTO(Person person);

    /**
     * Convert Person entity to PersonSummaryDTO.
     * isOwner and isTenant are ignored here and set manually in the service.
     */
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    PersonSummaryDTO toSummaryDTO(Person person);

    /**
     * Convert CreatePersonRequest to Person entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Person toEntity(CreatePersonRequest request);

    /**
     * Apply UpdatePersonRequest fields onto an existing Person entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePersonRequest request, @MappingTarget Person person);
}