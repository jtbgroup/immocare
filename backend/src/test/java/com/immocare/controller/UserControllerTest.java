package com.immocare.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.immocare.exception.CannotDeleteLastAdminException;
import com.immocare.exception.CannotDeleteSelfException;
import com.immocare.exception.EmailTakenException;
import com.immocare.exception.UserNotFoundException;
import com.immocare.exception.UsernameTakenException;
import com.immocare.model.dto.ChangePasswordRequest;
import com.immocare.model.dto.CreateUserRequest;
import com.immocare.model.dto.UpdateUserRequest;
import com.immocare.model.dto.UserDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.service.UserService;

@SpringBootTest
class UserControllerTest {

        @Autowired
        MockMvc mockMvc;
        @Autowired
        ObjectMapper objectMapper;

        @MockitoBean
        UserService userService;

        private UserDTO sampleDTO;

        @BeforeEach
        void setUp() {
                sampleDTO = new UserDTO(1L, "admin", "admin@example.com", "ADMIN",
                                LocalDateTime.now(), LocalDateTime.now());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void getAll_returnsOkWithList() throws Exception {
                when(userService.getAllUsers()).thenReturn(List.of(sampleDTO));

                mockMvc.perform(get("/api/v1/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].username").value("admin"))
                                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
        }

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
                String body = """
                                {"username":"","email":"x@x.com","password":"Password1",
                                 "confirmPassword":"Password1","role":"ADMIN"}""";

                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.fieldErrors.username").exists());
        }

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