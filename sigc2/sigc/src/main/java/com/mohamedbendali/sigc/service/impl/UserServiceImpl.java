package com.mohamedbendali.sigc.service.impl;

import com.mohamedbendali.sigc.dto.UserDTO; // Assurez-vous que ce DTO existe
import com.mohamedbendali.sigc.entity.Candidate; // Pour lier lors de la création
import com.mohamedbendali.sigc.entity.User;
import com.mohamedbendali.sigc.entity.User.Role; // Importer l'enum Role
import com.mohamedbendali.sigc.exception.ResourceNotFoundException; // Si nécessaire
import com.mohamedbendali.sigc.exception.DuplicateResourceException; // Nouvelle exception possible
import com.mohamedbendali.sigc.repository.CandidateRepository;
import com.mohamedbendali.sigc.repository.UserRepository;
import com.mohamedbendali.sigc.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository; // Injecté pour lier le profil
    private final PasswordEncoder passwordEncoder;

    /**
     * Méthode principale utilisée par Spring Security pour l'authentification.
     * Recherche l'utilisateur par nom d'utilisateur OU email.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Attempting to load user by username or email: {}", usernameOrEmail);

        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        log.info("User found: {}", user.getUsername());

        // Convertir les rôles de l'entité User en GrantedAuthority pour Spring Security
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .collect(Collectors.toSet());

        // Retourner l'implémentation UserDetails de Spring Security
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), // Utiliser l'email comme "username" principal pour UserDetails? ou user.getUsername()
                user.getPassword(),
                user.isEnabled(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    @Override
    public UserDTO createUser(UserDTO userDto) {
        log.info("Attempting to create user with username: {} and email: {}", userDto.getUsername(), userDto.getEmail());

        if (existsByUsername(userDto.getUsername())) {
            log.warn("Username {} already exists.", userDto.getUsername());
            throw new DuplicateResourceException("User", "username", userDto.getUsername());
        }
        if (existsByEmail(userDto.getEmail())) {
            log.warn("Email {} already exists.", userDto.getEmail());
            throw new DuplicateResourceException("User", "email", userDto.getEmail());
        }

        User user = convertToEntity(userDto);
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // Hacher le mot de passe
        user.setEnabled(true); // Activer par défaut (ou ajouter vérification email)

        // Attribuer un rôle par défaut (ex: CANDIDATE)
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of(Role.ROLE_CANDIDATE)); // Rôle par défaut
            log.debug("Assigning default role ROLE_CANDIDATE to user {}", user.getUsername());
        }

        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Si l'utilisateur est un candidat, créer automatiquement le profil Candidate lié
        if (savedUser.getRoles().contains(Role.ROLE_CANDIDATE)) {
            createAndLinkCandidateProfile(savedUser, userDto);
        }

        return convertToDto(savedUser);
    }

    // Méthode privée pour créer et lier le profil Candidat
    private void createAndLinkCandidateProfile(User user, UserDTO userDto) {
        if (candidateRepository.findByUserId(user.getId()).isEmpty()) {
            log.debug("Creating associated Candidate profile for user ID: {}", user.getId());
            Candidate candidate = new Candidate();
            candidate.setUser(user);
            candidate.setEmail(user.getEmail()); // Reporter l'email
            // Reporter prénom/nom si fournis dans le UserDTO initial
            candidate.setFirstName(userDto.getFirstName() != null ? userDto.getFirstName() : "N/A");
            candidate.setLastName(userDto.getLastName() != null ? userDto.getLastName() : "N/A");
            candidateRepository.save(candidate);
            log.info("Candidate profile created and linked for user ID: {}", user.getId());
        } else {
            log.warn("Candidate profile already exists for user ID: {}", user.getId());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Boolean existsByUsername(String username) {
        return userRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    // --- Mapping Helpers ---
    // Ajouter un UserDTO
    // src/main/java/com/mohamedbendali/sigc/dto/UserDTO.java
    /*
    @Data
    public class UserDTO {
        private Long id;
        @NotBlank @Size(max=50) private String username;
        @NotBlank @Email @Size(max=100) private String email;
        @NotBlank @Size(min=6, max=100) private String password; // Seulement pour la création/maj
        private String firstName; // Pour la création du profil candidat
        private String lastName; // Pour la création du profil candidat
        private Set<String> roles; // Noms des rôles
        private boolean enabled;
        private LocalDateTime createdAt;
    }
    */

    private UserDTO convertToDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setEnabled(user.isEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream().map(Role::name).collect(Collectors.toSet()));
        }
        // Ne pas exposer le mot de passe !
        return dto;
    }

    private User convertToEntity(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId()); // Peut être null pour la création
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setEnabled(dto.isEnabled());
        // Le mot de passe est géré séparément (encodage)
        if (dto.getRoles() != null) {
            try {
                user.setRoles(dto.getRoles().stream().map(Role::valueOf).collect(Collectors.toSet()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role found in DTO: {}", dto.getRoles());
                // Gérer l'erreur ou assigner un rôle par défaut
                user.setRoles(Set.of(Role.ROLE_CANDIDATE));
            }
        }
        return user;
    }

    // Ajouter l'exception personnalisée si nécessaire
    // package com.mohamedbendali.sigc.exception;
    // public class DuplicateResourceException extends RuntimeException {
    //     public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
    //         super(String.format("%s with %s '%s' already exists.", resourceName, fieldName, fieldValue));
    //     }
    // }

    // Méthode utilitaire pour trouver par username OU email (utilisé dans loadUserByUsername)
    // Déjà dans le repo maintenant, mais pourrait être ici aussi.
    // private Optional<User> findByUsernameOrEmail(String username, String email) {
    //     return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(username, email);
    // }

}