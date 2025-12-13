// Correspond à ChatMessageDTO du backend
export interface ChatMessage {
    id: number;
    content: string;
    isFromBot: boolean; // true = AI, false = Candidate
    interviewId: number;
    timestamp: string; // ou Date
}

// Modèle pour l'envoi d'un message
export interface PostChatMessageRequest {
    content: string;
}