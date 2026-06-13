package com.exata.swissqrbill.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;

/**
 * ==================================================================================
 * ARCHITECTURE NOTES: JWT AUTHENTICATION FILTER
 * ==================================================================================
 * 
 * 1. FILTER HOOK POINT:
 *    - Injected before UsernamePasswordAuthenticationFilter to validate authorization tokens 
 *      early in the Spring Security filter chain.
 * 
 * 2. STATELESSNESS ENFORCEMENT:
 *    - Upon successful token verification, a UsernamePasswordAuthenticationToken is populated 
 *      and injected into the SecurityContextHolder. No HttpSession is created or stored, keeping 
 *      the system stateless and scaling easily across clustered container instances (Docker/Kubernetes).
 * 
 * 3. EXCEPTION HANDLING (FINMA / SOC2 Audits):
 *    - In a production setup, ensure any JwtException (expired, malformed, revoked) is handled 
 *      cleanly via a custom AuthenticationEntryPoint or a centralized Filter Chain Exception Handler 
 *      to avoid leaking stack traces in HTTP responses.
 * ==================================================================================
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    // Enforce constructor injection strictly
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Skip auth filter if Authorization header is missing or not a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        
        try {
            // Verify and extract username from JWT token
            if (jwtService.isTokenValid(jwt)) {
                username = jwtService.extractUsername(jwt);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    log.debug("Successfully authenticated request for user: {} using JWT skeleton", username);

                    // Map static authority role for local sandbox testing
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                log.warn("JWT Verification failed inside filter chain: invalid signature/expired token");
            }
        } catch (Exception e) {
            log.error("Failed to process JWT authentication filter due to an unexpected error", e);
        }

        filterChain.doFilter(request, response);
    }
}
