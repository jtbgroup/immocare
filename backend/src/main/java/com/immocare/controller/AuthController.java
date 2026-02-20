package com.immocare.controller;

import com.immocare.model.dto.AuthUserDTO;
import com.immocare.model.entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints.
 * <ul>
 *   <li>{@code GET  /api/v1/auth/me}     — returns current user info</li>
 *   <li>{@code POST /api/v1/auth/logout}  — invalidates the session</li>
 * </ul>
 * POST /login is handled automatically by Spring Security (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * Returns the currently authenticated user's username and role.
     * Requires an active session — returns 401 if not authenticated (handled by SecurityConfig).
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserDTO> me(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(new AuthUserDTO(user.getUsername(), user.getRole()));
    }

    /**
     * Programmatic logout endpoint for the Angular frontend.
     * Invalidates the HTTP session and removes the JSESSIONID cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
