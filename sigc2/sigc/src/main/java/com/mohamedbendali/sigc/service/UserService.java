package com.mohamedbendali.sigc.service;

import com.mohamedbendali.sigc.dto.UserDTO; // Vous aurez besoin d'un DTO pour User aussi
import com.mohamedbendali.sigc.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService; // Important !

import java.util.Optional;

public interface UserService extends UserDetailsService { // Étend UserDetailsService

    // Méthode de UserDetailsService (déjà définie mais peut être redéclarée)
    // UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException;

    // Méthodes supplémentaires pour gérer les utilisateurs de l'application
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    UserDTO createUser(UserDTO userDto); // Pour l'enregistrement
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    // Autres méthodes utiles (update, delete, findById etc.)
}