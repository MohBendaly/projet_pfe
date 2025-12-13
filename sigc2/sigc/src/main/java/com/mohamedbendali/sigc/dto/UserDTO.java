package com.mohamedbendali.sigc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for User entity.
 * Used for registration, fetching user details (without password), etc.
 */
@Data // Génère getters, setters, toString, equals, hashCode
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50, message = "Le nom d'utilisateur doit contenir entre 3 et 50 caractères")
    private String username;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 100, message = "L'email ne peut dépasser 100 caractères")
    private String email;

    // Le mot de passe est seulement utilisé pour la création ou la mise à jour.
    // Il ne devrait JAMAIS être renvoyé par l'API dans ce DTO.
    // On peut utiliser des groupes de validation ou des DTOs séparés (ex: CreateUserDTO) pour être plus strict.
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password; // Ne pas ajouter @NotBlank ici si la mise à jour sans changement de mdp est permise

    // Champs optionnels pour faciliter la création du profil Candidat lié lors de l'inscription
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    // Les rôles sont souvent définis par le backend, mais peuvent être inclus pour info ou création admin
    private Set<String> roles; // Noms des rôles (ex: "ROLE_CANDIDATE", "ROLE_ADMIN")

    private boolean enabled;

    // Champs en lecture seule généralement
    private LocalDateTime createdAt;

    // Constructeur spécifique si nécessaire (ex: sans password pour le retour API)
    public UserDTO(Long id, String username, String email, Set<String> roles, boolean enabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
        this.createdAt = createdAt;
        // Deliberately omitting password, firstName, lastName
    }
}