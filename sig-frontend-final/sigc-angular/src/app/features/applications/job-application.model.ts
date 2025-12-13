import { ApplicationStatus } from "./application-status.enum"; // Assurez-vous de créer ce fichier enum

// Correspond à JobApplicationDTO du backend
export interface JobApplication {
    id: number;
    status: ApplicationStatus; // Utiliser l'enum pour la clarté
    candidateId: number;
    candidateFullName?: string; // Optionnel, pratique pour l'affichage
    jobOfferId: number;
    jobOfferTitle?: string; // Optionnel, pratique pour l'affichage
    coverLetter?: string;
    applicationDate: string; // ou Date
    updatedAt?: string; // ou Date
    interviewIds?: number[];
    attachmentIds?: number[];
}

// Vous pourriez aussi avoir un DTO pour la création si différent
// export interface CreateJobApplicationRequest {
//     jobOfferId: number;
//     coverLetter?: string;
// }