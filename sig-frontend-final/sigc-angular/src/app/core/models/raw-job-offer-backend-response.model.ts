// src/app/core/models/raw-job-offer-backend-response.model.ts

export interface RawJobOfferBackendResponse {
  id: number;
  title: string;
  description: string;
  location?: string | null; // Make optional/nullable if backend can send null or omit
  date?: string | null; // This maps to publicationDate; make optional/nullable if backend can send null or omit
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'ARCHIVED' | 'FILLED';
  createdAt?: string | null; // Make optional/nullable if backend can send null or omit
  requirements?: string[] | null; // Often arrays can be null/undefined from backend
  requiredSkillNames?: string[] | null;
  salaryRange?: string | null;
  expirationDate?: string | null;
}