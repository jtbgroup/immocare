package com.immocare.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.immocare.model.dto.UserDTO;
import com.immocare.model.entity.AppUser;

/**
 * MapStruct mapper for {@link AppUser} ↔ {@link UserDTO}.
 * {@code passwordHash} is explicitly ignored and will never appear in any DTO.
 *
 * UC016 Phase 1: replaced {@code role} mapping with {@code isPlatformAdmin}.
 *
 * Note: MapStruct derives property names from getters by stripping the prefix.
 * For boolean getters, {@code isPlatformAdmin()} → property name
 * {@code "platformAdmin"}.
 * The target record field is named {@code isPlatformAdmin} to match the JSON
 * output.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "isPlatformAdmin", source = "platformAdmin")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    UserDTO toDTO(AppUser user);
}