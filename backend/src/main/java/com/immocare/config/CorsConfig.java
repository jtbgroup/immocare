package com.immocare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS configuration for ImmoCare.
 *
 * Allowed origins:
 *  - http://localhost:4200  Angular dev server (local without Docker)
 *  - http://localhost       Nginx reverse proxy (Docker, port 80)
 *  - http://localhost:8090  Docker Compose exposed port
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowCredentials(true);
    config.addAllowedOrigin("http://localhost:4200");
    config.addAllowedOrigin("http://localhost");
    config.addAllowedOrigin("http://localhost:8090");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
  }
}
