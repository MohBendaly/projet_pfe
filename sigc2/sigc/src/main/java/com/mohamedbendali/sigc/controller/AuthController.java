package com.mohamedbendali.sigc.controller;

import com.mohamedbendali.sigc.dto.LoginRequest; // Importer le DTO d'entrée
import com.mohamedbendali.sigc.dto.JwtResponse; // Importer le DTO de sortie
import com.mohamedbendali.sigc.dto.UserDTO; // Pour l'enregistrement (si vous l'ajoutez ici)
import com.mohamedbendali.sigc.security.JwtTokenUtil; // Importer l'utilitaire JWT
import com.mohamedbendali.sigc.service.UserService; // Importer le UserService
import jakarta.validation.Valid; // Pour valider le LoginRequest
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*; // Assurez-vous d'avoir les bonnes annotations

@RestController
@RequestMapping("/api/auth") // Préfixe pour toutes les méthodes
@RequiredArgsConstructor // Injecte les dépendances finales via le constructeur
@CrossOrigin(origins = "http://localhost:4200")// Optionnel: Autoriser les requêtes cross-origin pour ce contrôleur
@Slf4j // Pour les logs
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService; // Doit être votre implémentation UserDetailsService

    /**
     * Gère la requête de login.
     * Authentifie l'utilisateur et génère un token JWT si l'authentification réussit.
     * @param authenticationRequest Contient username/email et password.
     * @return ResponseEntity contenant le JwtResponse avec le token.
     * @throws Exception si l'authentification échoue.
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@Valid @RequestBody LoginRequest authenticationRequest) throws Exception {

        log.info("Authentication attempt for user: {}", authenticationRequest.getUsernameOrEmail());

        // 1. Authentifier l'utilisateur en utilisant l'AuthenticationManager
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    authenticationRequest.getUsernameOrEmail(),
                    authenticationRequest.getPassword()
            ));
            log.info("Authentication successful for user: {}", authenticationRequest.getUsernameOrEmail());
        } catch (DisabledException e) {
            log.warn("Authentication failed for disabled user: {}", authenticationRequest.getUsernameOrEmail());
            throw new Exception("USER_DISABLED", e); // Renvoyer une exception spécifique ou un statut 403/401
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed due to bad credentials for user: {}", authenticationRequest.getUsernameOrEmail());
            throw new Exception("INVALID_CREDENTIALS", e); // Renvoyer une exception spécifique ou un statut 401
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", authenticationRequest.getUsernameOrEmail(), e);
            throw new Exception("AUTHENTICATION_FAILED", e);
        }

        // 2. Si l'authentification réussit, charger les UserDetails
        //    (loadUserByUsername gère la recherche par username ou email)
        final UserDetails userDetails = userService
                .loadUserByUsername(authenticationRequest.getUsernameOrEmail());
        log.debug("UserDetails loaded for user: {}", userDetails.getUsername());


        // 3. Générer le token JWT
        final String token = jwtTokenUtil.generateToken(userDetails);
        log.info("JWT Token generated successfully for user: {}", userDetails.getUsername());

        // 4. Retourner le token dans la réponse
        return ResponseEntity.ok(new JwtResponse(token));
    }

    // Optionnel : Endpoint d'enregistrement

    @PostMapping("/register")
    public ResponseEntity<?> saveUser(@Valid @RequestBody UserDTO user) throws Exception {
        // Appeler userService.createUser(user) qui gère la logique,
        // y compris le hachage du mot de passe et la création du profil candidat.
        UserDTO createdUser = userService.createUser(user);
        // Retourner une réponse appropriée (ex: 201 Created avec l'URI ou juste 200 OK)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser); // Ne pas renvoyer le mot de passe !
    }

}