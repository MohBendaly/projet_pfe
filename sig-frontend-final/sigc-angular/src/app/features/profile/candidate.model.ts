// Correspond à CandidateDTO du backend
export interface Candidate {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    phone?: string;
    resumePath?: string; // Le chemin stocké, peut être utilisé pour construire l'URL de DL
    resumeDownloadUrl?: string; // URL complète générée côté client ou via DTO
    userId?: number;
    skillNames?: string[];
    applicationIds?: number[]; // Juste les IDs, les détails sont chargés via JobApplicationService
    createdAt?: string; // ou Date
    // Ajouter updatedAt si présent dans le DTO backend
}

// Modèle pour la mise à jour (peut omettre email/userId)
export interface UpdateCandidateRequest {
    firstName: string;
    lastName: string;
    phone?: string;
    // Ajouter d'autres champs modifiables (ex: skills)
    skillNames?: string[]; // <-- AJOUTER CE CHAMP

}