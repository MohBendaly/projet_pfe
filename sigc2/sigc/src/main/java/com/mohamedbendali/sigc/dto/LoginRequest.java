package com.mohamedbendali.sigc.dto; // Ou com.mohamedbendali.sigc.payload.request

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Génère getters, setters, etc.
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Le nom d'utilisateur ou l'email est obligatoire")
    private String usernameOrEmail; // Peut recevoir soit username soit email

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}