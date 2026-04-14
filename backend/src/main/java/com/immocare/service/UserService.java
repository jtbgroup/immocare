package com.immocare.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.CannotDeleteLastAdminException;
import com.immocare.exception.CannotDeleteSelfException;
import com.immocare.exception.EmailTakenException;
import com.immocare.exception.PasswordMismatchException;
import com.immocare.exception.UserNotFoundException;
import com.immocare.exception.UsernameTakenException;
import com.immocare.mapper.UserMapper;
import com.immocare.model.dto.ChangePasswordRequest;
import com.immocare.model.dto.CreateUserRequest;
import com.immocare.model.dto.UpdateUserRequest;
import com.immocare.model.dto.UserDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.repository.UserRepository;

/**
 * Business logic for UC007 — Manage Users.
 *
 * UC016 Phase 1: removed role-based logic; users are now identified as
 * PLATFORM_ADMIN (boolean) or regular users. The "last admin" guard now
 * checks {@code isPlatformAdmin} rather than a string role.
 */
@Service
@Transactional
public class UserService {

    /** BR-UC007-04: password complexity — 8+ chars, 1 upper, 1 lower, 1 digit. */
    private static final Pattern PASSWORD_COMPLEXITY = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------------------------------------------------------
    // US031 — List / Get
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        return userMapper.toDTO(findOrThrow(id));
    }

    // -------------------------------------------------------------------------
    // US032 — Create
    // -------------------------------------------------------------------------

    public UserDTO createUser(CreateUserRequest req) {
        // BR-UC007-02: username uniqueness (case-insensitive)
        if (userRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new UsernameTakenException(req.username());
        }
        // BR-UC007-03: email uniqueness
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new EmailTakenException(req.email());
        }
        // BR-UC007-04: password confirmation + complexity
        validatePassword(req.password(), req.confirmPassword());

        AppUser user = new AppUser();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setIsPlatformAdmin(req.isPlatformAdmin());
        user.setPasswordHash(passwordEncoder.encode(req.password()));

        return userMapper.toDTO(userRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // US033 — Update
    // -------------------------------------------------------------------------

    public UserDTO updateUser(Long id, UpdateUserRequest req) {
        AppUser user = findOrThrow(id);

        // BR-UC007-02: username uniqueness — exclude current user
        if (!user.getUsername().equalsIgnoreCase(req.username())
                && userRepository.existsByUsernameIgnoreCase(req.username())) {
            throw new UsernameTakenException(req.username());
        }
        // BR-UC007-03: email uniqueness — exclude current user
        if (!user.getEmail().equalsIgnoreCase(req.email())
                && userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new EmailTakenException(req.email());
        }

        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setIsPlatformAdmin(req.isPlatformAdmin());

        return userMapper.toDTO(userRepository.save(user));
    }

    // -------------------------------------------------------------------------
    // US034 — Change password
    // -------------------------------------------------------------------------

    public void changePassword(Long id, ChangePasswordRequest req) {
        AppUser user = findOrThrow(id);
        validatePassword(req.newPassword(), req.confirmPassword());
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // US035 — Delete
    // -------------------------------------------------------------------------

    public void deleteUser(Long id, Long currentUserId) {
        AppUser user = findOrThrow(id);

        // BR-UC007-05: cannot delete own account
        if (id.equals(currentUserId)) {
            throw new CannotDeleteSelfException();
        }

        // BR-UC007-06: cannot delete last PLATFORM_ADMIN
        if (user.isPlatformAdmin() && userRepository.countByIsPlatformAdminTrue() <= 1) {
            throw new CannotDeleteLastAdminException();
        }

        userRepository.delete(user);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AppUser findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Validates that passwords match and meet complexity requirements.
     * BR-UC007-04: min 8 chars, 1 uppercase, 1 lowercase, 1 digit.
     */
    private void validatePassword(String password, String confirm) {
        if (!password.equals(confirm)) {
            throw new PasswordMismatchException();
        }
        if (!PASSWORD_COMPLEXITY.matcher(password).matches()) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain at least "
                            + "one uppercase letter, one lowercase letter, and one digit");
        }
    }
}
