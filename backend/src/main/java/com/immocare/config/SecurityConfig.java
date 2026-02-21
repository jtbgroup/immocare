package com.immocare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Security configuration for ImmoCare.
 * Compatible with Spring Boot 4 / Spring Security 6.4.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/login", "/actuator/health").permitAll()
            .anyRequest().authenticated()
        )

        .formLogin(form -> form
            .loginProcessingUrl("/api/v1/auth/login")
            .successHandler((req, res, authentication) ->
                res.setStatus(HttpStatus.OK.value()))
            .failureHandler((req, res, exception) ->
                res.setStatus(HttpStatus.UNAUTHORIZED.value()))
            .permitAll()
        )

        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
        );

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
