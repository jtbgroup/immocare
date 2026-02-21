package com.immocare.service;

import com.immocare.exception.*;
import com.immocare.mapper.UserMapper;
import com.immocare.model.dto.*;
import com.immocare.model.entity.AppUser;
import com.immocare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private AppUser adminUser;
    private UserDTO adminDTO;

    @BeforeEach
    void setUp() {
        adminUser = new AppUser();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("ADMIN");
        adminUser.setPasswordHash("$2a$12$hash");

        adminDTO = new UserDTO(1L, "admin", "admin@example.com", "ADMIN",
                LocalDateTime.now(), LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // getAllUsers
    // -------------------------------------------------------------------------

    @Test
    void getAllUsers_returnsAllMappedDTOs() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(userMapper.toDTO(adminUser)).thenReturn(adminDTO);

        List<UserDTO> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("admin");
    }

    // -------------------------------------------------------------------------
    // getUserById
    // -------------------------------------------------------------------------

    @Test
    void getUserById_existingId_returnsDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userMapper.toDTO(adminUser)).thenReturn(adminDTO);

        UserDTO result = userService.getUserById(1L);

        assertThat(result.username()).isEqualTo("admin");
    }

    @Test
    void getUserById_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // createUser
    // -------------------------------------------------------------------------

    @Test
    void createUser_validRequest_returnsCreatedDTO() {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "new@example.com", "Password1", "Password1", "ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("$2a$12$encoded");
        when(userRepository.save(any())).thenReturn(adminUser);
        when(userMapper.toDTO(adminUser)).thenReturn(adminDTO);

        UserDTO result = userService.createUser(req);

        assertThat(result).isNotNull();
        verify(passwordEncoder).encode("Password1");
    }

    @Test
    void createUser_duplicateUsername_throwsUsernameTakenException() {
        CreateUserRequest req = new CreateUserRequest(
                "admin", "other@example.com", "Password1", "Password1", "ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(UsernameTakenException.class);
    }

    @Test
    void createUser_duplicateEmail_throwsEmailTakenException() {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "admin@example.com", "Password1", "Password1", "ADMIN");

        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(EmailTakenException.class);
    }

    @Test
    void createUser_passwordMismatch_throwsPasswordMismatchException() {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "new@example.com", "Password1", "Different1", "ADMIN");

        when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    void createUser_weakPassword_throwsIllegalArgumentException() {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "new@example.com", "weakpass", "weakpass", "ADMIN");

        when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 8 characters");
    }

    // -------------------------------------------------------------------------
    // updateUser
    // -------------------------------------------------------------------------

    @Test
    void updateUser_validRequest_returnsUpdatedDTO() {
        UpdateUserRequest req = new UpdateUserRequest("admin_updated", "new@example.com", "ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByUsernameIgnoreCase("admin_updated")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(adminUser)).thenReturn(adminUser);
        when(userMapper.toDTO(adminUser)).thenReturn(adminDTO);

        UserDTO result = userService.updateUser(1L, req);

        assertThat(result).isNotNull();
        verify(userRepository).save(adminUser);
    }

    @Test
    void updateUser_sameUsernameAllowed() {
        UpdateUserRequest req = new UpdateUserRequest("admin", "admin@example.com", "ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(false);
        when(userRepository.save(adminUser)).thenReturn(adminUser);
        when(userMapper.toDTO(adminUser)).thenReturn(adminDTO);

        assertThatNoException().isThrownBy(() -> userService.updateUser(1L, req));
    }

    // -------------------------------------------------------------------------
    // changePassword
    // -------------------------------------------------------------------------

    @Test
    void changePassword_validRequest_encodesPassword() {
        ChangePasswordRequest req = new ChangePasswordRequest("NewPass1", "NewPass1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.encode("NewPass1")).thenReturn("$2a$12$newHash");
        when(userRepository.save(adminUser)).thenReturn(adminUser);

        userService.changePassword(1L, req);

        verify(passwordEncoder).encode("NewPass1");
        verify(userRepository).save(adminUser);
    }

    @Test
    void changePassword_mismatch_throwsPasswordMismatchException() {
        ChangePasswordRequest req = new ChangePasswordRequest("NewPass1", "Different1");

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.changePassword(1L, req))
                .isInstanceOf(PasswordMismatchException.class);
    }

    // -------------------------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_validDelete_succeeds() {
        AppUser otherUser = new AppUser();
        otherUser.setRole("ADMIN");

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));
        when(userRepository.countByRole("ADMIN")).thenReturn(2L);

        userService.deleteUser(2L, 1L);

        verify(userRepository).delete(otherUser);
    }

    @Test
    void deleteUser_selfDelete_throwsCannotDeleteSelfException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(CannotDeleteSelfException.class);
    }

    @Test
    void deleteUser_lastAdmin_throwsCannotDeleteLastAdminException() {
        AppUser otherAdmin = new AppUser();
        otherAdmin.setRole("ADMIN");

        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
        when(userRepository.countByRole("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.deleteUser(2L, 1L))
                .isInstanceOf(CannotDeleteLastAdminException.class);
    }

    @Test
    void deleteUser_unknownId_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L, 1L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
