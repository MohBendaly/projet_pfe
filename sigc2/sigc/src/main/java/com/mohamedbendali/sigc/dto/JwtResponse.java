package com.mohamedbendali.sigc.dto; // Ou com.mohamedbendali.sigc.payload.response

import lombok.Data;
import lombok.RequiredArgsConstructor; // Pour le constructeur avec le token

import java.io.Serializable;
import java.util.List;

@Data // Génère getters, setters, etc.
@RequiredArgsConstructor // Crée un constructeur avec les champs 'final' ou annotés @NonNull
public class JwtResponse implements Serializable {

    private static final long serialVersionUID = -8091879091924046844L;
    private final String jwttoken; // Le token JWT généré
    private String type = "Bearer"; // Le type de token (standard)

    // Vous pouvez ajouter d'autres informations si nécessaire (ex: rôles, ID utilisateur)
     private Long id;
     private String username;
    private List<String> roles;
}