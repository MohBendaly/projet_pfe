package com.mohamedbendali.sigc.security;

import com.mohamedbendali.sigc.service.UserService; // Votre UserDetailsService
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // Important: pour s'exécuter une seule fois par requête

import java.io.IOException;

@Component // Déclare comme bean Spring pour être injecté dans SecurityConfig
public class JwtRequestFilter extends OncePerRequestFilter { // Hérite de OncePerRequestFilter

    private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

    // Injection des dépendances nécessaires
    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestURI = request.getRequestURI();
        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        log.debug("Processing request URI: {}", requestURI);

        // --- IGNORER les chemins d'authentification et Swagger ---
        if (requestURI.startsWith("/api/auth/") ||
                requestURI.startsWith("/swagger-ui") ||   // Ignorer Swagger UI
                requestURI.startsWith("/v3/api-docs")) {  // Ignorer OpenAPI docs
            log.debug("Skipping JWT filter for public URI: {}", requestURI);
            chain.doFilter(request, response); // Passer au filtre suivant sans traiter le token
            return;
        }
        // --- Fin de la section IGNORER ---

        // Le token JWT est dans le header "Authorization: Bearer token".
        // Enlever le mot "Bearer" pour obtenir seulement le Token
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                log.debug("JWT Token found, username extracted: {}", username);
            } catch (IllegalArgumentException e) {
                log.warn("Unable to get JWT Token");
            } catch (ExpiredJwtException e) {
                log.warn("JWT Token has expired");
            } catch (Exception e) {
                log.warn("Error parsing JWT Token: {}", e.getMessage());
            }
        } else {
            log.trace("Authorization header does not start with Bearer String or is missing for URI: {}", requestURI);
        }

        // Une fois qu'on a le token, valider le.
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Validating token for user: {}", username);
            UserDetails userDetails = this.userService.loadUserByUsername(username);

            // Si le token est valide, configurer Spring Security pour manuellement définir l'authentification
            if (jwtTokenUtil.validateToken(jwtToken, userDetails)) {
                log.info("JWT Token is valid for user: {}", username);
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Après avoir défini l'authentification dans le contexte, nous spécifions
                // que l'utilisateur courant est authentifié. Ainsi il passe les
                // Configurations Spring Security avec succès.
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                log.debug("User '{}' set in Security Context", username);
            } else {
                log.warn("JWT Token validation failed for user: {}", username);
            }
        } else {
            if (username == null && !requestURI.startsWith("/api/auth/")) { // Ne log pas pour les chemins publics déjà ignorés
                log.trace("No username extracted from token or authentication already present for URI: {}", requestURI);
            }
        }

        // Toujours appeler doFilter pour passer au filtre suivant
        chain.doFilter(request, response);
    }
}