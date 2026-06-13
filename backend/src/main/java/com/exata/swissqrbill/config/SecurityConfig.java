package com.exata.swissqrbill.config;

import com.exata.swissqrbill.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ==================================================================================
 * ARCHITECTURE NOTES: CENTRALIZED ENTERPRISE SECURITY CONFIGURATION
 * ==================================================================================
 * 
 * 1. SECURE ENDPOINTS & DEVELOPMENT BYPASS:
 *    - To maintain a seamless local developer experience (local sandbox sandbox mode), 
 *      we whitelisted the operational endpoints (/generate, /validate, /download) 
 *      using `.permitAll()`. 
 *    - To transition this into production-hardened mode, change these endpoints to 
 *      `.authenticated()` or map them to specific roles using `.hasRole("USER")`.
 * 
 * 2. STATELESS SESSION POLICY:
 *    - Set to SessionCreationPolicy.STATELESS. This disables session cookies and ensures 
 *      the system treats every request independently, fulfilling horizontal scalability 
 *      standards in Kubernetes/Docker environments.
 * 
 * 3. CORS AND CSRF MITIGATION:
 *    - CSRF is disabled (.disable()) since the API is strictly stateless and relies 
 *      on bearer tokens rather than browser cookie storage.
 *    - CORS is delegated to Spring Boot's CorsConfig properties to allow origin 
 *      matching from the Angular host (localhost:4200).
 * ==================================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Enforce constructor injection strictly
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Whitelist public services
                .requestMatchers("/api/v1/qrbill/health").permitAll()
                // Whitelist OpenAPI and Swagger UI paths
                .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-resources",
                        "/swagger-resources/**",
                        "/configuration/ui",
                        "/configuration/security",
                        "/swagger-ui/**",
                        "/webjars/**",
                        "/swagger-ui.html"
                ).permitAll()
                // Whitelist Swiss QR-Bill operational endpoints for local sandbox testing
                // PRODUCTION RULE: Change .permitAll() to .authenticated() to enforce JWT protection
                .requestMatchers(
                        "/api/v1/qrbill/generate",
                        "/api/v1/qrbill/validate",
                        "/api/v1/qrbill/download"
                ).permitAll()
                // Any other request must be fully authenticated
                .anyRequest().authenticated()
            )
            // Add custom JWT filter before the standard authentication processor
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
