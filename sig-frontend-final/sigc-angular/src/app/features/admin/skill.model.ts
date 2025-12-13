// Correspond à SkillDTO du backend
export interface Skill {
    id: number;
    name: string;
    category?: string;
}

// Modèle pour la création/mise à jour
export interface SkillRequest {
    name: string;
    category?: string;
}