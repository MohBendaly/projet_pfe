// src/app/features/auth/models/userDTO.model.ts
export interface UserDTO {
    username?: string; // Optionnel si l'email est le login principal
    email: string;
    password?: string; // Nécessaire pour l'enregistrement/MAJ mot de passe
    firstName?: string;
    lastName?: string;
    roles?: string[]; // Optionnel, souvent géré par le backend, sauf pour création admin
    // Ne pas inclure 'id', 'enabled', 'createdAt' ici, ils sont gérés par le backend ou définis autrement.
  }