package com.mohamedbendali.sigc.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component; // Important pour que Spring le détecte comme bean
import java.io.IOException;
import java.io.PrintWriter; // Pour écrire dans la réponse

@Component // Déclare cette classe comme un bean Spring
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        logger.error("Responding with unauthorized error. Message - {}", authException.getMessage());

        // Envoyer une réponse d'erreur 401 Unauthorized
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // Statut 401
        response.setContentType("application/json"); // Type de contenu
        response.setCharacterEncoding("UTF-8");

        // Écrire un message JSON simple dans le corps de la réponse (optionnel mais utile)
        PrintWriter out = response.getWriter();
        // Vous pouvez personnaliser ce message JSON
        out.print("{\"error\": \"Unauthorized\", \"message\": \"Authentification requise pour accéder à cette ressource.\", \"status\": 401}");
        out.flush(); // Vider le buffer
    }
}