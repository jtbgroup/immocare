package com.immocare.mapper;

import com.immocare.model.dto.PersonDTO;
import com.immocare.model.dto.PersonSummaryDTO;
import com.immocare.model.dto.CreatePersonRequest;
import com.immocare.model.dto.UpdatePersonRequest;
import com.immocare.model.entity.Person;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PersonMapper {

    PersonDTO toDTO(Person person);

    PersonSummaryDTO toSummaryDTO(Person person);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Person toEntity(CreatePersonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdatePersonRequest request, @MappingTarget Person person);
}
