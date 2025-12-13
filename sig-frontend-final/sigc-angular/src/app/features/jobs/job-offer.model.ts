// src/app/features/jobs/job-offer.model.ts

// Correspond à JobOfferDTO du backend
export interface JobOffer {
    id: number;
    title: string;
    description: string;
    // Utiliser des types chaîne littérale pour les statuts (plus sûr que string)
    status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED' | 'FILLED';
    requirements?: string[];
    requiredSkillNames?: string[]; // Noms des compétences
    salaryRange?: string;
    publicationDate?: string; // ou Date
    expirationDate?: string; // ou Date
    createdAt: string; // ou Date
    // Ajouter updatedAt si besoin
}

// Modèle pour la création/mise à jour (peut omettre id, createdAt, etc.)
export interface JobOfferRequest {
    title: string;
    description: string;
    status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED' | 'FILLED';
    requirements?: string[];
    requiredSkillNames?: string[];
    salaryRange?: string;
    publicationDate?: string; // ou Date
    expirationDate?: string; // ou Date
}