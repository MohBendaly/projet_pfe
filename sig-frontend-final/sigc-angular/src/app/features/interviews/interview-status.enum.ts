// Correspond à l'enum InterviewStatus du backend
export enum InterviewStatus {
    SCHEDULED = 'SCHEDULED',
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED',
    CANCELLED = 'CANCELLED',
    PENDING_FEEDBACK = 'PENDING_FEEDBACK'
}

// Fonction utilitaire (optionnelle) pour obtenir une étiquette lisible
export function getInterviewStatusLabel(status: InterviewStatus): string {
    switch (status) {
        case InterviewStatus.SCHEDULED: return 'Planifié';
        case InterviewStatus.IN_PROGRESS: return 'En cours';
        case InterviewStatus.COMPLETED: return 'Terminé';
        case InterviewStatus.CANCELLED: return 'Annulé';
        case InterviewStatus.PENDING_FEEDBACK: return 'En attente évaluation';
        default: return status;
    }
}
type PrimeNGSeverity = "success" | "secondary" | "info" | "warn" | "danger" | "contrast";

// Fonction utilitaire (optionnelle) pour obtenir une sévérité PrimeNG
export function getInterviewStatusSeverity(status: InterviewStatus): PrimeNGSeverity {
     switch (status) {
        case InterviewStatus.SCHEDULED: return 'info';
        case InterviewStatus.IN_PROGRESS: return 'warn';
        case InterviewStatus.COMPLETED: return 'success';
        case InterviewStatus.CANCELLED: return 'danger';
        case InterviewStatus.PENDING_FEEDBACK: return 'secondary';
        default: return 'secondary';
    }
}