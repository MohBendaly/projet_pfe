import { ChatMessage } from "./chat-message.model"; // Importer le modèle ChatMessage
import { InterviewStatus } from "./interview-status.enum"; // Assurez-vous de créer ce fichier enum

// Correspond à InterviewDTO du backend
export interface Interview {
    id: number;
    applicationId: number;
    startTime?: string; // ou Date
    endTime?: string; // ou Date
    status: InterviewStatus; // Utiliser l'enum
    aiEvaluationScore?: number;
    aiFeedback?: string;
    createdAt?: string; // ou Date
    chatMessages?: ChatMessage[]; // Peut être chargé séparément
}