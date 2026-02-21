package com.immocare.mapper;

import com.immocare.model.dto.UserDTO;
import com.immocare.model.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link AppUser} ↔ {@link UserDTO}.
 * {@code passwordHash} is explicitly ignored and will never appear in any DTO.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id",        source = "id")
    @Mapping(target = "username",  source = "username")
    @Mapping(target = "email",     source = "email")
    @Mapping(target = "role",      source = "role")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserDTO toDTO(AppUser user);
}
