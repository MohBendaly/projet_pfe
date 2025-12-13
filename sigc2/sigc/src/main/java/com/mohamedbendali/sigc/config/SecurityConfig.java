package com.mohamedbendali.sigc.config;

// ... tous les imports nécessaires ...
import com.mohamedbendali.sigc.security.JwtAuthenticationEntryPoint; // Supposons que vous avez ceci
import com.mohamedbendali.sigc.security.JwtRequestFilter;       // Supposons que vous avez ceci
import com.mohamedbendali.sigc.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// Importez ceci si ce n'est pas déjà fait :
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserService userService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint; // Assurez-vous d'injecter votre EntryPoint
    private final JwtRequestFilter jwtRequestFilter;                     // Assurez-vous d'injecter votre Filter

    // Injection via constructeur
    public SecurityConfig(UserService userService,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                          JwtRequestFilter jwtRequestFilter) {
        this.userService = userService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    // AuthenticationManager Bean (correct)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("--- CONFIGURING SecurityFilterChain ---");

        http
                // 1. Désactiver CSRF (ESSENTIEL pour API stateless)
                .csrf(AbstractHttpConfigurer::disable) // Méthode concise

                // 2. Configuration CORS (utilise le bean WebMvcConfigurer séparé)
                .cors(cors -> { /* Utilise la config globale */ })

                // 3. Gestion des exceptions -> Utiliser VOTRE EntryPoint pour les erreurs 401 non authentifiées
                .exceptionHandling(exceptions -> exceptions
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint) // Très important !
                        // .accessDeniedHandler(...) // Optionnel: pour gérer les 403 spécifiquement
                )

                // 4. Gestion de Session -> Mettre en STATELESS (ESSENTIEL pour API stateless JWT)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Ne pas créer de session HTTP
                )

                // 5. Désactiver les logins par formulaire et HTTP Basic par défaut s'ils ne sont pas utilisés
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)


                // 6. Règles d'autorisation (vérifier l'ordre attentivement)
                .authorizeHttpRequests(auth -> auth
                        // --- Règles publiques EN PREMIER ---
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll() // Pour Swagger
                        .requestMatchers("/error").permitAll() // Autoriser l'accès à la page d'erreur
                        // --- Règles GET publiques (si nécessaire) ---
                        .requestMatchers(HttpMethod.GET, "/api/offers", "/api/offers/*").permitAll()
                        // --- Règles spécifiques aux rôles ---
                        // .requestMatchers("/admin/**").hasRole("ADMIN")
                        // --- Règle générale pour le reste de l'API (ou des endpoints) ---
                        // .requestMatchers("/api/**").authenticated() // Attention si trop général
                        // --- Règle fourre-tout FINALE ---
                        .anyRequest().authenticated() // Tout le reste nécessite une authentification
                );

        // 7. Ajouter le filtre JWT AVANT le filtre d'authentification par username/password
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        System.out.println("--- SecurityFilterChain BUILT ---");
        return http.build();
    }
}