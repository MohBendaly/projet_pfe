package com.mohamedbendali.sigc.repository;

import com.mohamedbendali.sigc.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    // Ajoutez cette ligne :
    // Permet de chercher un utilisateur soit par son nom d'utilisateur, soit par son email (ignorant la casse)
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    Boolean existsByUsernameIgnoreCase(String username);

    Boolean existsByEmailIgnoreCase(String email);
}