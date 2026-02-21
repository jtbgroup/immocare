package com.immocare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.*;
import com.immocare.model.dto.*;
import com.immocare.model.entity.AppUser;
import com.immocare.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    private UserDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleDTO = new UserDTO(1L, "admin", "admin@example.com", "ADMIN",
                LocalDateTime.now(), LocalDateTime.now());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_returnsOkWithList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_existing_returnsOk() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleDTO);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_unknown_returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/users
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_validBody_returnsCreated() throws Exception {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "new@example.com", "Password1", "Password1", "ADMIN");
        when(userService.createUser(any())).thenReturn(sampleDTO);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateUsername_returns409() throws Exception {
        CreateUserRequest req = new CreateUserRequest(
                "admin", "new@example.com", "Password1", "Password1", "ADMIN");
        when(userService.createUser(any())).thenThrow(new UsernameTakenException("admin"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_duplicateEmail_returns409() throws Exception {
        CreateUserRequest req = new CreateUserRequest(
                "new_user", "admin@example.com", "Password1", "Password1", "ADMIN");
        when(userService.createUser(any())).thenThrow(new EmailTakenException("admin@example.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_missingUsername_returns400WithFieldError() throws Exception {
        // username blank — Bean Validation fires before service
        String body = """
                {"username":"","email":"x@x.com","password":"Password1",
                 "confirmPassword":"Password1","role":"ADMIN"}""";

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_validBody_returnsOk() throws Exception {
        UpdateUserRequest req = new UpdateUserRequest("admin_new", "new@example.com", "ADMIN");
        when(userService.updateUser(eq(1L), any())).thenReturn(sampleDTO);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/users/{id}/password
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "ADMIN")
    void changePassword_validBody_returnsNoContent() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest("NewPass1", "NewPass1");
        doNothing().when(userService).changePassword(eq(1L), any());

        mockMvc.perform(patch("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/users/{id}
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_valid_returnsNoContent() throws Exception {
        AppUser currentUser = new AppUser();
        currentUser.setUsername("current_admin");
        currentUser.setRole("ADMIN");
        currentUser.setEmail("current@example.com");
        currentUser.setPasswordHash("$2a$12$x");

        doNothing().when(userService).deleteUser(eq(2L), any());

        mockMvc.perform(delete("/api/v1/users/2")
                        .with(user(currentUser)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_selfDelete_returns403() throws Exception {
        AppUser currentUser = new AppUser();
        currentUser.setUsername("admin");
        currentUser.setRole("ADMIN");
        currentUser.setEmail("admin@example.com");
        currentUser.setPasswordHash("$2a$12$x");

        doThrow(new CannotDeleteSelfException())
                .when(userService).deleteUser(eq(1L), any());

        mockMvc.perform(delete("/api/v1/users/1")
                        .with(user(currentUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_lastAdmin_returns409() throws Exception {
        AppUser currentUser = new AppUser();
        currentUser.setUsername("admin");
        currentUser.setRole("ADMIN");
        currentUser.setEmail("admin@example.com");
        currentUser.setPasswordHash("$2a$12$x");

        doThrow(new CannotDeleteLastAdminException())
                .when(userService).deleteUser(eq(2L), any());

        mockMvc.perform(delete("/api/v1/users/2")
                        .with(user(currentUser)))
                .andExpect(status().isConflict());
    }
}
