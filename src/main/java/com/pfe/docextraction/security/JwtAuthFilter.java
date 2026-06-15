package com.pfe.docextraction.security;

import com.pfe.docextraction.entity.ApiKey;
import com.pfe.docextraction.entity.User;
import com.pfe.docextraction.repository.ApiKeyRepository;
import com.pfe.docextraction.service.auth.CustomUserDetailsService;
import com.pfe.docextraction.service.auth.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> WHITELISTED_PATHS = List.of(
        "/api/auth/",
        "/api/auth",
        "/actuator/",
        "/api/actuator/",
        "/swagger-ui/",
        "/swagger-ui.html",
        "/api-docs/",
        "/v3/api-docs/",
        "/webjars/"
    );

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return WHITELISTED_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);


        if (looksLikeApiKey(token)) {
            authenticateWithApiKey(token, request);
        } else {
            authenticateWithJwt(token, request);
        }

        filterChain.doFilter(request, response);
    }

 
    private boolean looksLikeApiKey(String token) {
        return !token.contains(".");
    }

    private void authenticateWithApiKey(String key, HttpServletRequest request) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyValue(key);
            if (apiKeyOpt.isEmpty()) return;

            ApiKey apiKey = apiKeyOpt.get();
            if (!apiKey.isValid()) return;

            User user = apiKey.getCreatedBy();
            if (user == null) return;

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

      
            apiKey.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(apiKey);
        } catch (Exception ignored) {
       
        }
    }

    private void authenticateWithJwt(String token, HttpServletRequest request) {
        try {
            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                        );

                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            
        }
    }
}
