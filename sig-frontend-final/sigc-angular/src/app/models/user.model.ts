// src/app/core/models/user.model.ts
export interface User {
    id: number;
    username: string;
    email: string;
    roles: string[];
    enabled: boolean;
    createdAt: string; // ou Date
    firstName?: string; // Optionnel selon le DTO de retour
    lastName?: string;  // Optionnel
  }
  
  export interface LoginRequest {
      usernameOrEmail: string;
      password?: string; // Password n'est pas toujours dans le modèle User
  }
  
  export interface JwtResponse {
      jwttoken: string;
      type?: string;
      // ajouter id, username, roles si l'API les renvoie au login
  }
  
  // src/app/features/jobs/job-offer.model.ts
  export interface JobOffer {
      id: number;
      title: string;
      description: string;
      status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED' | 'FILLED'; // Utiliser des types chaîne littérale
      requirements?: string[];
      requiredSkillNames?: string[];
      salaryRange?: string;
      publicationDate?: string; // ou Date
      expirationDate?: string; // ou Date
      createdAt: string; // ou Date
  }
  
  // Créez des interfaces similaires pour:
  // - JobApplication (job-application.model.ts)
  // - Interview (interview.model.ts)
  // - ChatMessage (chat-message.model.ts)
  // - Candidate (candidate.model.ts)
  // - Attachment (attachment.model.ts)
  // - Skill (skill.model.ts)
  // ... en vous basant sur les DTOs backend