// Correspond à AttachmentDTO du backend
export interface Attachment {
    id: number;
    fileName: string;
    fileType: string; // MIME type
    fileSize: number; // en octets
    uploadedAt: string; // ou Date
    ownerId?: number; // Candidate ID
    jobApplicationId?: number; // Application ID
    downloadUrl: string; // URL générée pour le téléchargement direct
}