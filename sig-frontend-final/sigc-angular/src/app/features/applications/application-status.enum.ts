// Correspond à l'enum ApplicationStatus du backend
export enum ApplicationStatus {
    RECEIVED = 'RECEIVED',
    UNDER_REVIEW = 'UNDER_REVIEW',
    INTERVIEW_SCHEDULED = 'INTERVIEW_SCHEDULED',
    INTERVIEW_COMPLETED = 'INTERVIEW_COMPLETED',
    ASSESSMENT_SENT = 'ASSESSMENT_SENT',
    ASSESSMENT_PASSED = 'ASSESSMENT_PASSED',
    OFFER_EXTENDED = 'OFFER_EXTENDED',
    ACCEPTED = 'ACCEPTED',
    REJECTED = 'REJECTED',
    WITHDRAWN = 'WITHDRAWN'
}

// Fonction utilitaire (optionnelle) pour obtenir une étiquette lisible
export function getApplicationStatusLabel(status: ApplicationStatus): string {
    switch (status) {
        case ApplicationStatus.RECEIVED: return 'Reçue';
        case ApplicationStatus.UNDER_REVIEW: return 'En cours d\'examen';
        case ApplicationStatus.INTERVIEW_SCHEDULED: return 'Entretien planifié';
        case ApplicationStatus.INTERVIEW_COMPLETED: return 'Entretien terminé';
        case ApplicationStatus.ASSESSMENT_SENT: return 'Évaluation envoyée';
        case ApplicationStatus.ASSESSMENT_PASSED: return 'Évaluation réussie';
        case ApplicationStatus.OFFER_EXTENDED: return 'Offre proposée';
        case ApplicationStatus.ACCEPTED: return 'Acceptée';
        case ApplicationStatus.REJECTED: return 'Refusée';
        case ApplicationStatus.WITHDRAWN: return 'Retirée';
        default: return status; // Retourner la clé si inconnu
    }
}
// En haut de votre fichier .ts (ex: my-applications.component.ts)
type PrimeNGSeverity = "success" | "secondary" | "info" | "warn" | "danger" | "contrast";
// Fonction utilitaire (optionnelle) pour obtenir une sévérité PrimeNG
export function getApplicationStatusSeverity(status: ApplicationStatus): PrimeNGSeverity | undefined {
     switch (status) {
        case ApplicationStatus.RECEIVED: return 'info';
        case ApplicationStatus.UNDER_REVIEW: return 'info';
        case ApplicationStatus.INTERVIEW_SCHEDULED: return 'warn';
        case ApplicationStatus.INTERVIEW_COMPLETED: return 'warn';
        case ApplicationStatus.ASSESSMENT_SENT: return 'info';
        case ApplicationStatus.ASSESSMENT_PASSED: return 'success';
        case ApplicationStatus.OFFER_EXTENDED: return 'success';
        case ApplicationStatus.ACCEPTED: return 'success';
        case ApplicationStatus.REJECTED: return 'danger';
        case ApplicationStatus.WITHDRAWN: return 'secondary';
        default: return 'secondary';
    }
}