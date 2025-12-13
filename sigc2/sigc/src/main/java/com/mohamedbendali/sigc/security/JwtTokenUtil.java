package com.mohamedbendali.sigc.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys; // Pour générer des clés sécurisées
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // Pour initialiser la clé après injection
import java.io.Serializable;
import java.security.Key; // Interface pour la clé
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil implements Serializable {

    private static final long serialVersionUID = -2550185165626007488L;

    // Durée de validité du token (ex: 5 heures) - à ajuster
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000; // en millisecondes

    // Clé secrète pour signer le token (NE PAS coder en dur en production !)
    @Value("${jwt.secret}") // Lire depuis application.properties
    private String secret;

    private Key key; // Clé de signature générée

    // Initialiser la clé après l'injection de la valeur 'secret'
    @PostConstruct
    public void init() {
        // Générer une clé sécurisée basée sur le secret (minimum requis pour HS256)
        // Assurez-vous que votre secret fait au moins 32 bytes (256 bits) pour HS256
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            // En production, gérer cela plus proprement (lever une exception, etc.)
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!! ATTENTION: Le secret JWT est trop court pour HS256 (minimum 32 bytes) !!!");
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            // Utiliser une clé par défaut non sécurisée pour le développement SEULEMENT si le secret est trop court
            // NE PAS FAIRE CELA EN PRODUCTION
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            System.err.println("!!! Utilisation d'une clé générée aléatoirement NON SÉCURISÉE pour JWT !!!");
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
    }


    // Récupérer le nom d'utilisateur (ou email) depuis le token jwt
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Récupérer la date d'expiration depuis le token jwt
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Récupérer une information spécifique (claim) depuis le token
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Pour récupérer toutes les informations depuis le token, nous avons besoin de la clé secrète
    private Claims getAllClaimsFromToken(String token) {
        // Utiliser la clé générée pour parser
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // Vérifier si le token a expiré
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Générer le token pour l'utilisateur
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // ---> AJOUTER CETTE PARTIE <---
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", roles); // Ajouter la liste des rôles dans les claims
        // ---> FIN AJOUT <---
        // Vous pouvez ajouter des claims personnalisés ici si nécessaire
        // claims.put("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        return doGenerateToken(claims, userDetails.getUsername()); // Utilise le username (ou email) comme sujet
    }

    // Pendant la création du token -
    // 1. Définir les claims du token, comme l'Issuer, Expiration, Subject, et l'ID
    // 2. Signer le JWT en utilisant l'algorithme HS256 et la clé secrète.
    // 3. Selon la spécification JWS Compact Serialization(https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-3.1)
    //    compactage du JWT en une chaîne URL-safe
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // Le nom d'utilisateur (ou email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256) // Utiliser la clé générée et l'algo
                .compact();
    }

    // Valider le token
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        // Vérifie si le nom d'utilisateur du token correspond et si le token n'est pas expiré
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}