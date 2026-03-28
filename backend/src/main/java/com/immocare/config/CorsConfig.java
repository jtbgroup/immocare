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
 * - http://localhost:4200 Angular dev server (accès direct)
 * - http://localhost:8080 nginx dev (docker-compose.dev.yml)
 * - http://localhost nginx prod (Docker port 80)
 * - http://localhost:8090 Docker Compose prod exposé
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowCredentials(true);
    config.addAllowedOrigin("http://localhost:4200"); // Angular direct
    config.addAllowedOrigin("http://localhost:8080"); // nginx dev
    config.addAllowedOrigin("http://localhost"); // nginx prod (port 80)
    config.addAllowedOrigin("http://localhost:8090"); // docker-compose prod
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");

    source.registerCorsConfiguration("/api/**", config);
    return new CorsFilter(source);
  }
}