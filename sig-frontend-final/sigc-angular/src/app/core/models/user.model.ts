// src/app/core/models/user.model.ts

// Modèle pour représenter l'utilisateur connecté (basé sur token ou API /me)
export interface User {
    id: number;
    username: string;
    email: string;
    roles: string[];
    enabled: boolean;
    createdAt: string; // ou Date
    firstName?: string;
    lastName?: string;
  }
  
  // Modèle pour la requête de login
  export interface LoginRequest {
    usernameOrEmail: string;
    password?: string; // Le mot de passe est envoyé, mais jamais stocké ici
  }
  
  // Modèle pour la réponse de login contenant le token
  export interface JwtResponse {
   jwttoken: string;
  refreshToken?: string;  // Make it optional with ?
  // Add other properties your backend might return
  userId?: string;
  token?:string;
  username?: string;
  email?: string;
  roles?: string[]; // Généralement "Bearer"
    // Ajouter id, username, roles si votre API login les renvoie
  }
  
  // Modèle pour les informations décodées du payload JWT
  export interface DecodedToken {
    sub: string; // Subject (souvent username ou email)
    roles?: string[]; // Les rôles de l'utilisateur
    exp?: number; // Expiration timestamp (secondes)
    iat?: number; // Issued at timestamp (secondes)
    [key: string]: any; // Permet d'autres propriétés (ex: userId)
  }
  
  // Vous pourriez ajouter d'autres modèles liés à l'utilisateur ici si nécessaire
  // export interface UserProfile extends User { ... } // Si plus détaillé